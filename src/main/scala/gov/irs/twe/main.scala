package gov.irs.twe

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.exceptions.InvalidFormConfig
import gov.irs.twe.generators.Website
import gov.irs.twe.parser.Flow
import java.io.File
import scala.io.Source
import scala.util.matching.Regex
import scala.util.Try
import scala.xml.Elem
import scala.xml.NodeBuffer
import smol.{ Config, Smol }

val FlowResourceRoot = "twe/flow"
val flagRegex = new Regex("""--(\w*)""")

case class OptionContent(name: String, description: Option[String])
case class FgSetContent(question: String, options: Option[Map[String, OptionContent]])
case class FgAlertContent(heading: String, body: Map[String, String])

def main(args: Array[String]): Unit = {
  val flags = Map.from(
    args.map(flag =>
      flag match
        case flagRegex(name) => (name, true)
        case _               =>
          throw new Error(s"Unable to recognize parameter: $flag"),
    ),
  )

  // Processing for handling multiple xml files for facts
  val dictionaryConfig = FileLoaderHelper.getAllFacts()

  // Get flow root
  val flowFile = Source.fromResource(s"$FlowResourceRoot/index.xml").getLines().mkString("\n")
  val flowConfig = xml.XML.loadString(flowFile)
  val children = flowConfig \\ "FlowConfig" \ "_"

  // Resolve modules
  val resolvedChildren = children.map(child =>
    child.label match {
      case "module" => resolveModule(child)
      case _        => child
    },
  )

  val resolvedConfig = <FlowConfig>{resolvedChildren}</FlowConfig>
  generateFlowLocalFile(resolvedConfig)

  val factDictionary = FactDictionary.fromXml(dictionaryConfig)
  val flow = Flow.fromXmlConfig(resolvedConfig, factDictionary)
  val site = Website.generate(flow, dictionaryConfig, flags)

  // Delete out/ directory and add files to it
  val outDir = os.pwd / "out"
  site.save(outDir)

  if !flags.contains("serve") then return // Only start smol if 'serve' flag is set

  // Marshall smol config vars
  object SmolConfig {
    val Host = "localhost"
    val Port = 3000
    val OutputDir = "out"
    val LogEnabled = true
  }

  // Start server in-process, but do not block.
  // If it’s already running from a previous ~run cycle, starting again will throw BindException - ignore and continue.
  try
    val server = Smol.start(
      Config(
        dir = outDir.toString(),
        host = SmolConfig.Host,
        port = sys.props.get("smol.port").flatMap(s => Try(s.toInt).toOption).getOrElse(SmolConfig.Port),
        logEnabled = SmolConfig.LogEnabled,
      ),
    )
    sys.addShutdownHook(server.stop(0))
    println(
      s"[smol] started at http://${SmolConfig.Host}:${sys.props.get("smol.port").getOrElse(SmolConfig.Port.toString)} => ${outDir.toString}",
    )
  catch
    case _: java.net.BindException =>
      println(
        s"[smol] already serving on ${SmolConfig.Host}:${sys.props.get("smol.port").getOrElse(SmolConfig.Port.toString)} — leaving it running",
      )
}

object FileLoaderHelper:
  def getAllFacts(): Elem = {
    val factDirectoryPath = os.pwd / "src" / "main" / "resources" / "twe" / "facts"
    val factsDirectory = new File(factDirectoryPath.toString)
    val listOfFiles = if (factsDirectory.exists && factsDirectory.isDirectory) {
      factsDirectory.listFiles.filter(_.isFile).filter(_.getName.endsWith(".xml")).toList
    } else {
      List.empty[File]
    }

    val facts = new NodeBuffer()
    for (file <- listOfFiles) {
      val fileName = file.getName()
      val factsFile = Source.fromResource(s"twe/facts/$fileName").getLines().mkString("\n")
      val factDictionary = xml.XML.loadString(factsFile)
      val factNodes = factDictionary \ "Facts" \ "_"
      facts ++= factNodes
    }
    <FactDictionaryModule><Facts>{facts}</Facts></FactDictionaryModule>
  }

def resolveModule(node: xml.Node): xml.NodeSeq = {
  val src = node \@ "src"
  // Remove the ./ prefix in the src attribute
  // We support this so that people can use local file path resolution in their text editors
  val resolvedSrc = src.replaceAll("^\\./", "")

  val moduleFile = Source.fromResource(s"$FlowResourceRoot/$resolvedSrc").getLines().mkString("\n")

  val flowConfigModule = xml.XML.loadString(moduleFile)
  if (flowConfigModule.label != "FlowConfigModule") {
    throw InvalidFormConfig(s"Module file $src does not have a top-level FlowConfigModule")
  }

  flowConfigModule \ "_"
}

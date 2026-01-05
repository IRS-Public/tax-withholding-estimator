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
import smol.*

val FlowResourceRoot = "twe/flow"
val flagRegex = new Regex("""--(\w*)""")

case class OptionContent(name: String, description: Option[String])
case class FgSetContent(question: String, options: Option[Map[String, OptionContent]])
case class FgAlertContent(heading: String, body: Map[String, String])

@main def main(args: String*): Unit = {
  val flags = Map.from(
    args.map {
      case flagRegex(name) => (name, true)
      case flag            =>
        throw new Error(s"Unable to recognize parameter: $flag")
    },
  )

  // Processing for handling multiple xml files for facts

  // Get flow root
  val flowFile = Source.fromResource(s"$FlowResourceRoot/index.xml").getLines().mkString("\n")
  val flowConfig = xml.XML.loadString(flowFile)
  val children = flowConfig \\ "FlowConfig" \ "_"

  // Resolve modules
  // Note that modules can only appear in the top level
  val resolvedChildren = children.map(child =>
    child.label match {
      case "module" => resolveModule(child)
      case _        => child
    },
  )

  val resolvedConfig = <FlowConfig>{resolvedChildren}</FlowConfig>
  generateFlowLocalFile(resolvedConfig)

  val tweFactDictionary = loadTweFactDictionary()
  val flow = Flow.fromXmlConfig(resolvedConfig, tweFactDictionary.factDictionary)
  val site = Website.generate(flow, tweFactDictionary.xml, flags)

  // Delete out/ directory and add files to it
  val outDir = os.pwd / "out"

  // We save everything to a /twe subdirectory so that the CDN team can set up consistent rules
  // for our application at the /twe subroute.
  site.save(outDir / "twe")

  if !flags.contains("serve") then return // Only start smol if 'serve' flag is set

  val host = "localhost"
  val port = sys.props
    .get("smol.port")
    .flatMap(s => Try(s.toInt).toOption)
    .getOrElse(3000)
  val config = smol.Config(outDir.toString(), host, port, logEnabled = true)

  // Start server in-process, but do not block.
  // If it’s already running from a previous ~run cycle, starting again will throw BindException - ignore and continue.
  try
    val server = smol.Smol.start(config)
    sys.addShutdownHook(server.stop(0))
    val url = s"http://${host}:${port}/twe"
    val green = "\u001b[32m"
    val cyan = "\u001b[36m"
    val bold = "\u001b[1m"
    val reset = "\u001b[0m"
    println(s"\n${green}${bold}✓${reset} ${bold}TWE Server${reset} ${cyan}ready${reset}")
    println(s"  ${bold}Local:${reset}   ${cyan}${url}${reset}\n")
  catch
    case _: java.net.BindException =>
      val url = s"http://${host}:${port}/twe"
      val yellow = "\u001b[33m"
      val cyan = "\u001b[36m"
      val bold = "\u001b[1m"
      val reset = "\u001b[0m"
      println(s"\n${yellow}${bold}⚠${reset} ${bold}Server${reset} ${yellow}already running${reset}")
      println(s"  ${bold}Local:${reset}   ${cyan}${url}${reset}\n")
}

def resolveModule(node: xml.Node): xml.NodeSeq = {
  val src = node \@ "src"
  // Remove the ./ prefix in the src attribute
  // We support this so that people can use local file path resolution in their text editors
  val resolvedSrc = src.replaceAll("^\\./", "")
  val moduleFile = Source.fromResource(s"$FlowResourceRoot/$resolvedSrc").getLines().mkString("\n")

  val flowConfigModule = xml.XML.loadString(moduleFile)
  if (flowConfigModule.label != "FlowConfig") {
    throw InvalidFormConfig(s"Module file $src does not have a top-level FlowConfig")
  }

  flowConfigModule \ "_"
}

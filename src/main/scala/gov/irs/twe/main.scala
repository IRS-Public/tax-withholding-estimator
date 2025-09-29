package gov.irs.twe

import gov.irs.twe.exceptions.InvalidFormConfig
import gov.irs.twe.generators.Website
import gov.irs.twe.Locale
import io.circe.yaml
import java.io.File
import scala.io.Source
import scala.xml.Elem
import scala.xml.NodeBuffer

val FlowResourceRoot = "twe/flow"

def main(args: Array[String]): Unit = {

  // Processing for handling multiple xml files for facts
  val allFacts = FileLoaderHelper.getAllFacts()

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

  val site = Website.fromXmlConfig(resolvedConfig, allFacts)

  // Delete out/ directory and add files to it
  val outDir = os.pwd / "out"
  site.save(outDir)
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

  def getLocaleContent(languageCode: String) = {
    val localeFile = Source.fromResource(s"twe/locales/${languageCode}.yaml")

    yaml.scalayaml.Parser.parse(localeFile.reader()) match {
      case Left(value) =>
        // Failing to load the content is an irrecoverable error
        throw new Exception(s"Failed to load the content for ${languageCode}", value)
      case Right(value) =>
        value
    }
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

package gov.irs.twe

import gov.irs.twe.generators.Website
import java.io.File
import scala.io.Source
import scala.xml.NodeBuffer

def main(args: Array[String]): Unit = {
  // Processing for handling multiple xml files for facts
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
  val allFacts = <FactDictionaryModule><Facts>{facts}</Facts></FactDictionaryModule>

  val flowFile = Source.fromResource("twe/flow.xml").getLines().mkString("\n")
  val flow = xml.XML.loadString(flowFile)
  val site = Website.fromXmlConfig(flow, allFacts)

  // Delete out/ directory and add files to it
  val outDir = os.pwd / "out"
  site.save(outDir)
}

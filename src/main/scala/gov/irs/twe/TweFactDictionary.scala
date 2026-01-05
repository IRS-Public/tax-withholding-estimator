package gov.irs.twe

import gov.irs.factgraph.FactDictionary
import java.io.File
import scala.io.Source
import scala.xml.{ Elem, NodeBuffer }

case class TweFactDictionary(factDictionary: FactDictionary, xml: Elem)

def loadFactXml(): Elem = {
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
    val factXmlNodes = xml.XML.loadString(factsFile)
    val factNodes = factXmlNodes \ "Facts" \ "_"
    facts ++= factNodes
  }

  <FactDictionaryModule>
    <Facts>
      {facts}
    </Facts>
  </FactDictionaryModule>
}

def loadTweFactDictionary(): TweFactDictionary = {
  val factXml = loadFactXml()
  val factDictionary = FactDictionary.fromXml(factXml)
  TweFactDictionary(factDictionary, factXml)
}

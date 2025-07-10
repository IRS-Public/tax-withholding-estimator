package gov.irs.twe

import gov.irs.twe.generators.Website

import scala.io.Source

def main(args: Array[String]): Unit = {

  val flowFile = Source.fromResource("twe/flow.xml").getLines().mkString("\n")
  val factsFile = Source.fromResource("twe/facts.xml").getLines().mkString("\n")

  val flow = xml.XML.loadString(flowFile)
  val facts = xml.XML.loadString(factsFile)

  val site = Website.fromXmlConfig(flow, facts)

  // Delete out/ directory and add files to it
  val outDir = os.pwd / "out"
  site.save(outDir)
}

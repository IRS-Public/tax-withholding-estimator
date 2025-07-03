package gov.irs.formflow

import gov.irs.formflow.generators.Website

import scala.sys.exit

val TEST_XML_FP = "./flows/about-you-basic.xml"

def main(args: Array[String]): Unit = {

  if (args.length != 1) {
    System.err.println("Provide a single argument, the path of the config file")
    exit(1)
  }

  val fileName = args.head
  val config = scala.xml.XML.loadFile(fileName)
  val site = Website.fromXmlConfig(config)

  // Print out the first page
  println(site.pages.head.content)
}

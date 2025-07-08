package gov.irs.formflow

import gov.irs.formflow.generators.Website
import org.jsoup.Jsoup

import scala.sys.exit

val TEST_XML_FP = "./flows/about-you-basic.xml"

def main(args: Array[String]): Unit = {

  if (args.length != 1) {
    System.err.println("Provide a single argument, the path of the config file")
    exit(1)
  }

  val fileName = args.head
  System.err.println(s"Loading flow config $fileName")

  // Load config and validate it
  val config = scala.xml.XML.loadFile(fileName)
  val site = Website.fromXmlConfig(config)

  // Delete out/ directory and add files to it
  val outDir = os.pwd / "out"
  site.save(outDir)
}

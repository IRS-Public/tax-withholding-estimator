package gov.irs.formflow

import gov.irs.formflow.generators.Website
import org.jsoup.Jsoup

import scala.sys.exit
import gov.irs.factgraph.FactDictionary

def main(args: Array[String]): Unit = {

  if (args.length != 2) {
    System.err.println("Usage: sbt run [flowPath] [FactDictionaryPath]")
    exit(1)
  }

  val flowFileName = args(0)
  System.err.println(s"Loading flow config $flowFileName")
  val config = xml.XML.loadFile(flowFileName)

  val dictionaryFileName = args(1)
  System.err.println(s"Loading dictionary config $dictionaryFileName")
  val dictionaryXml = xml.XML.loadFile(dictionaryFileName)
  val site = Website.fromXmlConfig(config, dictionaryXml)

  // Delete out/ directory and add files to it
  val outDir = os.pwd / "out"
  site.save(outDir)
}

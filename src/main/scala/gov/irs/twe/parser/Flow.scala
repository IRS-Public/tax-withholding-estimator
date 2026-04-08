package gov.irs.twe.parser

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.exceptions.InvalidFormConfig
import gov.irs.twe.generateFlowLocaleFile
import gov.irs.twe.Log
import scala.xml.Elem

case class Flow(
    pages: List[Page],
)

object Flow {
  def fromXmlConfig(flowConfig: Elem, factDictionary: FactDictionary): Flow = {
    if (flowConfig.label != "FlowConfig") {
      throw InvalidFormConfig(s"Expected a top-level <FlowConfig>, found ${flowConfig.label}")
    }

    val flowParser = FlowParser(factDictionary)
    val rootContext = TranslationContext()

    // FlowConfig is expected to have only `page` child elements relevant to parsing
    val pages = (flowConfig \ "page").collect { case pageElement: Elem =>
      Page.fromXml(pageElement, flowParser, rootContext)
    }.toList
    Log.info(s"Generated flow with ${pages.length} pages")

    generateFlowLocaleFile(rootContext.translationMap)

    Flow(pages)
  }
}

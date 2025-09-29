package gov.irs.twe.parser

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.exceptions.InvalidFormConfig
import gov.irs.twe.Log
import gov.irs.twe.TweTemplateEngine

case class Flow(pages: List[Page])

object Flow {
  def fromXmlConfig(config: xml.Elem, factDictionary: FactDictionary, templateEngine: TweTemplateEngine): Flow = {
    if (config.label != "FlowConfig") {
      throw InvalidFormConfig(s"Expected a top-level <FlowConfig>, found ${config.label}")
    }

    val pages = (config \ "page").map(page => Page.parse(page, factDictionary, templateEngine)).toList
    Log.info(s"Generated flow with ${pages.length} pages")

    Flow(pages)
  }
}

package gov.irs.twe.parser

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.Log
import gov.irs.twe.exceptions.InvalidFormConfig

import scala.xml.{Elem, Group, SpecialNode}

case class Flow(pages: List[Page])

object Flow {
  def fromXmlConfig(config: xml.Elem, factDictionary: FactDictionary): Flow = {
    if (config.label != "FormConfig") {
      throw InvalidFormConfig(s"Expected a top-level <FormConfig>, found ${config.label}")
    }

    val pages = (config \ "page").map(page => Page.parse(page, factDictionary)).toList
    Log.info(s"Generated flow with ${pages.length} pages")

    Flow(pages)
  }
}

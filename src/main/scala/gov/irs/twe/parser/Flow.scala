package gov.irs.twe.parser

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.Log
import gov.irs.twe.exceptions.InvalidFormConfig

import scala.xml.{Elem, Group, SpecialNode}

case class Flow(sections: List[Section])

object Flow {
  def fromXmlConfig(config: xml.Elem, factDictionary: FactDictionary): Flow = {
    if (config.label != "FormConfig") {
      throw InvalidFormConfig(s"Expected a top-level <FormConfig>, found ${config.label}")
    }

    val sections = (config \ "section").map(section => Section.parse(section, factDictionary)).toList
    Flow(sections)
  }

}

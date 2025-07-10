package gov.irs.twe.parser

import gov.irs.factgraph.FactDictionary

enum SectionNode {
  case question(question: Question)
  case html(node: xml.Node)
}

case class Section(nodes: List[SectionNode])

object Section {
  def parse(section: xml.Node, factDictionary: FactDictionary): Section = {
    val nodes = (section \ "_").map(node => node.label match {
      case "question" => SectionNode.question(Question.parse(node, factDictionary))
      case _ => SectionNode.html(node)
    }).toList

    Section(nodes)
  }

}
package gov.irs.twe.parser

import gov.irs.factgraph.FactDictionary

enum SectionNode {
  case fgCollection(collection: FgCollection)
  case fgSet(question: FgSet)
  case html(node: xml.Node)
}

case class Section(nodes: List[SectionNode])

object Section {
  def parse(section: xml.Node, factDictionary: FactDictionary): Section = {
    val nodes = (section \ "_").map(node => node.label match {
      case "fg-collection" => SectionNode.fgCollection(FgCollection.parse(node, factDictionary))
      case "fg-set" => SectionNode.fgSet(FgSet.parse(node, factDictionary))
      case _ => SectionNode.html(node)
    }).toList

    Section(nodes)
  }

}

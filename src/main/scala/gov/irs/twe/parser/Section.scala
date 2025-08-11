package gov.irs.twe.parser

import gov.irs.factgraph.FactDictionary

enum SectionNode {
  case fgCollection(fgCollection: FgCollection)
  case fgSet(fgSet: FgSet)
  case rawHTML(node: xml.Node)
}

case class Section(nodes: List[SectionNode]) {
  def html(): xml.Elem = {
    val sectionXml = this.nodes.map {
      case SectionNode.fgCollection(x) => x.html()
      case SectionNode.fgSet(x)        => x.html()
      case SectionNode.rawHTML(x)      => x
    }

    <section>{sectionXml}</section>
  }
}

object Section {
  def parse(section: xml.Node, factDictionary: FactDictionary): Section = {
    val nodes = (section \ "_")
      .map(node =>
        node.label match {
          case "fg-collection" => SectionNode.fgCollection(FgCollection.parse(node, factDictionary))
          case "fg-set"        => SectionNode.fgSet(FgSet.parse(node, factDictionary))
          case _               => SectionNode.rawHTML(node)
        },
      )
      .toList

    Section(nodes)
  }

}

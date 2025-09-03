package gov.irs.twe.parser

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.TweTemplateEngine

enum SectionNode {
  case fgCollection(fgCollection: FgCollection)
  case fgSet(fgSet: FgSet)
  case rawHTML(node: xml.Node)
}

case class Section(nodes: List[SectionNode]) {
  def html(templateEngine: TweTemplateEngine): String = {
    val sectionHtml = this.nodes
      .map {
        case SectionNode.fgCollection(x) => x.html(templateEngine)
        case SectionNode.fgSet(x)        => x.html(templateEngine)
        case SectionNode.rawHTML(x)      => x
      }
      .mkString("\n")

    "<section>" + sectionHtml + "</section>"
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

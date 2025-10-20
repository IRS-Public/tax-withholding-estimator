package gov.irs.twe.parser

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.TweTemplateEngine

enum SectionNode {
  case fgCollection(fgCollection: FgCollection)
  case fgSet(fgSet: FgSet)
  case fgAlert(fgSet: FgAlert)
  case fgSectionGate(fgSectionGate: FgSectionGate)
  case rawHTML(node: xml.Node)
}

case class Section(nodes: List[SectionNode], factDictionary: FactDictionary) {
  def html(templateEngine: TweTemplateEngine): String = {
    val sectionHtml = this.nodes
      .map {
        case SectionNode.fgCollection(x)  => x.html(templateEngine)
        case SectionNode.fgSet(x)         => x.html(templateEngine)
        case SectionNode.fgAlert(x)       => x.html(templateEngine)
        case SectionNode.fgSectionGate(x) => x.html(templateEngine)
        case SectionNode.rawHTML(x)       => renderNode(x, templateEngine)
      }
      .mkString("\n")

    "<section class=\"flow\">" + sectionHtml + "</section>"
  }

  private def renderNode(node: xml.Node, templateEngine: TweTemplateEngine): String =
    // Check if this node or its descendants contain special elements
    if (containsSpecialElements(node)) {
      // Process children and reconstruct the node
      val processedChildren = (node \ "_").map { child =>
        child.label match {
          case "fg-collection"   => SectionNode.fgCollection(FgCollection.parse(child, factDictionary))
          case "fg-set"          => SectionNode.fgSet(FgSet.parse(child, factDictionary))
          case "fg-section-gate" => SectionNode.fgSectionGate(FgSectionGate.parse(child))
          case _                 => SectionNode.rawHTML(child)
        }
      }

      val childrenHtml = processedChildren.map {
        case SectionNode.fgCollection(x)  => x.html(templateEngine)
        case SectionNode.fgSet(x)         => x.html(templateEngine)
        case SectionNode.fgSectionGate(x) => x.html(templateEngine)
        case SectionNode.rawHTML(x)       => renderNode(x, templateEngine)
      }.mkString

      // Reconstruct the node with processed children
      val attributes = node.attributes.asAttrMap
        .map { case (k, v) => s"""$k="$v"""" }
        .mkString(" ")
      val attrString = if (attributes.nonEmpty) s" $attributes" else ""

      s"<${node.label}$attrString>$childrenHtml</${node.label}>"
    } else {
      // No special elements, return as-is
      node.toString
    }

  private def containsSpecialElements(node: xml.Node): Boolean =
    (node \\ "_").exists(n => n.label == "fg-collection" || n.label == "fg-set" || n.label == "fg-section-gate")
}

object Section {
  def parse(section: xml.Node, pageRoute: String, factDictionary: FactDictionary): Section = {
    val nodes = (section \ "_")
      .map(node => processNode(node, pageRoute, factDictionary))
      .toList

    Section(nodes, factDictionary)
  }

  private def processNode(node: xml.Node, pageRoute: String, factDictionary: FactDictionary): SectionNode =
    node.label match {
      case "fg-collection"   => SectionNode.fgCollection(FgCollection.parse(node, factDictionary))
      case "fg-set"          => SectionNode.fgSet(FgSet.parse(node, factDictionary))
      case "fg-alert"        => SectionNode.fgAlert(FgAlert.parse(node, pageRoute, factDictionary))
      case "fg-section-gate" => SectionNode.fgSectionGate(FgSectionGate.parse(node))
      case _                 => SectionNode.rawHTML(node)
    }
}

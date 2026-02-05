package gov.irs.twe.parser

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.exceptions.InvalidFormConfig
import gov.irs.twe.parser.Utils.validateFact
import gov.irs.twe.TweTemplateEngine
import org.thymeleaf.context.Context

enum FgCollectionNode {
  case fgSet(fact: FgSet)
  case fgSectionGate(fgSectionGate: FgSectionGate)

  /** A div whose element children are parsed (fg-set, fg-section-gate, fg-detail, rawHTML). */
  case divWithContent(divNode: xml.Node, children: List[SectionNode])
  case rawHTML(node: xml.Node)
}

case class FgCollection(
    path: String,
    itemName: String,
    disallowEmpty: String,
    condition: Option[Condition],
    nodes: List[FgCollectionNode],
    factDictionary: FactDictionary,
    pageRoute: String,
) {
  def html(templateEngine: TweTemplateEngine): String = {
    val collectionFacts = this.nodes
      .map {
        case FgCollectionNode.fgSet(x)                          => x.html(templateEngine)
        case FgCollectionNode.fgSectionGate(x)                  => x.html(templateEngine)
        case FgCollectionNode.divWithContent(divNode, children) =>
          val childrenHtml = FgCollection.renderSectionNodes(children, templateEngine, "\n")
          val attrs = divNode.attributes.asAttrMap.map { case (k, v) => s"""$k="$v"""" }.mkString(" ")
          val attrStr = if (attrs.nonEmpty) s" $attrs" else ""
          s"<div$attrStr>$childrenHtml</div>"
        case FgCollectionNode.rawHTML(x) => x
      }
      .mkString("\n")

    val context = new Context()
    context.setVariable("path", path)
    context.setVariable("itemName", itemName)
    context.setVariable("disallowEmpty", disallowEmpty)
    context.setVariable("collectionFacts", collectionFacts)
    context.setVariable("condition", this.condition.map(_.path).orNull)
    context.setVariable("operator", this.condition.map(_.operator.toString).orNull)

    templateEngine.process("nodes/fg-collection", context)
  }
}

object FgCollection {
  def parse(node: xml.Node, pageRoute: String, factDictionary: FactDictionary): FgCollection = {
    val path = node \@ "path"
    val itemName = node \@ "item-name"
    val disallowEmpty = node \@ "disallow-empty"
    val condition = Condition.getCondition(node, factDictionary)

    validateFgCollection(path, factDictionary)

    val nodes = (node \ "_")
      .map(node =>
        node.label match {
          case "fg-set"          => FgCollectionNode.fgSet(FgSet.parse(node, factDictionary))
          case "fg-section-gate" => FgCollectionNode.fgSectionGate(FgSectionGate.parse(node))
          case "div"             =>
            val children = (node \ "_").map(c => Section.processNode(c, pageRoute, factDictionary)).toList
            FgCollectionNode.divWithContent(node, children)
          case _ => FgCollectionNode.rawHTML(node)
        },
      )
      .toList

    FgCollection(path, itemName, disallowEmpty, condition, nodes, factDictionary, pageRoute)
  }

  private def validateFgCollection(path: String, factDictionary: FactDictionary): Unit = {
    validateFact(path, factDictionary)
    if (factDictionary.getDefinition(path).typeNode != "CollectionNode")
      throw InvalidFormConfig(s"Path $path must be of type CollectionNode")
  }

  def renderSectionNodes(nodes: List[SectionNode], templateEngine: TweTemplateEngine, separator: String = ""): String =
    nodes
      .map {
        case SectionNode.fgCollection(x)             => x.html(templateEngine)
        case SectionNode.fgSet(x)                    => x.html(templateEngine)
        case SectionNode.fgAlert(x)                  => x.html(templateEngine)
        case SectionNode.fgSectionGate(x)            => x.html(templateEngine)
        case SectionNode.fgDetail(x)                 => x.html(templateEngine)
        case SectionNode.fgWithholdingAdjustments(x) => x.html(templateEngine)
        case SectionNode.rawHTML(x)                  => x.toString
      }
      .mkString(separator)
}

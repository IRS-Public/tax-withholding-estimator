package gov.irs.twe.parser

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.exceptions.InvalidFormConfig
import gov.irs.twe.parser.Utils.validateFact
import gov.irs.twe.TweTemplateEngine
import org.thymeleaf.context.Context

enum FgCollectionNode {
  case fgSet(fact: FgSet)
  case rawHTML(node: xml.Node)
}

case class FgCollection(
    path: String,
    itemName: String,
    disallowEmpty: String,
    condition: Option[Condition],
    nodes: List[FgCollectionNode],
) {
  def html(templateEngine: TweTemplateEngine): String = {
    val collectionFacts = this.nodes
      .map {
        case FgCollectionNode.fgSet(x)   => x.html(templateEngine)
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
  def parse(node: xml.Node, factDictionary: FactDictionary): FgCollection = {
    val path = node \@ "path"
    val itemName = node \@ "item-name"
    val disallowEmpty = node \@ "disallow-empty"
    val condition = Condition.getCondition(node, factDictionary)

    validateFgCollection(path, factDictionary)

    val nodes = (node \ "_")
      .map(node =>
        node.label match {
          case "fg-set" => FgCollectionNode.fgSet(FgSet.parse(node, factDictionary))
          case _        => FgCollectionNode.rawHTML(node)
        },
      )
      .toList

    FgCollection(path, itemName, disallowEmpty, condition, nodes)
  }

  private def validateFgCollection(path: String, factDictionary: FactDictionary): Unit = {
    validateFact(path, factDictionary)
    if (factDictionary.getDefinition(path).typeNode != "CollectionNode")
      throw InvalidFormConfig(s"Path $path must be of type CollectionNode")
  }
}

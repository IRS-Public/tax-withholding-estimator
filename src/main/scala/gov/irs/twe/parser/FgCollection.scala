package gov.irs.twe.parser

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.exceptions.InvalidFormConfig
import gov.irs.twe.parser.Utils.validateFact

enum FgCollectionNode {
  case fgSet(fact: FgSet)
  case html(node: xml.Node)
}

case class FgCollection(path: String, condition: Option[Condition], nodes: List[FgCollectionNode])

object FgCollection {
  def parse(node: xml.Node, factDictionary: FactDictionary): FgCollection = {
    val path = node \@ "path"
    val condition = Condition.getCondition(node, factDictionary)

    validateFgCollection(path, factDictionary)

    val nodes = (node \ "_").map(node => node.label match {
      case "fg-set" => FgCollectionNode.fgSet(FgSet.parse(node, factDictionary))
      case _ => FgCollectionNode.html(node)
    }).toList

    FgCollection(path, condition, nodes)
  }

  private def validateFgCollection(path: String, factDictionary: FactDictionary): Unit = {
    validateFact(path, factDictionary)
    if (factDictionary.getDefinition(path).typeNode != "CollectionNode") throw InvalidFormConfig(s"Path $path must be of type CollectionNode")
  }
}

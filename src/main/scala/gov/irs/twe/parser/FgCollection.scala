package gov.irs.twe.parser

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.exceptions.InvalidFormConfig

enum FgCollectionNode {
  case fgSet(fact: FgSet)
  case html(node: xml.Node)
}

case class FgCollection(path: String, condition: Option[Condition], nodes: List[FgCollectionNode])

object FgCollection {
  def parse(node: xml.Node, factDictionary: FactDictionary): FgCollection = {
    val path = node \@ "path"
    val condition = Condition.getCondition(node, factDictionary)

    // Validate that the fact exists
    val factDefinition = factDictionary.getDefinition(path)
    if (factDefinition == null) {
      throw InvalidFormConfig(s"Path $path not found in the fact dictionary")
    }

    val nodes = (node \ "_").map(node => node.label match {
      case "fg-set" => FgCollectionNode.fgSet(FgSet.parse(node, factDictionary))
      case _ => FgCollectionNode.html(node)
    }).toList

    FgCollection(path, condition, nodes)
  }
}

package gov.irs.twe.parser

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.exceptions.InvalidFormConfig

enum FgCollectionNode {
  case fgSet(fact: FgSet)
  case html(node: xml.Node)
}

case class FgCollection(path: String, nodes: List[FgCollectionNode])

object FgCollection {
  def parse(collectionNode: xml.Node, factDictionary: FactDictionary): FgCollection = {
    val path = collectionNode \@ "path"

    // Validate that the fact exists
    val factDefinition = factDictionary.getDefinition(path)
    if (factDefinition == null) {
      throw InvalidFormConfig(s"Path $path not found in the fact dictionary")
    }

    val nodes = (collectionNode \ "_").map(node => node.label match {
      case "fg-set" => FgCollectionNode.fgSet(FgSet.parse(node, factDictionary))
      case _ => FgCollectionNode.html(node)
    }).toList

    FgCollection(path, nodes)
  }
}

package gov.irs.twe.parser

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.exceptions.InvalidFormConfig

enum FgSetNode {
  case input(input: Input)
  case html(node: xml.Node)
}

case class FgSet(path: String, condition: Option[Condition], input: Input, nodes: List[FgSetNode])

object FgSet {
  def parse(node: xml.Node, factDictionary: FactDictionary): FgSet = {
    val path = node \@ "path"
    val condition = Condition.getCondition(node, factDictionary)

    // Validate that the fact exists
    val factDefinition = factDictionary.getDefinition(path)
    if (factDefinition == null) {
      throw InvalidFormConfig(s"Path $path not found in the fact dictionary")
    }

    val input = Input.extractFromQuestion(node, factDictionary)
    val nodes = (node \ "_").map(node => node.label match {
      case "input" | "select" => FgSetNode.input(input)
      case _ => FgSetNode.html(node)
    }).toList

    FgSet(path, condition, input, nodes)
  }

}

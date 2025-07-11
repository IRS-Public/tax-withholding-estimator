package gov.irs.twe.parser

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.exceptions.InvalidFormConfig

enum FgSetNode {
  case input(input: Input)
  case html(node: xml.Node)
}

case class FgSet(path: String, input: Input, nodes: List[FgSetNode]) {
}

object FgSet {
  def parse(question: xml.Node, factDictionary: FactDictionary): FgSet = {
    val path = question \@ "path"

    // Validate that the fact exists
    val factDefinition = factDictionary.getDefinition(path)
    if (factDefinition == null) {
      throw InvalidFormConfig(s"Path $path not found in the fact dictionary")
    }
    // TODO validate the fact matches the input

    val input = Input.extractFromQuestion(question, factDictionary)
    val nodes = (question \ "_").map(node => node.label match {
      case "input" | "select" => FgSetNode.input(Input.extractFromQuestion(question, factDictionary))
      case _ => FgSetNode.html(node)
    }).toList

    FgSet(path, input, nodes)
  }
}
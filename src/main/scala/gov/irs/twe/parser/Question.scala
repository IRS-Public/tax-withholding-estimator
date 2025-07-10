package gov.irs.twe.parser

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.exceptions.InvalidFormConfig

enum QuestionNode {
  case input(input: Input)
  case html(node: xml.Node)
}

case class Question(path: String, input: Input, nodes: List[QuestionNode]) {
}

object Question {
  def parse(question: xml.Node, factDictionary: FactDictionary): Question = {
    val path = question \@ "path"

    // Validate that the fact exists
    val factDefinition = factDictionary.getDefinition(path)
    if (factDefinition == null) {
      throw InvalidFormConfig(s"Path $path not found in the fact dictionary")
    }
    // TODO validate the fact matches the input

    val input = Input.extractFromQuestion(question, factDictionary)
    val nodes = (question \ "_").map(node => node.label match {
      case "input" | "select" => QuestionNode.input(Input.extractFromQuestion(question, factDictionary))
      case _ => QuestionNode.html(node)
    }).toList
    println(nodes)

    Question(path, input, nodes)
  }
}

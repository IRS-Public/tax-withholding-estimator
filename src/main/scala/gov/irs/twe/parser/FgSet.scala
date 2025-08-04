package gov.irs.twe.parser

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.exceptions.InvalidFormConfig
import gov.irs.twe.parser.Utils.validateFact
import scala.annotation.switch

enum FgSetNode {
  case input(input: Input)
  case html(node: xml.Node)
}

case class FgSet(path: String, condition: Option[Condition], input: Input, nodes: List[FgSetNode])

object FgSet {
  def parse(node: xml.Node, factDictionary: FactDictionary): FgSet = {
    val path = node \@ "path"
    val condition = Condition.getCondition(node, factDictionary)
    val input = Input.extractFromQuestion(node, factDictionary)

    validateFgSet(path, input, factDictionary)
    val nodes = (node \ "_").map(node => node.label match {
      case "input" | "select" => FgSetNode.input(input)
      case _ => FgSetNode.html(node)
    }).toList

    FgSet(path, condition, input, nodes)
  }

  private def validateFgSet(path: String,  input: Input, factDictionary: FactDictionary): Unit = {
    validateFact(path, factDictionary)
    val typeNode = factDictionary.getDefinition(path).typeNode
    val inputAndNodeTypeMismatch = input match {
      case Input.text => typeNode != "StringNode"
      case Input.boolean => typeNode != "BooleanNode"
      case Input.dollar => typeNode != "DollarNode"
      case Input.date => typeNode != "DayNode"
      // We could make this more strict
      case Input.select(_, _) => typeNode != "EnumNode"
    }
    if (inputAndNodeTypeMismatch) throw InvalidFormConfig(s"Path $path must be of type $input")
  }
}

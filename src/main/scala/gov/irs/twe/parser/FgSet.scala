package gov.irs.twe.parser

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.exceptions.InvalidFormConfig
import gov.irs.twe.parser.Utils.validateFact

enum FgSetNode {
  case input(input: Input)
  case question(question: String)
  case hint(hint: String)
  case rawHTML(node: xml.Node)
}

case class FgSet(path: String, condition: Option[Condition], input: Input, nodes: List[FgSetNode]) {
  def html(): xml.Elem = {

    val usesFieldset = input.typeString == "boolean" || input.typeString == "date"
    val questionXml = this.nodes.map {
      case FgSetNode.input(input)       => input.html(path)
      case FgSetNode.question(question) =>
        if (usesFieldset) "" else <label class="usa-label twe-question" for={path}>{question}</label>
      case FgSetNode.hint(hint) =>
        if (usesFieldset) "" else <div class="usa-hint" id={s"${path}-hint"}>{hint}</div>
      case FgSetNode.rawHTML(x) => x
    }

    val condition = this.condition.map(_.path).orNull
    val operator = this.condition.map(_.operator.toString).orNull

    <fg-set
      path={this.path}
      inputType={this.input.typeString}
      condition={condition}
      operator={operator}>
      {questionXml}
    </fg-set>
  }
}

object FgSet {
  def parse(node: xml.Node, factDictionary: FactDictionary): FgSet = {
    val path = node \@ "path"
    val condition = Condition.getCondition(node, factDictionary)
    val input = Input.extractFromFgSet(node, factDictionary)

    validateFgSet(path, input, factDictionary)
    val nodes = (node \ "_")
      .map(node =>
        node.label match {
          case "input" | "select" => FgSetNode.input(input)
          case "question"         => FgSetNode.question(node.text)
          case "hint"             => FgSetNode.hint(node.text)
          case _                  => FgSetNode.rawHTML(node)
        },
      )
      .toList

    FgSet(path, condition, input, nodes)
  }

  private def validateFgSet(path: String, input: Input, factDictionary: FactDictionary): Unit = {
    validateFact(path, factDictionary)
    val typeNode = factDictionary.getDefinition(path).typeNode
    val inputAndNodeTypeMismatch = input match {
      case Input.text(_)       => typeNode != "StringNode"
      case Input.int(_)        => typeNode != "IntNode"
      case Input.boolean(_, _) => typeNode != "BooleanNode"
      case Input.dollar(_)     => typeNode != "DollarNode"
      case Input.date(_, _)    => typeNode != "DayNode"
      // We could make this more strict
      case Input.select(_, _, _) => typeNode != "EnumNode"
    }
    if (inputAndNodeTypeMismatch) throw InvalidFormConfig(s"Path $path must be of type $input")
  }
}

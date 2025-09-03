package gov.irs.twe.parser

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.exceptions.InvalidFormConfig
import gov.irs.twe.parser.Utils.validateFact
import gov.irs.twe.TweTemplateEngine
import org.thymeleaf.context.Context

enum FgSetNode {
  case input(input: Input)
  case question(question: String)
  case hint(hint: String)
  case rawHTML(node: xml.Node)
}

case class FgSet(path: String, condition: Option[Condition], input: Input, nodes: List[FgSetNode]) {
  def html(templateEngine: TweTemplateEngine): String = {
    val usesFieldset = input.typeString == "boolean" || input.typeString == "date"

    def renderQuestion(question: String): String = {
      val context = new Context()
      context.setVariable("path", this.path)
      context.setVariable("question", question)
      if (!usesFieldset) templateEngine.process("nodes/question-label", context) else ""
    }

    def renderHint(hint: String): String = {
      val context = new Context()
      context.setVariable("path", this.path)
      context.setVariable("hint", hint)
      if (!usesFieldset) templateEngine.process("nodes/hint", context) else ""
    }

    val context = new Context()
    context.setVariable("path", this.path)
    context.setVariable("condition", this.condition.map(_.path).orNull)
    context.setVariable("operator", this.condition.map(_.operator.toString).orNull)
    context.setVariable("typeString", input.typeString)

    val questionHtml = this.nodes
      .map {
        case FgSetNode.input(input)       => input.html(templateEngine, path)
        case FgSetNode.question(question) => renderQuestion(question)
        case FgSetNode.hint(hint)         => renderHint(hint)
        case FgSetNode.rawHTML(x)         => x
      }
      .mkString("\n")

    context.setVariable("questionHtml", questionHtml)
    templateEngine.process("nodes/fg-set", context)
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

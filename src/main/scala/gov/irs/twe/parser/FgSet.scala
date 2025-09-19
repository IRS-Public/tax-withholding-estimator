package gov.irs.twe.parser

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.exceptions.InvalidFormConfig
import gov.irs.twe.parser.Utils.validateFact
import gov.irs.twe.TweTemplateEngine
import org.thymeleaf.context.Context

case class FgSet(
    path: String,
    condition: Option[Condition],
    input: Input,
    question: String,
    hint: String,
    optional: Boolean,
) {
  def html(templateEngine: TweTemplateEngine): String = {
    val usesFieldset = input.typeString == "boolean" || input.typeString == "date"

    val context = new Context()
    context.setVariable("path", this.path)
    context.setVariable("condition", this.condition.map(_.path).orNull)
    context.setVariable("operator", this.condition.map(_.operator.toString).orNull)
    context.setVariable("typeString", input.typeString)
    context.setVariable("optional", optional)
    context.setVariable("usesFieldset", usesFieldset)
    context.setVariable("question", question)
    context.setVariable("hint", hint)

    templateEngine.process("nodes/fg-set", context)
  }
}

object FgSet {
  def parse(node: xml.Node, factDictionary: FactDictionary): FgSet = {
    val path = node \@ "path"
    val isOptional = !(node \@ "optional").isEmpty()
    val condition = Condition.getCondition(node, factDictionary)
    val input = Input.extractFromFgSet(node, isOptional, factDictionary)

    val questionKey = node \ "question" \@ "content-key"
    val hint = (node \ "hint").text

    FgSet(path, condition, input, questionKey, hint, isOptional)
  }

  private def validateFgSet(path: String, input: Input, factDictionary: FactDictionary): Unit = {
    validateFact(path, factDictionary)
    val typeNode = factDictionary.getDefinition(path).typeNode
    val inputAndNodeTypeMismatch = input match {
      case Input.text(_, _)       => typeNode != "StringNode"
      case Input.int(_, _)        => typeNode != "IntNode"
      case Input.boolean(_, _, _) => typeNode != "BooleanNode"
      case Input.dollar(_, _)     => typeNode != "DollarNode"
      case Input.date(_, _, _)    => typeNode != "DayNode"
      // We could make this more strict
      case Input.select(_, _, _, _) => typeNode != "EnumNode"
    }
    if (inputAndNodeTypeMismatch) throw InvalidFormConfig(s"Path $path must be of type $input")
  }

}

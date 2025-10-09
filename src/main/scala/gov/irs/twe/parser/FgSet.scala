package gov.irs.twe.parser

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.exceptions.InvalidFormConfig
import gov.irs.twe.parser.Utils.validateFact
import gov.irs.twe.TweTemplateEngine
import org.thymeleaf.context.Context
import scala.collection.JavaConverters.asJavaIterableConverter

case class ThymeleafOption(name: String, value: String, description: String)
case class FgSet(
    path: String,
    question: String,
    condition: Option[Condition],
    input: Input,
    hint: String,
    optional: Boolean,
) {
  def html(templateEngine: TweTemplateEngine): String = {
    val usesFieldset = input.typeString == "boolean" || input.typeString == "date" || input.typeString == "enum"

    val context = new Context()
    context.setVariable("path", this.path)
    context.setVariable("condition", this.condition.map(_.path).orNull)
    context.setVariable("operator", this.condition.map(_.operator.toString).orNull)
    context.setVariable("typeString", input.typeString)
    context.setVariable("optional", optional)
    context.setVariable("usesFieldset", usesFieldset)
    context.setVariable("hint", hint)

    val contentKey = "fg-sets." + path
    context.setVariable("contentKey", contentKey)

    input match {
      // case select(options: List[HtmlOption], optionsPath: Option[String], hint: String, optional: Boolean = false)
      case Input.select(options, optionsPath, _, _) =>
        context.setVariable("options", options.asJava)
        context.setVariable("optionsPath", optionsPath)
      case Input.enumInput(options, optionsPath, _, _) =>
        val javaOptions = options.map { opt =>
          ThymeleafOption(opt.name, opt.value, opt.description.orNull)
        }
        context.setVariable("options", javaOptions.asJava)
        context.setVariable("optionsPath", optionsPath)
      case _ =>
    }

    templateEngine.process("nodes/fg-set", context)
  }
}

object FgSet {
  def parse(node: xml.Node, factDictionary: FactDictionary): FgSet = {
    val path = node \@ "path"
    val question = (node \ "question").text
    val isOptional = !(node \@ "optional").isEmpty()
    val condition = Condition.getCondition(node, factDictionary)
    val input = Input.extractFromFgSet(node, isOptional, factDictionary)

    val hint = (node \ "hint").text

    FgSet(path, question, condition, input, hint, isOptional)
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
      case Input.select(_, _, _, _)    => typeNode != "EnumNode"
      case Input.enumInput(_, _, _, _) => typeNode != "EnumNode"
    }
    if (inputAndNodeTypeMismatch) throw InvalidFormConfig(s"Path $path must be of type $input")
  }

}

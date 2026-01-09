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
    hint: Option[String],
    optional: Boolean,
    modalLink: Option[String],
) {
  def html(templateEngine: TweTemplateEngine): String = {
    val usesFieldset =
      input.typeString == "boolean" || input.typeString == "date" || input.typeString == "enum" || input.typeString == "multi-enum"

    val context = new Context()
    context.setVariable("path", this.path)
    context.setVariable("condition", this.condition.map(_.path).orNull)
    context.setVariable("operator", this.condition.map(_.operator.toString).orNull)
    context.setVariable("typeString", input.typeString)
    context.setVariable("optional", optional)
    context.setVariable("usesFieldset", usesFieldset)
    context.setVariable("hint", hint.orNull)
    context.setVariable("modalLink", modalLink.orNull)

    val contentKey = "fg-sets." + path
    context.setVariable("contentKey", contentKey)

    input match {
      case Input.select(options, optionsPath, _) =>
        context.setVariable("options", options.asJava)
        context.setVariable("optionsPath", optionsPath)
      case Input.enumInput(options, optionsPath, _) =>
        val javaOptions = options.map { opt =>
          ThymeleafOption(opt.name, opt.value, opt.description.orNull)
        }
        context.setVariable("options", javaOptions.asJava)
        context.setVariable("optionsPath", optionsPath)
      case Input.multiEnumInput(options, optionsPath, _) =>
        val javaOptions = options.map { opt =>
          ThymeleafOption(opt.name, opt.value, opt.description.orNull)
        }
        context.setVariable("options", javaOptions.asJava)
        context.setVariable("optionsPath", optionsPath)
      case Input.boolean(_, options) =>
        if (options.nonEmpty) {
          val trueOption = options.find(_.value == "true")
          val falseOption = options.find(_.value == "false")
          context.setVariable("trueLabel", trueOption.map(_.name).orNull)
          context.setVariable("falseLabel", falseOption.map(_.name).orNull)
        }

      case _ =>
    }

    templateEngine.process("nodes/fg-set", context)
  }
}

object FgSet {
  def parse(node: xml.Node, factDictionary: FactDictionary): FgSet = {
    val path = node \@ "path"
    // Use .child.mkString instead of .text to preserve XML tags (e.g., <span>, <fg-show>) in mixed content
    val question = (node \ "question").head.child.mkString.trim
    if (question.isEmpty) {
      throw InvalidFormConfig(s"fg-set at path: $path has an empty question tag. This is required.")
    }
    val isOptional = !(node \@ "optional").isEmpty()
    val condition = Condition.getCondition(node, factDictionary)
    val input = Input.extractFromFgSet(node, isOptional, factDictionary)

    val hintNode = node \ "hint"
    val hint = if (hintNode.isEmpty) {
      None
    } else {
      Some(hintNode.head.child.mkString.trim)
    }
    val modalLinkNode = node \ "modal-link"
    val modalLink = if (modalLinkNode.isEmpty) {
      None
    } else {
      Some(modalLinkNode.head.toString.trim)
    }

    validateFact(path, factDictionary)
    val typeNode = factDictionary.getDefinition(path).typeNode
    val inputAndNodeTypeMismatch = input match {
      case Input.text(_)       => typeNode != "StringNode"
      case Input.int(_)        => typeNode != "IntNode"
      case Input.boolean(_, _) => typeNode != "BooleanNode"
      case Input.dollar(_)     => typeNode != "DollarNode"
      case Input.date(_)       => typeNode != "DayNode"
      // We could make this more strict
      case Input.select(_, _, _)         => typeNode != "EnumNode"
      case Input.enumInput(_, _, _)      => typeNode != "EnumNode"
      case Input.multiEnumInput(_, _, _) => typeNode != "MultiEnumNode"
    }
    if (inputAndNodeTypeMismatch) throw InvalidFormConfig(s"Path $path must be of type $input")

    FgSet(path, question, condition, input, hint, isOptional, modalLink)
  }
}

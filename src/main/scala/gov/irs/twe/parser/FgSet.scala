package gov.irs.twe.parser

import gov.irs.factgraph.Path
import gov.irs.twe.exceptions.InvalidFormConfig
import gov.irs.twe.parser.Utils.validateFact
import gov.irs.twe.TweTemplateEngine
import org.thymeleaf.context.Context
import scala.jdk.CollectionConverters.IterableHasAsJava
import scala.xml.Elem

case class ThymeleafOption(name: String, value: String, description: String)
case class FgSet(
    path: String,
    condition: Option[Condition],
    input: Input,
    optional: Boolean,
    translationContext: TranslationContext,
) extends FlowNode {
  override def html(templateEngine: TweTemplateEngine): String = {
    val usesFieldset =
      input.typeString == "boolean" || input.typeString == "date" || input.typeString == "enum" || input.typeString == "multi-enum"

    val context = new Context()
    context.setVariable("path", this.path)
    context.setVariable("condition", this.condition.map(_.path).orNull)
    context.setVariable("operator", this.condition.map(_.operator.toString).orNull)
    context.setVariable("typeString", input.typeString)
    context.setVariable("optional", optional)
    context.setVariable("usesFieldset", usesFieldset)
    val contentKey = translationContext.fullKey()
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

case class FgSetOption(
    value: String,
    name: String,
    description: Option[String],
)

object FgSet extends FlowNodeParser {
  override def fromXml(
      fgSetElement: Elem,
      flowParser: FlowParser,
      parentTranslationContext: TranslationContext,
  ): FgSet = {
    val factDictionary = flowParser.factDictionary
    val path = fgSetElement \@ "path"
    if (path.isEmpty) {
      throw InvalidFormConfig("fg-set attribute `path` is required but was missing or empty")
    }
    validateFact(path, factDictionary)

    val factDefinitionNode = factDictionary.getDefinitionsAsNodes()(Path(path))
    val isOptional = (factDefinitionNode \ "Placeholder").nonEmpty

    val input = Input.extractFromFgSet(fgSetElement, isOptional, factDictionary)
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

    // Use .child.mkString instead of .text to preserve XML tags (e.g., <span>, <fg-show>) in mixed content
    val question = (fgSetElement \ "question").head.child.mkString.strip
    if (question.isEmpty) {
      throw InvalidFormConfig(s"fg-set at path: $path has an empty question tag. This is required.")
    }

    val condition = Condition.getCondition(fgSetElement, factDictionary)
    val hintNode = fgSetElement \ "hint"
    val hint = if (hintNode.isEmpty) {
      None
    } else {
      Some(hintNode.head.child.mkString.strip)
    }
    val modalLinkNode = fgSetElement \ "modal-link"
    val modalLink = if (modalLinkNode.isEmpty) {
      None
    } else {
      Some(modalLinkNode.head.toString.strip)
    }

    val options = (fgSetElement \\ "option").map { option =>
      val value = option \@ "value"
      val name = option.head.child.mkString.strip
      val description = option \@ "description-key"
      val descriptionValue = Option(description).filter(_.nonEmpty)
      FgSetOption(value, name, descriptionValue)
    }

    val translationContext = parentTranslationContext.forChildWithId(path)

    translationContext.updateValue("question", question)
    if (hint.nonEmpty) {
      translationContext.updateValue("hint", hint.get)
    }
    if (modalLink.nonEmpty) {
      translationContext.updateValue("modalLink", modalLink.get)
    }

    if (options.nonEmpty) {
      val optionsContext = translationContext.forChildWithId("options")
      options.foreach(option => {
        val specificOptionContext = optionsContext.forChildWithId(option.value)
        specificOptionContext.updateValue("name", option.name)
        if (option.description.isDefined) specificOptionContext.updateValue("description", option.description.get)
      })
    }

    FgSet(path, condition, input, isOptional, translationContext)
  }
}

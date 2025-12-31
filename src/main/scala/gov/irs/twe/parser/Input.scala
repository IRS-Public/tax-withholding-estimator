package gov.irs.twe.parser

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.{ Log, TweTemplateEngine }
import gov.irs.twe.exceptions.InvalidFormConfig
import org.thymeleaf.context.Context
import scala.collection.JavaConverters.asJavaIterableConverter

case class HtmlOption(name: String, value: String, description: Option[String] = None)

enum Input {
  case select(options: List[HtmlOption], optionsPath: Option[String], optional: Boolean = false)
  case text(optional: Boolean = false)
  case int(optional: Boolean = false)
  case boolean(optional: Boolean = false, options: List[HtmlOption] = List.empty)
  case enumInput(options: List[HtmlOption], optionsPath: String, optional: Boolean = false)
  case multiEnumInput(options: List[HtmlOption], optionsPath: String, optional: Boolean = false)
  case dollar(optional: Boolean = false)
  case date(optional: Boolean = false)

  def typeString: String = this match {
    case Input.text(_)                 => "text"
    case Input.int(_)                  => "int"
    case Input.boolean(_, _)           => "boolean"
    case Input.enumInput(_, _, _)      => "enum"
    case Input.multiEnumInput(_, _, _) => "multi-enum"
    case Input.dollar(_)               => "dollar"
    case Input.select(_, _, _)         => "select"
    case Input.date(_)                 => "date"
  }
}

object Input {
  def extractFromFgSet(node: xml.Node, isOptional: Boolean, factDictionary: FactDictionary): Input = {
    val path = node \@ "path"

    // Handle the <select> as a special case
    val selectNode = node \ "select"
    if (selectNode.nonEmpty) {
      val optionsPath = Option(selectNode \@ "options-path").filter(_.nonEmpty)
      val options = (selectNode \ "option").map { node =>
        val name = node.text
        var value = node \@ "value"
        if (value == "") value = name

        HtmlOption(name, value)
      }.toList
      // TODO validate that the options match the num path

      if (options.isEmpty) {
        Log.warn(s"Empty options for fg-set: $path")
      }
      return Input.select(options, optionsPath, isOptional)
    }

    // Otherwise parse the <input>
    val inputNode = node \ "input"
    if (inputNode.isEmpty) {
      throw InvalidFormConfig(s"Missing an input for question $path")
    }

    inputNode \@ "type" match {
      case "text"    => Input.text(isOptional)
      case "int"     => Input.int(isOptional)
      case "boolean" =>
        val options = (inputNode \ "option").map { node =>
          val name = node.mkString.trim
          val value = node \@ "value"

          if (value != "true" && value != "false") {
            throw InvalidFormConfig(s"Boolean option must have value 'true' or 'false', got '$value' at path $path")
          }
          HtmlOption(name, value)
        }.toList

        Input.boolean(isOptional, options)
      case "enum" =>
        val optionsPath = inputNode \@ "optionsPath"
        val options = (inputNode \ "option").map { node =>
          val name = node.text
          val value = node \@ "value"
          val finalValue = if (value.isEmpty) name else value
          val descriptionKey = node \@ "description-key"
          val description = if (descriptionKey.nonEmpty) Some(descriptionKey) else None
          HtmlOption(name, finalValue, description)
        }.toList
        Input.enumInput(options, optionsPath, isOptional)
      case "multi-enum" =>
        val optionsPath = inputNode \@ "optionsPath"
        val options = (inputNode \ "option").map { node =>
          val name = node.text
          val value = node \@ "value"
          val finalValue = if (value.isEmpty) name else value
          val descriptionKey = node \@ "description-key"
          val description = if (descriptionKey.nonEmpty) Some(descriptionKey) else None
          HtmlOption(name, finalValue, description)
        }.toList
        Input.multiEnumInput(options, optionsPath, isOptional)
      case "dollar" => Input.dollar(isOptional)
      case "date"   => Input.date(isOptional)
      case x        => throw InvalidFormConfig(s"Unexpected input type \"$x\" for question $path")
    }
  }
}

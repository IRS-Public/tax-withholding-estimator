package gov.irs.twe.parser

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.{ Log, TweTemplateEngine }
import gov.irs.twe.exceptions.InvalidFormConfig
import org.thymeleaf.context.Context
import scala.collection.JavaConverters.asJavaIterableConverter

case class HtmlOption(name: String, value: String)

enum Input {
  case select(options: List[HtmlOption], optionsPath: Option[String], hint: String, optional: Boolean = false)
  case text(hint: String, optional: Boolean = false)
  case int(hint: String, optional: Boolean = false)
  case boolean(question: String, hint: String, optional: Boolean = false)
  case dollar(hint: String, optional: Boolean = false)
  case date(question: String, hint: String, optional: Boolean = false)

  def typeString: String = this match {
    case Input.text(_, _)         => "text"
    case Input.int(_, _)          => "int"
    case Input.boolean(_, _, _)   => "boolean"
    case Input.dollar(_, _)       => "dollar"
    case Input.select(_, _, _, _) => "select"
    case Input.date(_, _, _)      => "date"
  }
}
object Input {
  def extractFromFgSet(node: xml.Node, isOptional: Boolean, factDictionary: FactDictionary): Input = {
    val path = node \@ "path"
    val question = (node \ "question").text.trim
    val hint = (node \ "hint").text.trim
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
      return Input.select(options, optionsPath, hint, isOptional)
    }

    // Otherwise parse the <input>
    val inputNode = node \ "input"
    if (inputNode.isEmpty) {
      throw InvalidFormConfig(s"Missing an input for question $path")
    }

    inputNode \@ "type" match {
      case "text"    => Input.text(hint, isOptional)
      case "int"     => Input.int(hint, isOptional)
      case "boolean" => Input.boolean(question, hint, isOptional)
      case "dollar"  => Input.dollar(hint, isOptional)
      case "date"    => Input.date(question, hint, isOptional)
      case x         => throw InvalidFormConfig(s"Unexpected input type \"$x\" for question $path")
    }
  }
}

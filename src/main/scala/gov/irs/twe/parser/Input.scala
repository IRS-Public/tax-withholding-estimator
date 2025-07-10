package gov.irs.twe.parser

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.Log
import gov.irs.twe.exceptions.InvalidFormConfig

case class HtmlOption(name: String, value: String)

enum Input {
  case text
  case boolean
  case dollar
  case select(options: List[HtmlOption], optionsPath: Option[String])

  def typeString: String = this match {
    case Input.text => "text"
    case Input.boolean => "boolean"
    case Input.dollar => "dollar"
    case Input.select(_, _) => "select"
  }
}

object Input {
  def extractFromQuestion(question: xml.Node, factDictionary: FactDictionary): Input = {
    val path = question \@ "path"
    val optionsPath = Option(question \@ "optionsPath").filter(_.nonEmpty)

    // Handle the <select> as a special case
    val selectNode = question \ "select"
    if (selectNode.nonEmpty) {
      val options = (selectNode \ "option").map(node => {
        val name = node.text
        var value = node \@ "value"
        if (value == "") value = name

        HtmlOption(name, value)
      }).toList
      // TODO validate that the options match the num path

      if (options.isEmpty) {
        Log.warn(s"Empty options for question $path")
      }
      return Input.select(options, optionsPath)
    }

    // Otherwise parse the <input>
    val inputNode = question \ "input"
    if (inputNode.isEmpty) {
      throw InvalidFormConfig(s"Missing an input for question $path")
    }

    inputNode \@ "type" match {
      case "text" => Input.text
      case "boolean" => Input.boolean
      case "dollar" => Input.dollar
      case x => throw InvalidFormConfig(s"Unexpected input type \"$x\" for question $path")
    }
  }
}

package gov.irs.twe.parser

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.Log
import gov.irs.twe.exceptions.InvalidFormConfig

case class HtmlOption(name: String, value: String)

enum Input {
  case select(options: List[HtmlOption], optionsPath: Option[String])
  case text
  case boolean
  case dollar
  case date

  def typeString: String = this match {
    case Input.text => "text"
    case Input.boolean => "boolean"
    case Input.dollar => "dollar"
    case Input.select(_, _) => "select"
    case Input.date => "date"
  }

  def html(path: String): xml.Elem = {
    this match {
      case Input.boolean => <div>
        <label>Yes
          <input type="radio" value="true" name={path} autocomplete="off"/>
        </label>
        <label>No
          <input type="radio" value="false" name={path} autocomplete="off"/>
        </label>
      </div>
      case Input.select(options, optionsPath) => <select optionsPath={optionsPath.getOrElse("")} name={path}>
        <option value={""} disabled="true" selected="true">
          {"-- Select one --"}
        </option>{options.map(option => <option value={option.value}>
          {option.name}
        </option>)}
      </select>
      case Input.dollar => <input type="number" step="0.01" name={path} autocomplete="off"/>
      case Input.date => <input type="date" name={path} autocomplete="off"/>
      case Input.text => <input type="text" name={path} autocomplete="off"/>
    }
  }
}

object Input {
  def extractFromQuestion(node: xml.Node, factDictionary: FactDictionary): Input = {
    val path = node \@ "path"
    // Handle the <select> as a special case
    val selectNode = node \ "select"
    if (selectNode.nonEmpty) {
      val optionsPath = Option(selectNode \@ "options-path").filter(_.nonEmpty)
      val options = (selectNode \ "option").map(node => {
        val name = node.text
        var value = node \@ "value"
        if (value == "") value = name

        HtmlOption(name, value)
      }).toList
      // TODO validate that the options match the num path

      if (options.isEmpty) {
        Log.warn(s"Empty options for fg-set: $path")
      }
      return Input.select(options, optionsPath)
    }

    // Otherwise parse the <input>
    val inputNode = node \ "input"
    if (inputNode.isEmpty) {
      throw InvalidFormConfig(s"Missing an input for question $path")
    }

    inputNode \@ "type" match {
      case "text" => Input.text
      case "boolean" => Input.boolean
      case "dollar" => Input.dollar
      case "date" => Input.date
      case x => throw InvalidFormConfig(s"Unexpected input type \"$x\" for question $path")
    }
  }
}

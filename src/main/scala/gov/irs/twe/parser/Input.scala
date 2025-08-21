package gov.irs.twe.parser

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.exceptions.InvalidFormConfig
import gov.irs.twe.Log

case class HtmlOption(name: String, value: String)

enum Input {
  case select(options: List[HtmlOption], optionsPath: Option[String])
  case text
  case int
  case boolean(question: String)
  case dollar
  case date

  def typeString: String = this match {
    case Input.text         => "text"
    case Input.int          => "int"
    case Input.boolean(_)   => "boolean"
    case Input.dollar       => "dollar"
    case Input.select(_, _) => "select"
    case Input.date         => "date"
  }

  def html(path: String): xml.Elem =
    this match {
      case Input.boolean(question) =>
        <fieldset class="usa-fieldset">
          <legend class="usa-legend">{question}</legend>
          <div class="usa-radio">
            <input id={s"${path}-yes"} class="usa-radio__input" type="radio" value="true" name={path} required="true"/>
            <label for={s"${path}-yes"} class="usa-radio__label">Yes</label>
          </div>
          <div class="usa-radio">
            <input id={s"${path}-no"} class="usa-radio__input" type="radio" value="false" name={path} required="true"/>
            <label for={s"${path}-no"} class="usa-radio__label">No</label>
          </div>
        </fieldset>
      case Input.select(options, optionsPath) =>
        <select id={path} class="usa-select" optionsPath={optionsPath.getOrElse("")} required="true">
        <option value={""} disabled="true" selected="true">
          {"-- Select one --"}
        </option>{
          options.map(option => <option value={option.value}>
          {option.name}
        </option>)
        }
      </select>
      case Input.dollar =>
        <input class="usa-input" id={path} type="number" step="0.01" name={path} autocomplete="off" required="true"/>
      case Input.date => <input id={path} class="usa-input" type="date" name={path} autocomplete="off" required="true"/>
      case Input.text => <input id={path} class="usa-input" type="text" name={path} autocomplete="off" required="true"/>
      case Input.int  => <input id={path} class="usa-input" type="text" name={path} autocomplete="off" required="true"/>
    }
}

object Input {
  def extractFromFgSet(node: xml.Node, factDictionary: FactDictionary): Input = {
    val path = node \@ "path"
    val question = node \ "question"
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
      return Input.select(options, optionsPath)
    }

    // Otherwise parse the <input>
    val inputNode = node \ "input"
    if (inputNode.isEmpty) {
      throw InvalidFormConfig(s"Missing an input for question $path")
    }

    inputNode \@ "type" match {
      case "text"    => Input.text
      case "int"     => Input.int
      case "boolean" => Input.boolean(question.text.trim)
      case "dollar"  => Input.dollar
      case "date"    => Input.date
      case x         => throw InvalidFormConfig(s"Unexpected input type \"$x\" for question $path")
    }
  }
}

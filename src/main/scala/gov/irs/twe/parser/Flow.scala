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

  override def toString: String = this match {
    case Input.text => "text"
    case Input.boolean => "boolean"
    case Input.dollar => "dollar"
    case Input.select(options, optionsPath) => "select"
  }
}

case class Question(path: String, input: Input, innerXml: xml.NodeSeq)
case class Section(questions: List[Question])
case class Flow(sections: List[Section])

object Flow {
  def fromXmlConfig(config: xml.Elem, factDictionary: FactDictionary): Flow = {
    if (config.label != "FormConfig") {
      throw InvalidFormConfig(s"Expected a top-level <FormConfig>, found ${config.label}")
    }

    val sections = (config \ "section").map(section => convertSection(section, factDictionary)).toList
    Flow(sections)
  }

  private def convertSection(section: xml.Node, factDictionary: FactDictionary): Section = {
    val questions = (section \ "question").map(section => convertQuestion(section, factDictionary)).toList
    Section(questions)
  }

  private def convertQuestion(question: xml.Node, factDictionary: FactDictionary): Question = {
    val path = question \@ "path"

    // Validate that the fact exists
    val factDefinition = factDictionary.getDefinition(path)
    if (factDefinition == null) {
      throw InvalidFormConfig(s"Path $path not found in the fact dictionary")
    }
    // TODO validate the fact matches the input

    val innerXml = question \ "_"
    val input = getInput(question, factDictionary)
    Question(path, input, innerXml)
  }

  private def getInput(question: xml.Node, factDictionary: FactDictionary): Input = {
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

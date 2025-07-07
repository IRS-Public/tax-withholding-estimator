package gov.irs.formflow.parser

import gov.irs.formflow.Log
import gov.irs.formflow.exceptions.InvalidFormConfig

case class Option(name: String, value: String)

enum Input {
  case text
  case boolean
  case select(options: List[Option])
}

case class Question(path: String, input: Input, innerXml: xml.NodeSeq)
case class Section(questions: List[Question])
case class Flow(sections: List[Section])

object Flow {
  def fromXmlConfig(config: xml.Elem): Flow = {
    if (config.label != "FormConfig") {
      throw InvalidFormConfig(s"Expected a top-level <FormConfig>, found ${config.label}")
    }

    val sections = (config \ "section").map(convertSection).toList
    Flow(sections)
  }

  private def convertSection(section: xml.Node): Section = {
    val questions = (section \ "question").map(convertQuestion).toList
    Section(questions)
  }

  private def convertQuestion(question: xml.Node): Question = {
    val path = question \@ "path"
    val innerXml = question \ "_"
    val input = getInput(question)
    Question(path, input, innerXml)
  }

  private def getInput(question: xml.Node): Input = {
    val path = question \@ "path"

    // Handle the <select> as a special case
    val selectNode = question \ "select"
    if (selectNode.nonEmpty) {
      val options = (selectNode \ "option").map(node => {
        val name = node.text
        var value = node \@ "value"
        if (value == "") value = name
        Option(name, value)
      }).toList

      if (options.isEmpty) {
        Log.warn(s"Empty options for question $path")
      }
      return Input.select(options)
    }

    // Otherwise parse the <input>
    val inputNode = question \ "input"
    if (inputNode.isEmpty) {
      throw InvalidFormConfig(s"Missing an input for question $path")
    }

    inputNode \@ "type" match {
      case "text" => Input.text
      case "boolean" => Input.boolean
      case x => throw InvalidFormConfig(s"Unexpected input type \"$x\" for question $path")
    }

  }

}

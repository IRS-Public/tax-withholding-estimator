package gov.irs.formflow.parser

import gov.irs.formflow.exceptions.InvalidFormConfig


enum InputType {
  case text
  case boolean
  case select
}

case class Input(inputType: InputType)
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

    val inputXml = question \ "input"
    val inputType = inputXml \@ "type" match {
      case "text" => InputType.text
      case "boolean" => InputType.boolean
      case "select" => InputType.select
      case x => throw InvalidFormConfig(s"Unexpected input type \"$x\" for question $path")
    }

    val input = Input(inputType)
    Question(path, input, innerXml)
  }
}

package gov.irs.formflow.generators

import gov.irs.formflow.parser.{Flow, Question, Section}

type Xhtml = scala.xml.Elem

case class Page(route: String, content: Xhtml)

case class Website(pages: List[Page])

object Website {
  def generate(flow: Flow): Website = {
    val sectionHtml = flow.sections.map(generateSection)

    val content = <html><body>{ sectionHtml }</body></html>
    val page = Page("index.html", content)
    Website(List(page))
  }

  def fromXmlConfig(config: xml.Elem): Website = {
    val flow = Flow.fromXmlConfig(config)
    generate(flow)
  }

  private def generateSection(section: Section): xml.Elem = {
    val questions = section.questions.map(convertQuestion)
    <section>
      {questions}
    </section>
  }

  private def convertQuestion(question: Question): xml.Elem = {
    <fieldset path={question.path}>
      {question.innerXml}
    </fieldset>
  }

}

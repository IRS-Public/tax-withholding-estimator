package gov.irs.formflow.generators

import gov.irs.formflow.parser.{Flow, Input, Question, Section}
import org.jsoup.Jsoup
import os.Path

import scala.io.Source

case class Page(route: String, content: xml.Elem)

case class Website(pages: List[Page]) {
  def save(directoryPath: Path): Unit = {
    os.remove.all(directoryPath)

    // Write the pages
    for (page <- this.pages) {
      val content = "<!DOCTYPE html>" + page.content.toString
      val document = Jsoup.parse(content)
      val target = directoryPath / page.route

      os.write(target, document.html(), null, createFolders = true)
    }

    // Write the static resources to a "/resources"
    // I think this should eventually be configured *outside* the scala application
    addStaticResource(directoryPath, "stylesheet.css")
  }

  private def addStaticResource(directoryPath: os.Path, filename: String): Unit = {
    val stylesheet = Source.fromResource(s"website-static/$filename").getLines()
    val stylesheetPath = directoryPath / "resources" / filename
    os.write(stylesheetPath, stylesheet, null, createFolders = true)
  }
}

object Website {
  def fromXmlConfig(config: xml.Elem): Website = {
    val flow = Flow.fromXmlConfig(config)
    generate(flow)
  }

  def generate(flow: Flow): Website = {

    val content = <html>
      <link rel="stylesheet" href="/resources/stylesheet.css"></link>
      <body>
        <header>
          <h1>Tax Witholding Estimator</h1>
          <p>
            Use your best estimates for the year ahead to determine how to complete Form W-4 or W-4P so you don't have
            too much or too little federal income tax withheld.
          </p>
        </header>
        <main>{flow.sections.map(generateSection)}</main>
      </body>
    </html>

    val page = Page("index.html", content)
    Website(List(page))
  }


  private def generateSection(section: Section): xml.Elem = {
    <section>
      {section.questions.map(convertQuestion)}
    </section>
  }

  private def convertQuestion(question: Question): xml.Node = {
    val questionXml = question.innerXml.map(node => {
      node.label match {
        case "input" => convertInput(question)
        case _ => node
      }
    })


    <fieldset path={question.path} class="question">
      {questionXml}
    </fieldset>
  }

  private def convertInput(question: Question): xml.Node = {
    question.input match {
      case Input.boolean => <div>
        <label>Yes <input type="radio" value="true" name={question.path} /></label>
        <label>No <input type="radio" value="false" name={question.path} /></label>
      </div>
      case x => <input type={x.toString}></input>
    }
  }

}

package gov.irs.twe.generators

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.parser.FgSetNode.html
import gov.irs.twe.parser.SectionNode.fgSet
import gov.irs.twe.parser.{FgCollection, FgCollectionNode, FgSet, FgSetNode, Flow, Input, Section, SectionNode}
import org.jsoup.Jsoup
import org.jsoup.parser.Tag
import os.Path

import scala.io.Source

case class Page(route: String, content: xml.Elem) {
  def html(): String = {
    val content = "<!DOCTYPE html>" + this.content.toString
    val document = Jsoup.parse(content)

    // This is to set certain elements to "block" formatting
    // So that they look nice in view-source
    // https://github.com/jhy/jsoup/issues/2141#issuecomment-2795853753
    val setElement = document.expectFirst("fg-set")
    val tag = setElement.tag()
    tag.set(Tag.Block)
    setElement.children().forEach(child => child.tag().set(Tag.Block))

    // Convert to an HTML string
    // I added a newline after each <fg-set> block to make them easier to see
    var html = document.html()
    html = html.replace("</fg-set>", "</fg-set>\n")

    html
  }
}

case class Website(pages: List[Page], factDictionary: xml.Elem) {
  def save(directoryPath: Path): Unit = {
    os.remove.all(directoryPath)

    // Write the pages
    for (page <- this.pages) {
      val target = directoryPath / page.route
      os.write(target, page.html(), null, createFolders = true)
    }

    // This is a hack
    // I think this should eventually be configured *outside* the scala application
    // It should also automatically introspect the directory
    addStaticResource(directoryPath, "stylesheet.css")
    addStaticResource(directoryPath, "factgraph-3.1.0.js")
    addStaticResource(directoryPath, "fg-components.js")
    addStaticResource(directoryPath, "debug-components.js")
    addStaticResource(directoryPath, "irs-logo.svg")
  }

  private def addStaticResource(directoryPath: os.Path, filename: String): Unit = {
    val resource = Source.fromResource(s"twe/website-static/$filename").getLines().mkString("\n")
    val pathInWebsite = directoryPath / "resources" / filename
    os.write(pathInWebsite, resource, null, createFolders = true)
  }
}

object Website {
  def fromXmlConfig(config: xml.Elem , dictionaryConfig: xml.Elem): Website = {
    val factDictionary = FactDictionary.fromXml(dictionaryConfig)
    val flow = Flow.fromXmlConfig(config, factDictionary)
    generate(flow, dictionaryConfig)
  }

  def generate(flow: Flow, dictionaryConfig: xml.Elem): Website = {
    val content = <html>
      <link rel="stylesheet" href="/resources/stylesheet.css"></link>
      <script type="module" src="/resources/factgraph-3.1.0.js"></script>
      <script type="module" src="/resources/fg-components.js"></script>
      <script type="module" src="/resources/debug-components.js"></script>

      <body>
        <header>
          <div class="logo-banner"><img src="/resources/irs-logo.svg" /></div>
        </header>

        <main class="hidden">
        <h1>Tax Witholding Estimator</h1>
        <p>
          Use your best estimates for the year ahead to determine how to complete Form W-4 or W-4P so you don't have
          too much or too little federal income tax withheld.
        </p>

        <fg-reset><button>Reset</button></fg-reset>

        {flow.sections.map(generateSection)}

        </main>

      <script type="text" id="fact-dictionary">{dictionaryConfig}</script>

      </body>
    </html>

    val page = Page("index.html", content)
    Website(List(page), dictionaryConfig)
  }


  private def generateSection(section: Section): xml.Elem = {
    val sectionXml = section.nodes.map {
      case SectionNode.fgCollection(x) => convertCollection(x)
      case SectionNode.fgSet(x) => convertQuestion(x)
      case SectionNode.html(x) => x
    }

    <section>
      {sectionXml}
    </section>
  }

  private def convertCollection(collection: FgCollection): xml.Node = {
    val collectionFacts = collection.nodes.map {
      case FgCollectionNode.fgSet(x) => convertQuestion(x)
      case FgCollectionNode.html(x) => x
    }

    <fg-collection path={collection.path}>
      {collectionFacts}
    </fg-collection>
  }

  private def convertQuestion(question: FgSet): xml.Node = {
    val questionXml = question.nodes.map {
      case FgSetNode.input(input) => convertInput(input, question.path)
      case FgSetNode.html(x) => x
    }

    <fg-set
      path={question.path}
      inputType={question.input.typeString}
      condition={question.condition.getOrElse(null)}
      >
      {questionXml}
    </fg-set>
  }

  private def convertInput(input: Input, path: String): xml.Node = {
    input match {
      case Input.boolean => <div>
        <label>Yes <input type="radio" value="true" name={path} /></label>
        <label>No <input type="radio" value="false" name={path} /></label>
      </div>
      case Input.select(options, optionsPath) => <select optionsPath={optionsPath.getOrElse("")} name={path}>
        {options.map(option => <option value={option.value}>{option.name}</option>)}
      </select>
      case Input.dollar => <input type="number" step="0.01" name={path} />
      case Input.day => <input type="date" name={path} />
      case Input.text => <input type="text" name={path} />
    }
  }

}

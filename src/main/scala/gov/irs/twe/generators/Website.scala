package gov.irs.twe.generators

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.parser.FgSetNode.html
import gov.irs.twe.parser.SectionNode.fgSet
import gov.irs.twe.parser.{FgCollection, FgCollectionNode, FgSet, FgSetNode, Flow, Input, Page, PageNode, Section, SectionNode}
import org.jsoup.Jsoup
import org.jsoup.parser.Tag
import os.Path

import scala.io.Source

case class WebsitePage(route: String, content: xml.Elem) {
  def html(): String = {
    val content = "<!DOCTYPE html>" + this.content.toString

    // Many of the changes here are tweaks to make the output easy to read in view-source
    val document = Jsoup.parse(content)

    // Set certain elements to "block" formatting
    // https://github.com/jhy/jsoup/issues/2141#issuecomment-2795853753
    val setElement = document.selectFirst("fg-set")
    if (setElement != null) {
      val tag = setElement.tag()
      tag.set(Tag.Block)
      setElement.children().forEach(child => child.tag().set(Tag.Block))
    }


    // Convert to an HTML string
    var html = document.html()
    // Adding a newline after each <fg-set> block to make them easier to see
    html = html.replace("</fg-set>", "</fg-set>\n")

    html
  }
}

case class Website(pages: List[WebsitePage], factDictionary: xml.Elem) {
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
    val navElements = flow.pages.map(page => <li><a href={page.route}>{page.title}</a></li>)
    val nav = <nav>
      <ul>
        {navElements}
      </ul>
    </nav>

    val pages = flow.pages.map(generatePage(_, dictionaryConfig, nav))
    Website(pages, dictionaryConfig)
  }

  private def generatePage(page: Page, dictionaryConfig: xml.Elem, nav: xml.Elem): WebsitePage = {
    val pageXml = page.nodes.map {
      case PageNode.section(x) => generateSection(x)
      case PageNode.html(x) => x
    }

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
          {nav}
          {pageXml}
        </main>
        <script type="text" id="fact-dictionary">{dictionaryConfig}</script>
      </body>
    </html>

    val route = if (page.route == "/") "index.html" else s"${page.route}.html"
    WebsitePage(route, content)
  }

  private def generateSection(section: Section): xml.Elem = {
    val sectionXml = section.nodes.map {
      case SectionNode.fgCollection(x) => convertCollection(x)
      case SectionNode.fgSet(x) => convertFgSet(x)
      case SectionNode.html(x) => x
    }

    <section>
      {sectionXml}
    </section>
  }

  private def convertCollection(collection: FgCollection): xml.Node = {
    val collectionFacts = collection.nodes.map {
      case FgCollectionNode.fgSet(x) => convertFgSet(x)
      case FgCollectionNode.html(x) => x
    }

    val condition = collection.condition.map(_.path).orNull
    val operator = collection.condition.map(_.operator.toString).orNull

    <fg-collection
      path={collection.path}
      condition={condition}
      operator={operator}
    >
      {collectionFacts}
    </fg-collection>
  }

  private def convertFgSet(fgSet: FgSet): xml.Node = {
    val questionXml = fgSet.nodes.map {
      case FgSetNode.input(input) => convertInput(input, fgSet.path)
      case FgSetNode.html(x) => x
    }

    val condition = fgSet.condition.map(_.path).orNull
    val operator = fgSet.condition.map(_.operator.toString).orNull

    <fg-set
      path={fgSet.path}
      inputType={fgSet.input.typeString}
      condition={condition}
      operator={operator}
      >
      {questionXml}
    </fg-set>
  }

  private def convertInput(input: Input, path: String): xml.Node = {
    input match {
      case Input.boolean => <div>
        <label>Yes <input type="radio" value="true" name={path} autocomplete="off"/></label>
        <label>No <input type="radio" value="false" name={path} autocomplete="off"/></label>
      </div>
      case Input.select(options, optionsPath) => <select optionsPath={optionsPath.getOrElse("")} name={path}>
        <option value={""} disabled="true" selected="true">{"-- Select one --"}</option>
        {options.map(option => <option value={option.value}>{option.name}</option>)}
      </select>
      case Input.dollar => <input type="number" step="0.01" name={path} autocomplete="off"/>
      case Input.date => <input type="date" name={path} autocomplete="off"/>
      case Input.text => <input type="text" name={path} autocomplete="off"/>
    }
  }

}

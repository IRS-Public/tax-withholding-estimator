package gov.irs.twe.generators

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.parser.{Flow, Page, PageNode}
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

    val resourcesSource = os.pwd / "src" / "main"/ "resources" / "twe" / "website-static"
    val resourcesTarget = directoryPath / "resources"
    os.copy(resourcesSource, resourcesTarget)
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
      case PageNode.section(x) => x.html()
      case PageNode.rawHTML(x) => x
    }

    val title = s"Tax Withholding Estimator - ${page.title} | Internal Revenue Service"

    val content = <html lang="en">
      <meta charset="utf-8" />
      <meta http-equiv="X-UA-Compatible" content="IE=edge" />
      <meta name="HandheldFriendly" content="True" />
      <meta name="MobileOptimized" content="320" />
      <meta name="viewport" content="width=device-width, initial-scale=1.0" />
      <title>{title}</title>
      <link rel="stylesheet" href="/resources/stylesheet.css"></link>
      <script type="module" src="/resources/factgraph-3.1.0.js"></script>
      <script type="module" src="/resources/fg-components.js"></script>
      <script type="module" src="/resources/debug-components.js"></script>

      <body>

        <header>
          <div class="logo-banner"><img src="/resources/irs-logo.svg" alt="" /></div>
        </header>

        <main class="hidden" id="main-content">
          {nav}
          {pageXml}
        </main>
        <script type="text" id="fact-dictionary">{dictionaryConfig}</script>

      </body>
    </html>

    val route = if (page.route == "/") "index.html" else s"${page.route}.html"
    WebsitePage(route, content)
  }

}

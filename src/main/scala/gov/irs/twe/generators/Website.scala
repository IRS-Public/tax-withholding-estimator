package gov.irs.twe.generators

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.parser.{ Flow, PageNode }
import gov.irs.twe.TweTemplateEngine
import org.jsoup.parser.Tag
import org.jsoup.Jsoup
import org.thymeleaf.context.Context
import os.Path
import scala.jdk.CollectionConverters.*

case class WebsitePage(route: String, content: String) {
  def html(): String = {
    // This step largely serves to make the output easy to read in view-source
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

    val resourcesSource = os.pwd / "src" / "main" / "resources" / "twe" / "website-static"
    val resourcesTarget = directoryPath / "resources"
    os.copy(resourcesSource, resourcesTarget)
  }
}

object Website {
  private val templateEngine = new TweTemplateEngine()

  def generate(flow: Flow, dictionaryConfig: xml.Elem, flags: Map[String, Boolean]): Website = {
    val templateEngine = new TweTemplateEngine()
    val navPages = flow.pages.filter(p => p.exclude == false)
    val excludedPageLength = flow.pages.length - navPages.size

    val pages = flow.pages.zipWithIndex.map { (page, index) =>
      val route = if (page.route == "/") "index.html" else s"${page.route}.html"
      val title = s"Tax Withholding Estimator - ${page.title} | Internal Revenue Service"
      val stepTitle = page.title

      val context = new Context()
      context.setVariable("exclude", page.exclude)
      context.setVariable("dictionaryConfig", dictionaryConfig.toString)
      context.setVariable("title", title)
      context.setVariable("stepTitle", stepTitle)
      context.setVariable("stepIndex", (index - excludedPageLength) % flow.pages.length)
      context.setVariable("stepTotal", navPages.size)
      context.setVariable("pages", navPages.asJava) // th:each requires Java Iterables
      context.setVariable("flags", flags.asJava)

      // Add a link for the next page if it's not the last one
      if (index + 1 < navPages.size) {
        val nextPageHref = flow.pages(index + 1).route
        context.setVariable("nextPageHref", nextPageHref)
      }
      // Add a link for the last page if it's not the first one
      if (index > 0) {
        val lastPageHref = flow.pages(index - 1).route
        context.setVariable("lastPageHref", lastPageHref)
      } else {
        context.setVariable("first", true)
      }

      // Turn all the pages into HTML representations and join them together
      val pageXml = page.html(templateEngine)

      context.setVariable("pageXml", pageXml)

      val content = templateEngine.process("page", context)
      WebsitePage(route, content)
    }

    Website(
      AllScreens.generate(flow, dictionaryConfig) +: pages,
      dictionaryConfig,
    )
  }
}

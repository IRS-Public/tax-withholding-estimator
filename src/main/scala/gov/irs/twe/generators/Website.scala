package gov.irs.twe.generators

import gov.irs.twe.build.Flags
import gov.irs.twe.parser.Flow
import gov.irs.twe.TweTemplateEngine
import org.jsoup.parser.Tag
import org.jsoup.Jsoup
import org.thymeleaf.context.Context
import os.Path
import scala.jdk.CollectionConverters.*

case class WebsitePage(route: String, content: String, languageCode: String) {
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

  def filepath(root: Path): Path = {
    val isTranslated = languageCode != "en"
    val isNamedRoute = route != "/"

    var path = root
    if (isTranslated) path = path / languageCode
    if (isNamedRoute) path = path / route.substring(1)

    path / "index.html"
  }
}

case class Website(pages: List[WebsitePage], factDictionary: xml.Elem) {
  def save(directoryPath: Path): Unit = {
    os.remove.all(directoryPath)

    // Write the pages
    for (page <- this.pages) {
      val target = page.filepath(directoryPath)
      os.write(target, page.html(), null, createFolders = true)
    }

    val resourcesSource = os.pwd / "src" / "main" / "resources" / "twe" / "website-static"
    val resourcesTarget = directoryPath / "resources"
    os.copy(resourcesSource, resourcesTarget)

    val dictionaryString = factDictionary.toString
    os.write(resourcesTarget / "fact-dictionary.xml", dictionaryString, null)
  }
}

object Website {
  def generate(
      flow: Flow,
      dictionaryXml: xml.Elem,
      flags: Map[String, Boolean],
  ): Website = {
    val locales = if (flags.contains(Flags.spanishTranslations)) List("en", "es") else List("en")
    var pages = locales.flatMap { languageCode =>
      val templateEngine = new TweTemplateEngine(languageCode)
      val navPages = flow.pages.filter(p => !p.exclude)
      val excludedPageLength = flow.pages.length - navPages.size

      flow.pages.zipWithIndex.map { (page, index) =>
        val titleValue = templateEngine.messageResolver.resolveMessage(page.titleKey)
        val titlePrefix = templateEngine.messageResolver.resolveMessage("title.prefix")
        val titleSuffix = templateEngine.messageResolver.resolveMessage("title.suffix")

        val title = s"$titlePrefix - $titleValue | $titleSuffix"

        val context = new Context()
        context.setVariable("exclude", page.exclude)
        context.setVariable("title", title)
        context.setVariable("stepTitle", titleValue)
        context.setVariable("stepIndex", (index - excludedPageLength) % flow.pages.length)
        context.setVariable("stepTotal", navPages.size)
        context.setVariable("pages", navPages.asJava) // th:each requires Java Iterables
        context.setVariable("flags", flags.asJava)
        context.setVariable("languageCode", languageCode)

        // Add a link for the next page if it's not the last one
        if (index < flow.pages.size - 1) {
          val nextPageHref = flow.pages(index + 1).href(languageCode)
          context.setVariable("nextPageHref", nextPageHref)
        }
        // Add a link for the last page if it's not the first one
        if (index > 0) {
          val lastPageHref = flow.pages(index - 1).href(languageCode)
          context.setVariable("lastPageHref", lastPageHref)
        } else {
          context.setVariable("first", true)
        }

        // Turn all the pages into HTML representations and join them together
        val pageHtml = page.html(templateEngine)

        context.setVariable("pageHtml", pageHtml)

        val content = templateEngine.process("page", context)
        WebsitePage(page.route, content, languageCode)
      }
    }

    if (flags.contains(Flags.allScreens)) {
      val allScreens = AllScreens.generate(flow)
      pages = pages :+ allScreens
    }

    Website(pages, dictionaryXml)
  }
}

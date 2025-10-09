package gov.irs.twe.generators

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.parser.{ Flow, Page, PageNode }
import gov.irs.twe.TweTemplateEngine
import org.jsoup.parser.Tag
import org.jsoup.Jsoup
import org.thymeleaf.context.Context
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import org.thymeleaf.TemplateEngine
import os.Path
import scala.jdk.CollectionConverters.*

case class AllScreens(pages: List[WebsitePage], factDictionary: xml.Elem) {
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

object AllScreens {
  def generate(flow: Flow, dictionaryConfig: xml.Elem): WebsitePage = {
    val templateEngine = new TweTemplateEngine()
    val context = new Context()
    context.setVariable("title", "All Screens")
    context.setVariable("dictionaryConfig", dictionaryConfig.toString)

    val pages = flow.pages.map(page =>
      Map(
        "route" -> page.route,
        "title" -> page.title,
        "content" -> page.html(templateEngine),
      ).asJava,
    )
    context.setVariable("pages", pages.asJava)

    val content = templateEngine.process("all-screens", context)

    WebsitePage("all-screens.html", content)
  }
}

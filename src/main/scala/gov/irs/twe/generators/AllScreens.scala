package gov.irs.twe.generators

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.parser.{ Flow, Page, PageNode }
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
    val resolver = new ClassLoaderTemplateResolver()
    resolver.setTemplateMode(TemplateMode.HTML)
    resolver.setCharacterEncoding("UTF-8")
    resolver.setPrefix("/twe/templates/")
    resolver.setSuffix(".html")

    val templateEngine = new TemplateEngine()
    templateEngine.setTemplateResolver(resolver)

    val context = new Context()
    context.setVariable("title", "All Screens")
    context.setVariable("pages", flow.pages.asJava)
    context.setVariable("dictionaryConfig", dictionaryConfig.toString)

    val content = templateEngine.process("all-screens", context)

    WebsitePage("all-screens.html", content)
  }
}

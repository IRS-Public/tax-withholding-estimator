package gov.irs.twe.parser

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.{ FlowResourceRoot, TweTemplateEngine }
import gov.irs.twe.exceptions.InvalidFormConfig
import gov.irs.twe.parser.Utils.optionString
import scala.io.Source
import scala.util.matching.Regex

enum PageNode {
  case section(section: Section)
  case modal(modal: Modal)
  case rawHTML(html: String)
}

case class Page(
    title: String,
    route: String,
    exclude: Boolean,
    nodes: List[PageNode],
):
  def html(templateEngine: TweTemplateEngine): String = {
    val pageContent = nodes
      .map {
        case PageNode.section(x) => x.html(templateEngine)
        case PageNode.modal(x)   => x.html(templateEngine)
        case PageNode.rawHTML(x) => x
      }
      .mkString("")

    // Coerce all fg-show nodes into open, empty tags because HTML doesn't allow custom, self-closing tags
    val regex = new Regex("""<(fg-show) ([^>]*)>""", "nodeName", "attributes")
    val pageXml = regex.replaceAllIn(
      pageContent,
      m => s"<\\${m group "nodeName"} \\${m group "attributes"}></\\${m group "nodeName"}>",
    )

    pageXml
  }

  // The "canonical" URL for each page is described with `../` because at the moment we do not have access to
  // the domain root, i.e. in some environments are at example.com/twe/ and other we are at example.com/nest/route/twe/
  // We do guarantee that the route will always end in a trailing slash, so `../` works everywhere except the home page:
  //
  // Clicking <a href="../income"> on:
  //   example.com/twe/             -> example.com/income
  //   example.com/twe/adjustments/ -> example.com/twe/income
  //
  // This is handled in Website.scala by removing a leading dot from the next-page link when it's on the home page.
  def href(): String = {
    val trailingSlash = if (route != "/") "/" else ""
    ".." + route + trailingSlash
  }

object Page {
  def parse(page: xml.Node, factDictionary: FactDictionary): Page = {
    val route = optionString(page \@ "route").getOrElse(throw InvalidFormConfig("<page> is missing a route attribute"))
    val title = optionString(page \@ "title").getOrElse(throw InvalidFormConfig("<page> is missing a title attribute"))
    val exclude = (page \@ "exclude-from-stepper").toBooleanOption.getOrElse(false)

    // If there's an html-src attribute, don't process any of the child notes
    // I think an html <include> attribute would be a little nicer but I don't have the time to make
    // that available everywhere, and I think it would be weird if you could only use it in <page>
    val htmlSrc = optionString(page \@ "html-src")
    if (htmlSrc.isDefined) {
      if ((page \ "_").length > 0) throw InvalidFormConfig("<page> can't have any children if it has html-src")
      val resolvedSrc = htmlSrc.get.replaceAll("^\\./", "")
      val moduleFile = Source.fromResource(s"$FlowResourceRoot/$resolvedSrc").getLines().mkString("\n")
      val htmlNode = PageNode.rawHTML(moduleFile)
      return Page(title, route, exclude, List(htmlNode))
    }

    val nodes = (page \ "_")
      .map(node =>
        node.label match {
          case "section"      => PageNode.section(Section.parse(node, route, factDictionary))
          case "modal-dialog" => PageNode.modal(Modal.parse(node))
          case _              => PageNode.rawHTML(node.toString)
        },
      )
      .toList

    Page(title, route, exclude, nodes)
  }
}

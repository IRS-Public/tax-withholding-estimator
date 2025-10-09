package gov.irs.twe.parser

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.exceptions.InvalidFormConfig
import gov.irs.twe.parser.Utils.optionString
import gov.irs.twe.TweTemplateEngine
import scala.util.matching.Regex

enum PageNode {
  case section(section: Section)
  case modal(modal: Modal)
  case rawHTML(node: xml.Node)
}

case class Page(
    title: String,
    route: String,
    exclude: Boolean,
    nodes: List[PageNode],
):
  def html(templateEngine: TweTemplateEngine) = {
    val pageContent = nodes
      .map {
        case PageNode.section(x) => x.html(templateEngine)
        case PageNode.modal(x)   => x.html(templateEngine)
        case PageNode.rawHTML(x) => x
      }
      .mkString("")

    // Coerce all fg-show nodes into open, empty tags because HTML doesn't allow custom, self-closing tags
    val regex = new Regex("""<(fg-show) ([^>]*)>""", "nodeName", "attributes")
    var pageXml = regex.replaceAllIn(
      pageContent,
      m => s"<\\${m group "nodeName"} \\${m group "attributes"}></\\${m group "nodeName"}>",
    )

    pageXml
  }

object Page {
  def parse(page: xml.Node, factDictionary: FactDictionary): Page = {
    val route = optionString(page \@ "route").getOrElse(throw InvalidFormConfig("<page> is missing a route attribute"))
    val title = optionString(page \@ "title").getOrElse(throw InvalidFormConfig("<page> is missing a title attribute"))
    val exclude = (page \@ "exclude-from-stepper").toBooleanOption.getOrElse(false)

    val nodes = (page \ "_")
      .map(node =>
        node.label match {
          case "section"      => PageNode.section(Section.parse(node, factDictionary))
          case "modal-dialog" => PageNode.modal(Modal.parse(node))
          case _              => PageNode.rawHTML(node)
        },
      )
      .toList

    Page(title, route, exclude, nodes)
  }
}

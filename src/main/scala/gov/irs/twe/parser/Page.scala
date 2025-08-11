package gov.irs.twe.parser

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.exceptions.InvalidFormConfig
import gov.irs.twe.parser.Utils.optionString

enum PageNode {
  case section(section: Section)
  case rawHTML(node: xml.Node)
}

case class Page(title: String, route: String, nodes: List[PageNode])

object Page {
  def parse(page: xml.Node, factDictionary: FactDictionary): Page = {
    val route = optionString(page \@ "route").getOrElse(throw InvalidFormConfig("<page> is missing a route attribute"))
    val title = optionString(page \@ "title").getOrElse(throw InvalidFormConfig("<page> is missing a title attribute"))

    val nodes = (page \ "_")
      .map(node =>
        node.label match {
          case "section" => PageNode.section(Section.parse(node, factDictionary))
          case _         => PageNode.rawHTML(node)
        },
      )
      .toList

    Page(title, route, nodes)
  }

}

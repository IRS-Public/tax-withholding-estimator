package gov.irs.twe.parser

import gov.irs.twe.TweTemplateEngine
import scala.xml.Elem

abstract class Html extends FlowNode

case class HtmlLeafNode(htmlElement: Elem) extends Html {
  override def html(templateEngine: TweTemplateEngine): String = {
    htmlElement.mkString.strip
  }
}

case class HtmlWithChildren(tag: String, attrs: String, children: Seq[FlowNode]) extends Html {
  override def html(templateEngine: TweTemplateEngine): String = {
    // get current element as HTML element
    val openTag = if (attrs.isEmpty) s"<$tag>" else s"<$tag $attrs>"
    val closeTag = s"</$tag>"

    // parse children
    val childrenHtml = children.html(templateEngine)

    // return children inside of current element tag as html
    s"$openTag$childrenHtml$closeTag"
  }
}

object Html extends FlowNodeParser {
  override def fromXml(htmlElement: Elem, flowParser: FlowParser, level: Int): Html = {
    if (isLeafNode(htmlElement)) {
      HtmlLeafNode(htmlElement)
    } else {
      val tag = htmlElement.label
      val attrs = htmlElement.attributes.asAttrMap
        .map { case (k, v) => s"""$k="$v"""" }
        .mkString(" ")
      val children = flowParser.parseChildElements(htmlElement, level)
      HtmlWithChildren(tag, attrs, children)
    }
  }

  private val LEAF_NODES = Set("p", "li", "caption", "th", "td", "h1", "h2", "h3", "h4", "h5", "h6")
  private def isLeafNode(element: xml.Elem) = {
    LEAF_NODES.contains(element.label)
  }
}

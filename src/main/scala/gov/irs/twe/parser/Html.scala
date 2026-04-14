package gov.irs.twe.parser

import gov.irs.twe.TweTemplateEngine
import scala.xml.Elem

abstract class Html extends FlowNode

case class HtmlLeafNode(htmlElement: Elem, openTag: String, closeTag: String, translationKey: String) extends Html {
  override def html(templateEngine: TweTemplateEngine): String = {
    val content = templateEngine.messageResolver.resolveMessage(translationKey)
    s"$openTag$content$closeTag"
  }
}

case class HtmlWithChildren(openTag: String, closeTag: String, children: Seq[FlowNode]) extends Html {
  override def html(templateEngine: TweTemplateEngine): String = {
    // parse children
    val childrenHtml = children.html(templateEngine)

    // return children inside of current element tag as html
    s"$openTag$childrenHtml$closeTag"
  }
}

object Html extends FlowNodeParser {
  override def fromXml(
      htmlElement: Elem,
      flowParser: FlowParser,
      parentTranslationContext: TranslationContext,
  ): Html = {
    val openTag = getOpenTag(htmlElement)
    val closeTag = getClosingTag(htmlElement)
    if (isLeafNode(htmlElement)) {
      val content = htmlElement.head.child.mkString.strip
      val childKey = parentTranslationContext.getHashKey(htmlElement.label, content)
      val translationKey = parentTranslationContext.fullKey(childKey)
      parentTranslationContext.updateValue(childKey, content)

      // Since this is a leaf node we just pass in the translation key directly and don't update translationContext
      HtmlLeafNode(htmlElement, openTag, closeTag, translationKey)
    } else {
      val ignoredElements = List("div", "details", "summary")
      val translationContext =
        if (ignoredElements.contains(htmlElement.label)) parentTranslationContext
        else parentTranslationContext.forChildWithoutUniqueId(htmlElement.label)
      val children = flowParser.parseChildElements(htmlElement, translationContext)
      HtmlWithChildren(openTag, closeTag, children)
    }
  }

  private def getOpenTag(htmlElem: Elem): String = {
    val tag = htmlElem.label
    val attrs = htmlElem.attributes.asAttrMap
      .map { case (k, v) => s"""$k="$v"""" }
      .mkString(" ")
    val openTag = if (attrs.isEmpty) s"<$tag>" else s"<$tag $attrs>"
    openTag
  }

  private def getClosingTag(htmlElem: Elem): String = {
    val tag = htmlElem.label
    s"</$tag>"
  }

  private val LEAF_NODES = Set("p", "li", "caption", "th", "td", "h1", "h2", "h3", "h4", "h5", "h6")
  private def isLeafNode(element: xml.Elem) = {
    LEAF_NODES.contains(element.label)
  }
}

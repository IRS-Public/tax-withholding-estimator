package gov.irs.twe.parser

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.TweTemplateEngine
import org.thymeleaf.context.Context

case class FgDetail(
    summary: String,
    children: List[SectionNode],
    useChevron: Boolean,
    headingTag: String,
    open: Boolean,
    condition: Option[Condition],
    factDictionary: FactDictionary,
    pageRoute: String,
) {
  def html(templateEngine: TweTemplateEngine): String = {
    val childrenHtml = FgCollection.renderSectionNodes(children, templateEngine)

    val context = new Context()
    context.setVariable("summary", summary)
    context.setVariable("childrenHtml", childrenHtml)
    context.setVariable("useChevron", java.lang.Boolean.valueOf(useChevron))
    context.setVariable("headingTag", headingTag)
    context.setVariable("open", java.lang.Boolean.valueOf(open))
    context.setVariable("condition", condition.map(_.path).orNull)
    context.setVariable("operator", condition.map(c => c.operator.toString).orNull)

    templateEngine.process("nodes/fg-detail", context)
  }
}

object FgDetail {
  private val ValidHeadingTags = Set("h2", "h3", "h4", "h5", "h6")
  def parse(node: xml.Node, pageRoute: String, factDictionary: FactDictionary): FgDetail = {
    val summary = (node \ "summary").headOption match {
      case Some(summaryNode) => summaryNode.child.map(_.toString).mkString.trim
      case None              => ""
    }
    val childNodes = (node \ "_").filter(_.label != "summary")
    val children = childNodes.map(child => Section.processNode(child, pageRoute, factDictionary)).toList
    val useChevron = (node \@ "icon") == "chevron"
    val rawHeadingTag = (node \@ "heading-tag").trim.toLowerCase
    val headingTag = if (ValidHeadingTags.contains(rawHeadingTag)) rawHeadingTag else "h4"
    val open = (node \@ "open").trim.equalsIgnoreCase("true")
    val condition = Condition.getCondition(node, factDictionary)

    FgDetail(summary, children, useChevron, headingTag, open, condition, factDictionary, pageRoute)
  }
}

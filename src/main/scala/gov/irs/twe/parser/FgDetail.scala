package gov.irs.twe.parser

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.parser.{ Condition, FgDetail }
import gov.irs.twe.TweTemplateEngine
import org.thymeleaf.context.Context
import org.thymeleaf.TemplateEngine
import scala.xml.Elem

case class FgDetail(
    summary: String,
    children: Seq[FlowNode],
    useChevron: Boolean,
    headingTag: String,
    open: Boolean,
    condition: Option[Condition],
) extends FlowNode {
  override def html(templateEngine: TweTemplateEngine): String = {
    val context = new Context()
    context.setVariable("summary", summary)
    val childrenHtml = children.html(templateEngine)
    context.setVariable("childrenHtml", childrenHtml)
    context.setVariable("useChevron", java.lang.Boolean.valueOf(useChevron))
    context.setVariable("headingTag", headingTag)
    context.setVariable("open", java.lang.Boolean.valueOf(open))
    context.setVariable("condition", condition.map(_.path).orNull)
    context.setVariable("operator", condition.map(c => c.operator.toString).orNull)

    templateEngine.process("nodes/fg-detail", context)
  }
}

object FgDetail extends FlowNodeParser {
  private val VALID_HEADING_TAGS = Set("h2", "h3", "h4", "h5", "h6")

  override def fromXml(fgDetailElement: Elem, flowNodeParser: FlowParser, level: Int = 0): FgDetail = {
    val summary = (fgDetailElement \ "summary").headOption match {
      case Some(summaryNode) => summaryNode.child.map(_.toString).mkString.trim
      case None              => ""
    }

    val childrenHtml = flowNodeParser.parseChildElements(fgDetailElement, List("summary"), level)
    val useChevron = (fgDetailElement \@ "icon") == "chevron"
    val rawHeadingTag = (fgDetailElement \@ "heading-tag").trim.toLowerCase
    val headingTag = if (VALID_HEADING_TAGS.contains(rawHeadingTag)) rawHeadingTag else "h4"
    val open = (fgDetailElement \@ "open").trim.equalsIgnoreCase("true")
    val condition = Condition.getCondition(fgDetailElement, flowNodeParser.factDictionary)

    FgDetail(summary, childrenHtml, useChevron, headingTag, open, condition)
  }
}

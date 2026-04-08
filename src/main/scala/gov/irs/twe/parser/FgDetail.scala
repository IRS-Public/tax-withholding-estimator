package gov.irs.twe.parser

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.parser.{ Condition, FgDetail }
import gov.irs.twe.TweTemplateEngine
import org.thymeleaf.context.Context
import org.thymeleaf.TemplateEngine
import scala.xml.Elem

case class FgDetail(
    translationContext: TranslationContext,
    children: Seq[FlowNode],
    useChevron: Boolean,
    detailsClass: Option[String],
    headingTag: String,
    open: Boolean,
    condition: Option[Condition],
) extends FlowNode {
  override def html(templateEngine: TweTemplateEngine): String = {
    val context = new Context()
    val summaryKey = translationContext.fullKey("summary")
    val summary = templateEngine.messageResolver.resolveMessage(summaryKey)
    context.setVariable("summary", summary)
    val childrenHtml = children.html(templateEngine)
    context.setVariable("childrenHtml", childrenHtml)
    context.setVariable("useChevron", java.lang.Boolean.valueOf(useChevron))
    context.setVariable("detailsClass", detailsClass.orNull)
    context.setVariable("headingTag", headingTag)
    context.setVariable("open", java.lang.Boolean.valueOf(open))
    context.setVariable("condition", condition.map(_.path).orNull)
    context.setVariable("operator", condition.map(c => c.operator.toString).orNull)

    templateEngine.process("nodes/fg-detail", context)
  }
}

object FgDetail extends FlowNodeParser {
  private val VALID_HEADING_TAGS = Set("h2", "h3", "h4", "h5", "h6")

  override def fromXml(
      fgDetailElement: Elem,
      flowNodeParser: FlowParser,
      parentTranslationContext: TranslationContext,
  ): FgDetail = {
    val summary = (fgDetailElement \ "summary").headOption match {
      case Some(summaryNode) => summaryNode.child.map(_.toString).mkString.strip
      case None              => ""
    }
    val useChevron = (fgDetailElement \@ "icon") == "chevron"
    val classAttribute = (fgDetailElement \@ "class").strip
    val detailsClass = if (classAttribute.nonEmpty) Some(classAttribute) else None
    val rawHeadingTag = (fgDetailElement \@ "heading-tag").strip.toLowerCase
    val headingTag = if (VALID_HEADING_TAGS.contains(rawHeadingTag)) rawHeadingTag else "h4"
    val open = (fgDetailElement \@ "open").strip.equalsIgnoreCase("true")
    val condition = Condition.getCondition(fgDetailElement, flowNodeParser.factDictionary)

    val translationContext = parentTranslationContext.forChildWithoutUniqueId(fgDetailElement.label)
    if (summary.nonEmpty) {
      translationContext.updateValue("summary", summary)
    }

    val childrenHtml = flowNodeParser.parseChildElements(fgDetailElement, translationContext, List("summary"))

    FgDetail(translationContext, childrenHtml, useChevron, detailsClass, headingTag, open, condition)
  }
}

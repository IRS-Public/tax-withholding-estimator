package gov.irs.twe.parser

import gov.irs.twe.TweTemplateEngine
import org.thymeleaf.context.Context
import scala.xml.Elem

case class Section(children: Seq[FlowNode]) extends FlowNode {
  override def html(templateEngine: TweTemplateEngine): String = {
    val childrenHtml = children.html(templateEngine)

    val context = new Context()
    context.setVariable("childrenHtml", childrenHtml)
    templateEngine.process("nodes/section", context)
  }
}
object Section extends FlowNodeParser {
  override def fromXml(section: Elem, flowParser: FlowParser, parentTranslationContext: TranslationContext): Section = {
    val children = flowParser.parseChildElements(section, parentTranslationContext)

    Section(children)
  }
}

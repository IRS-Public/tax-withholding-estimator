package gov.irs.twe.parser

import gov.irs.twe.parser.{ Condition, ConditionOperator }
import gov.irs.twe.parser.{ FgAlert, FlowNode, FlowNodeParser, FlowParser }
import gov.irs.twe.TweTemplateEngine
import org.thymeleaf.context.Context
import scala.xml.Elem

case class FgAlert(
    condition: Option[Condition],
    alertType: String,
    translationContext: TranslationContext,
    children: Seq[FlowNode],
) extends FlowNode {
  override def html(templateEngine: TweTemplateEngine): String = {
    val context = new Context()
    context.setVariable("condition", condition.map(_.path).orNull)
    context.setVariable("operator", condition.map(_.operator.toString).orNull)
    context.setVariable("alertType", alertType)
    val headingKey = translationContext.fullKey("heading")
    val heading = templateEngine.messageResolver.resolveMessage(headingKey)
    context.setVariable("heading", heading)
    val childrenHtml = children.html(templateEngine)
    context.setVariable("children", childrenHtml)

    templateEngine.process("nodes/fg-alert", context)
  }
}

object FgAlert extends FlowNodeParser {
  override def fromXml(
      fgAlertElement: Elem,
      flowParser: FlowParser,
      parentTranslationContext: TranslationContext,
  ): FgAlert = {
    val alertType = fgAlertElement \@ "alert-type"

    val heading = (fgAlertElement \ "heading").head.child.mkString.strip

    val conditionPath = fgAlertElement \@ "condition"
    val conditionOperator = fgAlertElement \@ "operator"
    val condition = Option.when(conditionPath.nonEmpty && conditionOperator.nonEmpty)(
      Condition(conditionPath, ConditionOperator.fromAttribute(conditionOperator)),
    )

    val translationContext = parentTranslationContext.forChildWithoutUniqueId(fgAlertElement.label)
    translationContext.updateValue("heading", heading)
    val children = flowParser.parseChildElements(fgAlertElement, translationContext, List("heading"))

    FgAlert(condition, alertType, translationContext, children)
  }
}

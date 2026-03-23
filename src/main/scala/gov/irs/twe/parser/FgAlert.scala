package gov.irs.twe.parser

import gov.irs.twe.parser.{ Condition, ConditionOperator }
import gov.irs.twe.parser.{ FgAlert, FlowNode, FlowNodeParser, FlowParser }
import gov.irs.twe.TweTemplateEngine
import org.thymeleaf.context.Context
import scala.xml.Elem

case class FgAlert(
    condition: Option[Condition],
    alertType: String,
    heading: String,
    children: Seq[FlowNode],
) extends FlowNode {
  override def html(templateEngine: TweTemplateEngine): String = {
    val context = new Context()
    context.setVariable("condition", condition.map(_.path).orNull)
    context.setVariable("operator", condition.map(_.operator.toString).orNull)
    context.setVariable("alertType", alertType)

    context.setVariable("heading", heading)
    val childrenHtml = children.html(templateEngine)
    context.setVariable("children", childrenHtml)

    templateEngine.process("nodes/fg-alert", context)
  }
}

object FgAlert extends FlowNodeParser {
  override def fromXml(fgAlertElement: Elem, flowParser: FlowParser, level: Int): FgAlert = {
    val alertType = fgAlertElement \@ "alert-type"

    val heading = (fgAlertElement \ "heading").head.child.mkString.trim
    val children = flowParser.parseChildElements(fgAlertElement, List("heading"), level)

    val conditionPath = fgAlertElement \@ "condition"
    val conditionOperator = fgAlertElement \@ "operator"
    val condition = Option.when(conditionPath.nonEmpty && conditionOperator.nonEmpty)(
      Condition(conditionPath, ConditionOperator.fromAttribute(conditionOperator)),
    )

    FgAlert(condition, alertType, heading, children)
  }
}

package gov.irs.twe.parser

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.TweTemplateEngine
import org.thymeleaf.context.Context

case class FgAlert(
    condition: Option[Condition],
    alertType: String,
    pageRoute: String,
    heading: String,
    children: List[xml.Node],
) {
  def html(templateEngine: TweTemplateEngine): String = {
    val context = new Context()
    context.setVariable("condition", this.condition.map(_.path).orNull)
    context.setVariable("operator", this.condition.map(_.operator.toString).orNull)
    context.setVariable("alertType", alertType)

    context.setVariable("heading", heading)
    context.setVariable("children", children.mkString)

    templateEngine.process("nodes/fg-alert", context)
  }
}

object FgAlert {
  def parse(node: xml.Node, pageRoute: String, factDictionary: FactDictionary): FgAlert = {
    val alertType = node \@ "alert-type"

    val heading = (node \ "heading").head.child.mkString.trim
    val childNodes = (node \ "_").filter(_.label != "heading")
    val children = childNodes.toList

    val conditionPath = node \@ "condition"
    val conditionOperator = node \@ "operator"
    val condition = Option.when(conditionPath.nonEmpty && conditionOperator.nonEmpty)(
      Condition(conditionPath, ConditionOperator.fromAttribute(conditionOperator)),
    )

    FgAlert(condition, alertType, pageRoute, heading, children)
  }
}

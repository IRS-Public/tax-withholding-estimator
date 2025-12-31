package gov.irs.twe.parser

import gov.irs.twe.TweTemplateEngine
import org.thymeleaf.context.Context

case class FgSectionGate(
    condition: String,
    operator: String,
    state: String,
) {
  def html(templateEngine: TweTemplateEngine): String = {

    val context = new Context()
    context.setVariable("gateCondition", this.condition)
    context.setVariable("gateOperator", this.operator)
    context.setVariable("gateState", this.state)
    context.setVariable("gateId", "section-gates." + condition + "-" + operator)

    templateEngine.process("nodes/fg-section-gate", context)
  }
}

object FgSectionGate {
  def parse(node: xml.Node): FgSectionGate = {
    val condition = node \@ "condition"
    val operator = node \@ "operator"
    val state = node \@ "state"

    FgSectionGate(condition, operator, state)
  }
}

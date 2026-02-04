package gov.irs.twe.parser

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.TweTemplateEngine
import org.thymeleaf.context.Context

enum FormType(val typeString: String):
  case W_4 extends FormType("w-4")
  case W_4P extends FormType("w-4p")

object FormType:
  def fromAttribute(typeString: String): FormType =
    values
      .find(it => it.typeString.equalsIgnoreCase(typeString))
      .getOrElse(throw new IllegalArgumentException(s"typeString $typeString was invalid."))

case class FgWithholdingAdjustments(
    path: String,
    condition: Option[Condition],
    formType: FormType,
) {
  def html(templateEngine: TweTemplateEngine): String = {

    val context = new Context()
    context.setVariable("path", path)
    context.setVariable("condition", condition.map(_.path).orNull)
    context.setVariable("operator", condition.map(_.operator.toString).orNull)

    formType match {
      case FormType.W_4  => templateEngine.process("nodes/fg-withholding-adjustments-w-4", context)
      case FormType.W_4P => templateEngine.process("nodes/fg-withholding-adjustments-w-4p", context)
    }
  }
}

object FgWithholdingAdjustments {
  def parse(node: xml.Node, factDictionary: FactDictionary): FgWithholdingAdjustments = {

    val path = node \@ "path"
    val condition = Condition.getCondition(node, factDictionary)
    val formType = FormType.fromAttribute((node \@ "form-type"))

    FgWithholdingAdjustments(path, condition, formType)
  }
}

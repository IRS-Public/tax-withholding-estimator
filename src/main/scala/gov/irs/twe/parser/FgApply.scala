package gov.irs.twe.parser

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.exceptions.InvalidFormConfig
import gov.irs.twe.parser.Utils.validateFact
import gov.irs.twe.TweTemplateEngine
import org.thymeleaf.context.Context
import scala.xml.Elem

case class FgApply(path: String, value: String) extends FlowNode {
  def html(templateEngine: TweTemplateEngine): String = {
    val context = new Context()
    context.setVariable("path", this.path)
    context.setVariable("value", this.value)
    templateEngine.process("nodes/fg-apply", context)
  }
}

object FgApply extends FlowNodeParser {
  override def fromXml(
      fgApplyElement: Elem,
      flowParser: FlowParser,
      parentTranslationContext: TranslationContext,
  ): FgApply = {
    val path = fgApplyElement \@ "path"
    if (path.isEmpty) {
      throw new InvalidFormConfig("fg-apply attribute `path` is required but was missing or empty")
    }
    validateFact(path, flowParser.factDictionary)

    val value = fgApplyElement \@ "value"
    if (value.isEmpty) {
      throw new InvalidFormConfig("fg-apply attribute `value` is required but was missing or empty")
    }

    FgApply(path, value)
  }
}

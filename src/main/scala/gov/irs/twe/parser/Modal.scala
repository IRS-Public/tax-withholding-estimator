package gov.irs.twe.parser
import gov.irs.twe.TweTemplateEngine
import org.thymeleaf.context.Context

case class Modal(
    id: String,
    modalHeadingKey: String,
    modalContentKey: String,
) {
  def html(templateEngine: TweTemplateEngine): String = {
    val context = new Context()
    context.setVariable("modalId", this.id)
    context.setVariable("modalHeading", modalHeadingKey)
    context.setVariable("modalContent", modalContentKey)

    templateEngine.process("nodes/modal-dialog", context)
  }
}

object Modal {
  def parse(node: xml.Node): Modal = {
    val id = node \@ "id"
    val modalHeadingKey = node \ "modal-heading" \@ "content-key"
    val modalContentKey = node \ "modal-content" \@ "content-key"

    Modal(id, modalHeadingKey, modalContentKey)
  }
}

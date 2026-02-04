package gov.irs.twe.parser
import gov.irs.twe.exceptions.InvalidFormConfig
import gov.irs.twe.TweTemplateEngine
import org.thymeleaf.context.Context

case class Modal(
    id: String,
    modalHeading: String,
    modalContent: String,
) {
  def html(templateEngine: TweTemplateEngine): String = {
    val context = new Context()
    context.setVariable("modalId", this.id)
    context.setVariable("modalHeading", modalHeading)
    context.setVariable("modalContent", modalContent)

    templateEngine.process("nodes/modal-dialog", context)
  }
}

object Modal {
  def parse(node: xml.Node): Modal = {
    val id = node \@ "id"
    if (id == null) { throw InvalidFormConfig(s"Modal is missing an ID") }

    val modalHeadingNode = (node \ "modal-heading").head
    if (modalHeadingNode.isEmpty) { throw InvalidFormConfig(s"Modal $id is missing a heading") }
    val modalHeading = modalHeadingNode.child.mkString

    val modalContentNode = (node \ "modal-content").head
    if (modalContentNode.isEmpty) { throw InvalidFormConfig(s"Modal $id is missing content") }
    val modalContent = modalContentNode.child.mkString

    Modal(id, modalHeading, modalContent)
  }
}

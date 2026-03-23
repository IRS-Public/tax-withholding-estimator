package gov.irs.twe.parser

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.exceptions.InvalidFormConfig
import scala.xml.Elem

enum FlowNodeType {
  case FG_ALERT
  case FG_COLLECTION
  case FG_DETAIL
  case FG_SET
  case FG_WITHHOLDING_ADJUSTMENTS
  case HTML
  case MODAL
  case PAGE
  case SECTION
}
object FlowNodeType:
  def fromLabel(label: String): FlowNodeType = label match
    // Named elements with custom parsing logic
    case "fg-alert"                   => FlowNodeType.FG_ALERT
    case "fg-collection"              => FlowNodeType.FG_COLLECTION
    case "fg-detail"                  => FlowNodeType.FG_DETAIL
    case "fg-set"                     => FlowNodeType.FG_SET
    case "fg-withholding-adjustments" => FlowNodeType.FG_WITHHOLDING_ADJUSTMENTS
    case "modal-dialog"               => FlowNodeType.MODAL
    case "page"                       => FlowNodeType.PAGE
    case "section"                    => FlowNodeType.SECTION
    // Treat all other elements as HTML
    case _ => FlowNodeType.HTML

case class FlowParser(
    factDictionary: FactDictionary,
) {
  def parseChildElements(parent: Elem, level: Int = 0): Seq[FlowNode] = {
    parseChildElements(parent, Seq.empty[String], level)
  }

  def parseChildElements(parent: Elem, excludedLabels: Seq[String], level: Int): Seq[FlowNode] = {
    val childElements = (parent \ "_").filter(c => !excludedLabels.contains(c.label))

    if (childElements.isEmpty) {
      throw InvalidFormConfig(s"Encountered an empty element for which there is no parser configured: $parent")
    }

    childElements.collect { case element: Elem =>
      parseElement(element, level + 1)
    }
  }

  private def parseElement(element: Elem, level: Int = 0): FlowNode = FlowNodeType.fromLabel(element.label) match {
    case FlowNodeType.FG_ALERT                   => FgAlert.fromXml(element, this, level)
    case FlowNodeType.FG_COLLECTION              => FgCollection.fromXml(element, this, level)
    case FlowNodeType.FG_DETAIL                  => FgDetail.fromXml(element, this, level)
    case FlowNodeType.FG_SET                     => FgSet.fromXml(element, this, level)
    case FlowNodeType.FG_WITHHOLDING_ADJUSTMENTS => FgWithholdingAdjustments.fromXml(element, this, level)
    case FlowNodeType.HTML                       => Html.fromXml(element, this, level)
    case FlowNodeType.MODAL                      => Modal.fromXml(element, this, level)
    case FlowNodeType.SECTION                    => Section.fromXml(element, this, level)
    case FlowNodeType.PAGE                       =>
      throw InvalidFormConfig(
        s"Encountered 'page' element outside of flow config root. Pages are only supported as top-level flow config elements.",
      )
  }
}

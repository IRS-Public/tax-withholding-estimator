package gov.irs.twe.parser

import gov.irs.twe.TweTemplateEngine
import scala.xml.Elem

trait FlowNode {
  def html(templateEngine: TweTemplateEngine): String
}

extension (flowNodes: Seq[FlowNode]) {
  def html(templateEngine: TweTemplateEngine): String = flowNodes.map(node => node.html(templateEngine)).mkString("")
}

trait FlowNodeParser {
  def fromXml(element: Elem, flowParser: FlowParser, level: Int): FlowNode
}

package gov.irs.twe.parser

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.exceptions.InvalidFormConfig
import gov.irs.twe.parser.Utils.validateFact
import gov.irs.twe.TweTemplateEngine
import org.thymeleaf.context.Context
import scala.collection.JavaConverters.asJavaIterableConverter

case class FgAlert(
    condition: Option[Condition],
    alertType: String,
    pageRoute: String,
    headingKey: String,
    bodyKeys: Seq[String],
) {
  def html(templateEngine: TweTemplateEngine): String = {
    val context = new Context()
    context.setVariable("condition", this.condition.map(_.path).orNull)
    context.setVariable("operator", this.condition.map(_.operator.toString).orNull)
    context.setVariable("alertType", alertType)

    context.setVariable("headingKey", headingKey)
    context.setVariable("bodyKeys", bodyKeys.asJava)

    templateEngine.process("nodes/fg-alert", context)
  }
}

object FgAlert {
  def getBodyNodes(node: xml.Node) = (node \ "p" ++ node \ "ul" ++ node \ "ol")

  def parse(node: xml.Node, pageRoute: String, factDictionary: FactDictionary): FgAlert = {
    val alertType = node \@ "alert-type"
    val slim = node.attribute("slim").getOrElse("false")
    val icon = node.attribute("icon").getOrElse("true")

    val defaultKeyBase = s"flow.${pageRoute.split("\\.")(0)}.alerts.${node \@ "alert-key"}"

    val headingKey = (node \ "heading").head
      .attribute("content-key")
      .asInstanceOf[Option[Seq[String]]]
      .getOrElse(Seq(s"${defaultKeyBase}.heading"))
      .head
    val bodyKeys = getBodyNodes(node).zipWithIndex
      .map((node, index) =>
        node
          .attribute("content-key")
          .asInstanceOf[Option[Seq[String]]]
          .getOrElse(Seq(s"${defaultKeyBase}.body.${index}-${node.label}")),
      )
      .head

    val condition = Condition.getCondition(node, factDictionary)

    FgAlert(condition, alertType, pageRoute, headingKey, bodyKeys)
  }
}

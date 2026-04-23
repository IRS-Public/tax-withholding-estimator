package gov.irs.twe.parser

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.exceptions.InvalidFormConfig
import gov.irs.twe.parser.Condition
import gov.irs.twe.parser.Utils.validateFact
import gov.irs.twe.TweTemplateEngine
import org.thymeleaf.context.Context
import scala.xml.Elem

case class FgCollection(
    path: String,
    disallowEmpty: String,
    condition: Option[Condition],
    translationContext: TranslationContext,
    children: Seq[FlowNode],
) extends FlowNode {
  def html(templateEngine: TweTemplateEngine): String = {
    val context = new Context()
    context.setVariable("path", path)
    val translationKeyBase = translationContext.fullKey()
    context.setVariable("disallowEmpty", disallowEmpty)
    val childrenHtml = children.html(templateEngine)
    context.setVariable("collectionFacts", childrenHtml)
    context.setVariable("condition", condition.map(_.path).orNull)
    context.setVariable("operator", condition.map(_.operator.toString).orNull)

    templateEngine.process("nodes/fg-collection", context)
  }
}

object FgCollection extends FlowNodeParser {
  override def fromXml(
      fgCollectionElement: Elem,
      flowParser: FlowParser,
      parentTranslationContext: TranslationContext,
  ): FgCollection = {
    val factDictionary = flowParser.factDictionary

    val path = fgCollectionElement \@ "path"
    val disallowEmpty = fgCollectionElement \@ "disallow-empty"
    val condition = Condition.getCondition(fgCollectionElement, factDictionary)

    validateFgCollection(path, factDictionary)

    val translationContext = parentTranslationContext.forChildWithId("collection" + path)

    val children = flowParser.parseChildElements(fgCollectionElement, translationContext)

    FgCollection(path, disallowEmpty, condition, translationContext, children)
  }

  private def validateFgCollection(path: String, factDictionary: FactDictionary): Unit = {
    validateFact(path, factDictionary)
    if (factDictionary.getDefinition(path).typeNode != "CollectionNode")
      throw InvalidFormConfig(s"Path $path must be of type CollectionNode")
  }
}

package gov.irs.twe.parser

import gov.irs.twe.exceptions.InvalidFormConfig
import gov.irs.twe.parser.Utils.optionString
import gov.irs.twe.TweTemplateEngine
import scala.util.matching.Regex
import scala.xml.Elem

case class Page(
    translationContext: TranslationContext,
    route: String,
    exclude: Boolean,
    children: Seq[FlowNode],
) extends FlowNode {
  val titleKey: String = translationContext.fullKey("title")

  def href(languageCode: String): String = {
    val languagePortion = if (languageCode == "en") "" else s"/$languageCode"
    val routePortion = if (route == "/") "/" else s"$route/"
    s"/app/tax-withholding-estimator$languagePortion$routePortion"
  }

  override def html(templateEngine: TweTemplateEngine): String = {
    val pageContent = children.html(templateEngine)
    // Coerce all fg-show nodes into open, empty tags because HTML doesn't allow custom, self-closing tags
    val regex = new Regex("""<fg-show ([^>]*)>""", "attributes")
    val pageHtml = regex.replaceAllIn(
      pageContent,
      m => s"<fg-show \\${m group "attributes"}></fg-show>",
    )

    pageHtml
  }
}

object Page extends FlowNodeParser {
  override def fromXml(page: Elem, flowParser: FlowParser, parentTranslationContext: TranslationContext): Page = {
    val route =
      optionString(page \@ "route").getOrElse(throw InvalidFormConfig("<page> is missing a route attribute"))
    val title =
      optionString(page \@ "title").getOrElse(throw InvalidFormConfig("<page> is missing a title attribute"))
    val exclude = (page \@ "exclude-from-stepper").toBooleanOption.getOrElse(false)

    val translationContext = parentTranslationContext.forChildWithId(route)
    translationContext.updateValue("title", title)

    val children = flowParser.parseChildElements(page, translationContext)
    Page(translationContext, route, exclude, children)
  }
}

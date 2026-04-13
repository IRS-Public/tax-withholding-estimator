package gov.irs.twe

import gov.irs.twe.Locale
import java.text.MessageFormat
import org.thymeleaf.context.{ Context, ITemplateContext }
import org.thymeleaf.messageresolver.AbstractMessageResolver
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import org.thymeleaf.TemplateEngine

case class TweMessageResolver(locale: Locale) extends AbstractMessageResolver:
  def createAbsentMessageRepresentation(
      context: ITemplateContext,
      origin: Class[?],
      key: String,
      messageParameters: Array[Object],
  ): String = {
    Log.warn(s"Could not find key ${key}")
    s"!!${key}!!"
  }

  override def resolveMessage(
      context: ITemplateContext,
      origin: Class[?],
      key: String,
      messageParameters: Array[Object],
  ): String =
    val rawMsg = locale.get(key).as[String].getOrElse(null)
    if (messageParameters != null && messageParameters.nonEmpty) {
      // MessageFormat.format makes it so ' are removed we would need to use '' if we want one to be displayed
      MessageFormat.format(rawMsg, messageParameters*)
    } else {
      rawMsg
    }

  /** Custom convenience overload for contractually obligated resolveMessage override
    */
  def resolveMessage(key: String): String = Option(resolveMessage(null, null, key, null)).getOrElse(s"??$key??")

class TweTemplateEngine(languageCode: String = "en") {
  private val resolver = new ClassLoaderTemplateResolver()
  resolver.setTemplateMode(TemplateMode.HTML)
  resolver.setCharacterEncoding("UTF-8")
  resolver.setPrefix("/twe/templates/")
  resolver.setSuffix(".html")

  private val locale = Locale(languageCode)
  private val templateEngine = new TemplateEngine()
  val messageResolver = TweMessageResolver(locale)
  templateEngine.setTemplateResolver(resolver)
  templateEngine.addMessageResolver(messageResolver)

  def process(templateName: String, context: Context): String =
    templateEngine.process(templateName, context)
}

package gov.irs.twe

import gov.irs.twe.Locale
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

  def resolveMessage(
      context: ITemplateContext,
      origin: Class[?],
      key: String,
      messageParameters: Array[Object],
  ): String =
    locale.get(key).as[String].getOrElse(null)

class TweTemplateEngine {
  private val resolver = new ClassLoaderTemplateResolver()
  resolver.setTemplateMode(TemplateMode.HTML)
  resolver.setCharacterEncoding("UTF-8")
  resolver.setPrefix("/twe/templates/")
  resolver.setSuffix(".html")

  private val locale = Locale("en")
  private val templateEngine = new TemplateEngine()
  private val messageResolver = TweMessageResolver(locale)
  templateEngine.setTemplateResolver(resolver)
  templateEngine.addMessageResolver(messageResolver)

  def process(templateName: String, context: Context): String =
    templateEngine.process(templateName, context)
}

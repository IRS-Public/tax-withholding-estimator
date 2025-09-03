package gov.irs.twe

import org.thymeleaf.context.Context
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import org.thymeleaf.TemplateEngine

class TweTemplateEngine {
  private val resolver = new ClassLoaderTemplateResolver()
  resolver.setTemplateMode(TemplateMode.HTML)
  resolver.setCharacterEncoding("UTF-8")
  resolver.setPrefix("/twe/templates/")
  resolver.setSuffix(".html")

  private val templateEngine = new TemplateEngine()
  templateEngine.setTemplateResolver(resolver)

  def process(templateName: String, context: Context): String =
    templateEngine.process(templateName, context)
}

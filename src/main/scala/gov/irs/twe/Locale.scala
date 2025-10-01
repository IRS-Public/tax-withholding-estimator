package gov.irs.twe

import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._
import io.circe.yaml.scalayaml.Parser
import scala.util.matching.Regex

case class Locale(code: String) {
  private val content = FileLoaderHelper.getLocaleContent(code)

  // Regex to match modal-link tags like <modal-link for="modal-name">text</modal-link>
  private val modalTagPattern: Regex = """<modal-link\s+for="([^"]+)">(.*?)</modal-link>""".r

  def get(key: String): Json = {
    val keyParts = key.split('.')

    val cursor = content.hcursor.downFields(keyParts.head, keyParts.tail: _*)

    cursor.as[String] match {
      case Right(str) => Json.fromString(convertModalTags(str))
      case Left(_)    => cursor.focus.getOrElse(Json.Null)
    }
  }

  private def convertModalTags(text: String): String =
    modalTagPattern.replaceAllIn(
      text,
      m => {
        val modalName = m.group(1)
        val linkText = m.group(2)
        s"""<a class="usa-link" href="#$modalName">$linkText</a>"""
      },
    )
}

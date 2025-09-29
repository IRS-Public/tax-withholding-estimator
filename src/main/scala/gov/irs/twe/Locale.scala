package gov.irs.twe

import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._
import io.circe.yaml.scalayaml.Parser

case class Locale(code: String) {
  private val content = FileLoaderHelper.getLocaleContent(code)

  def get(key: String) = {
    var keyParts = key.split('.')

    content.hcursor.downFields(keyParts.head, keyParts.tail*)
  }
}

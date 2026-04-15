package gov.irs.twe

import io.circe.*
import io.circe.syntax.*
import io.circe.yaml.Printer
import scala.collection.mutable
import scala.io.Source

val generatedFlowContentPath = os.pwd / "src" / "main" / "resources" / "twe" / "locales" / s"flow_en.yaml"
private def translatedFlowContentPath(languageCode: String) =
  os.pwd / "src" / "main" / "resources" / "twe" / "locales" / s"flow_$languageCode.yaml"

case class Locale(languageCode: String) {
  private val localeFilePath = s"twe/locales/${languageCode}.yaml"
  private val localeFile = Source.fromResource(localeFilePath)
  private val mainContent = yaml.scalayaml.Parser
    .parse(localeFile.reader())
    .getOrElse(throw new Exception(s"Failed to parse the content at $localeFilePath"))

  private val flowContentPath =
    if (languageCode == "en") generatedFlowContentPath else translatedFlowContentPath(languageCode)
  private val flowContentString = os.read(flowContentPath)
  private val flowContent = yaml.scalayaml.Parser
    .parse(flowContentString)
    .getOrElse(throw new Exception(s"Failed to parse the content at $flowContentPath"))

  def get(key: String): Json = {
    // Look at the main content file first, then the automatically-generated one
    val mainContentValue = GetValueFromLocaleJson(key, mainContent)
    mainContentValue match {
      case Some(value) => value
      case None        => GetValueFromLocaleJson(key, flowContent).getOrElse(Json.Null)
    }
  }
}

implicit val anyEncoder: Encoder[Any] = Encoder.instance {
  case m: mutable.LinkedHashMap[_, _] => Json.obj(m.map { case (k, v) => (k.toString, anyEncoder(v)) }.toSeq*)
  case s: String                      => Json.fromString(s)
}

/** Generate the flow_en.yaml locale file.
  *
  * @param translationMap
  *   A populated map of all of the key-value pairs for translations
  */
def generateFlowLocaleFile(translationMap: mutable.LinkedHashMap[String, Any]): Unit = {
  val json = translationMap.asJson
  val yamlString = Printer(dropNullKeys = true, preserveOrder = true).pretty(json)
  os.write.over(generatedFlowContentPath, s"# DO NOT EDIT, THIS IS A GENERATED FILE\n$yamlString")
  Log.info(s"Generated flow content at ${generatedFlowContentPath}")
}

private def GetValueFromLocaleJson(key: String, content: Json): Option[Json] = {
  val keyParts = key.split('.')
  val cursor = content.hcursor.downFields(keyParts.head, keyParts.tail*)

  cursor.as[String] match {
    case Right(str) => Some(Json.fromString(str))
    case Left(_)    => cursor.focus
  }
}

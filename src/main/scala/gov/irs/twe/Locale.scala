package gov.irs.twe

import gov.irs.twe.parser.FgAlert
import io.circe.*
import io.circe.generic.auto.deriveEncoder
import io.circe.syntax.*
import io.circe.yaml.Printer
import scala.collection.mutable
import scala.io.Source

// This is in /target for now, but I will figure out an idiomatic place for it to go where we can commit it.
// It isn't in /resources because that triggers a rebuild loop.
private val generatedFlowContentPath = os.pwd / "target" / s"flow_en.yaml"

case class Locale(languageCode: String) {
  private val localeFilePath = s"twe/locales/${languageCode}.yaml"
  private val localeFile = Source.fromResource(localeFilePath)
  private val mainContent = yaml.scalayaml.Parser
    .parse(localeFile.reader())
    .getOrElse(throw new Exception(s"Failed to parse the content at $localeFilePath"))

  private val flowContent = if (os.exists(generatedFlowContentPath)) {
    val flowContentString = os.read(generatedFlowContentPath)
    yaml.scalayaml.Parser
      .parse(flowContentString)
      .getOrElse(throw new Exception(s"Failed to parse the content at $generatedFlowContentPath"))
  } else {
    Json.obj()
  }

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
  os.write.over(generatedFlowContentPath, yamlString)
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

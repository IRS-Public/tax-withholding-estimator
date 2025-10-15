package gov.irs.twe

import io.circe.*
import io.circe.generic.auto.deriveEncoder
import io.circe.syntax.*
import io.circe.yaml.Printer
import scala.io.Source
import scala.util.matching.Regex

// This is in /target for now, but I will figure out an idiomatic place for it to go where we can commit it.
// It isn't in /resources because that triggers a rebuild loop.
private val generatedFlowContentPath = os.pwd / "target" / s"flow_en.yaml"

case class Locale(languageCode: String) {
  private val localeFilePath = s"twe/locales/${languageCode}.yaml"
  private val localeFile = Source.fromResource(localeFilePath)
  private val mainContent = yaml.scalayaml.Parser
    .parse(localeFile.reader())
    .getOrElse(throw new Exception(s"Failed to parse the content at $localeFilePath"))

  private val flowContentString = os.read(generatedFlowContentPath)
  private val flowContent = yaml.scalayaml.Parser
    .parse(flowContentString)
    .getOrElse(throw new Exception(s"Failed to parse the content at $localeFilePath"))

  def get(key: String): Json = {
    // Look at the main content file first, then the automatically-generated one
    val mainContentValue = GetValueFromLocaleJson(key, mainContent)
    mainContentValue match {
      case Some(value) => value
      case None        => GetValueFromLocaleJson(key, flowContent).getOrElse(Json.Null)
    }
  }
}

/** Generate the flow_en.yaml locale file.
  *
  * @param flowConfig
  *   A fully-resolved (no modules) Flow Config
  */
def generateFlowLocalFile(flowConfig: xml.Node): Unit = {
  val fgSets = (flowConfig \\ "fg-set").map { fgSet =>
    val path = fgSet \@ "path"
    val question = (fgSet \ "question").text.trim
    val optionsList = (fgSet \\ "option").map { option =>
      val value = option \@ "value"
      val name = option.text.trim
      val description = None
      (value -> OptionContent(name, description))
    }
    val options = if (optionsList.nonEmpty) Some(optionsList.toMap) else None
    val content = FgSetContent(question, options)
    (path -> content)
  }.toMap

  val contentJson = Json.obj("fg-sets" -> fgSets.asJson)
  val contentYaml = Printer(dropNullKeys = true, preserveOrder = true)
    .pretty(contentJson)

  os.write.over(generatedFlowContentPath, contentYaml, null, createFolders = false)
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

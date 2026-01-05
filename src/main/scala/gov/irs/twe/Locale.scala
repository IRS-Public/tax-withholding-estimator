package gov.irs.twe

import gov.irs.twe.parser.FgAlert
import io.circe.*
import io.circe.generic.auto.deriveEncoder
import io.circe.syntax.*
import io.circe.yaml.Printer
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

case class FlowContent(alerts: Map[String, FgAlertContent])

/** Generate the flow_en.yaml locale file.
  *
  * @param flowConfig
  *   A fully-resolved (no modules) Flow Config
  */
def generateFlowLocalFile(flowConfig: xml.Node): Unit = {
  // TODO: Move these to page groupings
  val fgSets = (flowConfig \\ "fg-set").map { fgSet =>
    val path = fgSet \@ "path"
    // We use mkString to preserve other tags, we trim to remove whitespace and new line characters when tags and content are on differnt lines
    val question = (fgSet \ "question").head.child.mkString.trim
    val optionsList = (fgSet \\ "option").map { option =>
      val value = option \@ "value"
      val name = option.head.child.mkString.trim
      val description = None
      (value -> OptionContent(name, description))
    }
    val options = if (optionsList.nonEmpty) Some(optionsList.toMap) else None
    val content = FgSetContent(question, options)
    (path -> content)
  }.toMap

  val flowContent = (flowConfig \\ "page").map { pageNode =>
    val alerts = (pageNode \\ "fg-alert").map { alertNode =>
      val alertKey = (alertNode \@ "alert-key")
      val headingContent = (alertNode \\ "heading").head.child.mkString.trim
      val bodyNodes = FgAlert.getBodyNodes(alertNode)
      var bodyContent = bodyNodes.zipWithIndex
        .map((node, index) => s"${index}-${node.label}" -> node.descendant.mkString(" ").trim)
        .toMap

      (alertKey -> FgAlertContent(headingContent, bodyContent))
    }.toMap

    val hasAlerts = alerts.nonEmpty

    (((pageNode \@ "route").split("\\.")(0)) -> (if (hasAlerts) Some(FlowContent(alerts)) else None))
  }.toMap

  val sectionGateContent = (flowConfig \\ "fg-section-gate").map { sectionGate =>
    val heading = (sectionGate \ "heading").head.child.mkString.trim
    val content = (sectionGate \ "content").head.child.mkString.trim
    val id = sectionGate \@ "condition" + "-" + sectionGate \@ "operator"
    (id -> Map("heading" -> heading, "content" -> content))
  }.toMap

  val contentJson =
    Json.obj("flow" -> flowContent.asJson, "fg-sets" -> fgSets.asJson, "section-gates" -> sectionGateContent.asJson)
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

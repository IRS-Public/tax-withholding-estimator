package gov.irs.twe

import gov.irs.twe.generatedFlowContentPath
import io.circe.{ Json, ParsingFailure }
import io.circe.yaml.parser
import org.scalatest.funspec.AnyFunSpec
import scala.io.Source
import scala.util.{ Failure, Success, Try }
import scala.xml.{ Elem, Node, XML }

class YamlValidatorSpec extends AnyFunSpec {
  private val EN_YAML = Source.fromResource("twe/locales/en.yaml").mkString
  private val ES_YAML = Source.fromResource("twe/locales/es.yaml").mkString

  // Can't access fromResource for the EN flow file because of sbt setting
  private val FLOW_EN_YAML = os.read(generatedFlowContentPath)
  private val FLOW_ES_YAML = Source.fromResource("twe/locales/flow_es.yaml").mkString

  private val ignoredTranslationKeys: Set[String] = Set(
    // If a translation key is verified to be correct, but causes the test to fail in a way that we cannot update the
    // test logic to account for, its full path can be added here:
    "/income.collection/jobs.fg-detail-bbdbfc.p-af9c2c",
    "/income.collection/socialSecuritySources./socialSecuritySources/*/monthlyIncome.question",
    "/income.collection/socialSecuritySources./socialSecuritySources/*/withheldRate.question", // ES exclusively uses /socialSecuritySources/*/notSpouses
    "/income./unemploymentIncomeWithholding.question",
    "/income.modal-job-gross-ytd-end-en.p-a181b2",
    "/income.modal-fed-tax-pay-period-en.ul-0.li-c119f5",
    "/adjustments.p-3269b3",
    "/adjustments.ul-1.li-05453e",
    "/adjustments.ul-4.li-a7cbed",
    "/adjustments.ul-4.li-e1fa4e",
    "/deductions.p-5ea630",
    "/deductions.p-b6c1cd",
    "/deductions.p-999102",
    "/deductions.p-10956c",
    "/additional-deductions.p-13d235",
    "/additional-deductions./wantsDerivedQBIDeduction.question",
    "/additional-deductions./qBIDeductionOverrideAmount.question",
  )
  private val ignoredAttributeValues = Set("href")

  describe("main yaml") {
    it("should have the same keys in en and sp") {
      val enKeys = parser.parse(EN_YAML).map(getAllKeys(_))
      val esKeys = parser.parse(ES_YAML).map(getAllKeys(_))
      findKeyDifferences(enKeys, esKeys)
    }

    it("each content key should include equivalent tags (and attributes) in en and sp") {
      findTagDifferences(parser.parse(EN_YAML), parser.parse(ES_YAML))
    }
  }

  describe("flow yaml") {
    it("should have the same keys in en and sp") {
      val enKeys = parser.parse(FLOW_EN_YAML).map(getAllKeys(_))
      val esKeys = parser.parse(FLOW_ES_YAML).map(getAllKeys(_))
      findKeyDifferences(enKeys, esKeys)
    }

    it("each content key should include equivalent tags (and attributes) in en and sp") {
      findTagDifferences(parser.parse(FLOW_EN_YAML), parser.parse(FLOW_ES_YAML))
    }
  }

  def getAllKeys(json: Json, prefix: String = ""): Set[String] = {
    json.fold(
      jsonNull = Set.empty,
      jsonBoolean = _ => Set.empty,
      jsonNumber = _ => Set.empty,
      jsonString = _ => Set.empty,
      // We don't have arrays so we can ignore this
      jsonArray = arr => Set.empty,
      jsonObject = obj =>
        obj.toList.flatMap { case (k, v) =>
          val path = if (prefix.isEmpty) k else s"$prefix.$k"
          Set(path) ++ getAllKeys(v, path)
        }.toSet,
    )
  }

  def findKeyDifferences(
      sourceKeys: Either[ParsingFailure, Set[String]],
      secondaryKeys: Either[ParsingFailure, Set[String]],
  ): Unit = {
    (sourceKeys, secondaryKeys) match {
      case (Right(k1), Right(k2)) =>
        var clue: List[String] = List.empty
        val missingInK2 = k1 -- k2
        if (missingInK2.nonEmpty) clue = clue :+ s"Missing in Spanish file: ${missingInK2.mkString(", ")}"
        val missingInK1 = k2 -- k1
        if (missingInK1.nonEmpty)
          clue = clue :+ s"Additional key(s) found in Spanish File: ${missingInK1.mkString(", ")}"
        if (missingInK1.nonEmpty || missingInK2.nonEmpty) {
          fail(s"Yaml Mismatch! ${clue.mkString(" ")}")
        }

      case (Left(e), _) => fail(s"Failed to parse File 1: ${e.getMessage}")
      case (_, Left(e)) => fail(s"Failed to parse File 2: ${e.getMessage}")
    }
  }

  case class TagSignature(name: String, attrs: Map[String, String])

  // Inexpensive pre-check used to skip XML parsing entirely for plain-text content.
  val CONTAINS_TAGS: scala.util.matching.Regex = """<[a-zA-Z]""".r

  /** Extract tags and their attributes for validation from a given content string */
  def extractTags(content: String): List[TagSignature] = {
    if (CONTAINS_TAGS.findFirstIn(content).isEmpty) List.empty
    else {
      val tempRoot = XML.loadString(s"<root>$content</root>")
      tempRoot.child.toList.flatMap(collectElements).map(e => TagSignature(e.label, e.attributes.asAttrMap))
    }
  }

  /** Recursively collect every Elem in a node's subtree, including the node itself if it's an Elem. */
  def collectElements(node: Node): List[Elem] = {
    val self: List[Elem] = node match {
      case e: Elem => List(e)
      case _       => List.empty
    }
    self ++ node.child.toList.flatMap(collectElements)
  }

  case class ComparisonContext(path: String, enContent: String, esContent: String)

  // Recursively walks both JSON trees in parallel, returning a ComparisonContext for every
  // leaf string pair that exists at the same path in both files.
  def collectTranslationComparisonContexts(en: Json, es: Json, prefix: String = ""): List[ComparisonContext] = {
    (en.asObject, es.asObject) match {
      case (Some(enJsonObject), Some(esJsonObject)) =>
        enJsonObject.toList.flatMap { case (key, enValueJson) =>
          val path = if (prefix.isEmpty) key else s"$prefix.$key"
          esJsonObject(key) match {
            // Only collect pairs with both English and Spanish content for validation.
            case Some(esValueJson) => collectTranslationComparisonContexts(enValueJson, esValueJson, path)
            case None              => List.empty // Ignore pairs where the Spanish value has not yet been added.
          }
        }
      case _ =>
        (en.asString, es.asString) match {
          case (Some(enContent), Some(esContent)) => List(ComparisonContext(prefix, enContent, esContent))
          case _                                  => List.empty
        }
    }
  }

  def findTagDifferences(
      enParsed: Either[ParsingFailure, Json],
      esParsed: Either[ParsingFailure, Json],
  ): Unit = {
    (enParsed, esParsed) match {
      case (Right(enJson), Right(esJson)) =>
        val tagDifferences = collectTranslationComparisonContexts(enJson, esJson)
          .filterNot(context => ignoredTranslationKeys.contains(context.path))
          .flatMap { context =>
            (
              Try(extractTags(context.enContent).map(normalizeForValidation).toSet),
              Try(extractTags(context.esContent).map(normalizeForValidation).toSet),
            ) match {
              case (Failure(e), _) =>
                Some(s"  ${context.path}:\n    Failed to parse EN content as XML: ${e.getMessage}")
              case (_, Failure(e)) =>
                Some(s"  ${context.path}:\n    Failed to parse ES content as XML: ${e.getMessage}")
              case (Success(enTags), Success(esTags)) =>
                if (enTags != esTags) Some(describeTagMismatch(context.path, enTags, esTags)) else None
            }
          }
        if (tagDifferences.nonEmpty) {
          fail(
            s"Tag mismatches found:\n${tagDifferences.mkString("\n\n")}",
          )
        }

      case (Left(e), _) => fail(s"Failed to parse File 1: ${e.getMessage}")
      case (_, Left(e)) => fail(s"Failed to parse File 2: ${e.getMessage}")
    }
  }

  /** Normalize a tag signature for cross-language equivalence comparison.
    *
    * For example, in English a single conditional element might be sufficient to produce a dynamic translation. In
    * another language, it might require multiple elements with the same condition and the same or different operators.
    */
  def normalizeForValidation(tag: TagSignature): TagSignature = {
    val validateableAttributes = tag.attrs.map { case (k, v) =>
      // Preserve the key but ignore the value for attributes we expect to be different between languages
      if (ignoredAttributeValues.contains(k)) k -> "" else k -> v
    }
    val normalizedAttributes = {
      // We only need to validate that conditional tags between languages use the same condition, but we can be operator agnostic
      if (validateableAttributes.contains("condition")) validateableAttributes - "operator"
      else validateableAttributes
    }
    tag.copy(attrs = normalizedAttributes)
  }

  /** For a given tag where we've identified a mismatch, attempt to describe the mismatch so that it's easy to triage
    * and fix
    */
  def describeTagMismatch(path: String, enTags: Set[TagSignature], esTags: Set[TagSignature]): String = {
    val onlyInEn = (enTags -- esTags).toList.sortBy(_.name)
    val onlyInEs = (esTags -- enTags).toList.sortBy(_.name)

    val sections = List(
      Option.when(onlyInEn.nonEmpty)(
        "    Tags present in EN but missing in ES:\n" +
          onlyInEn.map(t => s"      ${printTag(t)}").mkString("\n"),
      ),
      Option.when(onlyInEs.nonEmpty)(
        "    Tags present in ES but missing in EN:\n" +
          onlyInEs.map(t => s"      ${printTag(t)}").mkString("\n"),
      ),
    ).flatten

    s"  $path:\n${sections.mkString("\n")}"
  }

  def printTag(tag: TagSignature): String = {
    val attrs = tag.attrs.toList.sortBy(_._1).map { case (k, v) => s"""$k="$v"""" }
    if (attrs.isEmpty) s"<${tag.name}>" else s"<${tag.name} ${attrs.mkString(" ")}>"
  }
}

package gov.irs.twe

import io.circe.yaml.parser
import io.circe.Json
import io.circe.ParsingFailure
import org.scalatest.funspec.AnyFunSpec
import scala.io.Source

class YamlValidatorSpec extends AnyFunSpec {
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

  describe("main yaml") {
    it("should have the same keys in en and sp") {
      val enFile = Source.fromResource("twe/locales/en.yaml").mkString
      val esFile = Source.fromResource("twe/locales/es.yaml").mkString

      val enKeys = parser.parse(enFile).map(getAllKeys(_))
      val esKeys = parser.parse(esFile).map(getAllKeys(_))
      findKeyDifferences(enKeys, esKeys)
    }
  }
  describe("flow yaml") {
    it("should have the same keys in en and sp") {
      // Can't access fromResource because of sbt setting
      val enFile = os.read(generatedFlowContentPath)
      val esFile = Source.fromResource("twe/locales/flow_es.yaml").mkString

      val enKeys = parser.parse(enFile).map(getAllKeys(_))
      val esKeys = parser.parse(esFile).map(getAllKeys(_))
      findKeyDifferences(enKeys, esKeys)
    }
  }
}

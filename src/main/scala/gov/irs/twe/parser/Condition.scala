package gov.irs.twe.parser

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.exceptions.InvalidFormConfig
import gov.irs.twe.parser.Utils.optionString
import gov.irs.twe.parser.Utils.validateFact

// Building this out in anticipation that we will add other types of conditions, such as isComplete
enum ConditionOperator {
  case isTrue
  case isFalse
}

case class Condition(path: String, operator: ConditionOperator)

object Condition {
  def getCondition(node: xml.Node, factDictionary: FactDictionary): Option[Condition] = {
    // Validate that the condition, if it exists, is properly defined
    val path = optionString(node \@ "path")
    val ifTrue = optionString(node \@ "if-true")
    val ifFalse = optionString(node \@ "if-false")

    if (ifTrue.isDefined & ifFalse.isDefined) {
      throw InvalidFormConfig(s"Path $path has both an if-true condition and a if-false condition defined")
    }
    validateCondition(factDictionary, ifTrue)
    validateCondition(factDictionary, ifFalse)

    // We break down the friendly "if-true", "if-false" config attributes into a more standardized
    // "condition" and "operator" implementation
    if (ifTrue.isDefined) {
      return Option(Condition(ifTrue.get, ConditionOperator.isTrue))
    } else if (ifFalse.isDefined) {
      return Option(Condition(ifFalse.get, ConditionOperator.isFalse))
    }

    None
  }

  private def validateCondition(factDictionary: FactDictionary, conditionPath: Option[String]): Unit =
    if (conditionPath.isDefined) {
      val condition = conditionPath.get
      validateFact(condition, factDictionary)

      if (factDictionary.getDefinition(condition).isBoolean == false) {
        throw InvalidFormConfig(s"Condition $condition must be of type Boolean")
      }
    }
}

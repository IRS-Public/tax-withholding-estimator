package gov.irs.twe.parser

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.exceptions.InvalidFormConfig
import gov.irs.twe.parser.Utils.optionString

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

  // TODO roll this together with the other "is this fact real" validations
  private def validateCondition(factDictionary: FactDictionary, conditionPath: Option[String]): Unit = {
    // TODO validate the fact matches the input
    // Right now this just validates that the fact exists
    if (conditionPath.isDefined) {
      val condition = conditionPath.get
      if (factDictionary.getDefinition(condition) == null) {
        throw InvalidFormConfig(s"Condition $condition not found in the fact dictionary")
      }
    }
  }
}

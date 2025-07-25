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
    val ifCondition = optionString(node \@ "if")
    val ifNotCondition = optionString(node \@ "if-not")

    if (ifCondition.isDefined & ifNotCondition.isDefined) {
      throw InvalidFormConfig(s"Path $path has both an if condition and a ifnot condition defined")
    }
    validateCondition(factDictionary, ifCondition)
    validateCondition(factDictionary, ifNotCondition)

    // We break down the friendly "if", "ifnot" config attributes into a more standardized
    // "condition" and "operator" implementation
    if (ifCondition.isDefined) {
      return Option(Condition(ifCondition.get, ConditionOperator.isTrue))
    } else if (ifNotCondition.isDefined) {
      return Option(Condition(ifNotCondition.get, ConditionOperator.isFalse))
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

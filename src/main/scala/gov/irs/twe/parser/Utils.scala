package gov.irs.twe.parser

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.exceptions.InvalidFormConfig

object Utils {
  def optionString(string: String): Option[String] =
    if (string.isEmpty) None else Option(string)

  /** Validate that the fact exists
    *
    * @param path
    *   some fact eg: /totalIncome
    * @param factDictionary
    */
  def validateFact(path: String, factDictionary: FactDictionary): Unit = {
    val factDefinition = factDictionary.getDefinition(path)
    if (factDefinition == null) {
      throw InvalidFormConfig(s"$path not found in the fact dictionary")
    }
  }
}

package gov.irs.twe.factDictionary.scenarios

import gov.irs.factgraph.types.Dollar
import gov.irs.twe.scenarios.loadScenarioFromCsv
import org.scalatest.funsuite.AnyFunSuite
import os.Path

class ComplexScenarioSpec extends AnyFunSuite {
  val CSV_ROOT: Path = os.pwd / "src" / "test" / "resources" / "csv"
  val BATCH_ONE: Path = CSV_ROOT / "complex-scenarios-batch-1.csv"

  test("MFJ, High Income, 1 child, Multi, Car loan") {
    val scenario = loadScenarioFromCsv(BATCH_ONE, 1)

    val expectedAgi = Dollar(scenario.getInput("AGI"))
    val expectedLine4c = Dollar(scenario.getInput("W-4 Line4cAmount1"))

    assert(scenario.getFact("/agi") == expectedAgi)
    assert(scenario.getFact("/jobSelectedForExtraWithholding/w4Line4c") == expectedLine4c)
  }

  test("Single, low wages, EITC, no dependents") {
    val scenario = loadScenarioFromCsv(BATCH_ONE, 3)

    val expectedAgi = Dollar(scenario.getInput("AGI"))
    val expectedTax = Dollar(scenario.getInput("anticipatedTaxB4RefundableCredits"))

    assert(scenario.getFact("/agi") == expectedAgi)
    assert(scenario.getFact("/totalTax") == expectedTax)
    assert(scenario.getFact("/totalPayments") == Dollar(498)) // Note: this is off by $2 from the spreadsheet
    assert(scenario.getFact("/jobSelectedForExtraWithholding/w4Line4c") == Dollar(0))
  }
}

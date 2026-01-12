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

  test("Single, SS, part time, senior deduction") {
    val scenario = loadScenarioFromCsv(BATCH_ONE, 2)

    val expectedAgi = Dollar(scenario.getInput("AGI"))
    val expectedLine4c = Dollar(scenario.getInput("W-4 Line4cAmount1"))
    val expectedSeniorDeduction = Dollar(scenario.getInput("Additional Elder Deduction (70103)"))

    assert(scenario.getFact("/agi") == expectedAgi)
    assert(scenario.getFact("/jobSelectedForExtraWithholding/w4Line4c") == expectedLine4c)
    assert(scenario.getFact("/seniorDeduction") == expectedSeniorDeduction)
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

  test("Married filing jointly, salary, 1 child, multiple incomes, car loan interest") {
    val scenario = loadScenarioFromCsv(BATCH_ONE, 7)

    val expectedIncomeTotal = Dollar(scenario.getInput("Net pre-tax income"))
    val expectedAdjustmentsToIncome = Dollar(scenario.getInput("Total Adjustments"))
    val expectedAgi = Dollar(scenario.getInput("AGI"))
    val expectedTotalDeductions = Dollar(scenario.getInput("Total standard or itemized deductions"))
    val expectedTaxableIncome = Dollar(scenario.getInput("Taxable income"))
    val expectedTotalNonRefundableCredits = Dollar(scenario.getInput("Total non-refundable credits"))
    val expectedTotalRefundableCredits = Dollar(scenario.getInput("Total refundable credits"))
    val expectedTotalTax = Dollar(scenario.getInput("Income tax before refundable credits"))
    val expectedTotalOwed = Dollar(scenario.getInput("Total tax after refundable credits"))
    val expectedLine4c = Dollar(scenario.getInput("W-4 Line4cAmount1"))

    assert(scenario.getFact("/incomeTotal") == expectedIncomeTotal)
    assert(scenario.getFact("/adjustmentsToIncome") == expectedAdjustmentsToIncome)
    assert(scenario.getFact("/agi") == expectedAgi)
    assert(scenario.getFact("/totalDeductions") == expectedTotalDeductions)
    assert(scenario.getFact("/taxableIncome") == expectedTaxableIncome)
    assert(scenario.getFact("/totalNonRefundableCredits") == expectedTotalNonRefundableCredits)
    assert(scenario.getFact("/totaRefundableCredits") == expectedTotalRefundableCredits)
    assert(scenario.getFact("/totalTax") == Dollar(34662)) // note: this is off by $2 from spreadsheet
    assert(scenario.getFact("/totalOwed") == expectedTotalOwed)
    assert(scenario.getFact("/jobSelectedForExtraWithholding/w4Line4c") == Dollar(595)) // note: $1 off
  }
}

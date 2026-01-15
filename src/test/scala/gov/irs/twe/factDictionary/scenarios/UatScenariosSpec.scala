package gov.irs.twe.factDictionary.scenarios

import gov.irs.factgraph.types.Dollar
import gov.irs.twe.scenarios
import org.scalatest.{ funsuite, TestData }
import org.scalatest.funsuite.AnyFunSuite
import os.Path

// UAT testing scenarios
// Each tests pulls a specific scenario from the UAT testing sheet and asserts key values
// The test name must exactly match the "Key scenario feature" row of the spreadsheet
class UatScenariosSpec extends funsuite.FixtureAnyFunSuite {
  val CSV_ROOT: Path = os.pwd / "src" / "test" / "resources" / "csv"
  val UAT_SHEET: Path = CSV_ROOT / "twe-uat-2026-01-13.csv"

  case class FixtureParam(scenario: scenarios.Scenario)

  // Calls each test with a "fixture" parameter that contains the scenario named by the test
  // https://www.scalatest.org/scaladoc/3.2.19/org/scalatest/funsuite/AnyFunSuite.html
  def withFixture(test: OneArgTest) = {
    val scenario = scenarios.loadScenarioByName(UAT_SHEET, test.name)
    val fixture = FixtureParam(scenario)
    withFixture(test.toNoArgTest(fixture))
  }

  // Column Z
  test("MFJ, High Income, 1 child, Multi, Car loan") { td =>
    val scenario = td.scenario
    val expectedAgi = Dollar(scenario.getInput("AGI"))
    val expectedLine4c = Dollar(scenario.getInput("W-4 Line4cAmount1"))

    assert(scenario.getFact("/agi") == expectedAgi)
    assert(scenario.getFact("/jobSelectedForExtraWithholding/w4Line4c") == expectedLine4c)
  }
  // Column AA
  test("Single, SS, part time, senior deduction") { td =>
    val scenario = td.scenario
    val expectedAgi = Dollar(scenario.getInput("AGI"))
    val expectedLine4c = Dollar(scenario.getInput("W-4 Line4cAmount1"))
    val expectedSeniorDeduction = Dollar(scenario.getInput("Additional Elder Deduction (70103)"))

    assert(scenario.getFact("/agi") == expectedAgi)
    assert(scenario.getFact("/jobSelectedForExtraWithholding/w4Line4c") == expectedLine4c)
    assert(scenario.getFact("/seniorDeduction") == expectedSeniorDeduction)
  }

  // Column AB
  test("Single, low wages, EITC, no dependents") { td =>
    val scenario = td.scenario
    val expectedAgi = Dollar(scenario.getInput("AGI"))
    val expectedTax = Dollar(scenario.getInput("anticipatedTaxB4RefundableCredits"))

    assert(scenario.getFact("/agi") == expectedAgi)
    assert(scenario.getFact("/totalTax") == expectedTax)
    assert(scenario.getFact("/totalPayments") == Dollar(498)) // Note: this is off by $2 from the spreadsheet
    assert(scenario.getFact("/jobSelectedForExtraWithholding/w4Line4c") == Dollar(0))
  }

  // Column D
  test("Married filing jointly, salary, 1 child, multiple incomes, car loan interest") { td =>
    val scenario = td.scenario

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

  // Column AF
  test("Single, salary, full deduction") { td =>
    val scenario = td.scenario

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
    assert(scenario.getFact("/totalTax") == expectedTotalTax)
    assert(scenario.getFact("/totalOwed") == expectedTotalOwed)
    assert(scenario.getFact("/jobSelectedForExtraWithholding/w4Line4c") == expectedLine4c)
  }

  // Column AG
  test("Single, salary, partial deduction, standard") { td =>
    val scenario = td.scenario

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
    assert(scenario.getFact("/totalTax") == expectedTotalTax)
    assert(scenario.getFact("/totalOwed") == expectedTotalOwed)
    assert(scenario.getFact("/jobSelectedForExtraWithholding/w4Line4c") == expectedLine4c)
  }
}

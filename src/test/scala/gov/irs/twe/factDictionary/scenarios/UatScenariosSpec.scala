package gov.irs.twe.factDictionary.scenarios

import gov.irs.factgraph.types.Dollar
import gov.irs.twe.scenarios
import gov.irs.twe.scenarios.Scenario
import org.scalatest.funsuite
import os.Path
import scala.math.Fractional.Implicits.infixFractionalOps

/* UAT Testing Scenarios
 *
 * Each test pulls a scenario column from the UAT Spreadsheet, converts it into a Fact Graph, then
 * verifies that the Fact Graph calculation matches both the spreadsheet and an expected value.
 *
 * The column chosen automatically corresponds to the test name, which matches the value on Row 2 of
 * the spreadsheet ("Key scenario feature"). For instance, the scenario "Single filer, new job" is
 * the fifth column of the spreadsheet (Column E in Excel/LibreOffice). The test harness finds that
 * column based on the test name, and then loads passes it to the test for assertions.
 *
 * We have two types of assertions: `assertEquals` and `assertOffset`. Asserting that the fact graph calculation differs
 * from the spreadsheet by some amount makes no normative statements about whether the fact graph or the
 * spreadsheet is "correct" in its estimation value; it merely reifies that the disparity exists.
 *
 * If you want to add a new test, you might need to edit `Scenario.scala`, which owns the mappings between rows from
 * the spreadsheet and fact shapes.
 */
class UatScenariosSpec extends funsuite.FixtureAnyFunSuite {
  val CSV_ROOT: Path = os.pwd / "src" / "test" / "resources" / "csv"
  val UAT_SHEET: Path = CSV_ROOT / "twe-uat-2026-01-22.csv"

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
    scenario.assertEquals("/agi", 242571)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 52)
  }

  // Column AA
  test("Single, SS, part time, senior deduction") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 32880)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 1207)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 0)
    // Scenario-specific assertions
    scenario.assertEquals("/seniorDeduction", 6000)
  }

  // Column AB
  test("Single, low wages, EITC, no dependents") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 13000)
    scenario.assertEquals("/totalTax", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 0)
    // Scenario-specific assertions
    scenario.assertOffset("/earnedIncomeCredit", 498, 2)
  }

  // Column D
  test("Married filing jointly, salary, 1 child, multiple incomes, car loan interest") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 252000)
    scenario.assertEquals("/totalOwed", 34662)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 0)
    scenario.assertOffset("/jobSelectedForExtraWithholding/w4Line4c", 595, -1)
    // Scenario-specific assertions
    scenario.assertEquals("/qualifiedPersonalVehicleLoanInterestDeduction", 4600)
  }

  // Column AC
  test("SE, Wages, QBI") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 79881)
    scenario.assertOffset("/totalOwed", 11760, 1)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 209)
    scenario.assertOffset("/qualifiedBusinessIncomeDeduction", 5576.20, -0.2)
  }

  // Column AF
  test("Single, salary, full deduction") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 52000)
    scenario.assertEquals("/totalOwed", 3097)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 1109)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 0)
  }

  // Column AG
  test("Single, salary, partial deduction, standard") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 112000)
    scenario.assertEquals("/totalTax", 14529)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 22)
  }

  // Column AM
  test("HH, pension, partial deduction, standard") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 84000)
    scenario.assertEquals("/totalTax", 1279)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 6493)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 0)
  }

  // Column AJ
  test("MFJ, salary, full deduction, standard") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 81500)
    scenario.assertEquals("/totalTax", 4505)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 1214)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 2500)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 0)
  }

  // Column AN
  test("MFS, wage, partial deduction, itemized") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 168000)
    scenario.assertEquals("/seniorDeduction", 420)
    // this scenario expects users to choose their deduction method in spite of deduction values.
    // user selection of dediction method is not supported in TWE 2.0.
    // this scenario is effectively out of scope, and therfore assertions on W4 values cannot be made.
  }

  // Column BE
  test("Single, phasedout deduction") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 104000)
    scenario.assertEquals("/totalTax", 12472)
    scenario.assertEquals("/qualifiedPersonalVehicleLoanInterestDeduction", 7200)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 0)
    // scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 0) // incomplete fact. resolved by #1186
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 550)
  }

  // Column BF
  test("Single, zero deduction") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 152000)
    scenario.assertEquals("/tentativeTaxFromTaxableIncomeWithoutNetGains", 25214)
    scenario.assertEquals("/studentLoanInterestDeduction", 0)
    scenario.assertEquals("/qualifiedPersonalVehicleLoanInterestDeduction", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 3512)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 2000)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 0)
  }

  // Column BG
  test("MFS, phased out, no SSN, CTC") { td =>
    val scenario = td.scenario
    scenario.graph.set("/ctcEligibleDependents", 0)
    scenario.assertEquals("/agi", 104000)
    scenario.assertEquals("/tentativeTaxFromTaxableIncomeWithoutNetGains", 12472)
    scenario.assertEquals("/qualifiedPersonalVehicleLoanInterestDeduction", 7200)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 283)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 0)
    // scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 0) // incomplete fact. resolved by #1186
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 0)
  }

  // Column BH
  test("MFJ, max amount deduction") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 104000)
    scenario.assertEquals("/tentativeTaxFromTaxableIncomeWithoutNetGains", 6923)
    scenario.assertEquals("/qualifiedPersonalVehicleLoanInterestDeduction", 10000)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 1829)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 0)
    // scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 0) // incomplete fact. resolved by #1186
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 0)
  }

  // Column BI
  test("HH,  phased out deduction, CTC, QBI") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 172587)
    scenario.assertEquals("/tentativeTaxFromTaxableIncomeWithoutNetGains", 25520)
    scenario.assertEquals("/qualifiedPersonalVehicleLoanInterestDeduction", 0)
    scenario.assertOffset("/qualifiedBusinessIncomeDeduction", 3717.40, -0.4)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 14870)
    // scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 0) // incomplete fact. resolved by #1186
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 260)
  }
}

/*
 * Extend the scenario object with some convenient assertions.
 * These assertion functions allow us to write tests that plainly state what values we expect for each scenario,
 * while doing the work of comparing those values to the spreadsheet automatically.
 */
extension (scenario: Scenario) {
  /*
   * Verifies that the fact, the spreadsheet value, and a hard-coded dollar amount are all equal.
   */
  def assertEquals(factPath: String, expectedValue: Double): Unit = {
    val fact = scenario.getFact(factPath).asInstanceOf[Dollar]
    val (inputName, inputValue) = scenario.getExpectedSheetValueByFactPath(factPath)
    val sheetInput = Dollar(inputValue)
    if (fact != sheetInput) throw Exception(s"$factPath ($fact) did not match CSV $inputName ($sheetInput)")
    if (fact != Dollar(expectedValue))
      throw Exception(s"$factPath ($fact) did not match expected value ($expectedValue)")
  }

  /*
   * Verifies that the fact and a hard-coded dollar amount are all equal, and that the spreadsheet value is off by a
   * specific dollar amount.
   */
  def assertOffset(factPath: String, expectedValue: Double, offset: Double): Unit = {
    val dollarOffset = Dollar(offset)
    val fact = scenario.getFact(factPath).asInstanceOf[Dollar]
    val (inputName, inputValue) = scenario.getExpectedSheetValueByFactPath(factPath)
    val sheetInput = Dollar(inputValue)
    if (fact + dollarOffset != sheetInput)
      throw Exception(s"$factPath ($fact) + offset ($dollarOffset) did not match CSV $inputName ($sheetInput)")
    if (fact != Dollar(expectedValue))
      throw Exception(s"$factPath ($fact) did not match expected value ($expectedValue)")
  }
}

/* UAT Testing Scenarios
 *
 * Each test pulls a scenario column from the UAT Spreadsheet, converts it into a Fact Graph, then
 * verifies that the Fact Graph calculation matches both the spreadsheet and an expected value.
 *
 * The column chosen automatically corresponds to the test name, which matches the value on row B of
 * the spreadsheet. For instance, the scenario "Single filer, new job" is the fifth column of the
 * spreadsheet (Column E in Excel/LibreOffice). The test harness finds that column based on the test
 * name, and then loads passes it to the test for assertions.
 *
 * We have two assertions: `assertEquals` and `assertOffset`. Asserting that the fact graph
 * calculation differs from the spreadsheet by some amount makes no normative statements about
 * whether the fact graph or the spreadsheet is "correct" in its estimation value; it merely reifies
 * that the disparity exists.
 *
 * These assertions also require the tester to hard-code the expected value. This ensures that the
 * test is actually testing the scenario that they think it is. It also aids with merge conflicts:
 * without it, big subsections of the test bodies look identical to git and it gets confused.
 *
 * If you want to add a new test, you might need to edit `Scenario.scala`, which owns the mappings
 * between rows from the spreadsheet and fact paths.
 */
package gov.irs.twe.factDictionary.scenarios
import gov.irs.factgraph.types.Dollar
import gov.irs.twe.scenarios
import gov.irs.twe.scenarios.{ JOB_1_ID, JOB_2_ID, JOB_3_ID }
import gov.irs.twe.scenarios.Scenario
import org.scalatest.funsuite
import os.Path
import scala.math.Fractional.Implicits.infixFractionalOps

class UatScenariosSpec extends funsuite.FixtureAnyFunSuite {
  val CSV_ROOT: Path = os.pwd / "src" / "test" / "resources" / "csv"
  val UAT_SHEET: Path = CSV_ROOT / "twe-uat-split-withholdings-2026-04-30.csv"

  case class FixtureParam(scenario: scenarios.Scenario)

  // Calls each test with a "fixture" parameter that contains the scenario named by the test
  // https://www.scalatest.org/scaladoc/3.2.19/org/scalatest/funsuite/AnyFunSuite.html
  def withFixture(test: OneArgTest) = {
    val scenario = scenarios.loadScenarioByName(UAT_SHEET, test.name)
    val fixture = FixtureParam(scenario)
    withFixture(test.toNoArgTest(fixture))
  }

  // Column B
  test("Single, OB3, itemized") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/incomeTotal", 156000)
    scenario.assertEquals("/agi", 154587)
    scenario.assertEquals("/tentativeTaxFromTaxableIncome", 17910)
    scenario.assertEquals("/stateAndLocalTaxDeduction", 30000)
    scenario.assertEquals("/totalTaxNetRefundableCredits", 14136)
    scenario.assertEquals("/totalEndOfYearProjectedWithholding", 13600)
    scenario.assertEquals("/tentativeTaxNetNonRefundableCredits", 11310)
    scenario.assertEquals("/qualifiedTipDeduction", 9600)
    scenario.assertEquals("/totalNonRefundableCredits", 6600)
    scenario.assertEquals("/totalCtcAndOdc", 6600)
    scenario.assertEquals("/overtimeCompensationDeduction", 4600)
    scenario.assertEquals("/qualifiedBusinessIncomeDeduction", 3717)
    scenario.assertEquals("/selfEmploymentTax", 2826)
    scenario.assertEquals("/totalRefundableCredits", 0)
    scenario.assertEquals("/qualifiedPersonalVehicleLoanInterestDeduction", 0)

    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#$JOB_1_ID/w4Line4bWithSplitWithholdingStrategy", 0)
    scenario.assertOffset(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 10, 2)

    scenario.assertEquals(s"/jobs/#${JOB_2_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_2_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_2_ID}/w4Line4bWithSplitWithholdingStrategy", 14455)
    scenario.assertOffset(s"/jobs/#${JOB_2_ID}/w4Line4cWithSplitWithholdingStrategy", 5, -1)

    scenario.assertEquals("/additionalMedicareTax", 0)
  }

  // Column C
  test("Single, OB3, itemized, no SSN") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/incomeTotal", 156000)
    scenario.assertEquals("/agi", 154587)
    scenario.assertEquals("/taxableIncome", 120870)
    scenario.assertEquals("/standardOrItemizedDeduction", 30000)
    scenario.assertEquals("/stateAndLocalTaxDeduction", 30000)
    scenario.assertEquals("/tentativeTaxFromTaxableIncome", 21607)
    scenario.assertEquals("/totalTaxNetRefundableCredits", 24433)
    // scenario.assertEquals("/totalEndOfYearProjectedWithholding", 16600)
    scenario.assertEquals("/tentativeTaxNetNonRefundableCredits", 21607)
    scenario.assertEquals("/qualifiedTipDeduction", 0)
    scenario.assertEquals("/totalNonRefundableCredits", 0)
    scenario.assertEquals("/totalCtcAndOdc", 0)
    scenario.assertEquals("/overtimeCompensationDeduction", 0)
    scenario.assertEquals("/qualifiedBusinessIncomeDeduction", 3717)
    scenario.assertEquals("/selfEmploymentTax", 2826)
    scenario.assertEquals("/totalRefundableCredits", 0)
    scenario.assertEquals("/qualifiedPersonalVehicleLoanInterestDeduction", 0)
    scenario.assertEquals("/additionalMedicareTax", 0)

    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 0)
    scenario.assertOffset(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 113, 22)

    scenario.assertEquals(s"/jobs/#${JOB_2_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_2_ID}/w4Line4aWithSplitWithholdingStrategy", 970)
    scenario.assertEquals(s"/jobs/#${JOB_2_ID}/w4Line4bWithSplitWithholdingStrategy", 0)
    scenario.assertOffset(s"/jobs/#${JOB_2_ID}/w4Line4cWithSplitWithholdingStrategy", 56, -17)
  }

  // Column R
  test("MFJ, high itemized deductions (mortgage interest, SALT, charity, medical)") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 820000)
    scenario.assertEquals("/stateAndLocalTaxDeduction", 10000)
    scenario.assertEquals("/medicalAndDentalExpensesTotal", 3500)
    scenario.assertEquals("/standardOrItemizedDeduction", 41527)
    scenario.assertEquals("/taxableIncome", 778473)
    scenario.assertEquals("/additionalMedicareTax", 5130)
    scenario.assertEquals("/totalNonRefundableCredits", 5000)
    scenario.assertEquals("/totalEndOfYearProjectedWithholding", 154000)
    scenario.assertEquals("/totalTaxNetRefundableCredits", 210330)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 9327)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 3052)
  // Manual Pub15 validation - pending multi-year withholding logic
  }

  // Column S
  test("Single, wages + tips + overtime + car loan interest") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/incomeTotal", 104000)
    scenario.assertEquals("/agi", 104000)
    scenario.assertEquals("/qualifiedTipDeduction", 10000)
    scenario.assertEquals("/qualifiedPersonalVehicleLoanInterestDeduction", 8200)
    scenario.assertEquals("/overtimeCompensationDeduction", 6000)
    scenario.assertEquals("/qualifiedBusinessIncomeDeduction", 0)
    scenario.assertEquals("/stateAndLocalTaxDeduction", 8000)
    scenario.assertEquals("/taxableIncome", 63700)
    scenario.assertEquals("/tentativeTaxFromTaxableIncome", 8732)
    scenario.assertEquals("/totalTaxNetRefundableCredits", 8732)
    scenario.assertEquals("/totalEndOfYearProjectedWithholding", 13000)
    scenario.assertEquals("/tentativeTaxNetNonRefundableCredits", 8732)
    scenario.assertOffset(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 1275, -1)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 24200)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 0)
  }

  // Column X
  test("MFS, No tax on tips, No tax on overtime, SALT, phased out") { td =>
    val scenario = td.scenario
    scenario.graph.set("/wantsStandardDeduction", false)
    scenario.assertEquals("/incomeTotal", 256000)
    scenario.assertEquals("/agi", 256000)
    scenario.assertEquals("/taxableIncome", 236325)
    scenario.assertEquals("/standardOrItemizedDeduction", 19675)
    scenario.assertEquals("/stateAndLocalTaxDeduction", 19675)
    scenario.assertEquals("/tentativeTaxFromTaxableIncome", 52080)
    scenario.assertEquals("/totalTaxNetRefundableCredits", 53259)
    scenario.assertEquals("/totalEndOfYearProjectedWithholding", 64800)
    scenario.assertEquals("/tentativeTaxNetNonRefundableCredits", 52080)
    scenario.assertEquals("/qualifiedTipDeduction", 0)
    scenario.assertEquals("/totalNonRefundableCredits", 0)
    scenario.assertEquals("/totalCtcAndOdc", 0)
    scenario.assertEquals("/overtimeCompensationDeduction", 0)
    scenario.assertEquals("/qualifiedBusinessIncomeDeduction", 0)
    scenario.assertEquals("/selfEmploymentTax", 0)
    scenario.assertEquals("/seniorDeduction", 0)
    scenario.assertEquals("/totalRefundableCredits", 0)
    scenario.assertEquals("/qualifiedPersonalVehicleLoanInterestDeduction", 0)
    scenario.assertEquals("/additionalMedicareTax", 1179)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertOffset(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 2002, -16)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 278)
    scenario.assertEquals(s"/jobs/#${JOB_2_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_2_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertOffset(s"/jobs/#${JOB_2_ID}/w4Line4bWithSplitWithholdingStrategy", 1573, 16)
    scenario.assertEquals(s"/jobs/#${JOB_2_ID}/w4Line4cWithSplitWithholdingStrategy", 111)
  }

  // Column U
  // pending updated spreadsheet with derivation for LLC
  test("MFJ, college student dependent + tuition payments") { td =>
    val scenario = td.scenario
    scenario.graph.set("/odcEligibleDependents", 1)
    scenario.assertEquals("/incomeTotal", 104000)
    scenario.assertEquals("/agi", 104000)
    scenario.assertEquals("/taxableIncome", 71800)
    scenario.assertEquals("/totalCtcAndOdc", 500)
    scenario.assertOffset("/americanOpportunityCredit", 1000, -100)
    scenario.assertOffset("/lifetimeLearningCredit", 2000, -650)
    scenario.assertOffset("/totalRefundableCredits", 1000, -100)
    scenario.assertEquals("/additionalMedicareTax", 0)
    scenario.assertEquals("/selfEmploymentTax", 0)
    scenario.assertEquals("/netInvestmentIncomeTax", 0)
    // Should be 2695, but the differences in the the above credits and totalNonRefundableCredits
    // appear to result in a different `/taxGap` which affects Line 3.
    scenario.assertOffset(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 3670, -975)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 0)

  }

  // Column AK
  test("MFJ,  salary, pension, zero deduction, itemized") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/incomeTotal", 262000)
    scenario.assertEquals("/agi", 262000)
    scenario.assertEquals("/taxableIncome", 197960)
    scenario.assertEquals("/totalNonRefundableCredits", 0)
    scenario.assertEquals("/totalRefundableCredits", 0)
    scenario.assertEquals("/additionalMedicareTax", 0)
    scenario.assertEquals("/selfEmploymentTax", 0)
    scenario.assertEquals("/netInvestmentIncomeTax", 456)
    scenario.assertEquals(s"/pensions/#${JOB_1_ID}/w4pLine3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/pensions/#${JOB_1_ID}/w4pLine4aWithSplitWithholdingStrategy", 0)
    scenario.assertOffset(s"/pensions/#${JOB_1_ID}/w4pLine4bWithSplitWithholdingStrategy", 16557, -47)
    scenario.assertOffset(s"/pensions/#${JOB_1_ID}/w4pLine4cWithSplitWithholdingStrategy", 259, 8)
    scenario.assertEquals(s"/jobs/#$JOB_2_ID/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#$JOB_2_ID/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertOffset(s"/jobs/#$JOB_2_ID/w4Line4bWithSplitWithholdingStrategy", 15283, 47)
    scenario.assertOffset(s"/jobs/#$JOB_2_ID/w4Line4cWithSplitWithholdingStrategy", 111, -3)

  }

  // Column AL
  test("MFJ,  salary, pension, phased out deduction, one SSN") { td =>
    val scenario = td.scenario
    scenario.assertEquals(s"/pensions/#${JOB_1_ID}/w4pLine3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/pensions/#${JOB_1_ID}/w4pLine4aWithSplitWithholdingStrategy", 0)
    scenario.assertOffset(s"/pensions/#${JOB_1_ID}/w4pLine4bWithSplitWithholdingStrategy", 5320, -16)
    scenario.assertOffset(s"/pensions/#${JOB_1_ID}/w4pLine4cWithSplitWithholdingStrategy", 266, 8)
    scenario.assertEquals("/incomeTotal", 162000)
    scenario.assertEquals("/agi", 162000)
    scenario.assertEquals("/taxableIncome", 119570)
    scenario.assertEquals("/totalNonRefundableCredits", 0)
    scenario.assertEquals("/totalRefundableCredits", 0)
    scenario.assertEquals("/additionalMedicareTax", 0)
    scenario.assertEquals("/selfEmploymentTax", 0)
    scenario.assertEquals("/netInvestmentIncomeTax", 0)
  }

  // Column D
  test("Married filing jointly, salary, 1 child, multiple incomes, car loan interest") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 252000)
    scenario.assertEquals("/totalTaxNetRefundableCredits", 35766)
    scenario.assertEquals("/qualifiedPersonalVehicleLoanInterestDeduction", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 354)
    scenario.assertEquals(s"/jobs/#${JOB_2_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_2_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_2_ID}/w4Line4bWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_2_ID}/w4Line4cWithSplitWithholdingStrategy", 142)
  }

  // Column E
  test("Single filer, new job") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 124000)
    scenario.assertEquals("/taxableIncome", 107900)
    scenario.assertEquals("/standardOrItemizedDeduction", 16100)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 3500)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 38)
  }

  // Column F
  test("Single, low wages, 2 dependents") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 22786)
    scenario.assertEquals("/tentativeTaxFromTaxableIncome", 668)
    scenario.assertOffset("/totalEndOfYearProjectedWithholding", 455.71, .29)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 990)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 0)

    assert(scenario.graph.get("/cannotZeroOutRefundDueToExcessWithholding").get == true)
  }

  // Column G
  test("Head of Household, wages only") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 31200)
    scenario.assertEquals("/tentativeTaxFromTaxableIncome", 708)
    scenario.assertEquals("/additionalCtc", 3400)
    scenario.assertOffset("/earnedIncomeCredit", 5771, 6)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 705)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 0)

    assert(scenario.graph.get("/cannotZeroOutRefundDueToExcessWithholding").get == true)
  }

  // Column H
  test("Single, Social Security + part-time wages, senior deduction") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/incomeTotal", 39000)
    scenario.assertEquals("/agi", 39000)
    scenario.assertEquals("/taxableIncome", 14850)
    scenario.assertEquals("/totalNonRefundableCredits", 1000)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 993)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 8050)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 0)
  }

  // Column I
  test("Single, self-employed income, IRA contributions") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 49328)
    scenario.assertEquals("/qualifiedBusinessIncomeDeduction", 3346)
    scenario.assertEquals("/tentativeTaxFromTaxableIncome", 3337)
    scenario.assertEquals("/selfEmploymentTax", 2543)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertOffset(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 12182, 1)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 147)
  }

  // Column J
  test("Single, wages + dividends + interest + student loan interest") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 51300)
    scenario.assertEquals("/totalTaxNetRefundableCredits", 3619)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 733)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 4500)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 0)
  }

  // Column K
  test("Single, low wages, no dependents, EITC eligible") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 15600)
    scenario.assertOffset("/earnedIncomeCredit", 300, 1)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 0)

    assert(scenario.graph.get("/cannotZeroOutRefundDueToExcessWithholding").get == true)
  }

  // Column L
  test("Single, wages + overtime income") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 98800)
    scenario.assertEquals("/taxableIncome", 75700)
    scenario.assertEquals("/overtimeCompensationDeduction", 7000)
    scenario.assertEquals("/totalTaxNetRefundableCredits", 11372)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 7000)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 7)
  }

  // Column M
  test("Single, wages + tip income") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 98800)
    scenario.assertEquals("/totalTaxNetRefundableCredits", 7412)
    scenario.assertEquals("/qualifiedTipDeduction", 25000)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 537)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 25000)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 0)
  }

  // Column N
  test("Single, wages + short term gains + investment income") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 138000)
    scenario.assertEquals("/taxableIncome", 121900)
    scenario.assertEquals("/standardOrItemizedDeduction", 16100)
    scenario.assertEquals("/totalTaxNetRefundableCredits", 21584)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 34000)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 68)
  }

  // Column O
  test("Single, wages + rental income") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/incomeTotal", 131000)
    scenario.assertEquals("/agi", 131000)
    scenario.assertEquals("/taxableIncome", 109500)
    scenario.assertEquals("/totalTaxNetRefundableCredits", 18878)
    scenario.assertEquals("/totalEndOfYearProjectedWithholding", 15600)
    scenario.assertEquals("/tentativeTaxNetNonRefundableCredits", 18878)
    scenario.assertEquals("/tentativeTaxFromTaxableIncome", 18878)
    scenario.assertEquals("/standardOrItemizedDeduction", 16100)
    scenario.assertEquals("/qualifiedBusinessIncomeDeduction", 5400)
    scenario.assertEquals("/selfEmploymentTax", 0)
    scenario.assertEquals("/netInvestmentIncomeTax", 0)
    scenario.assertEquals("/additionalMedicareTax", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 21600)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 23)
  }

  // Column P
  test("Single, high wages +Cap Gains + Investment") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/incomeTotal", 682000)
    scenario.assertEquals("/agi", 667000)
    scenario.assertEquals("/taxableIncome", 648871)
    scenario.assertEquals("/totalNonRefundableCredits", 5000)
    scenario.assertEquals("/totalRefundableCredits", 0)
    scenario.assertEquals("/additionalMedicareTax", 3708)
    scenario.assertEquals("/selfEmploymentTax", 0)
    scenario.assertEquals("/netInvestmentIncomeTax", 760)
    scenario.assertEquals("/totalTaxNetRefundableCredits", 187842)
    // Manual Pub15 validation - pending multi-year withholding logic
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 52971)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 1552)
  }

  // Column Q
  test("Single, high wages +Cap Gains + Investment, overall limitation") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/incomeTotal", 682000)
    scenario.assertEquals("/agi", 667000)
    scenario.assertEquals("/stateAndLocalTaxDeduction", 10000)
    scenario.assertEquals("/taxableIncome", 648871)
    scenario.assertEquals("/qualifiedBusinessIncomeDeduction", 0)
    scenario.assertEquals("/netInvestmentIncomeTax", 760)
    scenario.assertEquals("/additionalMedicareTax", 3708)
    scenario.assertEquals("/totalTaxNetRefundableCredits", 187842)
    scenario.assertEquals("/tentativeTaxNetNonRefundableCredits", 183374)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 52971)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 1552)
    // Manual Pub15 validation - pending multi-year withholding logic
  }

  // Column T
  test("Single, wages + HSA + IRA + educator expenses") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/incomeTotal", 57200)
    scenario.assertEquals("/agi", 53850)
    scenario.assertEquals("/taxableIncome", 37750)
    scenario.assertEquals("/totalNonRefundableCredits", 0)
    scenario.assertEquals("/totalRefundableCredits", 0)
    scenario.assertEquals("/additionalMedicareTax", 0)
    scenario.assertEquals("/selfEmploymentTax", 0)
    scenario.assertEquals("/netInvestmentIncomeTax", 0)
    scenario.assertOffset(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 271, 1)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 3350)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 0)
  }

  // Column V
  test("Single, wages + foreign investment income") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/incomeTotal", 119000)
    scenario.assertEquals("/agi", 119000)
    scenario.assertEquals("/taxableIncome", 102900)
    scenario.assertEquals("/totalNonRefundableCredits", 5000)
    scenario.assertEquals("/totalRefundableCredits", 0)
    scenario.assertEquals("/additionalMedicareTax", 0)
    scenario.assertEquals("/selfEmploymentTax", 0)
    scenario.assertEquals("/netInvestmentIncomeTax", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 4415)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 15000)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 0)
  }

  // Column W
  test("Self-employed filer with business credit-eligible expenses") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 134587)
    scenario.assertEquals("/taxableIncome", 104320)
    scenario.assertEquals("/standardOrItemizedDeduction", 24150)
    scenario.assertEquals("/qualifiedBusinessIncomeDeduction", 6117)
    scenario.assertEquals("/tentativeTaxNetNonRefundableCredits", 11451)
    scenario.assertEquals("/totalTaxNetRefundableCredits", 14277)
    scenario.assertEquals("/additionalMedicareTax", 0)
    scenario.assertEquals("/selfEmploymentTax", 2826)
    scenario.assertEquals("/netInvestmentIncomeTax", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 3661)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 24470)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 0)
  }

  // Column Z
  test("MFJ, High Income, 1 child, Multi, Car loan") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 242571)
    scenario.assertEquals("/qualifiedPersonalVehicleLoanInterestDeduction", 1400)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 1400)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 61)
    scenario.assertEquals(s"/jobs/#${JOB_3_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_3_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_3_ID}/w4Line4bWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_3_ID}/w4Line4cWithSplitWithholdingStrategy", 31)
  }

  // Column AA
  test("Single, SS, part time, senior deduction") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 32880)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 314)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 1170)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 0)
    // Scenario-specific assertions
    scenario.assertEquals("/seniorDeduction", 6000)
  }

  // Column AB
  test("Single, low wages, EITC, no dependents") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 13000)
    scenario.assertEquals("/tentativeTaxNetNonRefundableCredits", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 0)
    // Scenario-specific assertions
    scenario.assertOffset("/earnedIncomeCredit", 498, 2)

    assert(scenario.graph.get("/cannotZeroOutRefundDueToExcessWithholding").get == true)
  }

  // Column AC
  test("SE, Wages, QBI") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 79881)
    scenario.assertOffset("/totalTaxNetRefundableCredits", 11760, 1)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertOffset(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 22305, -1)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 209)
    scenario.assertEquals("/qualifiedBusinessIncomeDeduction", 5576)
  }

  // Column AD
  test("MFJ, high itemized deductions (mortgage, SALT, charity, medical)") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 269714)
    scenario.assertOffset("/totalTaxNetRefundableCredits", 31597, 1)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 36622)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 39)
  }

  // Column AF
  test("Single, salary, full deduction") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 52000)
    scenario.assertEquals("/totalTaxNetRefundableCredits", 3097)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 143)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 8050)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 0)
  }

  // Column AG
  test("Single, salary, partial deduction, standard") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 112000)
    scenario.assertEquals("/tentativeTaxNetNonRefundableCredits", 14529)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 5830)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 49)
  }

  // Column AH
  test("Single, pension, SS, partial deduction, standard") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 100800)
    scenario.assertEquals("/tentativeTaxFromTaxableIncome", 11911)
    scenario.assertEquals(s"/pensions/#${JOB_1_ID}/w4pLine3WithSplitWithholdingStrategy", 2494)
    scenario.assertEquals(s"/pensions/#${JOB_1_ID}/w4pLine4aWithSplitWithholdingStrategy", 34298)
    scenario.assertEquals(s"/pensions/#${JOB_1_ID}/w4pLine4bWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/pensions/#${JOB_1_ID}/w4pLine4cWithSplitWithholdingStrategy", 0)
  }

  // Column AI
  test("Single, salary, full deduction, itemized") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/incomeTotal", 60000)
    scenario.assertEquals("/agi", 60000)
    assert(scenario.graph.get("/totalDeductionsNetQBI").value.get == 30700)
    scenario.assertEquals("/taxableIncome", 29300)
    scenario.assertEquals("/stateAndLocalTaxDeduction", 15000)
    scenario.assertEquals("/seniorDeduction", 6000)
    scenario.assertEquals("/tentativeTaxFromTaxableIncome", 3271)
    scenario.assertEquals("/tentativeTaxNetNonRefundableCredits", 3271)
    scenario.assertEquals("/totalTaxNetRefundableCredits", 3271)
    scenario.assertEquals("/totalEndOfYearProjectedWithholding", 4800)

    scenario.assertEquals(s"/pensions/#${JOB_1_ID}/w4pLine3WithSplitWithholdingStrategy", 1526)
    scenario.assertEquals(s"/pensions/#${JOB_1_ID}/w4pLine4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/pensions/#${JOB_1_ID}/w4pLine4bWithSplitWithholdingStrategy", 14600)
    scenario.assertEquals(s"/pensions/#${JOB_1_ID}/w4pLine4cWithSplitWithholdingStrategy", 0)
  }

  // Column AJ
  test("MFJ, salary, full deduction, standard") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 81500)
    scenario.assertEquals("/tentativeTaxNetNonRefundableCredits", 4505)
    scenario.assertEquals(s"/pensions/#${JOB_1_ID}/w4pLine3WithSplitWithholdingStrategy", 296)
    scenario.assertEquals(s"/pensions/#${JOB_1_ID}/w4pLine4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/pensions/#${JOB_1_ID}/w4pLine4bWithSplitWithholdingStrategy", 10150)
    scenario.assertEquals(s"/pensions/#${JOB_1_ID}/w4pLine4cWithSplitWithholdingStrategy", 0)
  }

  // Column AM
  test("HH, pension, partial deduction, standard") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 84000)
    scenario.assertEquals("/tentativeTaxNetNonRefundableCredits", 1279)
    scenario.assertEquals(s"/pensions/#${JOB_1_ID}/w4pLine3WithSplitWithholdingStrategy", 5346)
    scenario.assertEquals(s"/pensions/#${JOB_1_ID}/w4pLine4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/pensions/#${JOB_1_ID}/w4pLine4bWithSplitWithholdingStrategy", 9560)
    scenario.assertEquals(s"/pensions/#${JOB_1_ID}/w4pLine4cWithSplitWithholdingStrategy", 0)
  }

  // Column AN
  test("MFS, wage, partial deduction, itemized") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 168000)
    scenario.assertEquals("/seniorDeduction", 420)
    // this scenario expects users to choose their deduction method in spite of deduction values.
    // user selection of dediction method is not supported in TWE 2.0.
    // this scenario is effectively out of scope, and therefore assertions on W4 values cannot be made.
  }

  // Column AP
  test("Single, full tip deduction") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/incomeTotal", 78000)
    scenario.assertEquals("/agi", 78000)
    scenario.assertEquals("/taxableIncome", 51900)
    scenario.assertEquals("/totalNonRefundableCredits", 0)
    scenario.assertEquals("/totalRefundableCredits", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 493)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 10000)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 0)
  }

  // Column AQ
  test("Single, partial deduction") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/incomeTotal", 156000)
    scenario.assertEquals("/agi", 156000)
    scenario.assertEquals("/taxableIncome", 130500)
    scenario.assertEquals("/totalNonRefundableCredits", 0)
    scenario.assertEquals("/totalRefundableCredits", 0)
    scenario.assertEquals("/qualifiedTipDeduction", 9400)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 9400)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 96)
  }

  // Column AR
  test("Single, two tipped jobs") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/incomeTotal", 186000)
    scenario.assertEquals("/agi", 186000)
    scenario.assertEquals("/taxableIncome", 153500)
    scenario.assertEquals("/totalNonRefundableCredits", 0)
    scenario.assertEquals("/totalRefundableCredits", 0)
    scenario.assertEquals("/qualifiedTipDeduction", 16400)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 16400)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 167)
  }

  // Column AS
  test("MFJ, two tipped jobs, no SSN user") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/incomeTotal", 260000)
    scenario.assertEquals("/agi", 260000)
    scenario.assertEquals("/taxableIncome", 215800)
    scenario.assertEquals("/totalNonRefundableCredits", 0)
    scenario.assertEquals("/totalRefundableCredits", 0)
    scenario.assertEquals("/additionalMedicareTax", 90)
    scenario.assertEquals("/selfEmploymentTax", 0)
    scenario.assertEquals("/netInvestmentIncomeTax", 0)
    scenario.assertEquals("/qualifiedTipDeduction", 12000)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 7200)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 343)

    scenario.assertEquals(s"/jobs/#${JOB_3_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_3_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_3_ID}/w4Line4bWithSplitWithholdingStrategy", 4800)
    scenario.assertEquals(s"/jobs/#${JOB_3_ID}/w4Line4cWithSplitWithholdingStrategy", 114)
  }

  // Column AT
  test("MFJ, three tipped jobs, max") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/incomeTotal", 360000)
    scenario.assertEquals("/agi", 360000)
    scenario.assertEquals("/taxableIncome", 301000)
    scenario.assertEquals("/totalNonRefundableCredits", 4400)
    scenario.assertEquals("/totalRefundableCredits", 0)
    scenario.assertEquals("/additionalMedicareTax", 990)
    scenario.assertEquals("/selfEmploymentTax", 0)
    scenario.assertEquals("/netInvestmentIncomeTax", 0)
    scenario.assertEquals("/qualifiedTipDeduction", 19000)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 2198)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 16080)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_3_ID}/w4Line3WithSplitWithholdingStrategy", 1466)
    scenario.assertEquals(s"/jobs/#${JOB_3_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_3_ID}/w4Line4bWithSplitWithholdingStrategy", 10720)
    scenario.assertEquals(s"/jobs/#${JOB_3_ID}/w4Line4cWithSplitWithholdingStrategy", 0)
  }

  // Column AU
  test("HH, two tipped jobs, itemized") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/incomeTotal", 186000)
    scenario.assertEquals("/agi", 181000)
    scenario.assertEquals("/taxableIncome", 134950)
    scenario.assertEquals("/totalNonRefundableCredits", 4400)
    scenario.assertEquals("/totalRefundableCredits", 0)
    scenario.assertEquals("/additionalMedicareTax", 0)
    scenario.assertEquals("/selfEmploymentTax", 0)
    scenario.assertEquals("/netInvestmentIncomeTax", 0)
    scenario.assertEquals("/qualifiedTipDeduction", 21900)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 11481)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 26900)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 0)

    // regression prevention sanity check
    scenario.graph.set("/wantsStandardDeduction", false)
    assert(scenario.graph.get("/standardOrItemizedDeductionForQBIDeduction").hasValue)
  }

  // Column AW
  test("Single, 1 job, factor 2, partial deduction") { td =>
    {
      val scenario = td.scenario

      scenario.assertEquals("/agi", 156000)
      scenario.assertEquals("/overtimeCompensationDeduction", 9400)
      scenario.assertEquals("/taxableIncome", 130500)
      scenario.assertEquals("/totalTaxNetRefundableCredits", 23918)
      scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 324)
      scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
      scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 9400)
      scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 0)
    }
  }

  // Column AX
  test("Single, 1 job, factor 1.5, partial deduction") { td =>
    {
      val scenario = td.scenario
      scenario.assertEquals("/agi", 254650)
      scenario.assertEquals("/overtimeCompensationDeduction", 2100)
      scenario.assertEquals("/taxableIncome", 236450)
      scenario.assertEquals("/totalTaxNetRefundableCredits", 52660)
      scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 0)
      scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
      scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 7450)
      scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 12)
    }
  }

  // Column AY
  test("Single, 2 jobs,  partial deduction") { td =>
    {
      val scenario = td.scenario
      scenario.assertEquals("/agi", 186000)
      scenario.assertEquals("/overtimeCompensationDeduction", 3900)
      scenario.assertEquals("/taxableIncome", 166000)
      scenario.assertEquals("/totalTaxNetRefundableCredits", 32438)
      scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 0)
      scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
      scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 3900)
      scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 95)
    }
  }

  // Column AZ
  test("MFj, 2 jobs, maximum deduction") { td =>
    {
      val scenario = td.scenario
      scenario.assertEquals("/agi", 186000)
      scenario.assertEquals("/overtimeCompensationDeduction", 7500)
      scenario.assertEquals("/taxableIncome", 146300)
      scenario.assertEquals("/totalTaxNetRefundableCredits", 21610)
      scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 107)
      scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
      scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 7500)
      scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 0)
    }
  }

  // Column BA
  test("MFj, 2 jobs, phased out deduction, no SSN spouse") { td =>
    {
      val scenario = td.scenario
      scenario.assertEquals("/agi", 356000)
      scenario.assertEquals("/overtimeCompensationDeduction", 14400)
      scenario.assertEquals("/taxableIncome", 309400)
      scenario.assertEquals("/totalTaxNetRefundableCredits", 60406)
      scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 0)
      scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
      scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 14400)
      scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 681)
    }
  }

  // Column BB
  test("HH, 1 job, tips and OT") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 156000)
    scenario.assertEquals("/taxableIncome", 98050)
    scenario.assertEquals("/tentativeTaxNetNonRefundableCredits", 12278)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 3271)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 33800)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 0)
  }

  // Column BC
  test("MFS, 1 job, tips and OT") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 156000)
    scenario.assertEquals("/overtimeCompensationDeduction", 0)

    scenario.assertEquals("/taxableIncome", 139900)
    scenario.assertEquals("/totalTaxNetRefundableCredits", 24253)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 820)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 0)
  }

  // Column BE
  test("Single, phasedout deduction") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 104000)
    scenario.assertEquals("/tentativeTaxNetNonRefundableCredits", 12472)
    scenario.assertEquals("/qualifiedPersonalVehicleLoanInterestDeduction", 7200)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 7200)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 611)
  }

  // Column BF
  test("Single, zero deduction") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 152000)
    scenario.assertEquals("/tentativeTaxFromTaxableIncome", 25214)
    scenario.assertEquals("/studentLoanInterestDeduction", 0)
    scenario.assertEquals("/qualifiedPersonalVehicleLoanInterestDeduction", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 3512)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 2000)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 0)
  }

  // Column BG
  test("MFS, phased out, no SSN, CTC") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 104000)
    scenario.assertEquals("/tentativeTaxFromTaxableIncome", 12472)
    scenario.assertEquals("/qualifiedPersonalVehicleLoanInterestDeduction", 7200)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 7200)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 50)
  }

  // Column BH
  test("MFJ, max amount deduction") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 104000)
    scenario.assertEquals("/tentativeTaxFromTaxableIncome", 6923)
    scenario.assertEquals("/qualifiedPersonalVehicleLoanInterestDeduction", 10000)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 629)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 10000)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 0)
  }

  // Column BI
  test("HH,  phased out deduction, CTC, QBI") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 172587)
    scenario.assertEquals("/tentativeTaxFromTaxableIncome", 25520)
    scenario.assertEquals("/qualifiedPersonalVehicleLoanInterestDeduction", 0)
    scenario.assertEquals("/qualifiedBusinessIncomeDeduction", 3717)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 14870)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 260)
  }

  // Column BK
  test("Single, one child, non-refundable") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 52000)
    scenario.assertEquals("/tentativeTaxFromTaxableIncome", 4063)
    scenario.assertEquals("/totalCtcAndOdc", 2200)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 4060)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 0)

    assert(scenario.graph.get("/cannotZeroOutRefundDueToExcessWithholding").get == true)
  }

  // Column BL
  test("Single, one child, refundable partial") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 38429)
    scenario.assertEquals("/tentativeTaxFromTaxableIncome", 2431)
    scenario.assertEquals("/totalCtcAndOdc", 2200)
    scenario.assertEquals("/earnedIncomeCredit", 2104)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 4060)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 0)

    assert(scenario.graph.get("/cannotZeroOutRefundDueToExcessWithholding").get == true)
  }

  // Column BM
  test("MFJ, 3 children, one SSN, full refundable") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 31729)
    scenario.assertEquals("/tentativeTaxFromTaxableIncome", 0)
    scenario.assertEquals("/totalCtcAndOdc", 0)
    scenario.assertEquals("/additionalCtc", 5100)
    scenario.assertEquals("/earnedIncomeCredit", 6700)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 1310)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 6700)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 0)

    assert(scenario.graph.get("/cannotZeroOutRefundDueToExcessWithholding").get == true)
  }

  // Column BN
  test("HH,1 child, non-refundable") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 52000)
    scenario.assertEquals("/tentativeTaxFromTaxableIncome", 2991)
    scenario.assertEquals("/totalCtcAndOdc", 1941)
    scenario.assertEquals("/totalRefundableCredits", 259)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 2988)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 0)
  }

  // Column BO
  test("MFS, 2 child, no SSN") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 52000)
    scenario.assertEquals("/tentativeTaxFromTaxableIncome", 4063)
    scenario.assertEquals("/totalCtcAndOdc", 0)
    scenario.assertEquals("/totalRefundableCredits", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 10)
  }

  // Column BQ
  test("Single, Full QBI, under threshold") { td =>
    val scenario = td.scenario

    // CSV has $3,532 for SE tax deduction (7065/2 rounded down) where as we have 3,533 (7065/2 rounded up)
    scenario.assertOffset("/agi", 176467, 1)
    scenario.assertEquals("/taxableIncome", 151074)
    scenario.assertEquals("/standardOrItemizedDeduction", 16100)
    scenario.assertEquals("/totalTaxNetRefundableCredits", 35921)
    scenario.assertEquals("/tentativeTaxNetNonRefundableCredits", 28856)
    scenario.assertEquals("/additionalMedicareTax", 0)
    scenario.assertEquals("/selfEmploymentTax", 7065)
    scenario.assertEquals("/netInvestmentIncomeTax", 0)
    scenario.assertOffset(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 7007, 4)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 37174)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 0)

    scenario.assertOffset("/qualifiedBusinessIncomeDeduction", 9293, 1)
  }

  // Column BR
  test("Single, small QBI, under threshold") { td =>
    val scenario = td.scenario

    scenario.assertEquals("/agi", 130836)
    scenario.assertEquals("/taxableIncome", 114569)
    scenario.assertEquals("/standardOrItemizedDeduction", 16100)
    scenario.assertEquals("/qualifiedBusinessIncomeDeduction", 167)
    scenario.assertEquals("/totalTaxNetRefundableCredits", 20222)
    scenario.assertEquals("/tentativeTaxNetNonRefundableCredits", 20095)
    scenario.assertEquals("/additionalMedicareTax", 0)
    scenario.assertEquals("/selfEmploymentTax", 127)
    scenario.assertEquals("/netInvestmentIncomeTax", 0)
    scenario.assertOffset(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 4304, 2)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 669)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 0)

  }

  // Column BS
  test("Single, 1k QBI, under threshold") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 131022)
    scenario.assertEquals("/taxableIncome", 114522)
    scenario.assertEquals("/standardOrItemizedDeduction", 16100)
    scenario.assertEquals("/qualifiedBusinessIncomeDeduction", 400)
    scenario.assertEquals("/tentativeTaxNetNonRefundableCredits", 20083)
    scenario.assertEquals("/totalTaxNetRefundableCredits", 20238)
    scenario.assertEquals("/additionalMedicareTax", 0)
    scenario.assertEquals("/selfEmploymentTax", 155)
    scenario.assertEquals("/netInvestmentIncomeTax", 0)
    scenario.assertOffset(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 4154, -7)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 622)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 0)
  }

  // Column BT
  test("Single, 10k QBI, over low threshold") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 224866)
    scenario.assertEquals("/qualifiedBusinessIncomeDeduction", 1789)
    scenario.assertEquals("/taxableIncome", 206977)
    scenario.assertEquals("/standardOrItemizedDeduction", 16100)
    scenario.assertEquals("/tentativeTaxNetNonRefundableCredits", 42689)
    scenario.assertEquals("/selfEmploymentTax", 268)
    scenario.assertEquals("/additionalMedicareTax", 218)
    scenario.assertEquals("/netInvestmentIncomeTax", 0)
    scenario.assertEquals("/totalTaxNetRefundableCredits", 43175)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 8077)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 750)
  }

  // Column BU
  test("Single, QBI, over high threshold") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 299732)
    scenario.assertEquals("/standardOrItemizedDeduction", 16100)
    scenario.assertEquals("/taxableIncome", 283232)
    scenario.assertEquals("/qualifiedBusinessIncomeDeduction", 400)
    scenario.assertEquals("/selfEmploymentTax", 536)
    scenario.assertEquals("/additionalMedicareTax", 436)
    scenario.assertEquals("/netInvestmentIncomeTax", 0)
    scenario.assertOffset("/tentativeTaxNetNonRefundableCredits", 67900, 1)
    scenario.assertOffset("/totalTaxNetRefundableCredits", 68872, 1)

    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 69332)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 417)
  }

  // Column BV
  test("MFJ, QBI full, under threshold") { td =>
    val scenario = td.scenario
    // CSV has $3,532 for SE tax deduction (7065/2 rounded down) where as we have 3,533 (7065/2 rounded up)
    scenario.assertOffset("/agi", 254467, 1)
    scenario.assertEquals("/taxableIncome", 212974)
    scenario.assertOffset("/qualifiedBusinessIncomeDeduction", 9293, 1)
    scenario.assertEquals("/standardOrItemizedDeduction", 32200)
    scenario.assertEquals("/tentativeTaxNetNonRefundableCredits", 29710)
    scenario.assertEquals("/selfEmploymentTax", 7065)
    scenario.assertEquals("/netInvestmentIncomeTax", 0)
    scenario.assertEquals("/additionalMedicareTax", 38)
    scenario.assertOffset("/totalTaxNetRefundableCredits", 36813, -1)
    scenario.assertOffset(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 864, 1)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 37174)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 0)
  }

  // Column BW
  test("HH, partial QBI, over low threshold") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 287330)
    scenario.assertEquals("/taxableIncome", 260310)
    scenario.assertEquals("/standardOrItemizedDeduction", 24150)
    scenario.assertOffset("/qualifiedBusinessIncomeDeduction", 2870, 1)
    scenario.assertEquals("/selfEmploymentTax", 1339)
    scenario.assertEquals("/netInvestmentIncomeTax", 1140)
    scenario.assertEquals("/additionalMedicareTax", 488)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 76460)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 0)

    scenario.assertOffset("/tentativeTaxNetNonRefundableCredits", 55870, -3)
    scenario.assertOffset("/tentativeTaxFromTaxableIncome", 58070, -3)
    scenario.assertOffset(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 58069, -2)

    assert(scenario.graph.get("/cannotZeroOutRefundDueToExcessWithholding").get == true)
  }

  // Column BX
  test("MFS, partial QBI, over high threshold") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 357330)
    scenario.assertEquals("/taxableIncome", 340830)
    scenario.assertEquals("/qualifiedBusinessIncomeDeduction", 400)
    scenario.assertEquals("/additionalMedicareTax", 1163)
    scenario.assertEquals("/selfEmploymentTax", 1339)
    scenario.assertEquals("/netInvestmentIncomeTax", 3800)
    scenario.assertEquals("/tentativeTaxNetNonRefundableCredits", 88060)
    scenario.assertOffset("/totalTaxNetRefundableCredits", 94362, -1)
    scenario.assertOffset(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 48471, 1)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 148930)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 0)
  }

  // Column BZ
  test("Single, SALT under 10k") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 530000)
    scenario.assertEquals("/stateAndLocalTaxDeduction", 9000)

    scenario.assertEquals("/taxableIncome", 513900)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 1601)
  }

  // Column CA
  test("HH, SALT, phased out under 40k") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 530000)
    scenario.assertEquals("/stateAndLocalTaxDeduction", 32900)
    scenario.assertEquals("/taxableIncome", 497100)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 8750)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 720)
  }

  // Column CB
  test("QW, SALT, max amount") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 500000)
    scenario.assertEquals("/stateAndLocalTaxDeduction", 40400)
    scenario.assertEquals("/taxableIncome", 459600)
    scenario.assertEquals("/totalTaxNetRefundableCredits", 102684)
    scenario.assertEquals("/standardOrItemizedDeduction", 40400)
    scenario.assertEquals("/additionalMedicareTax", 2700)
    scenario.assertEquals("/selfEmploymentTax", 0)
    scenario.assertEquals("/netInvestmentIncomeTax", 0)
    scenario.assertEquals("/totalEndOfYearProjectedWithholding", 109000)

    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 8200)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 711)
  }

  // Column CC
  test("MFJ, no phase out") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 230000)
    scenario.assertEquals("/stateAndLocalTaxDeduction", 35000)
    scenario.assertEquals("/taxableIncome", 195000)
    scenario.assertEquals("/tentativeTaxNetNonRefundableCredits", 32324)
    scenario.assertEquals("/standardOrItemizedDeduction", 35000)

    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 2800)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 777)

  }

  // Column CD
  test("MFj, SALT, phased out down to 10k") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 634000)
    scenario.assertEquals("/stateAndLocalTaxDeduction", 10000)
    scenario.assertEquals("/taxableIncome", 601800)
    scenario.assertEquals("/standardOrItemizedDeduction", 32200)

    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 0)
    scenario.assertOffset(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 451, 20)

    scenario.assertEquals(s"/jobs/#${JOB_2_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_2_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_2_ID}/w4Line4bWithSplitWithholdingStrategy", 0)
    scenario.assertOffset(s"/jobs/#${JOB_2_ID}/w4Line4cWithSplitWithholdingStrategy", 180, -10)
  }

  // Column CE
  test("MFS, SALT, phased out") { td =>
    val scenario = td.scenario
    scenario.graph.set("/wantsStandardDeduction", false)
    scenario.assertEquals("/agi", 256000)
    scenario.assertEquals("/stateAndLocalTaxDeduction", 19675)
    scenario.assertEquals("/taxableIncome", 236325)
    scenario.assertEquals("/standardOrItemizedDeduction", 19675)

    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertOffset(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 2002, -16)
    scenario.assertOffset(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 322, 18)

    scenario.assertEquals(s"/jobs/#${JOB_2_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_2_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertOffset(s"/jobs/#${JOB_2_ID}/w4Line4bWithSplitWithholdingStrategy", 1573, 16)
    scenario.assertOffset(s"/jobs/#${JOB_2_ID}/w4Line4cWithSplitWithholdingStrategy", 129, -9)
  }

  // Column CF
  test("MFS, SALT, phased out to min") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 412000)
    scenario.assertEquals("/stateAndLocalTaxDeduction", 5000)
    scenario.assertEquals("/taxableIncome", 395900)
    scenario.assertEquals("/standardOrItemizedDeduction", 16100)
    scenario.assertEquals("/totalTaxNetRefundableCredits", 110148)
    scenario.assertEquals("/additionalMedicareTax", 2583)
    scenario.assertEquals("/selfEmploymentTax", 0)
    scenario.assertEquals("/netInvestmentIncomeTax", 0)
    scenario.assertEquals("/totalEndOfYearProjectedWithholding", 96000)

    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 0)
    scenario.assertOffset(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 420, -83)

    scenario.assertEquals(s"/jobs/#${JOB_2_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_2_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_2_ID}/w4Line4bWithSplitWithholdingStrategy", 0)
    scenario.assertOffset(s"/jobs/#${JOB_2_ID}/w4Line4cWithSplitWithholdingStrategy", 420, 40)
  }

  // Column CJ
  test("Single, No tax on overtime, factor 2, itemized,SSN") { td =>
    {
      val scenario = td.scenario

      scenario.assertEquals("/agi", 154587)
      scenario.assertEquals("/taxableIncome", 106695)

      scenario.assertEquals("/qualifiedTipDeduction", 9600)
      scenario.assertEquals("/qualifiedBusinessIncomeDeduction", 3717)
      scenario.assertEquals("/overtimeCompensationDeduction", 3350)
      assert(scenario.graph.get("/ctcEligibleDependents").value.get === 3) // This is calculated from multiple rows
      scenario.assertEquals("/totalTaxNetRefundableCredits", 14431)

      // Pending spreadsheet update: it expects 2505 which appears to do using
      // the old strategy of using the standard witholding amount of 270.19
      scenario.assertOffset(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 2089, 416)
      scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
      scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 0)
      scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 0)

      // Pending spreadsheet update: it expects 724 which appears to do using
      // the old strategy of using the standard witholding amount of  78.08
      scenario.assertOffset(s"/jobs/#${JOB_2_ID}/w4Line3WithSplitWithholdingStrategy", 1045, -321)
      scenario.assertEquals(s"/jobs/#${JOB_2_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
      scenario.assertEquals(s"/jobs/#${JOB_2_ID}/w4Line4bWithSplitWithholdingStrategy", 13205)
      scenario.assertEquals(s"/jobs/#${JOB_2_ID}/w4Line4cWithSplitWithholdingStrategy", 0)
    }
  }

  // Column CK
  test("HH, 2 jobs") { td =>
    {
      val scenario = td.scenario

      scenario.assertEquals("/agi", 155000)
      scenario.assertEquals("/overtimeCompensationDeduction", 7250)
      assert(scenario.graph.get("/ctcEligibleDependents").value.get === 2) // This is calculated from multiple rows
      scenario.assertEquals("/taxableIncome", 123600)
      scenario.assertEquals("/totalTaxNetRefundableCredits", 16051)

      scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 0)
      scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
      scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 7250)
      scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 32)
    }
  }

  // Column CL
  // pending spreadsheet with updated derivation of non-MFS ceiling for phaseout
  test("Single, MIP phased out") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/incomeTotal", 137000)
    scenario.assertEquals("/agi", 106505)
    scenario.assertEquals("/taxableIncome", 85354)
    scenario.assertEquals("/totalTaxNetRefundableCredits", 14484)
    scenario.assertEquals("/tentativeTaxNetNonRefundableCredits", 13495)
    scenario.assertEquals("/tentativeTaxFromTaxableIncome", 13495)
    scenario.assertEquals("/totalEndOfYearProjectedWithholding", 13000)
    scenario.assertEquals("/overtimeCompensationDeduction", 3750)
    scenario.assertEquals("/qualifiedBusinessIncomeDeduction", 1301)
    scenario.assertEquals("/qualifiedMortgageInsurancePremiumDeductionTotal", 1500)
    scenario.assertEquals("/selfEmploymentTax", 989)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 28546)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 49)

  }

  // Column CM
  test("MFS, MIP phased out") { td =>
    val scenario = td.scenario
    scenario.graph.set("/wantsStandardDeduction", false)
    scenario.assertEquals("/incomeTotal", 52000)
    scenario.assertEquals("/agi", 52000)
    scenario.assertEquals("/stateAndLocalTaxDeduction", 9500)
    scenario.assertEquals("/medicalAndDentalExpensesTotal", 3100)
    scenario.assertEquals("/totalEndOfYearProjectedWithholding", 2600)
    scenario.assertEquals("/totalNonRefundableCredits", 2200)
    scenario.assertEquals("/totalCtcAndOdc", 2200)
    scenario.assertEquals("/qualifiedMortgageInsurancePremiumDeductionTotal", 1320)
    scenario.assertEquals("/taxableIncome", 30840)
    scenario.assertEquals("/tentativeTaxFromTaxableIncome", 3451)
    scenario.assertEquals("/totalTaxNetRefundableCredits", 1251)
    scenario.assertEquals("/tentativeTaxNetNonRefundableCredits", 1251)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 2447)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 5060)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 0)
    // Manual Pub15 validation - pending multi-year withholding logic
  }

  // Column CN
  test("MFJ, adoption 1 child full") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/incomeTotal", 167000)
    scenario.assertEquals("/agi", 163114)
    scenario.assertEquals("/taxableIncome", 111828)
    scenario.assertEquals("/selfEmploymentTax", 7771)
    scenario.assertEquals("/seniorDeduction", 5213)
    scenario.assertEquals("/totalCtcAndOdc", 2200)
    scenario.assertEquals("/stateAndLocalTaxDeduction", 9500)
    scenario.assertOffset(
      "/adoptionCreditNonRefundable",
      11826,
      724,
    ) // Pending spreadsheet update
    scenario.assertEquals("/qualifiedBusinessIncomeDeduction", 10223)
    scenario.assertEquals("/tentativeTaxFromTaxableIncome", 14026)
    scenario.assertEquals("/totalRefundableCredits", 5120)
    scenario.assertOffset("/totalNonRefundableCredits", 14026, 724)
    scenario.assertEquals("/totalTaxNetRefundableCredits", 2651)

    // Should be 2544, but the differences in the adoptionCreditNonRefundable and totalNonRefundableCredits
    // appear to result in a different `/taxGap` which affects Line 3.
    scenario.assertOffset(s"/jobs/#$JOB_1_ID/w4Line3WithSplitWithholdingStrategy", 1980, 564)
    scenario.assertEquals(s"/jobs/#$JOB_1_ID/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#$JOB_1_ID/w4Line4bWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#$JOB_1_ID/w4Line4cWithSplitWithholdingStrategy", 0)
    // Should be 3649, but the differences in the adoptionCreditNonRefundable and totalNonRefundableCredits
    // appear to result in a different `/taxGap` which affects Line 3.
    scenario.assertOffset(s"/pensions/#$JOB_2_ID/w4pLine3WithSplitWithholdingStrategy", 3315, 334)
    scenario.assertEquals(s"/pensions/#$JOB_2_ID/w4pLine4aWithSplitWithholdingStrategy", 32028)
    scenario.assertEquals(s"/pensions/#$JOB_2_ID/w4pLine4bWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/pensions/#$JOB_2_ID/w4pLine4cWithSplitWithholdingStrategy", 0)
  }

  // Column CO
  test("MFJ, adoption 2 children phasedout") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/incomeTotal", 286200)
    scenario.assertEquals("/agi", 282314)
    scenario.assertEquals("/taxableIncome", 225503)
    scenario.assertEquals("/standardOrItemizedDeduction", 42588)
    scenario.assertEquals("/tentativeTaxFromTaxableIncome", 39317)
    scenario.assertEquals("/stateAndLocalTaxDeduction", 28000)
    scenario.assertEquals("/qualifiedBusinessIncomeDeduction", 14223)
    scenario.assertEquals("/selfEmploymentTax", 7771)
    scenario.assertEquals("/totalCtcAndOdc", 6600)
    scenario.assertEquals("/netInvestmentIncomeTax", 1228)
    scenario.assertEquals("/studentLoanInterestDeduction", 0)
    scenario.assertEquals("/seniorDeduction", 0)
    scenario.assertEquals("/qualifiedTipDeduction", 0)
    scenario.assertEquals("/qualifiedPersonalVehicleLoanInterestDeduction", 0)
    scenario.assertEquals("/overtimeCompensationDeduction", 0)
    scenario.assertEquals("/medicalAndDentalExpensesTotal", 0)
    scenario.assertEquals("/earnedIncomeCredit", 0)
    scenario.assertEquals("/adoptionCreditRefundable", 10240)
    scenario.assertEquals("/adoptionCreditNonRefundable", 5696)
    scenario.assertEquals("/totalNonRefundableCredits", 12296)
    scenario.assertEquals("/totalRefundableCredits", 10240)
    scenario.assertEquals("/tentativeTaxNetNonRefundableCredits", 27021)
    scenario.assertEquals("/totalTaxNetRefundableCredits", 25780)

    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 70503)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 0)
    scenario.assertOffset(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 17, 1)

    scenario.assertEquals(s"/jobs/#${JOB_2_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_2_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_2_ID}/w4Line4bWithSplitWithholdingStrategy", 0)
    scenario.assertOffset(s"/jobs/#${JOB_2_ID}/w4Line4cWithSplitWithholdingStrategy", 14, -1)
    // Manual Pub15 validation - pending multi-year withholding logic
  }

  // Column CP
  test("MFJ, high income, itemized, charity, adoption") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/incomeTotal", 800000)
    scenario.assertEquals("/agi", 789403)
    scenario.assertEquals("/stateAndLocalTaxDeduction", 10000)
    scenario.assertEquals("/netInvestmentIncomeTax", 6080)
    scenario.assertEquals("/additionalMedicareTax", 3407)
    scenario.assertEquals("/qualifiedBusinessIncomeDeduction", 400)
    scenario.assertEquals("/selfEmploymentTax", 21194)
    scenario.assertEquals("/adoptionCreditRefundable", 0)
    scenario.assertEquals("/adoptionCreditNonRefundable", 0)
    scenario.assertEquals("/qualifiedMortgageInsurancePremiumDeductionTotal", 0)
    scenario.assertEquals("/taxableIncome", 734069)
    scenario.assertOffset("/totalTaxNetRefundableCredits", 225144, -1)
    scenario.assertEquals("/tentativeTaxNetNonRefundableCredits", 194463)
    scenario.assertEquals("/tentativeTaxFromTaxableIncome", 194463)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 276269)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 0)
    // TODO: Should be 1645
    scenario.assertOffset(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 1406, 239)

    scenario.assertEquals(s"/jobs/#${JOB_2_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_2_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_2_ID}/w4Line4bWithSplitWithholdingStrategy", 0)
    // TODO: Should be 464
    scenario.assertOffset(s"/jobs/#${JOB_2_ID}/w4Line4cWithSplitWithholdingStrategy", 703, -239)
    // Manual Pub15 validation - pending multi-year withholding logic
  }

  // Column CQ
  test("QW, overwithheld") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/incomeTotal", 100000)
    scenario.assertEquals("/agi", 100000)
    scenario.assertEquals("/taxableIncome", 67800)
    scenario.assertEquals("/standardOrItemizedDeduction", 32200)
    scenario.assertEquals("/totalRefundableCredits", 10240)
    scenario.assertEquals("/tentativeTaxFromTaxableIncome", 7643)
    scenario.assertEquals("/totalCtcAndOdc", 6600)
    scenario.assertEquals("/adoptionCreditRefundable", 10240)
    scenario.assertEquals("/adoptionCreditNonRefundable", 1043)
    scenario.assertEquals("/totalEndOfYearProjectedWithholding", 6300)
    scenario.assertEquals("/totalNonRefundableCredits", 7643)
    scenario.assertEquals("/totalTaxNetRefundableCredits", -10240)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 1980)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_2_ID}/w4Line3WithSplitWithholdingStrategy", 1580)
    scenario.assertEquals(s"/jobs/#${JOB_2_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_2_ID}/w4Line4bWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_2_ID}/w4Line4cWithSplitWithholdingStrategy", 0)
  }

  // Column CR
  test("MFJ, multiple incomes") { td =>
    val scenario = td.scenario
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 59146)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 0)
    scenario.assertOffset(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 840, -23)

    scenario.assertEquals(s"/jobs/#${JOB_2_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_2_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_2_ID}/w4Line4bWithSplitWithholdingStrategy", 0)
    scenario.assertOffset(s"/jobs/#${JOB_2_ID}/w4Line4cWithSplitWithholdingStrategy", 668, -18)

    scenario.assertEquals(s"/jobs/#${JOB_3_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_3_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_3_ID}/w4Line4bWithSplitWithholdingStrategy", 0)
    scenario.assertOffset(s"/jobs/#${JOB_3_ID}/w4Line4cWithSplitWithholdingStrategy", 955, -27)
  }

  // Column CS
  test("MFJ, multiple incomes, biggest jobs stops") { td =>
    val scenario = td.scenario
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_1_ID}/w4Line4bWithSplitWithholdingStrategy", 0)
    scenario.assertOffset(s"/jobs/#${JOB_1_ID}/w4Line4cWithSplitWithholdingStrategy", 874, -9)

    scenario.assertEquals(s"/jobs/#${JOB_2_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_2_ID}/w4Line4aWithSplitWithholdingStrategy", 59146)
    scenario.assertEquals(s"/jobs/#${JOB_2_ID}/w4Line4bWithSplitWithholdingStrategy", 0)
    scenario.assertOffset(s"/jobs/#${JOB_2_ID}/w4Line4cWithSplitWithholdingStrategy", 696, -8)

    scenario.assertEquals(s"/jobs/#${JOB_3_ID}/w4Line3WithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_3_ID}/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#${JOB_3_ID}/w4Line4bWithSplitWithholdingStrategy", 0)
    scenario.assertOffset(s"/jobs/#${JOB_3_ID}/w4Line4cWithSplitWithholdingStrategy", 994, -10)
  }

  // Column CT
  test("MFJ, multiple jobs, 2 jobs stop") { td =>
    val scenario = td.scenario
    // TODO: Should be 1459
    scenario.assertOffset(s"/jobs/#$JOB_1_ID/w4Line3WithSplitWithholdingStrategy", 1278, 181)
    scenario.assertEquals(s"/jobs/#$JOB_1_ID/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#$JOB_1_ID/w4Line4bWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#$JOB_1_ID/w4Line4cWithSplitWithholdingStrategy", 0)

    // TODO: Should be 1160
    scenario.assertOffset(s"/jobs/#$JOB_2_ID/w4Line3WithSplitWithholdingStrategy", 992, 168)
    scenario.assertEquals(s"/jobs/#$JOB_2_ID/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#$JOB_2_ID/w4Line4bWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#$JOB_2_ID/w4Line4cWithSplitWithholdingStrategy", 0)

    // TODO: Should be 765
    scenario.assertOffset(s"/jobs/#$JOB_3_ID/w4Line3WithSplitWithholdingStrategy", 639, 126)
    scenario.assertEquals(s"/jobs/#$JOB_3_ID/w4Line4aWithSplitWithholdingStrategy", 0)
    scenario.assertEquals(s"/jobs/#$JOB_3_ID/w4Line4bWithSplitWithholdingStrategy", 1650)
    scenario.assertEquals(s"/jobs/#$JOB_3_ID/w4Line4cWithSplitWithholdingStrategy", 0)
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
    if (fact != sheetInput) throw Exception(s"$factPath ($fact) did not match \"$inputName\" ($sheetInput)")
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
      throw Exception(s"$factPath ($fact) + offset ($dollarOffset) did not match \"$inputName\" ($sheetInput)")
    if (fact != Dollar(expectedValue))
      throw Exception(s"$factPath ($fact) did not match expected value ($expectedValue)")
  }

}

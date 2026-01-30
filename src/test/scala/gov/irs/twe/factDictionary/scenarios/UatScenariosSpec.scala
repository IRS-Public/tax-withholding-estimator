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
  val UAT_SHEET: Path = CSV_ROOT / "twe-uat-2026-01-27.csv"

  case class FixtureParam(scenario: scenarios.Scenario)

  // Calls each test with a "fixture" parameter that contains the scenario named by the test
  // https://www.scalatest.org/scaladoc/3.2.19/org/scalatest/funsuite/AnyFunSuite.html
  def withFixture(test: OneArgTest) = {
    val scenario = scenarios.loadScenarioByName(UAT_SHEET, test.name)
    val fixture = FixtureParam(scenario)
    withFixture(test.toNoArgTest(fixture))
  }

  // Column D
  test("Married filing jointly, salary, 1 child, multiple incomes, car loan interest") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 252000)
    scenario.assertEquals("/totalTaxNetRefundableCredits", 34662)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 594)
    // Scenario-specific assertions
    scenario.assertEquals("/qualifiedPersonalVehicleLoanInterestDeduction", 4600)
  }

  // Column E
  test("Single filer, new job") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 124000)
    scenario.assertEquals("/taxableIncome", 107900)
    scenario.assertEquals("/standardOrItemizedDeduction", 16100)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 3500)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 38)
  }

  // Column F
  test("Single, low wages, 2 dependents") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 22786)
    scenario.assertEquals("/tentativeTaxFromTaxableIncome", 668)
    scenario.assertOffset("/totalEndOfYearProjectedWithholding", 455.71, .29)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 990)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 0)
  }

  // Column G
  test("Head of Household, wages only") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 31200)
    scenario.assertEquals("/tentativeTaxFromTaxableIncome", 708)
    scenario.assertEquals("/additionalCtc", 3400)
    scenario.assertOffset("/earnedIncomeCredit", 5771, 6)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 705)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 0)
  }

  // Column I
  test("Single, self-employed income, IRA contributions") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 49328)
    scenario.assertEquals("/qualifiedBusinessIncomeDeduction", 3346)
    scenario.assertEquals("/tentativeTaxFromTaxableIncome", 3337)
    scenario.assertEquals("/selfEmploymentTax", 2543)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 0)
    scenario.assertOffset("/jobSelectedForExtraWithholding/w4Line4a", 12182, 1)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 147)
  }

  // Column J
  test("Single, wages + dividends + interest + student loan interest") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 51300)
    scenario.assertEquals("/totalTaxNetRefundableCredits", 3619)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 733)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 4500)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 0)
  }

  // Column K
  test("Single, low wages, no dependents, EITC eligible") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 15600)
    scenario.assertOffset("/earnedIncomeCredit", 300, 1)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 0)
  }

  // Column L
  test("Single, wages + overtime income") { td =>
    val scenario = td.scenario
    scenario.graph.set("/isFlsaNonExempt", true)
    scenario.assertEquals("/agi", 98800)
    scenario.assertEquals("/overtimeCompensationDeduction", 7000)
    scenario.assertEquals("/totalTaxNetRefundableCredits", 11372)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 1357)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 0)
  }

  // Column M
  test("Single, wages + tip income") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 104000)
    scenario.assertEquals("/totalTaxNetRefundableCredits", 8556)
    scenario.assertEquals("/qualifiedTipDeduction", 25000)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 5829)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 0)
  }

  // Column N
  test("Single, wages + short term gains + investment income") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 138000)
    scenario.assertEquals("/taxableIncome", 121900)
    scenario.assertEquals("/standardOrItemizedDeduction", 16100)
    scenario.assertEquals("/totalTaxNetRefundableCredits", 21584)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 34000)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 68)
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
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 21600)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 23)
  }

  // Column P
  test("Single, high wages +Cap Gains + Investment") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 55000)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 1544)
    scenario.assertEquals("/incomeTotal", 682000)
    scenario.assertEquals("/agi", 667000)
    scenario.assertEquals("/taxableIncome", 649900)
    scenario.assertEquals("/totalNonRefundableCredits", 5000)
    scenario.assertEquals("/totalRefundableCredits", 0)
    scenario.assertEquals("/additionalMedicareTax", 3708)
    scenario.assertEquals("/selfEmploymentTax", 0)
    scenario.assertEquals("/netInvestmentIncomeTax", 760)
  }

  // Column Q
  test("Q:Single, high wages +Cap Gains + Investment") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/incomeTotal", 682000)
    scenario.assertEquals("/agi", 667000)
    scenario.assertEquals("/stateAndLocalTaxDeduction", 10000)
    scenario.assertEquals("/taxableIncome", 649900)
    scenario.assertEquals("/qualifiedBusinessIncomeDeduction", 0)
    scenario.assertEquals("/netInvestmentIncomeTax", 760)
    scenario.assertEquals("/additionalMedicareTax", 3708)
    scenario.assertEquals("/totalTaxNetRefundableCredits", 188202)
    scenario.assertEquals("/tentativeTaxNetNonRefundableCredits", 183734)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 55000)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 1544)
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
    scenario.assertOffset("/jobSelectedForExtraWithholding/w4Line3", 271, 1)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 3350)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 0)
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
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 4415)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 15000)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 0)
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
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 3661)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 24470)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 0)
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
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 6880)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 0)
    // Scenario-specific assertions
    scenario.assertEquals("/seniorDeduction", 6000)
  }

  // Column AB
  test("Single, low wages, EITC, no dependents") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 13000)
    scenario.assertEquals("/tentativeTaxNetNonRefundableCredits", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 0)
    // Scenario-specific assertions
    scenario.assertOffset("/earnedIncomeCredit", 498, 2)
  }

  // Column AC
  test("SE, Wages, QBI") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 79881)
    scenario.assertOffset("/totalTaxNetRefundableCredits", 11760, 1)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 0)
    scenario.assertOffset("/jobSelectedForExtraWithholding/w4Line4a", 22305, -1)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 209)
    scenario.assertEquals("/qualifiedBusinessIncomeDeduction", 5576)
  }

  // Column AD
  test("MFJ, high itemized deductions (mortgage, SALT, charity, medical)") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 269714)
    scenario.assertOffset("/totalTaxNetRefundableCredits", 31597, 1)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 36622)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 66)
  }

  // Column AF
  test("Single, salary, full deduction") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 52000)
    scenario.assertEquals("/totalTaxNetRefundableCredits", 3097)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 1109)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 0)
  }

  // Column AG
  test("Single, salary, partial deduction, standard") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 112000)
    scenario.assertEquals("/tentativeTaxNetNonRefundableCredits", 14529)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 22)
  }

  // Column AH
  test("Single, pension, SS, partial deduction, standard") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 100800)
    scenario.assertEquals("/tentativeTaxFromTaxableIncome", 11911)
    scenario.assertEquals("/pensionSelectedForExtraWithholding/w4pLine3", 3924)
    scenario.assertEquals("/pensionSelectedForExtraWithholding/w4pLine4a", 40800)
    scenario.assertEquals("/pensionSelectedForExtraWithholding/w4pLine4b", 0)
    scenario.assertEquals("/pensionSelectedForExtraWithholding/w4pLine4c", 0)
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

    scenario.assertEquals("/pensionSelectedForExtraWithholding/w4pLine3", 2492)
    scenario.assertEquals("/pensionSelectedForExtraWithholding/w4pLine4a", 0)
    scenario.assertEquals("/pensionSelectedForExtraWithholding/w4pLine4b", 6550)
    scenario.assertEquals("/pensionSelectedForExtraWithholding/w4pLine4c", 0)
  }

  // Column AJ
  test("MFJ, salary, full deduction, standard") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 81500)
    scenario.assertEquals("/tentativeTaxNetNonRefundableCredits", 4505)
    scenario.assertEquals("/pensionSelectedForExtraWithholding/w4pLine3", 1214)
    scenario.assertEquals("/pensionSelectedForExtraWithholding/w4pLine4a", 0)
    scenario.assertEquals("/pensionSelectedForExtraWithholding/w4pLine4b", 2500)
    scenario.assertEquals("/pensionSelectedForExtraWithholding/w4pLine4c", 0)
  }

  // Column AM
  test("HH, pension, partial deduction, standard") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 84000)
    scenario.assertEquals("/tentativeTaxNetNonRefundableCredits", 1279)
    scenario.assertEquals("/pensionSelectedForExtraWithholding/w4pLine3", 6493)
    scenario.assertEquals("/pensionSelectedForExtraWithholding/w4pLine4a", 0)
    scenario.assertEquals("/pensionSelectedForExtraWithholding/w4pLine4b", 0)
    scenario.assertEquals("/pensionSelectedForExtraWithholding/w4pLine4c", 0)
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
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 2693)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 0)
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
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 9)
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
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 15)
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
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 489)
  }

  // Column AT
  test("MFJ, three tipped jobs, max") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/incomeTotal", 360000)
    scenario.assertEquals("/agi", 360000)
    scenario.assertEquals("/taxableIncome", 295000)
    scenario.assertEquals("/totalNonRefundableCredits", 4400)
    scenario.assertEquals("/totalRefundableCredits", 0)
    scenario.assertEquals("/additionalMedicareTax", 990)
    scenario.assertEquals("/selfEmploymentTax", 0)
    scenario.assertEquals("/netInvestmentIncomeTax", 0)
    scenario.assertEquals("/qualifiedTipDeduction", 25000)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 9112)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 7800)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 0)
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
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 16722)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 5000)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 0)
  }

  // Column AW
  test("Single, 1 job, factor 2, partial deduction") { td =>
    {
      val scenario = td.scenario
      scenario.graph.set("/isFlsaNonExempt", true)

      scenario.assertEquals("/agi", 156000)
      scenario.assertEquals("/overtimeCompensationDeduction", 9400)
      scenario.assertEquals("/taxableIncome", 130500)
      scenario.assertEquals("/totalTaxNetRefundableCredits", 23918)
      scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 2580)
      scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 0)
      scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 0)
      scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 0)
    }
  }

  // Column AX
  test("Single, 1 job, factor 1.5, partial deduction") { td =>
    {
      val scenario = td.scenario
      scenario.graph.set("/isFlsaNonExempt", true)

      scenario.assertEquals("/agi", 254650)
      scenario.assertEquals("/overtimeCompensationDeduction", 9600)
      scenario.assertEquals("/taxableIncome", 228950)
      scenario.assertEquals("/totalTaxNetRefundableCredits", 50260)
      scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 2803)
      scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 0)
      scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 5350)
      scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 0)
    }
  }

  // Column AY
  test("Single, 2 jobs,  partial deduction") { td =>
    {
      val scenario = td.scenario
      scenario.graph.set("/isFlsaNonExempt", true)
      scenario.assertEquals("/agi", 186000)
      scenario.assertEquals("/overtimeCompensationDeduction", 3900)
      scenario.assertEquals("/taxableIncome", 166000)
      scenario.assertEquals("/totalTaxNetRefundableCredits", 32438)
      scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 0)
      scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 0)
      scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 0)
      scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 59)
    }
  }

  // Column AZ
  test("MFj, 2 jobs, maximum deduction") { td =>
    {
      val scenario = td.scenario
      scenario.graph.set("/isFlsaNonExempt", true)
      scenario.assertEquals("/agi", 186000)
      scenario.assertEquals("/overtimeCompensationDeduction", 7500)
      scenario.assertEquals("/taxableIncome", 146300)
      scenario.assertEquals("/totalTaxNetRefundableCredits", 21610)
      scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 1757)
      scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 0)
      scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 0)
      scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 0)
    }
  }

  // Column BA
  test("MFj, 2 jobs, phased out deduction, no SSN spouse") { td =>
    {
      val scenario = td.scenario
      scenario.graph.set("/isFlsaNonExempt", true)
      scenario.assertEquals("/agi", 356000)
      scenario.assertEquals("/overtimeCompensationDeduction", 14400)
      scenario.assertEquals("/taxableIncome", 309400)
      scenario.assertEquals("/totalTaxNetRefundableCredits", 60406)
      scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 0)
      scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 0)
      scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 0)
      scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 560)
    }
  }

  // Column BB
  test("HH, 1 job, tips and OT") { td =>
    val scenario = td.scenario
    scenario.graph.set("/isFlsaNonExempt", true)
    scenario.assertEquals("/agi", 156000)
    scenario.assertEquals("/taxableIncome", 97450)
    scenario.assertEquals("/tentativeTaxNetNonRefundableCredits", 12146)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 11386)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 0)
  }

  // Column BC
  test("MFS, 1 job, tips and OT") { td =>
    val scenario = td.scenario
    scenario.graph.set("/isFlsaNonExempt", true)
    scenario.assertEquals("/agi", 156000)
    scenario.assertEquals("/overtimeCompensationDeduction", 0)

    scenario.assertEquals("/taxableIncome", 139900)
    scenario.assertEquals("/totalTaxNetRefundableCredits", 24253)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 820)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 0)
  }

  // Column BE
  test("Single, phasedout deduction") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 104000)
    scenario.assertEquals("/tentativeTaxNetNonRefundableCredits", 12472)
    scenario.assertEquals("/qualifiedPersonalVehicleLoanInterestDeduction", 7200)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 550)
  }

  // Column BF
  test("Single, zero deduction") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 152000)
    scenario.assertEquals("/tentativeTaxFromTaxableIncome", 25214)
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
    scenario.assertEquals("/tentativeTaxFromTaxableIncome", 12472)
    scenario.assertEquals("/qualifiedPersonalVehicleLoanInterestDeduction", 7200)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 283)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 0)
  }

  // Column BH
  test("MFJ, max amount deduction") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 104000)
    scenario.assertEquals("/tentativeTaxFromTaxableIncome", 6923)
    scenario.assertEquals("/qualifiedPersonalVehicleLoanInterestDeduction", 10000)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 1829)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 0)
  }

  // Column BI
  test("HH,  phased out deduction, CTC, QBI") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 172587)
    scenario.assertEquals("/tentativeTaxFromTaxableIncome", 25520)
    scenario.assertEquals("/qualifiedPersonalVehicleLoanInterestDeduction", 0)
    scenario.assertEquals("/qualifiedBusinessIncomeDeduction", 3717)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 14870)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 260)
  }

  // Column BK
  test("Single, one child, non-refundable") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 52000)
    scenario.assertEquals("/tentativeTaxFromTaxableIncome", 4063)
    scenario.assertEquals("/totalCtcAndOdc", 2200)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 4060)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 0)
  }

  // Column BL
  test("Single, one child, refundable partial") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 38429)
    scenario.assertEquals("/tentativeTaxFromTaxableIncome", 2431)
    scenario.assertEquals("/totalCtcAndOdc", 2200)
    scenario.assertEquals("/earnedIncomeCredit", 2104)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 4060)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 0)
  }

  // Column BM
  test("MFJ, 3 children, one SSN, full refundable") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 31729)
    scenario.assertEquals("/tentativeTaxFromTaxableIncome", 0)
    scenario.assertEquals("/totalCtcAndOdc", 0)
    scenario.assertEquals("/additionalCtc", 5100)
    scenario.assertEquals("/earnedIncomeCredit", 6700)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 1310)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 6700)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 0)
  }

  // Column BN
  test("HH,1 child, non-refundable") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 52000)
    scenario.assertEquals("/tentativeTaxFromTaxableIncome", 2991)
    scenario.assertEquals("/totalCtcAndOdc", 2200)
    scenario.assertEquals("/totalRefundableCredits", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 2526)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 0)
  }

  // Column BO
  test("MFS, 2 child, no SSN") { td =>
    val scenario = td.scenario
    scenario.graph.set("/ctcEligibleDependents", 0) // no ssn
    scenario.assertEquals("/agi", 52000)
    scenario.assertEquals("/tentativeTaxFromTaxableIncome", 4063)
    scenario.assertEquals("/totalCtcAndOdc", 0)
    scenario.assertEquals("/totalRefundableCredits", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 10)
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
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 37174)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 0)

    scenario.assertOffset("/qualifiedBusinessIncomeDeduction", 9293, 1)
    scenario.assertOffset("/jobSelectedForExtraWithholding/w4Line3", 7007, 4)
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
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 669)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 0)

    scenario.assertOffset("/jobSelectedForExtraWithholding/w4Line3", 4304, 2)

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
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 622)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 0)

    scenario.assertOffset("/jobSelectedForExtraWithholding/w4Line3", 4154, -7)
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
    scenario.assertEquals("/additionalMedicareTax", 225)
    scenario.assertEquals("/netInvestmentIncomeTax", 0)
    scenario.assertEquals("/totalTaxNetRefundableCredits", 43182)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 8077)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 753)
  }

  // Column BU
  test("Single, QBI, over high threshold") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 299732)
    scenario.assertEquals("/standardOrItemizedDeduction", 16100)
    scenario.assertEquals("/taxableIncome", 283232)
    scenario.assertEquals("/qualifiedBusinessIncomeDeduction", 400)
    scenario.assertEquals("/selfEmploymentTax", 536)
    scenario.assertEquals("/additionalMedicareTax", 450)
    scenario.assertEquals("/netInvestmentIncomeTax", 1900)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 69332)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 1055)

    scenario.assertOffset("/tentativeTaxNetNonRefundableCredits", 67900, 1)
    scenario.assertOffset("/totalTaxNetRefundableCredits", 70786, 1)
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
    scenario.assertEquals("/additionalMedicareTax", 72)
    scenario.assertEquals("/totalTaxNetRefundableCredits", 36847)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 37174)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 0)
    scenario.assertOffset("/jobSelectedForExtraWithholding/w4Line3", 808, 1)

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
    scenario.assertEquals("/additionalMedicareTax", 522)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 76460)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 0)

    scenario.assertOffset("/tentativeTaxNetNonRefundableCredits", 55870, -3)
    scenario.assertOffset("/tentativeTaxFromTaxableIncome", 58070, -3)
    scenario.assertOffset("/jobSelectedForExtraWithholding/w4Line3", 58069, -2)
  }

  // Column BX
  test("MFS, partial QBI, over high threshold") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 357330)
    scenario.assertEquals("/taxableIncome", 340830)
    scenario.assertEquals("/qualifiedBusinessIncomeDeduction", 400)
    scenario.assertEquals("/additionalMedicareTax", 1197)
    scenario.assertEquals("/selfEmploymentTax", 1339)
    scenario.assertEquals("/netInvestmentIncomeTax", 3800)
    scenario.assertEquals("/tentativeTaxNetNonRefundableCredits", 88060)
    scenario.assertEquals("/totalTaxNetRefundableCredits", 94396)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 48416)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 148930)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 0)
  }

  // Column BZ
  test("Single, SALT under 10k") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 530000)
    scenario.assertEquals("/stateAndLocalTaxDeduction", 9000)

    scenario.assertEquals("/taxableIncome", 513900)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 1601)
  }

  // Column CA
  test("HH, SALT, phased out under 40k") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 530000)
    scenario.assertEquals("/stateAndLocalTaxDeduction", 32900)
    scenario.assertEquals("/taxableIncome", 497100)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 8750)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 720)
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

    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 8200)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 711)
  }

  // Column CC
  test("MFJ, no phase out") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 230000)
    scenario.assertEquals("/stateAndLocalTaxDeduction", 35000)
    scenario.assertEquals("/taxableIncome", 195000)
    scenario.assertEquals("/tentativeTaxNetNonRefundableCredits", 32324)
    scenario.assertEquals("/standardOrItemizedDeduction", 35000)

    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 2800)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 777)

  }

  // Column CD
  test("MFj, SALT, phased out down to 10k") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 634000)
    scenario.assertEquals("/stateAndLocalTaxDeduction", 10000)
    scenario.assertEquals("/taxableIncome", 601800)
    scenario.assertEquals("/standardOrItemizedDeduction", 32200)

    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 820)
  }

  // Column CE
  test("MFS, SALT, phased out") { td =>
    val scenario = td.scenario
    scenario.assertEquals("/agi", 256000)
    scenario.assertEquals("/stateAndLocalTaxDeduction", 19675)
    scenario.assertEquals("/taxableIncome", 236325)
    scenario.assertEquals("/standardOrItemizedDeduction", 19675)

    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 587)
//    The spreadsheet assumes that 100% of the /proportionOfYearEndJobsIncome goes to the job selected to the Extra withholding
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 3575)
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

    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 0)
    scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 0)

    assert(scenario.graph.get("/jobs/#a3006af1-a040-4235-9d31-68c5830c55fd/w4Line4c").value.get === 0)

//    W-4 Line4cAmount1 = 0 and W-4 Line4cAmount2 = 625. Because /jobSelectedForExtraWithholding maps directly to W-4 Line4cAmount1, this will always be a mismatch until we fix how the mapping occurs
//    We instead look Job 2 directly
    assert(scenario.graph.get("/jobs/#8955625f-6317-451b-bce9-48893d60e766/w4Line4c").value.get === 625)
  }

  // Column CK
  test("HH, 2 jobs") { td =>
    {
      val scenario = td.scenario
      scenario.graph.set("/isFlsaNonExempt", true)

      scenario.assertEquals("/agi", 155000)
      scenario.assertEquals("/overtimeCompensationDeduction", 7250)
      assert(scenario.graph.get("/ctcEligibleDependents").value.get === 2) // This is calculated from multiple rows
      scenario.assertEquals("/taxableIncome", 123600)
      scenario.assertEquals("/totalTaxNetRefundableCredits", 16051)

      scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line3", 767)
      scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4a", 0)
      scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4b", 0)
      scenario.assertEquals("/jobSelectedForExtraWithholding/w4Line4c", 0)
    }
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

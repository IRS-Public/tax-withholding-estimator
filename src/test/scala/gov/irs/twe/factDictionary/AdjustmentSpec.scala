package gov.irs.twe.factDictionary

import gov.irs.factgraph.types.Dollar
import gov.irs.factgraph.types.Enum
import gov.irs.factgraph.Path
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.prop.TableDrivenPropertyChecks

class AdjustmentSpec extends AnyFunSuite with TableDrivenPropertyChecks {
  val factDictionary = setupFactDictionary()
  val single = Enum("single", "/filingStatusOptions")
  val mfs = Enum("marriedFilingSeparately", "/filingStatusOptions")
  val mfj = Enum("marriedFilingJointly", "/filingStatusOptions")
  val qss = Enum("qualifiedSurvivingSpouse", "/filingStatusOptions")
  val hoh = Enum("headOfHousehold", "/filingStatusOptions")

  test("test educator expense adjustment: applies correct limit depending on filing status") {
    val dataTable = Table(
      ("status", "totalIncome", "agi", "educatorExpensesAdjustmentAmount", "actualEducatorExpensesAdjustmentAmount"),
      // Non-MFJ cases, up to $300
      (single, "50000", "50000", "0", "0"),
      (single, "50000", "49750", "250", "250"),
      (single, "50000", "49700", "300", "300"),
      (single, "50000", "49700", "500", "300"),
      (mfs, "50000", "50000", "0", "0"),
      (mfs, "50000", "49750", "250", "250"),
      (mfs, "50000", "49700", "300", "300"),
      (mfs, "50000", "49700", "500", "300"),
      (qss, "50000", "50000", "0", "0"),
      (qss, "50000", "49750", "250", "250"),
      (qss, "50000", "49700", "300", "300"),
      (qss, "50000", "49700", "500", "300"),
      (hoh, "50000", "50000", "0", "0"),
      (hoh, "50000", "49750", "250", "250"),
      (hoh, "50000", "49700", "300", "300"),
      (hoh, "50000", "49700", "500", "300"),

      // MFJ cases, up to $600
      (mfj, "50000", "50000", "0", "0"),
      (mfj, "50000", "49750", "250", "250"),
      (mfj, "50000", "49700", "300", "300"),
      (mfj, "50000", "49450", "550", "550"),
      (mfj, "50000", "49400", "600", "600"),
      (mfj, "50000", "49400", "800", "600"),
      (mfj, "50000", "49400", "8000", "600"),
    )

    forAll(dataTable) {
      (status, totalIncome, expectedAgi, educatorExpensesAdjustmentAmount, actualEducatorExpensesAdjustmentAmount) =>
        val graph = makeGraphWith(
          factDictionary,
          Path("/filingStatus") -> status,
          Path("/totalIncome") -> Dollar(totalIncome),
          Path("/educatorExpenses") -> Dollar(educatorExpensesAdjustmentAmount),
          Path("/actualStudentLoanInterestAdjustmentAmount") -> Dollar(0),
          Path("/actualHsaTotalDeductibleAmount") -> Dollar(0),
          Path("/actualDeductionForTraditionalIRAContribution") -> Dollar(0),
          Path("/actualMovingExpensesForArmedServicesMembers") -> Dollar(0),
          Path("/actualAlimonyPaid") -> Dollar(0),
          Path("/actualPenaltyForEarlySavingsWithdrawal") -> Dollar(0),
          Path("/actualBusinessCreditsForEligible") -> Dollar(0),
          Path("/actualSelfEmploymentHealthInsuranceDeduction") -> Dollar(0),
          Path("/selfEmployedRetirementPlanDeduction") -> Dollar(0),
          Path("/totalEstimatedTaxesPaid") -> Dollar(0),
        )

        val actualAdjustments = graph.get("/adjustmentsToIncome")
        val agi = graph.get("/agi")

        assert(actualAdjustments.value.contains(Dollar(actualEducatorExpensesAdjustmentAmount)))
        assert(agi.value.contains(Dollar(expectedAgi)))
    }
    println(
      s"Completed ${dataTable.length} educator expense adjustment tests for calculating itemized deduction limits",
    )
  }
}

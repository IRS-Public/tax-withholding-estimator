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

  test("test HSA deduction: applies correct limit") {
    val dataTable = Table(
      ("status", "expected", "actual"),
      (single, "1000", "1000"),
      (single, "5000", "5000"),
      (single, "8000", "8000"),
      (single, "17000", "17000"),
      (single, "17700", "17000"),
      (single, "20000", "17000"),
      (mfj, "5000", "5000"),
      (mfj, "8000", "8000"),
      (mfj, "17000", "17000"),
      (mfj, "17700", "17000"),
      (mfj, "20000", "17000"),
    )

    forAll(dataTable) { (status, expected, actual) =>
      val graph = makeGraphWith(
        factDictionary,
        Path("/filingStatus") -> status,
        Path("/hsaContributionAmount") -> Dollar(expected),
      )

      val actualAmt = graph.get("/hsaDeduction")

      assert(actualAmt.value.contains(Dollar(actual)))
    }
  }
  test("test IRA deduction: applies correct limit") {
    val dataTable = Table(
      ("status", "expectedDeductionForTraditionalIRAContribution", "actualDeductionForTraditionalIRAContribution"),
      (single, "1000", "1000"),
      (single, "5000", "5000"),
      (single, "8000", "8000"),
      (single, "16000", "16000"),
      (single, "16700", "16000"),
      (single, "20000", "16000"),
      (mfj, "5000", "5000"),
      (mfj, "8000", "8000"),
      (mfj, "16000", "16000"),
      (mfj, "16600", "16000"),
      (mfj, "16700", "16000"),
      (mfj, "20000", "16000"),
    )

    forAll(dataTable) {
      (status, expectedDeductionForTraditionalIRAContribution, actualDeductionForTraditionalIRAContribution) =>
        val graph = makeGraphWith(
          factDictionary,
          Path("/filingStatus") -> status,
          Path("/deductionForTraditionalIRAContribution") -> Dollar(expectedDeductionForTraditionalIRAContribution),
        )

        val actual = graph.get("/traditionalIRAContributionDeduction")

        assert(actual.value.contains(Dollar(actualDeductionForTraditionalIRAContribution)))
    }
  }

  test("test student loan interest adjustment: applies correct limit depending on filing status") {
    val dataTable = Table(
      ("status", "incomeTotal", "agi", "studentLoanInterestAmount", "actualStudentLoanInterestAdjustment"),
      // Non-MFJ
      (single, "50000", "50000", "0", "0"),
      (single, "150000", "150000", "0", "0"),
      (single, "95000", "95000", "0", "0"),
      (single, "250000", "250000", "5000", "0"),
      (single, "80000", "78000", "2000", "2000"),
      (single, "80000", "77500", "2500", "2500"),
      (single, "80000", "77500", "5000", "2500"),
      (single, "85000", "83000", "2000", "2000"),
      (single, "85000", "82500", "5000", "2500"),
      (single, "91100", "90448", "1100", "652"),
      (single, "91100", "90210", "1500", "890"),
      (single, "91100", "89617", "5000", "1483"),
      (single, "95000", "94167", "5000", "833"),
      (single, "100000", "100000", "5000", "0"),
      (single, "105000", "105000", "5000", "0"),
      (hoh, "50000", "50000", "0", "0"),
      (hoh, "150000", "150000", "0", "0"),
      (hoh, "95000", "95000", "0", "0"),
      (hoh, "250000", "250000", "5000", "0"),
      (hoh, "80000", "78000", "2000", "2000"),
      (hoh, "80000", "77500", "2500", "2500"),
      (hoh, "80000", "77500", "5000", "2500"),
      (hoh, "85000", "83000", "2000", "2000"),
      (hoh, "85000", "82500", "5000", "2500"),
      (hoh, "91100", "90448", "1100", "652"),
      (hoh, "91100", "90210", "1500", "890"),
      (hoh, "91100", "89617", "5000", "1483"),
      (hoh, "95000", "94167", "5000", "833"),
      (hoh, "100000", "100000", "5000", "0"),
      (hoh, "105000", "105000", "5000", "0"),
      (qss, "50000", "50000", "0", "0"),
      (qss, "150000", "150000", "0", "0"),
      (qss, "95000", "95000", "0", "0"),
      (qss, "250000", "250000", "5000", "0"),
      (qss, "80000", "78000", "2000", "2000"),
      (qss, "80000", "77500", "2500", "2500"),
      (qss, "80000", "77500", "5000", "2500"),
      (qss, "85000", "83000", "2000", "2000"),
      (qss, "85000", "82500", "5000", "2500"),
      (qss, "91100", "90448", "1100", "652"),
      (qss, "91100", "90210", "1500", "890"),
      (qss, "91100", "89617", "5000", "1483"),
      (qss, "95000", "94167", "5000", "833"),
      (qss, "100000", "100000", "5000", "0"),
      (qss, "105000", "105000", "5000", "0"),

      //      MFS is ineligible
      (mfs, "50000", "50000", "2000", "0"),
      (mfs, "150000", "150000", "2000", "0"),
      (mfs, "95000", "95000", "2000", "0"),
      (mfs, "250000", "250000", "5000", "0"),

      // MFJ
      (mfj, "50000", "50000", "0", "0"),
      (mfj, "150000", "150000", "0", "0"),
      (mfj, "95000", "92500", "5000", "2500"),
      (mfj, "250000", "250000", "5000", "0"),
      (mfj, "80000", "78000", "2000", "2000"),
      (mfj, "80000", "77500", "2500", "2500"),
      (mfj, "80000", "77500", "5000", "2500"),
      (mfj, "85000", "83000", "2000", "2000"),
      (mfj, "85000", "82500", "5000", "2500"),
      (mfj, "91100", "90000", "1100", "1100"),
      (mfj, "91100", "89600", "1500", "1500"),
      (mfj, "91100", "88600", "5000", "2500"),
      (mfj, "165000", "162500", "5000", "2500"),
      (mfj, "165000", "163800", "1200", "1200"),
      (mfj, "170000", "167500", "5000", "2500"),
      (mfj, "170000", "168800", "1200", "1200"),
      (mfj, "175000", "172500", "5000", "2500"),
      (mfj, "175000", "173800", "1200", "1200"),
      (mfj, "180000", "177917", "5000", "2083"),
      (mfj, "180000", "179000", "1200", "1000"),
      (mfj, "185000", "183332", "5000", "1668"),
      (mfj, "185000", "184200", "1200", "800"),
      (mfj, "190000", "188750", "5000", "1250"),
      (mfj, "190000", "189400", "1200", "600"),
      (mfj, "195000", "194167", "5000", "833"),
      (mfj, "195000", "194600", "1200", "400"),
      (mfj, "200000", "199800", "1200", "200"),
      (mfj, "205000", "205000", "1200", "0"),
      (mfj, "206000", "206000", "1200", "0"),
      (mfj, "210000", "210000", "1200", "0"),
    )

    forAll(dataTable) {
      (status, incomeTotal, expectedAgi, studentLoanInterestAmount, actualStudentLoanInterestAdjustment) =>
        val graph = makeGraphWith(
          factDictionary,
          Path("/filingStatus") -> status,
          Path("/incomeTotal") -> Dollar(incomeTotal),
          Path("/studentLoanInterestAmount") -> Dollar(studentLoanInterestAmount),
          Path("/educatorExpensesAdjustmentTotal") -> Dollar(0),
          Path("/businessCreditsForEligible") -> Dollar(0),
          Path("/hsaDeduction") -> Dollar(0),
          Path("/selfEmploymentTaxDeduction") -> Dollar(0),
          Path("/movingExpensesForArmedServicesMembers") -> Dollar(0),
          Path("/selfEmploymentRetirementPlanDeduction") -> Dollar(0),
          Path("/selfEmploymentHealthInsuranceDeduction") -> Dollar(0),
          Path("/penaltyForEarlySavingsWithdrawal") -> Dollar(0),
          Path("/alimonyPaid") -> Dollar(0),
          Path("/traditionalIRAContributionDeduction") -> Dollar(0),
        )

        val actualAdjustments = graph.get("/adjustmentsToIncome")
        val agi = graph.get("/agi")

        assert(actualAdjustments.value.contains(Dollar(actualStudentLoanInterestAdjustment)))
        assert(agi.value.contains(Dollar(expectedAgi)))
    }
  }
}

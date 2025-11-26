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
      ("status", "expectedHsaTotalDeductibleAmount", "actualHsaTotalDeductibleAmount"),
      (single, "1000", "1000"),
      (single, "5000", "5000"),
      (single, "8000", "8000"),
      (single, "16600", "16600"),
      (single, "16700", "16600"),
      (single, "20000", "16600"),
      (mfj, "5000", "5000"),
      (mfj, "8000", "8000"),
      (mfj, "16600", "16600"),
      (mfj, "16700", "16600"),
      (mfj, "20000", "16600"),
    )

    forAll(dataTable) { (status, expectedHsaTotalDeductibleAmount, actualHsaTotalDeductibleAmount) =>
      val graph = makeGraphWith(
        factDictionary,
        Path("/filingStatus") -> status,
        Path("/hsaTotalDeductibleAmount") -> Dollar(expectedHsaTotalDeductibleAmount),
      )

      val actual = graph.get("/hsaDeduction")

      assert(actual.value.contains(Dollar(actualHsaTotalDeductibleAmount)))
    }
    println(
      s"Completed ${dataTable.length} HSA deduction scenarios",
    )
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
    println(
      s"Completed ${dataTable.length} IRA deduction scenarios",
    )
  }

  test("test educator expense adjustment: applies correct limit depending on filing status") {
    val dataTable = Table(
      ("status", "incomeTotal", "agi", "educatorExpensesAdjustmentAmount", "actualEducatorExpensesAdjustmentAmount"),
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
      (status, incomeTotal, expectedAgi, educatorExpensesAdjustmentAmount, actualEducatorExpensesAdjustmentAmount) =>
        val graph = makeGraphWith(
          factDictionary,
          Path("/filingStatus") -> status,
          Path("/incomeTotal") -> Dollar(incomeTotal),
          Path("/educatorExpenses") -> Dollar(educatorExpensesAdjustmentAmount),
          Path("/businessCreditsForEligible") -> Dollar(0),
          Path("/hsaDeduction") -> Dollar(0),
          Path("/selfEmploymentTaxDeduction") -> Dollar(0),
          Path("/movingExpensesForArmedServicesMembers") -> Dollar(0),
          Path("/selfEmploymentRetirementPlanDeduction") -> Dollar(0),
          Path("/selfEmploymentHealthInsuranceDeduction") -> Dollar(0),
          Path("/penaltyForEarlySavingsWithdrawal") -> Dollar(0),
          Path("/alimonyPaid") -> Dollar(0),
          Path("/traditionalIRAContributionDeduction") -> Dollar(0),
          Path("/studentLoanInterestDeduction") -> Dollar(0),
        )

        val actualAdjustments = graph.get("/adjustmentsToIncome")
        val agi = graph.get("/agi")

        assert(actualAdjustments.value.contains(Dollar(actualEducatorExpensesAdjustmentAmount)))
        assert(agi.value.contains(Dollar(expectedAgi)))
    }
    println(
      s"Completed ${dataTable.length} educator expense adjustment scenarios for calculating agi and total adjustments",
    )
  }

  test("test student loan interest adjustment: applies correct limit depending on filing status") {
    val dataTable = Table(
      ("status", "incomeTotal", "agi", "studentLoanInterestAmount", "actualStudentLoanInterestAdjustment"),
      // Non-MFJ
      (single, "50000", "50000", "0", "0"),
      (single, "150000", "150000", "0", "0"),
      (single, "95000", "95000", "5000", "0"),
      (single, "250000", "250000", "5000", "0"),
      (single, "80000", "78000", "2000", "2000"),
      (single, "80000", "77500", "2500", "2500"),
      (single, "80000", "77500", "5000", "2500"),
      (single, "85000", "83666", "2000", "1334"),
      (single, "85000", "83332", "5000", "1668"),
      (single, "91100", "90814", "1100", "286"),
      (single, "91100", "90710", "1500", "390"),
      (single, "91100", "90450", "5000", "650"),
      (hoh, "50000", "50000", "0", "0"),
      (hoh, "150000", "150000", "0", "0"),
      (hoh, "95000", "95000", "5000", "0"),
      (hoh, "250000", "250000", "5000", "0"),
      (hoh, "80000", "78000", "2000", "2000"),
      (hoh, "80000", "77500", "2500", "2500"),
      (hoh, "80000", "77500", "5000", "2500"),
      (hoh, "85000", "83666", "2000", "1334"),
      (hoh, "85000", "83332", "5000", "1668"),
      (hoh, "91100", "90814", "1100", "286"),
      (hoh, "91100", "90710", "1500", "390"),
      (hoh, "91100", "90450", "5000", "650"),
      (qss, "50000", "50000", "0", "0"),
      (qss, "150000", "150000", "0", "0"),
      (qss, "95000", "95000", "5000", "0"),
      (qss, "250000", "250000", "5000", "0"),
      (qss, "80000", "78000", "2000", "2000"),
      (qss, "80000", "77500", "2500", "2500"),
      (qss, "80000", "77500", "5000", "2500"),
      (qss, "85000", "83666", "2000", "1334"),
      (qss, "85000", "83332", "5000", "1668"),
      (qss, "91100", "90814", "1100", "286"),
      (qss, "91100", "90710", "1500", "390"),
      (qss, "91100", "90450", "5000", "650"),
      (mfs, "50000", "50000", "0", "0"),
      (mfs, "150000", "150000", "0", "0"),
      (mfs, "95000", "95000", "5000", "0"),
      (mfs, "250000", "250000", "5000", "0"),
      (mfs, "80000", "80000", "2000", "0"),
      (mfs, "80000", "80000", "2500", "0"),
      (mfs, "80000", "80000", "5000", "0"),
      (mfs, "85000", "85000", "2000", "0"),
      (mfs, "85000", "85000", "5000", "0"),
      (mfs, "91100", "91100", "1100", "0"),
      (mfs, "91100", "91100", "1500", "0"),
      (mfs, "91100", "91100", "5000", "0"),
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
      (mfj, "170000", "167917", "5000", "2083"),
      (mfj, "170000", "169000", "1200", "1000"),
      (mfj, "175000", "173332", "5000", "1668"),
      (mfj, "175000", "174200", "1200", "800"),
      (mfj, "180000", "178750", "5000", "1250"),
      (mfj, "180000", "179400", "1200", "600"),
      (mfj, "185000", "184167", "5000", "833"),
      (mfj, "185000", "184600", "1200", "400"),
      (mfj, "190000", "189582", "5000", "418"),
      (mfj, "190000", "189800", "1200", "200"),
      (mfj, "195000", "195000", "5000", "0"),
      (mfj, "195000", "195000", "1200", "0"),
    )

    forAll(dataTable) {
      (status, incomeTotal, expectedAgi, studentLoanInterestAmount, actualStudentLoanInterestAdjustment) =>
        val graph = makeGraphWith(
          factDictionary,
          Path("/filingStatus") -> status,
          Path("/incomeTotal") -> Dollar(incomeTotal),
          Path("/studentLoanInterestAmount") -> Dollar(studentLoanInterestAmount),
          Path("/educatorExpensesAdjustment") -> Dollar(0),
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
    println(
      s"Completed ${dataTable.length} student loan interest adjustments for calculating agi and total adjustments",
    )
  }

  test(
    "test educator expense and student loan interest adjustment: applies correct limit and phase out depending on filing status",
  ) {
    val dataTable = Table(
      (
        "status",
        "incomeTotal",
        "agi",
        "educatorExpensesAdjustmentAmount",
        "actualEducatorExpensesAdjustmentAmount",
        "studentLoanInterestAmount",
        "actualStudentLoanInterestAdjustment",
      ),
      // Non-MFJ cases, up to $300 educator expense and 2500 deduction with 80-95k MAGI phase out
      (single, "50000", "47750", "250", "250", "2000", "2000"),
      (single, "50000", "47700", "300", "300", "2000", "2000"),
      (single, "50000", "47700", "500", "300", "2000", "2000"),
      (single, "50000", "48700", "500", "300", "1000", "1000"),
      (single, "80000", "78700", "500", "300", "1000", "1000"),
      (single, "85000", "83326", "500", "300", "2000", "1374"),
      (single, "90000", "88994", "500", "300", "2000", "706"),
      (single, "95000", "94700", "500", "300", "0", "0"),
      (single, "150000", "149700", "500", "300", "0", "0"),
      (hoh, "50000", "47750", "250", "250", "2000", "2000"),
      (hoh, "50000", "47700", "300", "300", "2000", "2000"),
      (hoh, "50000", "47700", "500", "300", "2000", "2000"),
      (hoh, "50000", "48700", "500", "300", "1000", "1000"),
      (hoh, "80000", "78700", "500", "300", "1000", "1000"),
      (hoh, "85000", "83326", "500", "300", "2000", "1374"),
      (hoh, "90000", "88994", "500", "300", "2000", "706"),
      (hoh, "95000", "94700", "500", "300", "0", "0"),
      (hoh, "150000", "149700", "500", "300", "0", "0"),
      (qss, "50000", "47750", "250", "250", "2000", "2000"),
      (qss, "50000", "47700", "300", "300", "2000", "2000"),
      (qss, "50000", "47700", "500", "300", "2000", "2000"),
      (qss, "50000", "48700", "500", "300", "1000", "1000"),
      (qss, "80000", "78700", "500", "300", "1000", "1000"),
      (qss, "85000", "83326", "500", "300", "2000", "1374"),
      (qss, "90000", "88994", "500", "300", "2000", "706"),
      (qss, "95000", "94700", "500", "300", "0", "0"),
      (qss, "150000", "149700", "500", "300", "0", "0"),

      // MFS is ineligible for student loan interest adjustment, but remains eligible for educator expense adjustment
      (mfs, "50000", "49750", "250", "250", "2500", "0"),
      (mfs, "50000", "49700", "300", "300", "2500", "0"),
      (mfs, "50000", "49700", "500", "300", "2500", "0"),

      // MFJ cases, up to $600
      (mfj, "50000", "47750", "250", "250", "2000", "2000"),
      (mfj, "50000", "47700", "300", "300", "2000", "2000"),
      (mfj, "50000", "47400", "600", "600", "2000", "2000"),
      (mfj, "50000", "48400", "600", "600", "1000", "1000"),
      (mfj, "80000", "78400", "600", "600", "1000", "1000"),
      (mfj, "85000", "82400", "600", "600", "2000", "2000"),
      (mfj, "90000", "87400", "600", "600", "2000", "2000"),
      (mfj, "165000", "162200", "300", "300", "5000", "2500"),
      (mfj, "170000", "167267", "600", "600", "5000", "2133"),
      (mfj, "175000", "172900", "400", "400", "5000", "1700"),
      (mfj, "180000", "178100", "600", "600", "5000", "1300"),
      (mfj, "185000", "183625", "500", "500", "5000", "875"),
      (mfj, "190000", "188932", "600", "600", "5000", "468"),
      (mfj, "195000", "194457", "500", "500", "5000", "43"),
      (mfj, "195000", "194350", "600", "600", "5000", "50"),
      (mfj, "200000", "199500", "500", "500", "5000", "0"),
    )

    forAll(dataTable) {
      (
          status,
          incomeTotal,
          expectedAgi,
          educatorExpensesAdjustmentAmount,
          actualEducatorExpensesAdjustmentAmount,
          studentLoanInterestAmount,
          actualStudentLoanInterestAdjustment,
      ) =>
        val graph = makeGraphWith(
          factDictionary,
          Path("/filingStatus") -> status,
          Path("/incomeTotal") -> Dollar(incomeTotal),
          Path("/educatorExpenses") -> Dollar(educatorExpensesAdjustmentAmount),
          Path("/studentLoanInterestAmount") -> Dollar(studentLoanInterestAmount),
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
        val expectedAdjustments = Dollar.DollarIsFractional.plus(
          Dollar(actualEducatorExpensesAdjustmentAmount),
          Dollar(actualStudentLoanInterestAdjustment),
        )
        assert(actualAdjustments.value.contains(expectedAdjustments))
        assert(agi.value.contains(Dollar(expectedAgi)))
    }
    println(
      s"Completed ${dataTable.length} educator expense adjustment scenarios for calculating agi and total adjustments",
    )
  }
}

package gov.irs.twe.factDictionary

import gov.irs.factgraph.types.Dollar
import gov.irs.factgraph.types.Enum
import gov.irs.factgraph.FactDictionaryForTests
import gov.irs.factgraph.Path
import gov.irs.twe.FileLoaderHelper
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.prop.TableDrivenPropertyChecks

class SelfEmploymentSpec extends AnyFunSuite with TableDrivenPropertyChecks {
  val factDictionary = setupFactDictionary()
  val single = Enum("single", "/filingStatusOptions")
  val mfs = Enum("marriedFilingSeparately", "/filingStatusOptions")
  val mfj = Enum("marriedFilingJointly", "/filingStatusOptions")
  val qss = Enum("qualifiedSurvivingSpouse", "/filingStatusOptions")
  val hoh = Enum("headOfHousehold", "/filingStatusOptions")
  val netEarningsFromSelfEmployment = Path("/netSelfEmploymentIncome")
  val selfEmploymentTaxResult = Path("/selfEmploymentTax")

// this table accounts for totalJobIncome as a lump fact, eventually it will be split by self and spouse
  val dataTable = Table(
    (
      "status",
      "netSelfEmploymentIncomeSelf",
      "netSelfEmploymentIncomeSpouse",
      "jobsIncomeSelf",
      "jobsIncomeSpouse",
      "expectedScheduleSELine12Self",
      "expectedScheduleSELine12Spouse",
      "expectedScheduleSELine13Self",
      "expectedScheduleSELine13Spouse",
      "expectedSelfEmploymentTaxTotal",
      "expectedSelfEmploymentTaxDeductionTotal",
    ),
    ////////////////////////////// NON-MFJ///////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////// WAGES ONLY, NO SE INCOME///////////////////////////////////////////
    (single, "0", "0", "20000", "0", "0", "0", "0", "0", "0", "0"),
    (qss, "0", "0", "20000", "0", "0", "0", "0", "0", "0", "0"),
    (mfs, "0", "0", "20000", "0", "0", "0", "0", "0", "0", "0"),
    (hoh, "0", "0", "20000", "0", "0", "0", "0", "0", "0", "0"),
    (qss, "0", "0", "20000", "0", "0", "0", "0", "0", "0", "0"),

    ////////////////////////////// NO WAGES, SE INCOME ONLY///////////////////////////////////////////
    (single, "20000", "0", "0", "0", "2826", "0", "1413", "0", "2826", "1413"),
    (single, "74000", "0", "0", "0", "10456", "0", "5228", "0", "10456", "5228"),
    (single, "176100", "0", "0", "0", "24882", "0", "12441", "0", "24882", "12441"),
    (single, "180000", "0", "0", "0", "25434", "0", "12717", "0", "25434", "12717"),
    (single, "150000", "0", "0", "0", "21194", "0", "10597", "0", "21194", "10597"),
    (single, "200000", "0", "0", "0", "28234", "0", "14117", "0", "28234", "14117"),
    (single, "235200", "0", "0", "0", "29177", "0", "14589", "0", "29177", "14589"),
    (mfs, "20000", "0", "0", "0", "2826", "0", "1413", "0", "2826", "1413"),
    (mfs, "74000", "0", "0", "0", "10456", "0", "5228", "0", "10456", "5228"),
    (mfs, "176100", "0", "0", "0", "24882", "0", "12441", "0", "24882", "12441"),
    (mfs, "180000", "0", "0", "0", "25434", "0", "12717", "0", "25434", "12717"),
    (mfs, "150000", "0", "0", "0", "21194", "0", "10597", "0", "21194", "10597"),
    (mfs, "200000", "0", "0", "0", "28234", "0", "14117", "0", "28234", "14117"),
    (mfs, "235200", "0", "0", "0", "29177", "0", "14589", "0", "29177", "14589"),
    (hoh, "20000", "0", "0", "0", "2826", "0", "1413", "0", "2826", "1413"),
    (hoh, "74000", "0", "0", "0", "10456", "0", "5228", "0", "10456", "5228"),
    (hoh, "176100", "0", "0", "0", "24882", "0", "12441", "0", "24882", "12441"),
    (hoh, "180000", "0", "0", "0", "25434", "0", "12717", "0", "25434", "12717"),
    (hoh, "150000", "0", "0", "0", "21194", "0", "10597", "0", "21194", "10597"),
    (hoh, "200000", "0", "0", "0", "28234", "0", "14117", "0", "28234", "14117"),
    (hoh, "235200", "0", "0", "0", "29177", "0", "14589", "0", "29177", "14589"),
    (qss, "20000", "0", "0", "0", "2826", "0", "1413", "0", "2826", "1413"),
    (qss, "74000", "0", "0", "0", "10456", "0", "5228", "0", "10456", "5228"),
    (qss, "176100", "0", "0", "0", "24882", "0", "12441", "0", "24882", "12441"),
    (qss, "180000", "0", "0", "0", "25434", "0", "12717", "0", "25434", "12717"),
    (qss, "150000", "0", "0", "0", "21194", "0", "10597", "0", "21194", "10597"),
    (qss, "200000", "0", "0", "0", "28234", "0", "14117", "0", "28234", "14117"),
    (qss, "235200", "0", "0", "0", "29177", "0", "14589", "0", "29177", "14589"),

    ////////////////////////////// WAGES AND SE INCOME///////////////////////////////////////////
    (single, "15000", "0", "74000", "0", "2120", "0", "1060", "0", "2120", "1060"),
    (single, "24210", "0", "150000", "0", "3420", "0", "1710", "0", "3420", "1710"),
    (single, "24210", "0", "200000", "0", "648", "0", "324", "0", "648", "324"),
    (single, "25000", "0", "235200", "0", "670", "0", "335", "0", "670", "335"),
    (mfs, "15000", "0", "74000", "0", "2120", "0", "1060", "0", "2120", "1060"),
    (mfs, "24210", "0", "150000", "0", "3420", "0", "1710", "0", "3420", "1710"),
    (mfs, "24210", "0", "200000", "0", "648", "0", "324", "0", "648", "324"),
    (mfs, "25000", "0", "235200", "0", "670", "0", "335", "0", "670", "335"),
    (hoh, "15000", "0", "74000", "0", "2120", "0", "1060", "0", "2120", "1060"),
    (hoh, "24210", "0", "150000", "0", "3420", "0", "1710", "0", "3420", "1710"),
    (hoh, "24210", "0", "200000", "0", "648", "0", "324", "0", "648", "324"),
    (hoh, "25000", "0", "235200", "0", "670", "0", "335", "0", "670", "335"),
    (qss, "15000", "0", "74000", "0", "2120", "0", "1060", "0", "2120", "1060"),
    (qss, "24210", "0", "150000", "0", "3420", "0", "1710", "0", "3420", "1710"),
    (qss, "24210", "0", "200000", "0", "648", "0", "324", "0", "648", "324"),
    (qss, "25000", "0", "235200", "0", "670", "0", "335", "0", "670", "335"),

    ////////////////////////////// MFJ///////////////////////////////////////////

    ////////////////////////////// WAGES ONLY, NO SE INCOME///////////////////////////////////////////
    (mfj, "0", "0", "20000", "0", "0", "0", "0", "0", "0", "0"),

    ////////////////////////////// WAGES AND SELF SE INCOME, NO SPOUSE SE INCOME///////////////////////////////////////////
    (mfj, "15000", "0", "74000", "0", "2120", "0", "1060", "0", "2120", "1060"),
    (mfj, "24210", "0", "150000", "0", "3420", "0", "1710", "0", "3420", "1710"),
    (mfj, "24210", "0", "200000", "0", "648", "0", "324", "0", "648", "324"),
    (mfj, "25000", "0", "235200", "0", "670", "0", "335", "0", "670", "335"),

    ////////////////////////////// WAGES AND SPOUSE SE INCOME, NO SELF SE INCOME///////////////////////////////////////////
    (mfj, "0", "15000", "0", "74000", "0", "2120", "0", "1060", "2120", "1060"),
    (mfj, "0", "24210", "0", "150000", "0", "3420", "0", "1710", "3420", "1710"),
    (mfj, "0", "24210", "0", "200000", "0", "648", "0", "324", "648", "324"),
    (mfj, "0", "25000", "0", "235200", "0", "670", "0", "335", "670", "335"),
    ////////////////////////////// WAGES AND SELF SE INCOME AND SPOUSE SE INCOME///////////////////////////////////////////
    (mfj, "10000", "15000", "54000", "20000", "1413", "2120", "707", "1060", "3533", "1767"),
    (mfj, "20000", "15000", "54000", "20000", "2826", "2120", "1413", "1060", "4946", "2473"),
    (mfj, "10000", "24210", "100000", "50000", "1413", "3420", "707", "1710", "4833", "2417"),
    (mfj, "10000", "24210", "100000", "100000", "1413", "3420", "707", "1710", "4833", "2417"),
    (mfj, "10000", "25000", "135200", "100000", "1413", "3533", "707", "1767", "4946", "2474"),
    (mfj, "100000", "150000", "135200", "100000", "8791", "14495", "4396", "7248", "23286", "11644"),
    ////////////////////////////// NO WAGES AND SELF SE INCOME AND SPOUSE SE INCOME///////////////////////////////////////////
    (mfj, "10000", "15000", "0", "0", "1413", "2120", "707", "1060", "3533", "1767"),
    (mfj, "100000", "15000", "0", "0", "14129", "2120", "7065", "1060", "16249", "8125"),
    (mfj, "100000", "65000", "0", "0", "14129", "9184", "7065", "4592", "23313", "11657"),
    (mfj, "100000", "76100", "0", "0", "14129", "10753", "7065", "5377", "24882", "12442"),
    (mfj, "100000", "80000", "0", "0", "14129", "11304", "7065", "5652", "25433", "12717"),
    (mfj, "100000", "150000", "0", "0", "14129", "21194", "7065", "10597", "35323", "17662"),
  )

  test("test self employment tax and deduction scenarios") {
    forAll(dataTable) {
      (
          status,
          netSelfEmploymentIncomeSelf,
          netSelfEmploymentIncomeSpouse,
          jobsIncomeSelf,
          jobsIncomeSpouse,
          expectedScheduleSELine12Self,
          expectedScheduleSELine12Spouse,
          expectedScheduleSELine13Self,
          expectedScheduleSELine13Spouse,
          expectedSelfEmploymentTaxTotal,
          expectedSelfEmploymentTaxDeductionTotal,
      ) =>
        val graph = makeGraphWith(
          factDictionary,
          Path("/filingStatus") -> status,
          Path("/netSelfEmploymentIncomeSelf") -> Dollar(netSelfEmploymentIncomeSelf),
          Path("/netSelfEmploymentIncomeSpouse") -> Dollar(netSelfEmploymentIncomeSpouse),
          Path("/jobsIncomeSelf") -> Dollar(jobsIncomeSelf),
          Path("/jobsIncomeSpouse") -> Dollar(jobsIncomeSpouse),
        )
        val actualScheduleSELine12Self = graph.get("/scheduleSELine12Self")
        val actualScheduleSELine12Spouse = graph.get("/scheduleSELine12Spouse")
        val actualScheduleSELine13Self = graph.get("/scheduleSELine13Self")
        val actualScheduleSELine13Spouse = graph.get("/scheduleSELine13Spouse")
        val actualSelfEmploymentTaxTotal = graph.get("/selfEmploymentTax")
        val actualSelfEmploymentTaxDeductionTotal = graph.get("/selfEmploymentTaxDeduction")

        assert(actualScheduleSELine12Self.value.contains(Dollar(expectedScheduleSELine12Self)))
        assert(actualScheduleSELine12Spouse.value.contains(Dollar(expectedScheduleSELine12Spouse)))
        assert(actualScheduleSELine13Self.value.contains(Dollar(expectedScheduleSELine13Self)))
        assert(actualScheduleSELine13Spouse.value.contains(Dollar(expectedScheduleSELine13Spouse)))
        assert(actualSelfEmploymentTaxTotal.value.contains(Dollar(expectedSelfEmploymentTaxTotal)))
        assert(actualSelfEmploymentTaxDeductionTotal.value.contains(Dollar(expectedSelfEmploymentTaxDeductionTotal)))
    }
    println(s"Completed ${dataTable.length} tests for calculating self-employment tax and SE tax deduction")
  }

  test("test IRA deduction: applies correct limit") {
    val dataTable = Table(
      (
        "status",
        "netSelfEmploymentIncomeTotal",
        "selfEmployedRetirementPlanContributions",
        "selfEmploymentTaxDeduction",
        "expectedActualSelfEmployedRetirementPlanDeduction",
      ),

      // contribution is higher than (net SE income - SE tax deduction)
      (single, "20000", "19000", "1413", "18587"),
      // contribution is lower than (net SE income - SE tax deduction)
      (single, "20000", "5000", "1413", "5000"),
      (single, "74000", "10000", "5228", "10000"),
      (single, "176100", "10000", "12441", "10000"),
      (single, "180000", "20000", "12717", "20000"),
    )

    forAll(dataTable) {
      (
          status,
          netSelfEmploymentIncomeSelf,
          selfEmployedRetirementPlanContributions,
          selfEmploymentTaxDeduction,
          expectedActualSelfEmployedRetirementPlanDeduction,
      ) =>
        val graph = makeGraphWith(
          factDictionary,
          Path("/filingStatus") -> status,
          Path("/netSelfEmploymentIncomeSelf") -> Dollar(netSelfEmploymentIncomeSelf),
          Path("/netSelfEmploymentIncomeSpouse") -> Dollar(0),
          Path("/selfEmploymentRetirementPlanContributions") -> Dollar(selfEmployedRetirementPlanContributions),
          Path("/selfEmploymentTaxDeduction") -> Dollar(selfEmploymentTaxDeduction),
        )

        val actualRetirementPlanDeduction = graph.get("/selfEmploymentRetirementPlanDeduction")

        assert(actualRetirementPlanDeduction.value.contains(Dollar(expectedActualSelfEmployedRetirementPlanDeduction)))
    }
    println(
      s"Completed ${dataTable.length} SE retirement plan deduction scenarios",
    )
  }

  test("test SE health insurance deduction: applies correct limit") {
    val dataTable = Table(
      (
        "status",
        "netSelfEmploymentIncomeSelf",
        "netSelfEmploymentIncomeSpouse",
        "selfEmploymentHealthInsuranceContributions",
        "selfEmploymentTaxDeduction",
        "selfEmployedRetirementPlanDeduction",
        "expectedSelfEmploymentHealthInsuranceContributions",
      ),
      (single, "30000", "0", "5890", "2295", "12000", "5890"),
      (single, "87600", "0", "12000", "6189", "0", "12000"),
      (mfj, "15000", "15000", "5890", "2295", "12000", "5890"),
      (mfj, "40000", "47600", "12000", "6189", "0", "12000"),
    )

    forAll(dataTable) {
      (
          status,
          netSelfEmploymentIncomeSelf,
          netSelfEmploymentIncomeSpouse,
          selfEmploymentHealthInsuranceContributions,
          selfEmploymentTaxDeduction,
          actualSelfEmployedRetirementPlanDeduction,
          expectedSelfEmploymentHealthInsuranceDeduction,
      ) =>
        val graph = makeGraphWith(
          factDictionary,
          Path("/filingStatus") -> status,
          Path("/netSelfEmploymentIncomeSelf") -> Dollar(netSelfEmploymentIncomeSelf),
          Path("/netSelfEmploymentIncomeSpouse") -> Dollar(netSelfEmploymentIncomeSpouse),
          Path("/selfEmploymentHealthInsuranceContributions") -> Dollar(selfEmploymentHealthInsuranceContributions),
          Path("/selfEmploymentTaxDeduction") -> Dollar(selfEmploymentTaxDeduction),
          Path("/selfEmploymentRetirementPlanDeduction") -> Dollar(actualSelfEmployedRetirementPlanDeduction),
        )

        val actual = graph.get("/selfEmploymentHealthInsuranceDeduction")

        assert(actual.value.contains(Dollar(expectedSelfEmploymentHealthInsuranceDeduction)))
    }
    println(
      s"Completed ${dataTable.length} SE health insuracne deduction scenarios",
    )
  }

}

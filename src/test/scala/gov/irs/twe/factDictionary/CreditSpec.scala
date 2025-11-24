package gov.irs.twe.factDictionary

import gov.irs.factgraph.types.Dollar
import gov.irs.factgraph.types.Enum
import gov.irs.factgraph.Path
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.prop.TableDrivenPropertyChecks

class CreditSpec extends AnyFunSuite with TableDrivenPropertyChecks {
  val factDictionary = setupFactDictionary()
  val single = Enum("single", "/filingStatusOptions")
  val mfs = Enum("marriedFilingSeparately", "/filingStatusOptions")
  val mfj = Enum("marriedFilingJointly", "/filingStatusOptions")
  val qss = Enum("qualifiedSurvivingSpouse", "/filingStatusOptions")
  val hoh = Enum("headOfHousehold", "/filingStatusOptions")

  test("test CTC: derives and phaseouts correctly") {
    val dataTable = Table(
      ("status", "agi", "ctcEligibleDependents", "expectedTotalCtc"),
      // Non-MFJ cases, max CTC is the lesser of total tentative tax and potential phase out

      (single, "20000", 0, "0"),
      (single, "20000", 1, "428"),
      (single, "20000", 2, "428"),
      (single, "20000", 3, "428"),
      (single, "20000", 4, "428"),
      (single, "50000", 0, "0"),
      (single, "50000", 1, "2000"),
      (single, "50000", 2, "3875"),
      (single, "50000", 3, "3875"),
      (single, "50000", 4, "3875"),
      (single, "70000", 0, "0"),
      (single, "70000", 1, "2000"),
      (single, "70000", 2, "4000"),
      (single, "70000", 3, "6000"),
      (single, "70000", 4, "6855"),
      (single, "70000", 5, "6855"),
      // Phaseout begins to apply at 200k AGI
      (single, "190000", 1, "2000"),
      (single, "200000", 1, "2000"),
      (single, "210000", 1, "1500"),
      (single, "220000", 1, "1000"),
      (single, "230000", 1, "500"),
      (single, "240000", 1, "0"),
      (single, "190000", 2, "4000"),
      (single, "200000", 2, "4000"),
      (single, "210000", 2, "3500"),
      (single, "220000", 2, "3000"),
      (single, "230000", 2, "2500"),
      (single, "240000", 2, "2000"),
      (mfj, "20000", 0, "0"),
      (mfj, "20000", 1, "0"),
      (mfj, "20000", 2, "0"),
      (mfj, "20000", 3, "0"),
      (mfj, "20000", 4, "0"),
      (mfj, "50000", 0, "0"),
      (mfj, "50000", 1, "1853"),
      (mfj, "50000", 2, "1853"),
      (mfj, "50000", 3, "1853"),
      (mfj, "50000", 4, "1853"),
      (mfj, "70000", 0, "0"),
      (mfj, "70000", 1, "2000"),
      (mfj, "70000", 2, "4000"),
      (mfj, "70000", 3, "4146"),
      (mfj, "70000", 4, "4146"),
      (mfj, "70000", 5, "4146"),
      // Phaseout begins to apply at 400k AGI
      (mfj, "390000", 1, "2000"),
      (mfj, "400000", 1, "2000"),
      (mfj, "410000", 1, "1500"),
      (mfj, "420000", 1, "1000"),
      (mfj, "430000", 1, "500"),
      (mfj, "440000", 1, "0"),
      (mfj, "390000", 2, "4000"),
      (mfj, "400000", 2, "4000"),
      (mfj, "410000", 2, "3500"),
      (mfj, "420000", 2, "3000"),
      (mfj, "430000", 2, "2500"),
      (mfj, "440000", 2, "2000"),
    )

    forAll(dataTable) { (status, agi, ctcEligibleDependents, expectedTotalCtc) =>
      val graph = makeGraphWith(
        factDictionary,
        Path("/filingStatus") -> status,
        Path("/agi") -> Dollar(agi),
        Path("/ctcEligibleDependents") -> ctcEligibleDependents,
        Path("/treatFilersAsDependents") -> false,
        Path("/primaryFilerIsBlind") -> false,
        Path("/primaryFilerAge65OrOlder") -> false,
        Path("/secondaryFilerIsBlind") -> false,
        Path("/secondaryFilerAge65OrOlder") -> false,
        Path("/oB3Deductions") -> Dollar(0),
      )

      val actualCtc = graph.get("/totalCtc")

      assert(actualCtc.value.contains(Dollar(expectedTotalCtc)))
    }
    println(
      s"Completed ${dataTable.length} CTC scenarios for calculating total non-refundable CTC",
    )
  }

  test("test ODC: derives and phaseouts correctly") {
    val dataTable = Table(
      ("status", "agi", "odcEligibleDependents", "expectedTotalOdc"),
      // Non-MFJ cases, max ODC is the lesser of total potential ODC and potential phase out

      (single, "20000", 0, "0"),
      (single, "20000", 1, "428"),
      (single, "20000", 2, "428"),
      (single, "20000", 3, "428"),
      (single, "20000", 4, "428"),
      (single, "50000", 0, "0"),
      (single, "50000", 1, "500"),
      (single, "50000", 2, "1000"),
      (single, "50000", 3, "1500"),
      (single, "50000", 4, "2000"),
      (single, "70000", 0, "0"),
      (single, "70000", 1, "500"),
      (single, "70000", 2, "1000"),
      (single, "70000", 3, "1500"),
      (single, "70000", 4, "2000"),
      (single, "70000", 5, "2500"),
      // Non-MFJ Phaseout begins to apply at 200k AGI
      (single, "190000", 1, "500"),
      (single, "200000", 1, "500"),
      (single, "210000", 1, "0"),
      (single, "220000", 1, "0"),
      (single, "230000", 1, "0"),
      (single, "240000", 1, "0"),
      (single, "190000", 2, "1000"),
      (single, "200000", 2, "1000"),
      (single, "210000", 2, "500"),
      (single, "220000", 2, "0"),
      (mfj, "20000", 0, "0"),
      (mfj, "20000", 1, "0"),
      (mfj, "20000", 2, "0"),
      (mfj, "20000", 3, "0"),
      (mfj, "20000", 4, "0"),
      (mfj, "50000", 0, "0"),
      (mfj, "50000", 1, "500"),
      (mfj, "50000", 2, "1000"),
      (mfj, "50000", 3, "1500"),
      (mfj, "50000", 4, "1853"),
      (mfj, "70000", 0, "0"),
      (mfj, "70000", 1, "500"),
      (mfj, "70000", 2, "1000"),
      (mfj, "70000", 3, "1500"),
      (mfj, "70000", 4, "2000"),
      (mfj, "70000", 5, "2500"),
      // MFJ Phaseout begins to apply at 400k AGI
      (mfj, "390000", 1, "500"),
      (mfj, "400000", 1, "500"),
      (mfj, "410000", 1, "0"),
      (mfj, "420000", 1, "0"),
      (mfj, "430000", 1, "0"),
      (mfj, "440000", 1, "0"),
      (mfj, "390000", 2, "1000"),
      (mfj, "400000", 2, "1000"),
      (mfj, "410000", 2, "500"),
      (mfj, "420000", 2, "0"),
    )

    forAll(dataTable) { (status, agi, odcEligibleDependents, expectedTotalOdc) =>
      val graph = makeGraphWith(
        factDictionary,
        Path("/filingStatus") -> status,
        Path("/agi") -> Dollar(agi),
        Path("/odcEligibleDependents") -> odcEligibleDependents,
        Path("/treatFilersAsDependents") -> false,
        Path("/primaryFilerIsBlind") -> false,
        Path("/primaryFilerAge65OrOlder") -> false,
        Path("/secondaryFilerIsBlind") -> false,
        Path("/secondaryFilerAge65OrOlder") -> false,
        Path("/oB3Deductions") -> Dollar(0),
      )

      val actualOdc = graph.get("/totalOdc")

      assert(actualOdc.value.contains(Dollar(expectedTotalOdc)))
    }
    println(
      s"Completed ${dataTable.length} ODC scenarios for calculating total non-refundable ODC",
    )
  }

  test("test ACTC: derives and phaseouts correctly with no ODC dependents") {
    val dataTable = Table(
      (
        "status",
        "agi",
        "ctcEligibleDependents",
        "totalCtcAndOdc",
        "additionalCtc",
        "remainingCtcAndOdc",
        "totalPotentialActc",
      ),
      // Non-MFJ cases, max CTC is the lesser of total tentative tax and potential phase out

      (single, "20000", 0, "0", "0", "0", "0"),
      (single, "20000", 1, "428", "0", "1572", "1700"),
      (single, "20000", 2, "428", "0", "3572", "3400"),
      (single, "20000", 3, "428", "0", "5572", "5100"),
      (single, "20000", 4, "428", "0", "7572", "6800"),
      (single, "50000", 0, "0", "0", "0", "0"),
      (single, "50000", 1, "2000", "0", "0", "1700"),
      (single, "50000", 2, "3875", "0", "125", "3400"),
      (single, "50000", 3, "3875", "0", "2125", "5100"),
      (single, "50000", 4, "3875", "0", "4125", "6800"),
      (single, "70000", 0, "0", "0", "0", "0"),
      (single, "70000", 1, "2000", "0", "0", "1700"),
      (single, "70000", 2, "4000", "0", "0", "3400"),
      (single, "70000", 3, "6000", "0", "0", "5100"),
      (single, "70000", 4, "6855", "0", "1145", "6800"),
      (single, "70000", 5, "6855", "0", "3145", "8500"),
      (single, "190000", 1, "2000", "0", "0", "1700"),
      (single, "200000", 1, "2000", "0", "0", "1700"),
      (single, "210000", 1, "1500", "0", "0", "1700"),
      (single, "220000", 1, "1000", "0", "0", "1700"),
      (single, "230000", 1, "500", "0", "0", "1700"),
      (single, "240000", 1, "0", "0", "0", "1700"),
      (single, "190000", 2, "4000", "0", "0", "3400"),
      (single, "200000", 2, "4000", "0", "0", "3400"),
      (single, "210000", 2, "3500", "0", "0", "3400"),
      (single, "220000", 2, "3000", "0", "0", "3400"),
      (single, "230000", 2, "2500", "0", "0", "3400"),
      (single, "240000", 2, "2000", "0", "0", "3400"),
      (mfj, "20000", 0, "0", "0", "0", "0"),
      (mfj, "20000", 1, "0", "0", "2000", "1700"),
      (mfj, "20000", 2, "0", "0", "4000", "3400"),
      (mfj, "20000", 3, "0", "0", "6000", "5100"),
      (mfj, "20000", 4, "0", "0", "8000", "6800"),
      (mfj, "50000", 0, "0", "0", "0", "0"),
      (mfj, "50000", 1, "1853", "0", "147", "1700"),
      (mfj, "50000", 2, "1853", "0", "2147", "3400"),
      (mfj, "50000", 3, "1853", "0", "4147", "5100"),
      (mfj, "50000", 4, "1853", "0", "6147", "6800"),
      (mfj, "70000", 0, "0", "0", "0", "0"),
      (mfj, "70000", 1, "2000", "0", "0", "1700"),
      (mfj, "70000", 2, "4000", "0", "0", "3400"),
      (mfj, "70000", 3, "4146", "0", "1854", "5100"),
      (mfj, "70000", 4, "4146", "0", "3854", "6800"),
      (mfj, "70000", 5, "4146", "0", "5854", "8500"),
      (mfj, "390000", 1, "2000", "0", "0", "1700"),
      (mfj, "400000", 1, "2000", "0", "0", "1700"),
      (mfj, "410000", 1, "1500", "0", "0", "1700"),
      (mfj, "420000", 1, "1000", "0", "0", "1700"),
      (mfj, "430000", 1, "500", "0", "0", "1700"),
      (mfj, "440000", 1, "0", "0", "0", "1700"),
      (mfj, "390000", 2, "4000", "0", "0", "3400"),
      (mfj, "400000", 2, "4000", "0", "0", "3400"),
      (mfj, "410000", 2, "3500", "0", "0", "3400"),
      (mfj, "420000", 2, "3000", "0", "0", "3400"),
      (mfj, "430000", 2, "2500", "0", "0", "3400"),
      (mfj, "440000", 2, "2000", "0", "0", "3400"),
    )

    forAll(dataTable) {
      (status, agi, ctcEligibleDependents, totalCtcAndOdc, additionalCtc, remainingCtcAndOdc, totalPotentialActc) =>
        val graph = makeGraphWith(
          factDictionary,
          Path("/filingStatus") -> status,
          Path("/agi") -> Dollar(agi),
          Path("/ctcEligibleDependents") -> ctcEligibleDependents,
          Path("/treatFilersAsDependents") -> false,
          Path("/primaryFilerIsBlind") -> false,
          Path("/primaryFilerAge65OrOlder") -> false,
          Path("/secondaryFilerIsBlind") -> false,
          Path("/secondaryFilerAge65OrOlder") -> false,
          Path("/oB3Deductions") -> Dollar(0),
        )

        val actualTotalCtcAndOdc = graph.get("/totalCtcAndOdc")
        val actualAdditionalCtc = graph.get("/additionalCtc")
        val actualRemainingCtcAndOdc = graph.get("/remainingCtcAndOdc")
        val actualTotalPotentialActc = graph.get("/totalPotentialActc")

        assert(actualTotalCtcAndOdc.value.contains(Dollar(totalCtcAndOdc)))
        assert(actualAdditionalCtc.value.contains(Dollar(additionalCtc)))
        assert(actualRemainingCtcAndOdc.value.contains(Dollar(remainingCtcAndOdc)))
        assert(actualTotalPotentialActc.value.contains(Dollar(totalPotentialActc)))
    }
    println(
      s"Completed ${dataTable.length} ACTC scenarios for calculating total non-refundable CTC",
    )
  }

  test("test ACTC and total CDC/ODC: derives and phaseouts correctly with both CTC and ODC dependents") {
    val dataTable = Table(
      (
        "status",
        "agi",
        "ctcEligibleDependents",
        "odcEligibleDependents",
        "totalCtcAndOdc",
        "additionalCtc",
        "remainingCtcAndOdc",
        "totalPotentialActc",
      ),
      // Non-MFJ cases, max CTC is the lesser of total tentative tax and potential phase out

      (single, "20000", 0, 3, "428", "0", "1072", "0"),
      (single, "20000", 1, 3, "428", "0", "3072", "1700"),
      (single, "20000", 2, 1, "428", "0", "4072", "3400"),
      (single, "20000", 3, 0, "428", "0", "5572", "5100"),
      (single, "20000", 4, 0, "428", "0", "7572", "6800"),
      (single, "50000", 0, 0, "0", "0", "0", "0"),
      (single, "50000", 1, 1, "2500", "0", "0", "1700"),
      (single, "50000", 2, 0, "3875", "0", "125", "3400"),
      (single, "50000", 3, 2, "3875", "0", "3125", "5100"),
      (single, "50000", 4, 2, "3875", "0", "5125", "6800"),
      (single, "70000", 0, 0, "0", "0", "0", "0"),
      (single, "70000", 1, 0, "2000", "0", "0", "1700"),
      (single, "70000", 2, 1, "4500", "0", "0", "3400"),
      (single, "70000", 3, 1, "6500", "0", "0", "5100"),
      (single, "70000", 4, 2, "6855", "0", "2145", "6800"),
      (single, "70000", 5, 3, "6855", "0", "4645", "8500"),
      (single, "190000", 1, 1, "2500", "0", "0", "1700"),
      (single, "200000", 1, 3, "3500", "0", "0", "1700"),
      (single, "210000", 1, 0, "1500", "0", "0", "1700"),
      (single, "220000", 1, 0, "1000", "0", "0", "1700"),
      (single, "230000", 1, 3, "2000", "0", "0", "1700"),
      (single, "240000", 1, 1, "500", "0", "0", "1700"),
      (single, "190000", 2, 1, "4500", "0", "0", "3400"),
      (single, "200000", 2, 2, "5000", "0", "0", "3400"),
      (single, "210000", 2, 0, "3500", "0", "0", "3400"),
      (single, "220000", 2, 2, "4000", "0", "0", "3400"),
      (single, "230000", 2, 2, "3500", "0", "0", "3400"),
      (single, "240000", 2, 1, "2500", "0", "0", "3400"),
      (mfj, "20000", 0, 2, "0", "0", "1000", "0"),
      (mfj, "20000", 1, 0, "0", "0", "2000", "1700"),
      (mfj, "20000", 2, 0, "0", "0", "4000", "3400"),
      (mfj, "20000", 3, 0, "0", "0", "6000", "5100"),
      (mfj, "20000", 4, 1, "0", "0", "8500", "6800"),
      (mfj, "50000", 0, 0, "0", "0", "0", "0"),
      (mfj, "50000", 1, 0, "1853", "0", "147", "1700"),
      (mfj, "50000", 2, 3, "1853", "0", "3647", "3400"),
      (mfj, "50000", 3, 2, "1853", "0", "5147", "5100"),
      (mfj, "50000", 4, 2, "1853", "0", "7147", "6800"),
      (mfj, "70000", 0, 2, "1000", "0", "0", "0"),
      (mfj, "70000", 1, 1, "2500", "0", "0", "1700"),
      (mfj, "70000", 2, 0, "4000", "0", "0", "3400"),
      (mfj, "70000", 3, 2, "4146", "0", "2854", "5100"),
      (mfj, "70000", 4, 3, "4146", "0", "5354", "6800"),
      (mfj, "70000", 5, 1, "4146", "0", "6354", "8500"),
      (mfj, "390000", 1, 3, "3500", "0", "0", "1700"),
      (mfj, "400000", 1, 2, "3000", "0", "0", "1700"),
      (mfj, "410000", 1, 2, "2500", "0", "0", "1700"),
      (mfj, "420000", 1, 1, "1500", "0", "0", "1700"),
      (mfj, "430000", 1, 0, "500", "0", "0", "1700"),
      (mfj, "440000", 1, 2, "1000", "0", "0", "1700"),
      (mfj, "390000", 2, 0, "4000", "0", "0", "3400"),
      (mfj, "400000", 2, 0, "4000", "0", "0", "3400"),
      (mfj, "410000", 2, 0, "3500", "0", "0", "3400"),
      (mfj, "420000", 2, 2, "4000", "0", "0", "3400"),
      (mfj, "430000", 2, 0, "2500", "0", "0", "3400"),
      (mfj, "440000", 2, 2, "3000", "0", "0", "3400"),
    )

    forAll(dataTable) {
      (
          status,
          agi,
          ctcEligibleDependents,
          odcEligibleDependents,
          totalCtcAndOdc,
          additionalCtc,
          remainingCtcAndOdc,
          totalPotentialActc,
      ) =>
        val graph = makeGraphWith(
          factDictionary,
          Path("/filingStatus") -> status,
          Path("/agi") -> Dollar(agi),
          Path("/ctcEligibleDependents") -> ctcEligibleDependents,
          Path("/odcEligibleDependents") -> odcEligibleDependents,
          Path("/treatFilersAsDependents") -> false,
          Path("/primaryFilerIsBlind") -> false,
          Path("/primaryFilerAge65OrOlder") -> false,
          Path("/secondaryFilerIsBlind") -> false,
          Path("/secondaryFilerAge65OrOlder") -> false,
          Path("/oB3Deductions") -> Dollar(0),
        )

        val actualTotalCtcAndOdc = graph.get("/totalCtcAndOdc")
        val actualAdditionalCtc = graph.get("/additionalCtc")
        val actualRemainingCtcAndOdc = graph.get("/remainingCtcAndOdc")
        val actualTotalPotentialActc = graph.get("/totalPotentialActc")

        assert(actualTotalCtcAndOdc.value.contains(Dollar(totalCtcAndOdc)))
        assert(actualAdditionalCtc.value.contains(Dollar(additionalCtc)))
        assert(actualRemainingCtcAndOdc.value.contains(Dollar(remainingCtcAndOdc)))
        assert(actualTotalPotentialActc.value.contains(Dollar(totalPotentialActc)))
    }
    println(
      s"Completed ${dataTable.length} ACTC, CTC and ODC scenarios for calculating total non-refundable and refundable portions of CTC/ODC with CTC and ODC qualifying dependents",
    )
  }
  test("test CDCC: derives and phaseouts correctly with varying AGI, qualifying persons and qualifying expenses") {
    val dataTable = Table(
      (
        "status",
        "cdccQualifyingPersons",
        "cdccQualifyingExpenses",
        "agi",
        "earnedIncomeSelf",
        "earnedIncomeSpouse",
        "expectedCdcc",
      ),
      (single, 1, "1500", "12000", "15000", "0", "0"),
      (single, 1, "4000", "12000", "15000", "0", "0"),
      (single, 2, "7000", "12000", "25000", "0", "0"),
      (single, 1, "3000", "16500", "25000", "0", "76"),
      (single, 2, "6000", "23500", "25000", "0", "778"),
      (single, 1, "3000", "30000", "35000", "0", "810"),
      (single, 2, "6500", "42000", "55000", "0", "1260"),
      (single, 2, "5000", "45000", "50000", "0", "1000"),
      (single, 1, "3000", "45000", "52500", "0", "600"),
      (single, 2, "1000", "45000", "50000", "0", "200"),
      (single, 3, "30000", "26000", "35000", "0", "1028"),
      (single, 1, "1000", "35500", "40000", "0", "240"),
      (single, 1, "15000", "41500", "55000", "0", "630"),
      (single, 3, "30000", "50000", "57000", "0", "1200"),
      (single, 1, "30000", "75000", "82900", "0", "600"),
      (single, 3, "10000", "100000", "105000", "0", "1200"),
      (single, 1, "2000", "125000", "129000", "0", "400"),
      (single, 2, "30000", "150000", "160000", "0", "1200"),
      (single, 1, "30000", "175000", "185000", "0", "600"),
      (single, 2, "30000", "200000", "205000", "0", "1200"),
      //      We do not make any changes for MFS vs. other non-MFJ filing statuses
      (mfs, 1, "1500", "12000", "15000", "0", "0"),
      (mfs, 1, "4000", "12000", "15000", "0", "0"),
      (mfs, 2, "7000", "12000", "25000", "0", "0"),
      (mfs, 1, "3000", "16500", "25000", "0", "76"),
      (mfs, 2, "6000", "23500", "25000", "0", "778"),
      (mfs, 1, "3000", "30000", "35000", "0", "810"),
      (mfs, 2, "6500", "42000", "55000", "0", "1260"),
      (mfs, 2, "5000", "45000", "50000", "0", "1000"),
      (mfs, 1, "3000", "45000", "52500", "0", "600"),
      (mfs, 2, "1000", "45000", "50000", "0", "200"),
      (mfs, 3, "30000", "26000", "35000", "0", "1028"),
      (mfs, 1, "1000", "35500", "40000", "0", "240"),
      (mfs, 1, "15000", "41500", "55000", "0", "630"),
      (mfs, 3, "30000", "50000", "57000", "0", "1200"),
      (mfs, 1, "30000", "75000", "82900", "0", "600"),
      (mfs, 3, "10000", "100000", "105000", "0", "1200"),
      (mfs, 1, "2000", "125000", "129000", "0", "400"),
      (mfs, 2, "30000", "150000", "160000", "0", "1200"),
      (mfs, 1, "30000", "175000", "185000", "0", "600"),
      (mfs, 2, "30000", "200000", "205000", "0", "1200"),
      //      Form 2441 Line 6 chooses the lesser of earned income for self and spouse for deciding the expense cap
      (mfj, 1, "1500", "12000", "15000", "20000", "0"),
      (mfj, 1, "4000", "12000", "15000", "10000", "0"),
      (mfj, 2, "7000", "12000", "25000", "0", "0"),
      (mfj, 1, "3000", "16500", "25000", "35000", "0"),
      (mfj, 2, "6000", "23500", "25000", "15000", "0"),
      (mfj, 1, "3000", "30000", "35000", "25000", "0"),
      (mfj, 2, "6500", "42000", "55000", "0", "0"),
      (mfj, 2, "5000", "45000", "50000", "60000", "1000"),
      (mfj, 1, "3000", "45000", "52500", "60000", "600"),
      (mfj, 2, "1000", "45000", "50000", "60000", "200"),
      (mfj, 3, "30000", "26000", "35000", "60000", "0"),
      (mfj, 1, "1000", "35500", "40000", "60000", "240"),
      (mfj, 1, "15000", "41500", "55000", "60000", "630"),
      (mfj, 3, "30000", "50000", "57000", "60000", "1200"),
      (mfj, 1, "30000", "75000", "82900", "83000", "600"),
      (mfj, 3, "10000", "100000", "105000", "0", "0"),
      (mfj, 1, "2000", "125000", "129000", "0", "0"),
      (mfj, 2, "30000", "150000", "160000", "160000", "1200"),
      (mfj, 1, "30000", "175000", "185000", "190000", "600"),
      (mfj, 2, "30000", "200000", "205000", "210000", "1200"),
    )
    forAll(dataTable) {
      (
          status,
          cdccQualifyingPersons,
          cdccQualifyingExpenses,
          agi,
          earnedIncomeSelf,
          earnedIncomeSpouse,
          expectedCdcc,
      ) =>
        val graph = makeGraphWith(
          factDictionary,
          Path("/filingStatus") -> status,
          Path("/agi") -> Dollar(agi),
          Path("/cdccQualifyingPersons") -> cdccQualifyingPersons,
          Path("/cdccQualifyingExpenses") -> Dollar(cdccQualifyingExpenses),
          Path("/earnedIncomeSelf") -> Dollar(earnedIncomeSelf),
          Path("/earnedIncomeSpouse") -> Dollar(earnedIncomeSpouse),
          Path("/treatFilersAsDependents") -> false,
          Path("/primaryFilerIsBlind") -> false,
          Path("/primaryFilerAge65OrOlder") -> false,
          Path("/secondaryFilerIsBlind") -> false,
          Path("/secondaryFilerAge65OrOlder") -> false,
          Path("/oB3Deductions") -> Dollar(0),
        )

        val actualCdcc = graph.get("/creditForChildAndDependentCareExpenses")
        assert(actualCdcc.value.contains(Dollar(expectedCdcc)))
    }
    println(
      s"Completed ${dataTable.length} CDCC scenarios",
    )
  }
  test("test adoption credit: applies refundability and thresholds correctly") {
    val dataTable = Table(
      (
        "agi",
        "estimatedTotalQualifiedAdoptionExpenses",
        "adoptionEligibleChildren",
        "expectedAdoptionCreditRefundable",
        "expectedAdoptionCreditNonRefundable",
      ),
      ("30000", "2500", 1, "2500", "0"),
      ("45000", "3000", 1, "3000", "0"),
      ("55000", "1728", 1, "1728", "0"),
      ("70000", "3456", 2, "3456", "0"),
      ("85000", "3850", 1, "3850", "0"),
      ("100000", "10000", 1, "5000", "5000"),
      ("100000", "0", 1, "0", "0"),
      ("100000", "10000", 0, "0", "0"),
      ("100000", "17280", 1, "5000", "7746"),
      ("150000", "20000", 1, "5000", "12280"),
      ("150000", "4000", 1, "4000", "0"),
      ("150000", "6000", 1, "5000", "1000"),
      ("150000", "30000", 2, "5000", "15898"),
      ("150000", "40000", 2, "5000", "15898"),
      ("250000", "100000", 3, "5000", "38134"),
      ("259190", "20000", 1, "5000", "12280"),
      ("269190", "20000", 1, "5000", "12280"),
      ("279190", "20000", 1, "0", "0"),
      ("279190", "10000", 1, "0", "0"),
      ("289190", "40000", 2, "0", "0"),
      ("298000", "20000", 1, "0", "0"),
      ("299190", "20000", 1, "0", "0"),
      ("350000", "20000", 1, "0", "0"),
    )
    forAll(dataTable) {
      (
          agi,
          estimatedTotalQualifiedAdoptionExpenses,
          adoptionEligibleChildren,
          expectedAdoptionCreditRefundable,
          expectedAdoptionCreditNonRefundable,
      ) =>
        val graph = makeGraphWith(
          factDictionary,
          Path("/filingStatus") -> mfj,
          Path("/agi") -> Dollar(agi),
          Path("/estimatedTotalQualifiedAdoptionExpenses") -> Dollar(estimatedTotalQualifiedAdoptionExpenses),
          Path("/adoptionEligibleChildren") -> adoptionEligibleChildren,
          Path("/treatFilersAsDependents") -> false,
          Path("/primaryFilerIsBlind") -> false,
          Path("/primaryFilerAge65OrOlder") -> false,
          Path("/secondaryFilerIsBlind") -> false,
          Path("/secondaryFilerAge65OrOlder") -> false,
          Path("/oB3Deductions") -> Dollar(0),
        )

        val adoptionCreditRefundable = graph.get("/adoptionCreditRefundable")
        val adoptionCreditNonRefundable = graph.get("/adoptionCreditNonRefundable")
        assert(adoptionCreditRefundable.value.contains(Dollar(expectedAdoptionCreditRefundable)))
        assert(adoptionCreditNonRefundable.value.contains(Dollar(expectedAdoptionCreditNonRefundable)))
    }
    println(
      s"Completed ${dataTable.length} adoption credit scenarios",
    )
  }

  test("test education credits: applies refundability and thresholds correctly") {
    val dataTable = Table(
      (
        "status",
        "agi",
        "aotcQualifyingStudents",
        "aotcQualifiedEducationExpenses",
        "llcQualifiedEducationExpenses",
        "treatFilersAsDependents",
        "expectedAmericanOpportunityCredit",
        "expectedLifetimeLearningCredit",
      ),
      (single, "15000", 1, "4000", "0", false, "1600", "0"),
      (single, "20000", 1, "4000", "0", false, "1600", "428"),
      (single, "25000", 1, "4000", "0", false, "1600", "928"),
      (single, "40000", 1, "4000", "0", false, "1600", "2000"),
      (single, "50000", 1, "4000", "0", false, "1600", "2000"),
      (single, "60000", 1, "2000", "0", false, "800", "1200"),
      (single, "70000", 2, "4000", "0", false, "3200", "2000"),
      (single, "75000", 0, "0", "5000", false, "0", "1000"),
      (single, "79000", 1, "4000", "0", false, "1600", "2000"),
      (single, "80000", 1, "4000", "0", false, "1600", "2000"),
      (single, "82000", 1, "4000", "0", false, "1600", "2000"),
      (single, "85000", 1, "4000", "0", false, "1600", "2000"),
      (single, "88000", 1, "4000", "0", false, "0", "0"),
      (single, "89000", 1, "4000", "0", false, "0", "0"),
      (single, "90000", 1, "4000", "0", false, "0", "0"),
      (single, "95000", 1, "4000", "0", false, "0", "0"),
      (single, "45000", 1, "4000", "10000", false, "1600", "2000"),
      (single, "85000", 1, "4000", "10000", false, "1600", "2000"),
      (single, "110000", 2, "4000", "0", false, "0", "0"),
      (single, "125000", 1, "4000", "10000", false, "0", "0"),
      (mfj, "15000", 1, "13824", "3904", false, "1600", "0"),
      (mfj, "20000", 1, "24492", "26700", false, "1600", "0"),
      (mfj, "25000", 1, "27640", "25725", false, "1600", "0"),
      (mfj, "40000", 1, "4289", "25629", false, "1600", "853"),
      (mfj, "50000", 1, "848", "15024", false, "339.2", "1853"),
      (mfj, "60000", 1, "19282", "27557", false, "1600", "2000"),
      (mfj, "70000", 2, "28384", "530", false, "3200", "2000"),
      (mfj, "75000", 0, "10920", "21454", false, "0", "2000"),
      (mfj, "79000", 1, "16461", "28548", false, "1600", "2000"),
      (mfj, "80000", 1, "1413", "30034", false, "565.2", "2000"),
      (mfj, "82000", 1, "16709", "3689", false, "1600", "2000"),
      (mfj, "85000", 1, "235", "8037", false, "94", "1748.4"),
      (mfj, "88000", 1, "13002", "3527", false, "1600", "2000"),
      (mfj, "89000", 1, "575", "19145", false, "230", "2000"),
      (mfj, "90000", 1, "10418", "21296", false, "1600", "2000"),
      (mfj, "95000", 1, "6508", "1294", false, "1600", "2000"),
      (mfj, "45000", 1, "32797", "9502", false, "1600", "1353"),
      (mfj, "85000", 1, "30814", "1492", false, "1600", "2000"),
      (mfj, "110000", 2, "2263", "30554", false, "1810.4", "2000"),
      (mfj, "125000", 1, "10517", "29046", false, "1600", "2000"),
      (mfj, "140000", 3, "4002", "24037", false, "4800", "2000"),
      (mfj, "155000", 1, "12215", "26212", false, "1600", "2000"),
      (mfj, "170000", 3, "3694", "16008", false, "4432.8", "2000"),
      (mfj, "185000", 2, "23803", "14575", false, "0", "0"),
      (mfj, "200000", 1, "31675", "17961", false, "0", "0"),
      (mfj, "215000", 2, "32333", "6495", false, "0", "0"),
      (mfj, "230000", 3, "12415", "24022", false, "0", "0"),
    )
    forAll(dataTable) {
      (
          status,
          agi,
          aotcQualifyingStudents,
          aotcQualifiedEducationExpenses,
          llcQualifiedEducationExpenses,
          treatFilersAsDependents,
          expectedAmericanOpportunityCredit,
          expectedLifetimeLearningCredit,
      ) =>
        val graph = makeGraphWith(
          factDictionary,
          Path("/filingStatus") -> status,
          Path("/agi") -> Dollar(agi),
          Path("/aotcQualifyingStudents") -> aotcQualifyingStudents,
          Path("/aotcQualifiedEducationExpenses") -> Dollar(aotcQualifiedEducationExpenses),
          Path("/llcQualifiedEducationExpenses") -> Dollar(llcQualifiedEducationExpenses),
          Path("/treatFilersAsDependents") -> treatFilersAsDependents,
          Path("/primaryFilerIsBlind") -> false,
          Path("/primaryFilerAge65OrOlder") -> false,
          Path("/secondaryFilerIsBlind") -> false,
          Path("/secondaryFilerAge65OrOlder") -> false,
          Path("/oB3Deductions") -> Dollar(0),
        )

        val americanOpportunityCredit = graph.get("/americanOpportunityCredit")
        val lifetimeLearningCredit = graph.get("/lifetimeLearningCredit")

        assert(americanOpportunityCredit.value.contains(Dollar(expectedAmericanOpportunityCredit)))
        assert(lifetimeLearningCredit.value.contains(Dollar(expectedLifetimeLearningCredit)))
    }
    println(
      s"Completed ${dataTable.length} education credit eligibility and calculation scenarios",
    )
  }

  test("test education credit: $0 credit when filers are ineligible") {
    val dataTable = Table(
      (
        "status",
        "agi",
        "aotcQualifyingStudents",
        "aotcQualifiedEducationExpenses",
        "llcQualifiedEducationExpenses",
        "treatFilersAsDependents",
        "expectedAmericanOpportunityCredit",
        "expectedLifetimeLearningCredit",
      ),

      //      MFS is ineligible for either credit no matter the other inputs
      (mfs, "15000", 1, "4000", "7543.18", false, "0", "0"),
      (mfs, "20000", 1, "4000", "8456.7", false, "0", "0"),
      (mfs, "25000", 1, "4000", "22751.82", false, "0", "0"),
      (mfs, "40000", 1, "4000", "14803.12", false, "0", "0"),
      (mfs, "50000", 1, "4000", "20005.34", false, "0", "0"),
      (mfs, "60000", 1, "2000", "9089.8", false, "0", "0"),
      (mfs, "70000", 2, "4000", "23394.93", false, "0", "0"),
      (mfs, "75000", 0, "0", "2385.3", false, "0", "0"),
      (mfs, "79000", 1, "4000", "24544.87", false, "0", "0"),
      (mfs, "80000", 1, "4000", "23731.22", false, "0", "0"),
      (mfs, "82000", 1, "4000", "12102.55", false, "0", "0"),
      (mfs, "85000", 1, "4000", "20281.4", false, "0", "0"),
      (mfs, "88000", 1, "4000", "17619.49", false, "0", "0"),
      (mfs, "89000", 1, "4000", "4865.28", false, "0", "0"),
      (mfs, "90000", 1, "4000", "5123.66", false, "0", "0"),
      (mfs, "95000", 1, "4000", "17673.91", false, "0", "0"),
      (mfs, "45000", 1, "4000", "18918.72", false, "0", "0"),
      (mfs, "85000", 1, "4000", "3022.83", false, "0", "0"),
      (mfs, "110000", 2, "4000", "19558.02", false, "0", "0"),
      (mfs, "125000", 1, "4000", "23719.39", false, "0", "0"),

      //      if filers are dependents, they are ineligible for either credit no matter the other inputs
      (single, "15000", 1, "4000", "7543.18", true, "0", "0"),
      (single, "20000", 1, "4000", "8456.7", true, "0", "0"),
      (single, "25000", 1, "4000", "22751.82", true, "0", "0"),
      (single, "40000", 1, "4000", "14803.12", true, "0", "0"),
      (single, "50000", 1, "4000", "20005.34", true, "0", "0"),
      (single, "60000", 1, "2000", "9089.8", true, "0", "0"),
      (single, "70000", 2, "4000", "23394.93", true, "0", "0"),
      (single, "75000", 0, "0", "2385.3", true, "0", "0"),
      (single, "79000", 1, "4000", "24544.87", true, "0", "0"),
      (single, "80000", 1, "4000", "23731.22", true, "0", "0"),
      (mfj, "82000", 1, "4000", "12102.55", true, "0", "0"),
      (mfj, "85000", 1, "4000", "20281.4", true, "0", "0"),
      (mfj, "88000", 1, "4000", "17619.49", true, "0", "0"),
      (mfj, "89000", 1, "4000", "4865.28", true, "0", "0"),
      (mfj, "90000", 1, "4000", "5123.66", true, "0", "0"),
      (mfj, "95000", 1, "4000", "17673.91", true, "0", "0"),
      (mfj, "45000", 1, "4000", "18918.72", true, "0", "0"),
      (mfj, "85000", 1, "4000", "3022.83", true, "0", "0"),
      (mfj, "110000", 2, "4000", "19558.02", true, "0", "0"),
      (mfj, "125000", 1, "4000", "23719.39", true, "0", "0"),
    )
    forAll(dataTable) {
      (
          status,
          agi,
          aotcQualifyingStudents,
          aotcQualifiedEducationExpenses,
          llcQualifiedEducationExpenses,
          treatFilersAsDependents,
          expectedAmericanOpportunityCredit,
          expectedLifetimeLearningCredit,
      ) =>
        val graph = makeGraphWith(
          factDictionary,
          Path("/filingStatus") -> status,
          Path("/agi") -> Dollar(agi),
          Path("/aotcQualifyingStudents") -> aotcQualifyingStudents,
          Path("/aotcQualifiedEducationExpenses") -> Dollar(aotcQualifiedEducationExpenses),
          Path("/llcQualifiedEducationExpenses") -> Dollar(llcQualifiedEducationExpenses),
          Path("/treatFilersAsDependents") -> treatFilersAsDependents,
          Path("/primaryFilerIsBlind") -> false,
          Path("/primaryFilerAge65OrOlder") -> false,
          Path("/secondaryFilerIsBlind") -> false,
          Path("/secondaryFilerAge65OrOlder") -> false,
          Path("/oB3Deductions") -> Dollar(0),
        )

        val americanOpportunityCredit = graph.get("/americanOpportunityCredit")
        val lifetimeLearningCredit = graph.get("/lifetimeLearningCredit")

        assert(americanOpportunityCredit.value.contains(Dollar(expectedAmericanOpportunityCredit)))
        assert(lifetimeLearningCredit.value.contains(Dollar(expectedLifetimeLearningCredit)))
    }
    println(
      s"Completed ${dataTable.length} education credit ineligibility scenarios",
    )
  }

  test("test EITC: calculation and phase out are calculated correctly") {
    val dataTable = Table(
      (
        "status",
        "agi",
        "earnedIncomeSelf",
        "earnedIncomeSpouse",
        "eitcQualifyingChildren",
        "primaryFilerAge65OrOlder",
        "secondaryFilerAge65OrOlder",
        "treatFilersAsDependents",
        "nonTaxableInterestIncome",
        "interestExcludingNonTaxableIncome",
        "ordinaryDividendsIncome",
        "shortTermCapitalGainsIncome",
        "longTermCapitalGainsIncome",
        "expectedEitc",
      ),

      // non-MFJ should have the same results, as we aren't checking for filing status specific eligibility
      (single, "5000", "5000", "0", 1, false, false, false, "0", "0", "0", "0", "0", "1709"),
      (single, "5000", "5000", "0", 0, false, false, false, "0", "0", "0", "0", "0", "384"),
      (single, "6000", "6000", "0", 1, false, false, false, "0", "0", "0", "0", "0", "2049"),
      (single, "7000", "7000", "0", 2, false, false, false, "0", "0", "0", "0", "0", "2810"),
      (single, "9000", "9000", "0", 0, false, false, false, "0", "0", "0", "0", "0", "632"),
      (single, "12000", "12000", "0", 0, false, false, false, "0", "0", "0", "0", "0", "502"),
      (single, "15000", "15000", "0", 1, true, false, false, "0", "0", "0", "0", "0", "4213"),
      (single, "20000", "20000", "0", 2, false, false, false, "0", "0", "0", "0", "0", "6960"),
      (single, "20000", "20000", "0", 3, false, false, false, "0", "0", "0", "0", "0", "7830"),
      (single, "25100", "25000", "0", 1, false, false, false, "100", "0", "0", "0", "0", "3828"),
      (single, "30000", "30000", "0", 1, false, false, false, "0", "0", "0", "0", "0", "3045"),
      (single, "35000", "35000", "0", 2, true, false, false, "0", "0", "0", "0", "0", "4369"),
      (single, "35000", "20000", "0", 0, false, false, false, "15000", "0", "0", "0", "0", "0"),
      (single, "36001", "25000", "0", 1, false, false, false, "11001", "0", "0", "0", "0", "2086"),
      (single, "40000", "40000", "0", 3, false, false, false, "0", "0", "0", "0", "0", "4186"),
      (single, "56000", "56000", "0", 2, false, false, false, "0", "0", "0", "0", "0", "0"),
      (single, "65000", "50000", "0", 1, false, false, false, "0", "15000", "0", "0", "0", "0"),
      (single, "75000", "75000", "0", 2, false, false, false, "0", "0", "0", "0", "0", "0"),
      (single, "85000", "70000", "0", 0, false, false, false, "0", "7000", "4000", "4000", "0", "0"),
      (single, "90000", "75000", "0", 1, false, false, false, "0", "3000", "3000", "3000", "6000", "0"),
      (qss, "5000", "5000", "0", 1, false, false, false, "0", "0", "0", "0", "0", "1709"),
      (qss, "5000", "5000", "0", 0, false, false, false, "0", "0", "0", "0", "0", "384"),
      (qss, "6000", "6000", "0", 1, false, false, false, "0", "0", "0", "0", "0", "2049"),
      (qss, "7000", "7000", "0", 2, false, false, false, "0", "0", "0", "0", "0", "2810"),
      (qss, "9000", "9000", "0", 0, false, false, false, "0", "0", "0", "0", "0", "632"),
      (qss, "12000", "12000", "0", 0, false, false, false, "0", "0", "0", "0", "0", "502"),
      (qss, "15000", "15000", "0", 1, true, false, false, "0", "0", "0", "0", "0", "4213"),
      (qss, "20000", "20000", "0", 2, false, false, false, "0", "0", "0", "0", "0", "6960"),
      (qss, "20000", "20000", "0", 3, false, false, false, "0", "0", "0", "0", "0", "7830"),
      (qss, "25100", "25000", "0", 1, false, false, false, "100", "0", "0", "0", "0", "3828"),
      (qss, "30000", "30000", "0", 1, false, false, false, "0", "0", "0", "0", "0", "3045"),
      (qss, "35000", "35000", "0", 2, true, false, false, "0", "0", "0", "0", "0", "4369"),
      (qss, "35000", "20000", "0", 0, false, false, false, "15000", "0", "0", "0", "0", "0"),
      (qss, "36001", "25000", "0", 1, false, false, false, "11001", "0", "0", "0", "0", "2086"),
      (qss, "40000", "40000", "0", 3, false, false, false, "0", "0", "0", "0", "0", "4186"),
      (qss, "56000", "56000", "0", 2, false, false, false, "0", "0", "0", "0", "0", "0"),
      (qss, "65000", "50000", "0", 1, false, false, false, "0", "15000", "0", "0", "0", "0"),
      (qss, "75000", "75000", "0", 2, false, false, false, "0", "0", "0", "0", "0", "0"),
      (qss, "85000", "70000", "0", 0, false, false, false, "0", "7000", "4000", "4000", "0", "0"),
      (qss, "90000", "75000", "0", 1, false, false, false, "0", "3000", "3000", "3000", "6000", "0"),
      (mfs, "5000", "5000", "0", 1, false, false, false, "0", "0", "0", "0", "0", "1709"),
      (mfs, "5000", "5000", "0", 0, false, false, false, "0", "0", "0", "0", "0", "384"),
      (mfs, "6000", "6000", "0", 1, false, false, false, "0", "0", "0", "0", "0", "2049"),
      (mfs, "7000", "7000", "0", 2, false, false, false, "0", "0", "0", "0", "0", "2810"),
      (mfs, "9000", "9000", "0", 0, false, false, false, "0", "0", "0", "0", "0", "632"),
      (mfs, "12000", "12000", "0", 0, false, false, false, "0", "0", "0", "0", "0", "502"),
      (mfs, "15000", "15000", "0", 1, true, false, false, "0", "0", "0", "0", "0", "4213"),
      (mfs, "20000", "20000", "0", 2, false, false, false, "0", "0", "0", "0", "0", "6960"),
      (mfs, "20000", "20000", "0", 3, false, false, false, "0", "0", "0", "0", "0", "7830"),
      (mfs, "25100", "25000", "0", 1, false, false, false, "100", "0", "0", "0", "0", "3828"),
      (mfs, "30000", "30000", "0", 1, false, false, false, "0", "0", "0", "0", "0", "3045"),
      (mfs, "35000", "35000", "0", 2, true, false, false, "0", "0", "0", "0", "0", "4369"),
      (mfs, "35000", "20000", "0", 0, false, false, false, "15000", "0", "0", "0", "0", "0"),
      (mfs, "36001", "25000", "0", 1, false, false, false, "11001", "0", "0", "0", "0", "2086"),
      (mfs, "40000", "40000", "0", 3, false, false, false, "0", "0", "0", "0", "0", "4186"),
      (mfs, "56000", "56000", "0", 2, false, false, false, "0", "0", "0", "0", "0", "0"),
      (mfs, "65000", "50000", "0", 1, false, false, false, "0", "15000", "0", "0", "0", "0"),
      (mfs, "75000", "75000", "0", 2, false, false, false, "0", "0", "0", "0", "0", "0"),
      (mfs, "85000", "70000", "0", 0, false, false, false, "0", "7000", "4000", "4000", "0", "0"),
      (mfs, "90000", "75000", "0", 1, false, false, false, "0", "3000", "3000", "3000", "6000", "0"),
      (hoh, "5000", "5000", "0", 1, false, false, false, "0", "0", "0", "0", "0", "1709"),
      (hoh, "5000", "5000", "0", 0, false, false, false, "0", "0", "0", "0", "0", "384"),
      (hoh, "6000", "6000", "0", 1, false, false, false, "0", "0", "0", "0", "0", "2049"),
      (hoh, "7000", "7000", "0", 2, false, false, false, "0", "0", "0", "0", "0", "2810"),
      (hoh, "9000", "9000", "0", 0, false, false, false, "0", "0", "0", "0", "0", "632"),
      (hoh, "12000", "12000", "0", 0, false, false, false, "0", "0", "0", "0", "0", "502"),
      (hoh, "15000", "15000", "0", 1, true, false, false, "0", "0", "0", "0", "0", "4213"),
      (hoh, "20000", "20000", "0", 2, false, false, false, "0", "0", "0", "0", "0", "6960"),
      (hoh, "20000", "20000", "0", 3, false, false, false, "0", "0", "0", "0", "0", "7830"),
      (hoh, "25100", "25000", "0", 1, false, false, false, "100", "0", "0", "0", "0", "3828"),
      (hoh, "30000", "30000", "0", 1, false, false, false, "0", "0", "0", "0", "0", "3045"),
      (hoh, "35000", "35000", "0", 2, true, false, false, "0", "0", "0", "0", "0", "4369"),
      (hoh, "35000", "20000", "0", 0, false, false, false, "15000", "0", "0", "0", "0", "0"),
      (hoh, "36001", "25000", "0", 1, false, false, false, "11001", "0", "0", "0", "0", "2086"),
      (hoh, "40000", "40000", "0", 3, false, false, false, "0", "0", "0", "0", "0", "4186"),
      (hoh, "56000", "56000", "0", 2, false, false, false, "0", "0", "0", "0", "0", "0"),
      (hoh, "65000", "50000", "0", 1, false, false, false, "0", "15000", "0", "0", "0", "0"),
      (hoh, "75000", "75000", "0", 2, false, false, false, "0", "0", "0", "0", "0", "0"),
      (hoh, "85000", "70000", "0", 0, false, false, false, "0", "7000", "4000", "4000", "0", "0"),
      (hoh, "90000", "75000", "0", 1, false, false, false, "0", "3000", "3000", "3000", "6000", "0"),

      //      MFJ
      (mfj, "5000", "5000", "0", 1, false, false, false, "0", "0", "0", "0", "0", "1709"),
      (mfj, "5000", "5000", "0", 0, false, false, false, "0", "0", "0", "0", "0", "384"),
      (mfj, "6000", "6000", "0", 1, false, false, false, "0", "0", "0", "0", "0", "2049"),
      (mfj, "7000", "7000", "0", 2, false, false, false, "0", "0", "0", "0", "0", "2810"),
      (mfj, "9000", "9000", "0", 0, false, false, false, "0", "0", "0", "0", "0", "632"),
      (mfj, "12000", "12000", "0", 0, false, false, false, "0", "0", "0", "0", "0", "632"),
      (mfj, "15000", "15000", "0", 1, true, false, false, "0", "0", "0", "0", "0", "4213"),
      (mfj, "20000", "20000", "0", 2, false, false, false, "0", "0", "0", "0", "0", "6960"),
      (mfj, "20000", "20000", "0", 3, false, false, false, "0", "0", "0", "0", "0", "7830"),
      (mfj, "25100", "25000", "0", 1, false, false, false, "100", "0", "0", "0", "0", "4213"),
      (mfj, "30000", "30000", "0", 1, false, false, false, "0", "0", "0", "0", "0", "4151"),
      (mfj, "35000", "35000", "0", 2, true, false, false, "0", "0", "0", "0", "0", "5826"),
      (mfj, "35000", "20000", "0", 0, false, false, false, "15000", "0", "0", "0", "0", "0"),
      (mfj, "36001", "25000", "0", 1, false, false, false, "11001", "0", "0", "0", "0", "3192"),
      (mfj, "40000", "40000", "0", 3, false, false, false, "0", "0", "0", "0", "0", "5643"),
      (mfj, "56000", "56000", "0", 2, false, false, false, "0", "0", "0", "0", "0", "1403"),
      (mfj, "65000", "50000", "0", 1, false, false, false, "0", "15000", "0", "0", "0", "0"),
      (mfj, "75000", "75000", "0", 2, false, false, false, "0", "0", "0", "0", "0", "0"),
      (mfj, "85000", "70000", "0", 0, false, false, false, "0", "7000", "4000", "4000", "0", "0"),
      (mfj, "90000", "75000", "0", 1, false, false, false, "0", "3000", "3000", "3000", "6000", "0"),
    )
    forAll(dataTable) {
      (
          status,
          agi,
          earnedIncomeSelf,
          earnedIncomeSpouse,
          eitcQualifyingChildren,
          primaryFilerAge65OrOlder,
          secondaryFilerAge65OrOlder,
          treatFilersAsDependents,
          nonTaxableInterestIncome,
          interestExcludingNonTaxableIncome,
          ordinaryDividendsIncome,
          shortTermCapitalGainsIncome,
          longTermCapitalGainsIncome,
          expectedEitc,
      ) =>
        val graph = makeGraphWith(
          factDictionary,
          Path("/filingStatus") -> status,
          Path("/agi") -> Dollar(agi),
          Path("/earnedIncomeSelf") -> Dollar(earnedIncomeSelf),
          Path("/earnedIncomeSpouse") -> Dollar(earnedIncomeSpouse),
          Path("/eitcQualifyingChildren") -> eitcQualifyingChildren,
          Path("/primaryFilerAge65OrOlder") -> primaryFilerAge65OrOlder,
          Path("/secondaryFilerAge65OrOlder") -> secondaryFilerAge65OrOlder,
          Path("/treatFilersAsDependents") -> treatFilersAsDependents,
          Path("/nonTaxableInterestIncome") -> Dollar(nonTaxableInterestIncome),
          Path("/interestExcludingNonTaxableIncome") -> Dollar(interestExcludingNonTaxableIncome),
          Path("/ordinaryDividendsIncome") -> Dollar(ordinaryDividendsIncome),
          Path("/shortTermCapitalGainsIncome") -> Dollar(shortTermCapitalGainsIncome),
          Path("/longTermCapitalGainsIncome") -> Dollar(longTermCapitalGainsIncome),
          Path("/primaryFilerIsBlind") -> false,
          Path("/secondaryFilerIsBlind") -> false,
          Path("/oB3Deductions") -> Dollar(0),
        )

        val actualEitc = graph.get("/earnedIncomeCredit")

        assert(actualEitc.value.contains(Dollar(expectedEitc)))
    }
    println(
      s"Completed ${dataTable.length} EITC calculation scenarios",
    )
  }

  test("test EITC: ineligible taxpayers do not receive EITC") {
    val dataTable = Table(
      (
        "status",
        "agi",
        "earnedIncomeSelf",
        "earnedIncomeSpouse",
        "eitcQualifyingChildren",
        "primaryFilerAge65OrOlder",
        "secondaryFilerAge65OrOlder",
        "treatFilersAsDependents",
        "primaryFilerAge25OrOlderForEitc",
        "secondaryFilerAge25OrOlderForEitc",
        "nonTaxableInterestIncome",
        "interestExcludingNonTaxableIncome",
        "ordinaryDividendsIncome",
        "shortTermCapitalGainsIncome",
        "longTermCapitalGainsIncome",
        "expectedEitc",
      ),
      //    single no QCs, self is dependent = no EITC
      (single, "5000", "5000", "0", 0, false, false, true, true, true, "0", "0", "0", "0", "0", "0"),
      (single, "5000", "5000", "0", 0, false, false, true, true, true, "0", "0", "0", "0", "0", "0"),
      (single, "6000", "6000", "0", 0, false, false, true, true, true, "0", "0", "0", "0", "0", "0"),
      (single, "7000", "7000", "0", 0, false, false, true, true, true, "0", "0", "0", "0", "0", "0"),
      (single, "9000", "9000", "0", 0, false, false, true, true, true, "0", "0", "0", "0", "0", "0"),
      (single, "12000", "12000", "0", 0, false, false, true, true, true, "0", "0", "0", "0", "0", "0"),
      (single, "15000", "15000", "0", 0, true, false, true, true, true, "0", "0", "0", "0", "0", "0"),
      (single, "20000", "20000", "0", 0, false, false, true, true, true, "0", "0", "0", "0", "0", "0"),
      (single, "20000", "20000", "0", 0, false, false, true, true, true, "0", "0", "0", "0", "0", "0"),
      (single, "25100", "25000", "0", 0, false, false, true, true, true, "100", "0", "0", "0", "0", "0"),

      //    single no QCs, self is over 65 = no EITC
      (single, "30000", "30000", "0", 0, false, true, false, true, true, "0", "0", "0", "0", "0", "0"),
      (single, "35000", "35000", "0", 0, false, true, false, true, true, "0", "0", "0", "0", "0", "0"),
      (single, "35000", "20000", "0", 0, false, true, false, true, true, "15000", "0", "0", "0", "0", "0"),
      (single, "36001", "25000", "0", 0, false, true, false, true, true, "11001", "0", "0", "0", "0", "0"),
      (single, "40000", "40000", "0", 0, false, true, false, true, true, "0", "0", "0", "0", "0", "0"),
      (single, "56000", "56000", "0", 0, true, false, false, true, true, "0", "0", "0", "0", "0", "0"),

      //    single no QCs, self under 25 = no EITC
      (single, "30000", "30000", "0", 0, false, false, false, false, false, "0", "0", "0", "0", "0", "0"),
      (single, "35000", "35000", "0", 0, false, false, false, false, false, "0", "0", "0", "0", "0", "0"),
      (single, "35000", "20000", "0", 0, false, false, false, false, false, "15000", "0", "0", "0", "0", "0"),
      (single, "36001", "25000", "0", 0, false, false, false, false, false, "11001", "0", "0", "0", "0", "0"),
      (single, "40000", "40000", "0", 0, false, false, false, false, false, "0", "0", "0", "0", "0", "0"),
      (single, "56000", "56000", "0", 0, true, false, false, false, false, "0", "0", "0", "0", "0", "0"),

      //    MFJ no QCs, one or both filers are dependents = no EITC
      (mfj, "5000", "5000", "0", 0, false, false, true, true, true, "0", "0", "0", "0", "0", "0"),
      (mfj, "5000", "5000", "0", 0, false, false, true, true, true, "0", "0", "0", "0", "0", "0"),
      (mfj, "6000", "6000", "0", 0, false, false, true, true, true, "0", "0", "0", "0", "0", "0"),
      (mfj, "7000", "7000", "0", 0, false, false, true, true, true, "0", "0", "0", "0", "0", "0"),
      (mfj, "9000", "9000", "0", 0, false, false, true, true, true, "0", "0", "0", "0", "0", "0"),
      (mfj, "12000", "12000", "0", 0, false, false, true, true, true, "0", "0", "0", "0", "0", "0"),
      (mfj, "15000", "15000", "0", 0, true, false, true, true, true, "0", "0", "0", "0", "0", "0"),
      (mfj, "20000", "20000", "0", 0, false, false, true, true, true, "0", "0", "0", "0", "0", "0"),
      (mfj, "20000", "20000", "0", 0, false, false, true, true, true, "0", "0", "0", "0", "0", "0"),
      (mfj, "25100", "25000", "0", 0, false, false, true, true, true, "100", "0", "0", "0", "0", "0"),

      //    MFJ no QCs, self or spouse is over 65 = no EITC
      (mfj, "30000", "30000", "0", 0, false, true, false, true, true, "0", "0", "0", "0", "0", "0"),
      (mfj, "35000", "35000", "0", 0, false, true, false, true, true, "0", "0", "0", "0", "0", "0"),
      (mfj, "35000", "20000", "0", 0, false, true, false, true, true, "15000", "0", "0", "0", "0", "0"),
      (mfj, "36001", "25000", "0", 0, false, true, false, true, true, "11001", "0", "0", "0", "0", "0"),
      (mfj, "40000", "40000", "0", 0, false, true, false, true, true, "0", "0", "0", "0", "0", "0"),
      (mfj, "56000", "56000", "0", 0, true, false, false, true, true, "0", "0", "0", "0", "0", "0"),

      //    MFJ no QCs, self and spouse under 25 = no EITC
      (mfj, "30000", "30000", "0", 0, false, false, false, false, false, "0", "0", "0", "0", "0", "0"),
      (mfj, "35000", "35000", "0", 0, false, false, false, false, false, "0", "0", "0", "0", "0", "0"),
      (mfj, "35000", "20000", "0", 0, false, false, false, false, false, "15000", "0", "0", "0", "0", "0"),
      (mfj, "36001", "25000", "0", 0, false, false, false, false, false, "11001", "0", "0", "0", "0", "0"),
      (mfj, "40000", "40000", "0", 0, false, false, false, false, false, "0", "0", "0", "0", "0", "0"),
      (mfj, "56000", "56000", "0", 0, true, false, false, false, false, "0", "0", "0", "0", "0", "0"),
    )
    forAll(dataTable) {
      (
          status,
          agi,
          earnedIncomeSelf,
          earnedIncomeSpouse,
          eitcQualifyingChildren,
          primaryFilerAge65OrOlder,
          secondaryFilerAge65OrOlder,
          primaryFilerAge25OrOlderForEitc,
          secondaryFilerAge25OrOlderForEitc,
          treatFilersAsDependents,
          nonTaxableInterestIncome,
          interestExcludingNonTaxableIncome,
          ordinaryDividendsIncome,
          shortTermCapitalGainsIncome,
          longTermCapitalGainsIncome,
          expectedEitc,
      ) =>
        val graph = makeGraphWith(
          factDictionary,
          Path("/filingStatus") -> status,
          Path("/agi") -> Dollar(agi),
          Path("/earnedIncomeSelf") -> Dollar(earnedIncomeSelf),
          Path("/earnedIncomeSpouse") -> Dollar(earnedIncomeSpouse),
          Path("/eitcQualifyingChildren") -> eitcQualifyingChildren,
          Path("/primaryFilerAge65OrOlder") -> primaryFilerAge65OrOlder,
          Path("/secondaryFilerAge65OrOlder") -> secondaryFilerAge65OrOlder,
          Path("/primaryFilerAge25OrOlderForEitc") -> primaryFilerAge25OrOlderForEitc,
          Path("/secondaryFilerAge25OrOlderForEitc") -> secondaryFilerAge25OrOlderForEitc,
          Path("/treatFilersAsDependents") -> treatFilersAsDependents,
          Path("/nonTaxableInterestIncome") -> Dollar(nonTaxableInterestIncome),
          Path("/interestExcludingNonTaxableIncome") -> Dollar(interestExcludingNonTaxableIncome),
          Path("/ordinaryDividendsIncome") -> Dollar(ordinaryDividendsIncome),
          Path("/shortTermCapitalGainsIncome") -> Dollar(shortTermCapitalGainsIncome),
          Path("/longTermCapitalGainsIncome") -> Dollar(longTermCapitalGainsIncome),
          Path("/primaryFilerIsBlind") -> false,
          Path("/secondaryFilerIsBlind") -> false,
          Path("/oB3Deductions") -> Dollar(0),
        )

        val actualEitc = graph.get("/earnedIncomeCredit")
        assert(actualEitc.value.contains(Dollar(expectedEitc)))
    }
    println(
      s"Completed ${dataTable.length} EITC ineligibility scenarios",
    )
  }
}

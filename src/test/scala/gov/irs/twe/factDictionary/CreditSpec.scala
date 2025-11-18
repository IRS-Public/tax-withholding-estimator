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
}

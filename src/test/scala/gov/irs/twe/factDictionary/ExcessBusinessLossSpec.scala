package gov.irs.twe.factDictionary

import gov.irs.factgraph.types.{ Dollar, Enum }
import gov.irs.factgraph.FactDictionaryForTests
import gov.irs.factgraph.Path
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.prop.TableDrivenPropertyChecks

class ExcessBusinessLossSpec extends AnyFunSuite with TableDrivenPropertyChecks {
  // Assuming setupFactDictionary() and makeGraphWith() are available in the testing environment
  val factDictionary = setupFactDictionary()

  // Define the filing status boolean fact for threshold calculation (as used in the XML)
  val single = Enum("single", "/filingStatusOptions")
  val mfs = Enum("marriedFilingSeparately", "/filingStatusOptions")
  val mfj = Enum("marriedFilingJointly", "/filingStatusOptions")
  val qss = Enum("qualifiedSurvivingSpouse", "/filingStatusOptions")
  val hoh = Enum("headOfHousehold", "/filingStatusOptions")

  // Test data table for Form 461 calculation scenarios.
  // We minimize the variable inputs by setting most Line 9 dependencies to zero,
  // focusing on the primary loss drivers (netSelfEmploymentIncomeTotal, netScheduleEIncome).
  val dataTable = Table(
    (
      "status",
      "netSelfEmploymentIncomeTotal",
      "netScheduleEIncome",
      "expectedExcessBusinessLossAdjustment",
    ),
    (mfj, "-50000", "0", "0"),
    (mfj, "0", "300000", "0"),
    (mfj, "-313000", "0", "0"),
    (mfj, "0", "313000", "0"),
    (mfj, "-314000", "0", "1000"),
    (mfj, "0", "314000", "1000"),
    (mfj, "-200000", "100000", "0"),
    (mfj, "-200000", "113000", "0"),
    (mfj, "-300000", "300000", "287000"),
    (mfj, "-350000", "-10000", "27000"),
    (single, "-100000", "0", "0"),
    (single, "0", "600000", "0"),
    (single, "-626000", "0", "0"),
    (single, "0", "626000", "0"),
    (single, "-627000", "0", "1000"),
    (single, "0", "627000", "1000"),
    (single, "-300000", "300000", "0"),
    (single, "-400000", "226000", "0"),
    (single, "-500000", "500000", "374000"),
    (single, "-700000", "-50000", "24000"),

    // large SE income losses
    (single, "-600000", "0", "0"),
    (single, "-800000", "0", "174000"),
    (single, "-300000", "400000", "74000"),
    (mfj, "-300000", "0", "0"),
    (mfj, "-313000", "0", "0"),
    (mfj, "-500000", "0", "187000"),

    // large SE income loss with large Schedule E income
    (mfj, "-200000", "300000", "187000"),

    //    large schedule E income, large SE income
    (mfj, "100000", "50000", "0"),
    //    large schedule E income, no SE income
    (mfj, "0", "400000", "87000"),
  )

  test("test excess business loss scenarios for Form 461") {
    forAll(dataTable) {
      (
          status,
          netSelfEmploymentIncomeTotal,
          netScheduleEIncome,
          expectedExcessBusinessLossAdjustment,
      ) =>
        // Define all dependencies required by the XML, setting non-variable ones to 0
        val graph = makeGraphWith(
          factDictionary,
          Path("/filingStatus") -> status,
          Path("/netSelfEmploymentIncomeTotal") -> Dollar(netSelfEmploymentIncomeTotal),
          Path("/netScheduleEIncome") -> Dollar(netScheduleEIncome),
          // Set Line 9 dependencies not in the table to zero for consistent testing
          Path("/shortTermCapitalGainsIncome") -> Dollar("0"),
          Path("/longTermCapitalGainsIncome") -> Dollar("0"),
          Path("/sCorpNonPassiveIncome") -> Dollar("0"),
          Path("/rentalIncome") -> Dollar("0"),
          Path("/royaltyIncome") -> Dollar("0"),
        )
        val actual = graph.get("/excessBusinessLossAdjustment")

        assert(actual.value.contains(Dollar(expectedExcessBusinessLossAdjustment)))

    }
    println(s"Completed ${dataTable.length} tests for calculating excess business loss adjustment ")
  }
}

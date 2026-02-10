package gov.irs.twe.factDictionary

import gov.irs.factgraph.types.Collection
import gov.irs.factgraph.types.Day
import gov.irs.factgraph.types.Dollar
import gov.irs.factgraph.types.Enum
import gov.irs.factgraph.FactDictionaryForTests
import gov.irs.factgraph.Path
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.prop.TableDrivenPropertyChecks

class WithholdingCalculationsSpec extends AnyFunSuite with TableDrivenPropertyChecks {
  val factDictionary = setupFactDictionary()

  val jobs = Path("/jobs")
  val jobId = "14e10d4f-728b-496b-aaf9-41c627cf48bf"
  val jobsCollection = Collection(Vector(java.util.UUID.fromString(jobId)))

  val weekly = Enum("weekly", "/payFrequencyOptions");
  val biWeekly = Enum("biWeekly", "/payFrequencyOptions");
  val semiMonthly = Enum("semiMonthly", "/payFrequencyOptions");
  val monthly = Enum("monthly", "/payFrequencyOptions");

  val dataTable = Table(
    ("frequency", "runDate", "mostRecentPayDate", "payPeriodDelay"),
    (weekly, "2025-05-08", "2025-05-05", 2),
    (weekly, "2025-05-08", "2025-05-08", 2),
    (biWeekly, "2025-05-17", "2025-05-05", 1),
    (biWeekly, "2025-05-18", "2025-05-05", 1),
    (biWeekly, "2025-05-19", "2025-05-19", 1),
    (semiMonthly, "2025-01-16", "2025-01-16", 0),
    (semiMonthly, "2025-01-16", "2025-01-14", 1),
    (semiMonthly, "2025-01-15", "2025-01-14", 1),
    (semiMonthly, "2025-06-04", "2025-05-20", 1),
    (semiMonthly, "2025-02-13", "2025-02-07", 1),
    (semiMonthly, "2025-02-12", "2025-02-07", 1),
    (semiMonthly, "2025-02-17", "2025-02-10", 1),
    (semiMonthly, "2025-02-20", "2025-02-17", 1),
    (semiMonthly, "2025-01-31", "2025-01-30", 0),
    (semiMonthly, "2025-01-31", "2025-01-31", 0),
    (semiMonthly, "2025-01-30", "2025-01-30", 0),
    (semiMonthly, "2025-02-14", "2025-01-31", 2),
    (semiMonthly, "2025-02-14", "2025-01-30", 2),
    (monthly, "2025-05-20", "2025-05-05", 0),
    (monthly, "2025-05-21", "2025-05-04", 1),
    (monthly, "2025-06-04", "2025-05-05", 1),
    (monthly, "2025-02-12", "2025-01-31", 0),
    (monthly, "2025-02-14", "2025-01-31", 1),
  )

  test("payPeriodsBeforeW4ChangesAppear varies based on when pay dates and run date") {
    forAll(dataTable) { (payFreq, runDate, mostRecentPayDate, payPeriodDelay) =>
      val graph = makeGraphWith(
        factDictionary,
        jobs -> jobsCollection,
        Path(s"/jobs/#$jobId/payFrequency") -> payFreq,
        Path("/overrideDate") -> Day(runDate),
        Path(s"/jobs/#$jobId/mostRecentPayDate") -> Day(mostRecentPayDate),
      )
      // Sanity check the test inputs
      assert(Day(mostRecentPayDate).<=(Day(runDate)))

      val payPeriodsBeforeW4ChangesAppear = graph.get(Path(s"/jobs/#${jobId}/payPeriodsBeforeW4ChangesAppear"))
      assert(payPeriodsBeforeW4ChangesAppear.value.contains(payPeriodDelay))
    }
  }

  test("bonus pay withholding") {
    val mostRecentPayPeriodBonusAmount = Path(s"/jobs/#$jobId/mostRecentPayPeriodBonusAmount")
    val totalFutureBonus = Path(s"/jobs/#$jobId/totalFutureBonus")

    val bonusTable = Table(
      ("currentBonus", "futureBonus", "expectedWithholding"),
      (1000000, 1000, 370),
      (500000, 600000, 147000),
      (500000, 1000, 220),
    )
    forAll(bonusTable) { (currentBonus, futureBonus, expectedWithholding) =>
      val graph = makeGraphWith(
        factDictionary,
        jobs -> jobsCollection,
        mostRecentPayPeriodBonusAmount -> Dollar(currentBonus),
        totalFutureBonus -> Dollar(futureBonus),
      )
      assert(
        graph.get(Path(s"/jobs/#$jobId/withholdingsFromTotalBonusReceived")).value.contains(Dollar(expectedWithholding)),
      )
    }
  }
}

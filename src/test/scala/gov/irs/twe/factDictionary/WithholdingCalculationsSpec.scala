package gov.irs.twe.factDictionary

import gov.irs.factgraph.types.{ Collection, CollectionItem, Day, Dollar, Enum }
import gov.irs.factgraph.Path
import java.util.UUID
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.prop.TableDrivenPropertyChecks

class WithholdingCalculationsSpec extends AnyFunSuite with TableDrivenPropertyChecks {
  private val factDictionary = setupFactDictionary()

  private val jobs = Path("/jobs")
  private val jobId = "14e10d4f-728b-496b-aaf9-41c627cf48bf"
  private val jobsCollection = Collection(Vector(java.util.UUID.fromString(jobId)))

  private val weekly = Enum("weekly", "/payFrequencyOptions");
  private val biWeekly = Enum("biWeekly", "/payFrequencyOptions");
  private val semiMonthly = Enum("semiMonthly", "/payFrequencyOptions");
  private val monthly = Enum("monthly", "/payFrequencyOptions");

  private val dataTable = Table(
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

  test(
    "/mayNeedToMakeEstimatedPayments properly indicates whether or not the taxpayer is able to cover their tax balance via extra job withholding alone",
  ) {
    val testParameters = Table(
      (
        "withholdingAmountPerPayPeriod",
        "averagePayPerPayPeriodForWithholding",
        "expectedMayNeedToMakeEstimatedPayments",
      ),
      // TRUE - Will need to make estimated payments because recommended withholding exceeds average pay
      (1000, 999, true),

      // TRUE - Tiebreaker case. Taxpayer earn make enough to bring their balance to 0, however, that's only if they act
      //        immediately, and if everything they entered is accurate. Since we say "may need to make estimated payments",
      //        in the case of a tie, show the content anyway to encourage taxpayers to double-check when they might
      //        be in this scenario.
      (1000, 1000, true),

      // FALSE - Will not need to make estimated payments because remaining pay is larger than withholding
      (1000, 1001, false),
      (1000, 10000, false),

      // FALSE - Will not need to make estimated payments because there is no extra withholding
      (0, 0, false),
      (0, 1000, false),
    )

    forAll(testParameters) {
      (
          withholdingAmountPerPayPeriod,
          averagePayPerPayPeriodForWithholding,
          expectedMayNeedToMakeEstimatedPayments,
      ) =>
        // given
        val graph = makeGraphWith(
          factDictionary,
          Path("/jobSelectedForExtraWithholding") -> CollectionItem(UUID.randomUUID()),
          Path("/jobSelectedForExtraWithholding/averagePayPerPayPeriodForWithholding") -> Dollar(
            averagePayPerPayPeriodForWithholding,
          ),
          Path("/jobSelectedForExtraWithholding/withholdingAmountPerPayPeriod") -> Dollar(
            withholdingAmountPerPayPeriod,
          ),
        )

        // when
        val mayNeedToMakeEstimatedPayments = graph.get("/mayNeedToMakeEstimatedPayments")

        // then
        assert(mayNeedToMakeEstimatedPayments.hasValue)
        assert(mayNeedToMakeEstimatedPayments.get == expectedMayNeedToMakeEstimatedPayments)
    }
  }

  test(
    "/mayNeedToMakeEstimatedPayments properly indicates whether or not the taxpayer is able to cover their tax balance via extra pension withholding alone",
  ) {
    val testParameters = Table(
      (
        "withholdingAmountPerPayPeriod",
        "averagePayPerPayPeriodForWithholding",
        "expectedMayNeedToMakeEstimatedPayments",
      ),
      // TRUE - Will need to make estimated payments because recommended withholding exceeds average pay
      (1000, 999, true),

      // TRUE - Tiebreaker case. Taxpayer does earn enough to bring their balance to 0, however, that's only if they act
      //        immediately, and if everything they entered is accurate. Since we say "may need to make estimated payments",
      //        in the case of a tie, show the content anyway to encourage taxpayers to double-check when they might
      //        be in this scenario.
      (1000, 1000, true),

      // FALSE - Will not need to make estimated payments because remaining pay is larger than withholding
      (1000, 1001, false),
      (1000, 10000, false),

      // FALSE - Will not need to make estimated payments because there is no extra withholding
      (0, 0, false),
      (0, 1000, false),
    )

    forAll(testParameters) {
      (
          withholdingAmountPerPayPeriod,
          averagePayPerPayPeriodForWithholding,
          expectedMayNeedToMakeEstimatedPayments,
      ) =>
        val pensionId = UUID.randomUUID()
        val pensions = Collection(Vector(pensionId))
        val taxYear = 2026
        val today = Day(s"$taxYear-01-01")
        // given
        val graph = makeGraphWith(
          factDictionary,
          Path("/pensions") -> pensions,
          Path(s"/pensions/#$pensionId/isAllYear") -> true,
          Path(s"/pensions/#$pensionId/averagePayPerPayPeriodForWithholding") -> Dollar(
            averagePayPerPayPeriodForWithholding,
          ),
          Path(s"/pensions/#$pensionId/withholdingAmountPerPayPeriod") -> Dollar(
            withholdingAmountPerPayPeriod,
          ),
          Path(s"/pensions/#$pensionId/yearToDateIncome") -> Dollar(averagePayPerPayPeriodForWithholding),
          Path(s"/pensions/#$pensionId/yearToDateWithholding") -> Dollar(0),
          Path(s"/pensions/#$pensionId/payFrequency") -> Enum("monthly", "/payFrequencyOptions"),
          Path(s"/pensions/#$pensionId/mostRecentPayDate") -> today,
          Path("/today") -> today,
        )

        // when
        val mayNeedToMakeEstimatedPayments = graph.get("/mayNeedToMakeEstimatedPayments")

        // then
        assert(mayNeedToMakeEstimatedPayments.hasValue)
        assert(mayNeedToMakeEstimatedPayments.get == expectedMayNeedToMakeEstimatedPayments)
    }
  }
}

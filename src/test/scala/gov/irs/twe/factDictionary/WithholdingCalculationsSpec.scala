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
        val jobId = "11111111-1111-447e-a94d-0944cc8e3b6b"
        val jobsCollection = Collection(Vector(java.util.UUID.fromString(jobId)))
        val graph = makeGraphWith(
          factDictionary,
          Path("/hasJobsAvailableForExtraWithholding") -> true,
          Path("/jobs") -> jobsCollection,
          Path(s"/jobs/#$jobId/conditionForPossibleW4Jobs") -> true,
          Path(s"/jobs/#$jobId/averagePayPerPayPeriodForWithholding") -> Dollar(
            averagePayPerPayPeriodForWithholding,
          ),
          Path(s"/jobs/#$jobId/averageTaxablePayPerPayPeriod") -> Dollar(
            averagePayPerPayPeriodForWithholding,
          ),
          Path(s"/jobs/#$jobId/withholdingAmountPerPayPeriod") -> Dollar(
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

  test(
    "/mayNeedToMakeEstimatedPayments combines jobs and pensions: true only when total withholding exceeds total pay",
  ) {

    // Scenario: Job 1 has 2 remaining pay periods ($5,000/period, biweekly),
    // Job 2 has 1 remaining pay period ($10,000/period, biweekly). The tax gap is $3,000.
    // Individual job checks should match combined check.
    val job1Id = UUID.randomUUID()
    val job2Id = UUID.randomUUID()
    val jobsCollection = Collection(Vector(job1Id, job2Id))

    val testParameters = Table(
      (
        "job1Withholding",
        "job1Pay",
        "job2Withholding",
        "job2Pay",
        "expected",
        "description",
      ),
      // TRUE - Job 1 (2 periods, biweekly): withholding $19,500 > pay $5,000
      //        Job 2 (1 period, biweekly): withholding $39,000 > pay $10,000
      //        Combined: $58,500 > $15,000 — all agree: true
      (19500, 5000, 39000, 10000, true, "different periods, both jobs exceed pay individually and combined"),
      // FALSE - Same frequency structure but taxGap small enough that neither exceeds
      //         Job 1 withholding $500 < pay $5,000
      //         Job 2 withholding $1,000 < pay $10,000
      //         Combined: $1,500 < $15,000 — all agree: false
      (500, 5000, 1000, 10000, false, "different periods, neither job exceeds pay individually or combined"),
      // TRUE - Job 1 (monthly, 1 period): withholding $8,000 > pay $5,000
      //        Job 2 (biweekly, 2 periods): withholding $12,000 > pay $10,000
      //        Combined: $20,000 > $15,000 — all agree: true
      (8000, 5000, 12000, 10000, true, "different pay frequencies, both jobs exceed pay individually and combined"),
      // FALSE - Job 1 (monthly): withholding $4,000 < pay $5,000
      //         Job 2 (biweekly): withholding $9,000 < pay $10,000
      //         Combined: $13,000 < $15,000 — all agree: false
      (4000, 5000, 9000, 10000, false, "different pay frequencies, neither job exceeds pay individually or combined"),
    )

    forAll(testParameters) { (job1Withholding, job1Pay, job2Withholding, job2Pay, expected, description) =>
      val graph = makeGraphWith(
        factDictionary,
        Path("/jobs") -> jobsCollection,
        Path(s"/jobs/#$job1Id/conditionForPossibleW4Jobs") -> true,
        Path(s"/jobs/#$job1Id/averagePayPerPayPeriodForWithholding") -> Dollar(job1Pay),
        Path(s"/jobs/#$job1Id/averageTaxablePayPerPayPeriod") -> Dollar(job1Pay),
        Path(s"/jobs/#$job1Id/withholdingAmountPerPayPeriod") -> Dollar(job1Withholding),
        Path(s"/jobs/#$job2Id/conditionForPossibleW4Jobs") -> true,
        Path(s"/jobs/#$job2Id/averagePayPerPayPeriodForWithholding") -> Dollar(job2Pay),
        Path(s"/jobs/#$job2Id/averageTaxablePayPerPayPeriod") -> Dollar(job2Pay),
        Path(s"/jobs/#$job2Id/withholdingAmountPerPayPeriod") -> Dollar(job2Withholding),
      )

      val result = graph.get("/mayNeedToMakeEstimatedPayments")
      assert(result.hasValue)
      assert(result.get == expected, description)
    }
  }

  test(
    "/mayNeedToMakeEstimatedPayments combines two jobs: true only when total withholding exceeds total pay",
  ) {
    val job1Id = UUID.randomUUID()
    val job2Id = UUID.randomUUID()
    val jobsCollection = Collection(Vector(job1Id, job2Id))
    val taxYear = 2026
    val today = Day(s"$taxYear-01-01")

    val testParameters = Table(
      (
        "job1Withholding",
        "job1Pay",
        "job2Withholding",
        "job2Pay",
        "expected",
      ),
      // TRUE - combined withholding exceeds combined pay
      (800, 500, 300, 400, true),
      // TRUE - tiebreaker
      (600, 500, 400, 500, true),
      // FALSE - combined withholding less than combined pay even though
      //         job1 withholding alone exceeds job1 pay
      (800, 500, 0, 1000, false),
      // FALSE - neither job exceeds pay
      (400, 500, 300, 400, false),
    )

    forAll(testParameters) { (job1Withholding, job1Pay, job2Withholding, job2Pay, expected) =>
      val graph = makeGraphWith(
        factDictionary,
        Path("/jobs") -> jobsCollection,
        Path(s"/jobs/#$job1Id/conditionForPossibleW4Jobs") -> true,
        Path(s"/jobs/#$job1Id/averagePayPerPayPeriodForWithholding") -> Dollar(job1Pay),
        Path(s"/jobs/#$job1Id/averageTaxablePayPerPayPeriod") -> Dollar(job1Pay),
        Path(s"/jobs/#$job1Id/withholdingAmountPerPayPeriod") -> Dollar(job1Withholding),
        Path(s"/jobs/#$job2Id/conditionForPossibleW4Jobs") -> true,
        Path(s"/jobs/#$job2Id/averagePayPerPayPeriodForWithholding") -> Dollar(job2Pay),
        Path(s"/jobs/#$job2Id/averageTaxablePayPerPayPeriod") -> Dollar(job2Pay),
        Path(s"/jobs/#$job2Id/withholdingAmountPerPayPeriod") -> Dollar(job2Withholding),
      )

      val result = graph.get("/mayNeedToMakeEstimatedPayments")
      assert(result.hasValue)
      assert(
        result.get == expected,
        s"job1Withholding=$job1Withholding job1Pay=$job1Pay job2Withholding=$job2Withholding job2Pay=$job2Pay",
      )
    }
  }

  test(
    "/cannotZeroOutRefundDueToExcessWithholding combines jobs and pensions: true when total tentative withholding < total credits needed",
  ) {
    val job1Id = UUID.randomUUID()
    val pension1Id = UUID.randomUUID()
    val jobsCollection = Collection(Vector(job1Id))
    val pensionsCollection = Collection(Vector(pension1Id))
    val taxYear = 2026
    val today = Day(s"$taxYear-01-01")

    val testParameters = Table(
      (
        "jobTentative",
        "jobCredits",
        "pensionTentative",
        "pensionCredits",
        "expected",
      ),
      // TRUE - combined tentative < combined credits needed
      // job alone can't cover, pension alone can't cover, combined can't cover
      (500, 800, 400, 600, true),
      // TRUE - combined tentative < combined credits (job covers its share but pension can't)
      (1000, 500, 200, 800, true),
      // FALSE - combined tentative >= combined credits needed
      // even though job tentative < job credits, pension makes up the difference
      (500, 800, 1000, 100, false),
      // FALSE - both sources have enough capacity
      (1000, 500, 800, 400, false),
    )

    forAll(testParameters) { (jobTentative, jobCredits, pensionTentative, pensionCredits, expected) =>
      val graph = makeGraphWith(
        factDictionary,
        Path("/jobs") -> jobsCollection,
        Path("/pensions") -> pensionsCollection,
        Path(s"/jobs/#$job1Id/conditionForPossibleW4Jobs") -> true,
        Path(s"/jobs/#$job1Id/tentativeAnnualWithholdingAmount") -> Dollar(jobTentative),
        Path(s"/jobs/#$job1Id/creditsNeededToFullyBridgeTaxGap") -> Dollar(jobCredits),
        Path(s"/pensions/#$pension1Id/isAllYear") -> true,
        Path(s"/pensions/#$pension1Id/tentativeAnnualWithholdingAmount") -> Dollar(pensionTentative),
        Path(s"/pensions/#$pension1Id/creditsNeededToFullyBridgeTaxGap") -> Dollar(pensionCredits),
        Path(s"/pensions/#$pension1Id/averagePayPerPayPeriodForWithholding") -> Dollar(1000),
        Path(s"/pensions/#$pension1Id/yearToDateIncome") -> Dollar(1000),
        Path(s"/pensions/#$pension1Id/yearToDateWithholding") -> Dollar(0),
        Path(s"/pensions/#$pension1Id/payFrequency") -> Enum("monthly", "/payFrequencyOptions"),
        Path(s"/pensions/#$pension1Id/mostRecentPayDate") -> today,
        Path("/today") -> today,
      )

      val result = graph.get("/cannotZeroOutRefundDueToExcessWithholding")
      assert(result.hasValue)
      assert(
        result.get == expected,
        s"jobTentative=$jobTentative jobCredits=$jobCredits pensionTentative=$pensionTentative pensionCredits=$pensionCredits",
      )
    }
  }

  test(
    "/cannotZeroOutRefundDueToExcessWithholding combines two jobs: true when total tentative withholding < total credits needed",
  ) {
    val job1Id = UUID.randomUUID()
    val job2Id = UUID.randomUUID()
    val jobsCollection = Collection(Vector(job1Id, job2Id))

    val testParameters = Table(
      (
        "job1Tentative",
        "job1Credits",
        "job2Tentative",
        "job2Credits",
        "expected",
      ),
      // TRUE - combined tentative < combined credits
      (500, 800, 400, 600, true),
      // FALSE - combined tentative >= combined credits even though job1 alone can't cover
      (500, 800, 1000, 100, false),
      // FALSE - both jobs have enough capacity
      (1000, 500, 800, 400, false),
    )

    forAll(testParameters) { (job1Tentative, job1Credits, job2Tentative, job2Credits, expected) =>
      val graph = makeGraphWith(
        factDictionary,
        Path("/jobs") -> jobsCollection,
        Path(s"/jobs/#$job1Id/conditionForPossibleW4Jobs") -> true,
        Path(s"/jobs/#$job1Id/tentativeAnnualWithholdingAmount") -> Dollar(job1Tentative),
        Path(s"/jobs/#$job1Id/creditsNeededToFullyBridgeTaxGap") -> Dollar(job1Credits),
        Path(s"/jobs/#$job2Id/conditionForPossibleW4Jobs") -> true,
        Path(s"/jobs/#$job2Id/tentativeAnnualWithholdingAmount") -> Dollar(job2Tentative),
        Path(s"/jobs/#$job2Id/creditsNeededToFullyBridgeTaxGap") -> Dollar(job2Credits),
      )

      val result = graph.get("/cannotZeroOutRefundDueToExcessWithholding")
      assert(result.hasValue)
      assert(
        result.get == expected,
        s"job1Tentative=$job1Tentative job1Credits=$job1Credits job2Tentative=$job2Tentative job2Credits=$job2Credits",
      )
    }
  }

  test(
    "/eligibleRemainingPayPeriodsGreaterThanZero is true when any job or pension is available for extra withholding",
  ) {
    val job1Id = UUID.randomUUID()
    val pension1Id = UUID.randomUUID()
    val taxYear = 2026
    val today = Day(s"$taxYear-01-01")

    // TRUE - one job
    val graphWithJob = makeGraphWith(
      factDictionary,
      Path("/jobs") -> Collection(Vector(job1Id)),
      Path(s"/jobs/#$job1Id/conditionForPossibleW4Jobs") -> true,
      Path(s"/jobs/#$job1Id/averagePayPerPayPeriodForWithholding") -> Dollar(1000),
      Path(s"/jobs/#$job1Id/averageTaxablePayPerPayPeriod") -> Dollar(1000),
      Path(s"/jobs/#$job1Id/withholdingAmountPerPayPeriod") -> Dollar(100),
    )
    assert(
      graphWithJob.get("/eligibleRemainingPayPeriodsGreaterThanZero").get == true,
      "should be true when one job is available",
    )

    // TRUE - one pension
    val graphWithPension = makeGraphWith(
      factDictionary,
      Path("/pensions") -> Collection(Vector(pension1Id)),
      Path(s"/pensions/#$pension1Id/isAllYear") -> true,
      Path(s"/pensions/#$pension1Id/averagePayPerPayPeriodForWithholding") -> Dollar(1000),
      Path(s"/pensions/#$pension1Id/withholdingAmountPerPayPeriod") -> Dollar(100),
      Path(s"/pensions/#$pension1Id/yearToDateIncome") -> Dollar(1000),
      Path(s"/pensions/#$pension1Id/yearToDateWithholding") -> Dollar(0),
      Path(s"/pensions/#$pension1Id/payFrequency") -> Enum("monthly", "/payFrequencyOptions"),
      Path(s"/pensions/#$pension1Id/mostRecentPayDate") -> today,
      Path("/today") -> today,
    )
    assert(
      graphWithPension.get("/eligibleRemainingPayPeriodsGreaterThanZero").get == true,
      "should be true when one pension is available",
    )

    // TRUE - both a job and a pension available
    val graphWithBoth = makeGraphWith(
      factDictionary,
      Path("/jobs") -> Collection(Vector(job1Id)),
      Path("/pensions") -> Collection(Vector(pension1Id)),
      Path(s"/jobs/#$job1Id/conditionForPossibleW4Jobs") -> true,
      Path(s"/jobs/#$job1Id/averagePayPerPayPeriodForWithholding") -> Dollar(1000),
      Path(s"/jobs/#$job1Id/averageTaxablePayPerPayPeriod") -> Dollar(1000),
      Path(s"/jobs/#$job1Id/withholdingAmountPerPayPeriod") -> Dollar(100),
      Path(s"/pensions/#$pension1Id/isAllYear") -> true,
      Path(s"/pensions/#$pension1Id/averagePayPerPayPeriodForWithholding") -> Dollar(1000),
      Path(s"/pensions/#$pension1Id/withholdingAmountPerPayPeriod") -> Dollar(100),
      Path(s"/pensions/#$pension1Id/yearToDateIncome") -> Dollar(1000),
      Path(s"/pensions/#$pension1Id/yearToDateWithholding") -> Dollar(0),
      Path(s"/pensions/#$pension1Id/payFrequency") -> Enum("monthly", "/payFrequencyOptions"),
      Path(s"/pensions/#$pension1Id/mostRecentPayDate") -> today,
      Path("/today") -> today,
    )
    assert(
      graphWithBoth.get("/eligibleRemainingPayPeriodsGreaterThanZero").get == true,
      "should be true when both a job and pension are available",
    )

    // FALSE - no jobs or pensions
    val graphWithNeither = makeGraphWith(
      factDictionary,
    )
    assert(
      graphWithNeither.get("/eligibleRemainingPayPeriodsGreaterThanZero").get == false,
      "should be false when no jobs or pensions are available",
    )
  }
}

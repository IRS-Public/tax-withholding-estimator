package gov.irs.twe.factDictionary

import gov.irs.factgraph.types.Collection
import gov.irs.factgraph.types.Day
import gov.irs.factgraph.types.Dollar
import gov.irs.factgraph.types.Enum
import gov.irs.factgraph.types.Rational
import gov.irs.factgraph.FactDictionaryForTests
import gov.irs.factgraph.Path
import gov.irs.twe.FileLoaderHelper
import java.util.UUID
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.GivenWhenThen
import org.scalatest.OptionValues.convertOptionToValuable

class PayPeriodCalculationsSpec extends AnyFunSuite with GivenWhenThen with TableDrivenPropertyChecks {
  val factDictionary = setupFactDictionary()
  val dummyUUID = "9ba9d216-81a8-4944-81ac-9410b2fad150" // Previously used as a W2 UUID in cdcc tests
  val uuidVector = Vector(UUID.fromString(dummyUUID));

  val jobs = Path("/jobs")
  val payFrequency = Path(s"/jobs/#$dummyUUID/payFrequency")
  val startDate = Path(s"/jobs/#$dummyUUID/startDate")
  val endDate = Path(s"/jobs/#$dummyUUID/endDate")
  val recentPayPeriodEnd = Path(s"/jobs/#$dummyUUID/mostRecentPayPeriodEnd")
  val recentPayDate = Path(s"/jobs/#$dummyUUID/mostRecentPayDate")
  val tentativeRemainingPayPeriods = Path(s"/jobs/#$dummyUUID/tentativeRemainingPayPeriods")
  val remainingPayPeriodsInteger = Path(s"/jobs/#$dummyUUID/remainingPayPeriodsInteger")
  val ordinalEffectiveEndDate = Path(s"/jobs/#$dummyUUID/ordinalEffectiveEndDate")
  val restOfYearIncome = Path(s"/jobs/#$dummyUUID/restOfYearIncome")
  val averagePayPerPayPeriod = Path(s"/jobs/#$dummyUUID/averagePayPerPayPeriod")
  val isPastJob = Path(s"/jobs/#$dummyUUID/isPastJob")
  val isCurrentJob = Path(s"/jobs/#$dummyUUID/isCurrentJob")
  val isFutureJob = Path(s"/jobs/#$dummyUUID/isFutureJob")

  // TODO! Add cases where the most recent pay date is before the start date
  val weekly = Enum("weekly", "/payFrequencyOptions");
  val biWeekly = Enum("biWeekly", "/payFrequencyOptions");
  val semiMonthly = Enum("semiMonthly", "/payFrequencyOptions");

  val monthly = Enum("monthly", "/payFrequencyOptions");
  val dataTable = Table(
    (
      "frequencyValue",
      "startDateValue",
      "endDateValue",
      "recentPayPeriodEndValue",
      "recentPayDateValue",
      "expected tentative pay periods",
      "expected effective end date",
      "expected true pay periods",
      "expected rest of year income",
      "description",
    ),
    // Weekly Pay Periods
    (
      weekly,
      "2025-01-01",
      "2025-10-25",
      "2025-07-26",
      "2025-08-01",
      Some(13),
      Some(298),
      13,
      1300,
      "working for a portion of the year with no eoy overlap on weekly pay",
    ),
    (
      weekly,
      "2025-02-01",
      "2025-12-31",
      "2025-02-14",
      "2025-02-18",
      Some(46),
      Some(360),
      45,
      4500,
      "working for a portion of the year with eoy overlap on weekly pay",
    ),
    (
      weekly,
      "2025-01-01",
      "2025-12-31",
      "2025-07-26",
      "2025-08-01",
      Some(23),
      Some(354),
      21,
      2100,
      "working for the whole year with eoy overlap on weekly pay",
    ),
    (
      weekly,
      "2025-01-01",
      "2025-12-30",
      "2025-06-26",
      "2025-06-26",
      Some(27),
      Some(359),
      26,
      2600,
      "working for part of the year with eoy overlap on weekly pay",
    ),
    (
      weekly,
      "2025-01-01",
      "2025-05-22",
      "2024-12-15",
      "2025-01-05",
      Some(23),
      Some(146),
      23,
      2242.86,
      "weekly - partial year employment, handles period end dates in prior year",
    ),
    (
      weekly,
      "2025-01-01",
      "2025-12-31",
      "2024-12-15",
      "2025-01-05",
      Some(55),
      Some(342),
      51,
      5100,
      "weekly - full year employment, handles period end dates in prior year",
    ),
    (
      weekly,
      "2025-01-01",
      "2025-01-02",
      "2024-12-15",
      "2025-01-05",
      Some(3),
      Some(6),
      3,
      242.86,
      "weekly - minimal amount worked this year but has lagging payments from prior year",
    ),
    // Biweekly Pay Periods
    (
      biWeekly,
      "2025-01-01",
      "2025-10-25",
      "2025-07-26",
      "2025-08-01",
      Some(7),
      Some(305),
      7,
      650,
      "working for a portion of the year with no eoy overlap on biweekly pay",
    ),
    (
      biWeekly,
      "2025-02-01",
      "2025-12-31",
      "2025-02-10",
      "2025-02-14",
      Some(24),
      Some(349),
      22,
      2200,
      "working for a portion of the year with eoy overlap on biweekly pay",
    ),
    (
      biWeekly,
      "2025-01-01",
      "2025-12-31",
      "2025-07-26",
      "2025-08-01",
      Some(12),
      Some(347),
      10,
      1000,
      "working for the whole year with eoy overlap on biweekly pay",
    ),
    // Semi Monthly Pay Periods
    (
      semiMonthly,
      "2025-02-01",
      "2025-12-31",
      "2025-02-15",
      "2025-02-19",
      None,
      Some(349),
      20,
      2000,
      "working for a portion of the year with no eoy overlap on semimonthly pay",
    ),
    (
      semiMonthly,
      "2025-02-03",
      "2025-12-31",
      "2025-03-18",
      "2025-03-25",
      None,
      Some(352),
      18,
      1800,
      "working for a portion of the year with eoy overlap on semimonthly pay",
    ),
    (
      semiMonthly,
      "2025-01-01",
      "2025-12-31",
      "2025-08-05",
      "2025-08-11",
      None,
      Some(354),
      9,
      900,
      "working for the whole year with eoy overlap on semimonthly pay",
    ),
    (
      semiMonthly,
      "2025-01-01",
      "2025-12-10",
      "2025-08-05",
      "2025-08-11",
      None,
      Some(354),
      9,
      833.33,
      "working part of the year with eoy overlap on semimonthly pay",
    ),
    (
      semiMonthly,
      "2025-01-01",
      "2025-12-15",
      "2025-12-14",
      "2025-12-15",
      None,
      Some(363),
      1,
      6.67,
      "working part of the year with final payment in next period/this year on semimonthly pay",
    ),
    (
      semiMonthly,
      "2025-01-01",
      "2025-12-15",
      "2025-12-14",
      "2025-12-16",
      None,
      Some(348),
      0,
      0,
      "working part of the year with final payment in next period/next year on semimonthly pay",
    ),
    (
      semiMonthly,
      "2025-01-01",
      "2025-10-15",
      "2025-09-30",
      "2025-10-02",
      None,
      Some(288),
      1,
      100,
      "working part of the year paid in arrears across month boundary on semimonthly pay",
    ),
    (
      semiMonthly,
      "2025-01-01",
      "2025-12-03",
      "2025-08-05",
      "2025-08-11",
      None,
      Some(339),
      8,
      786.67,
      "working part of the year with one payment in final month on semimonthly pay",
    ),
    (
      semiMonthly,
      "2025-01-01",
      "2025-06-30",
      "2025-05-31",
      "2025-05-31",
      None,
      Some(181),
      2,
      200,
      "handles end date edge case",
    ),
    (
      semiMonthly,
      "2025-01-01",
      "2025-12-30",
      "2025-10-31",
      "2025-10-31",
      None,
      Some(365),
      4,
      393.33,
      "working part of the year with two payments in final month on semimonthly pay",
    ),
    (
      semiMonthly,
      "2025-11-01",
      "2025-12-15",
      "2025-11-15",
      "2025-11-20",
      None,
      Some(349),
      2,
      200,
      "partial year, with a gap between payment and end date",
    ),
    (
      semiMonthly,
      "2025-01-01",
      "2025-12-31",
      "2025-10-23",
      "2025-10-23",
      None,
      Some(357),
      4,
      400,
      "bug 428",
    ),
    (
      semiMonthly,
      "2025-01-01",
      "2025-09-15",
      "2025-01-15",
      "2025-05-05",
      None,
      Some(242),
      15,
      1500,
      "semi-monthly handles lagging pay pay date where you are limited by pay dates",
    ),
    (
      semiMonthly,
      "2025-01-01",
      "2025-09-16",
      "2024-11-15",
      "2025-01-05",
      None,
      Some(273),
      21,
      2006.67,
      "semi-monthly handles lagging pay pay date from the previous year",
    ),
    // Monthly Pay Periods
    (
      monthly,
      "2025-01-01",
      "2025-12-31",
      "2025-08-05",
      "2025-08-15",
      None,
      Some(339),
      4,
      400,
      "working for example when they have three remaining monthly pay periods",
    ),
    (
      monthly,
      "2025-01-01",
      "2025-12-31",
      "2025-11-10",
      "2025-11-12",
      None,
      Some(344),
      1,
      100,
      "working for a portion of the year with eoy overlap on monthly pay",
    ),
    (
      monthly,
      "2025-01-01",
      "2025-06-20",
      "2025-06-20",
      "2025-06-21",
      None,
      Some(171),
      0,
      0,
      "ends in the same pay period on monthly pay",
    ),
    (
      monthly,
      "2025-01-01",
      "2025-12-31",
      "2025-06-25",
      "2025-07-02",
      None,
      Some(329),
      5,
      500,
      "pay date is in future on monthly pay, ends last day",
    ),
    (
      monthly,
      "2025-01-01",
      "2025-12-30",
      "2025-06-25",
      "2025-07-02",
      None,
      Some(329),
      5,
      500,
      "pay date is in future on monthly pay, ends prior to last day",
    ),
    (
      monthly,
      "2025-01-01",
      "2025-02-22",
      "2025-01-10",
      "2025-01-10",
      None,
      Some(69),
      2,
      140,
      "need another paycheck to cover lag on monthly pay",
    ),
    (
      monthly,
      "2025-01-01",
      "2025-02-09",
      "2025-01-10",
      "2025-01-10",
      None,
      Some(41),
      1,
      100,
      "doesn't need another paycheck to cover lag on monthly pay",
    ),
    (
      monthly,
      "2025-02-01",
      "2025-03-31",
      "2025-02-28",
      "2025-02-28",
      None,
      Some(90),
      1,
      100,
      "doesn't need another paycheck when end date day is after pay period end day",
    ),
    (
      monthly,
      "2025-02-01",
      "2025-03-31",
      "2025-02-28",
      "2025-03-05",
      None,
      Some(90),
      1,
      100,
      "doesn't need another paycheck when end date day is after pay period end day and paid after period end",
    ),
    (
      monthly,
      "2025-01-01",
      "2025-09-15",
      "2025-01-15",
      "2025-03-05",
      None,
      Some(258),
      8,
      800,
      "handles lagging pay pay date",
    ),
    (
      monthly,
      "2025-01-01",
      "2025-09-15",
      "2025-01-15",
      "2025-05-05",
      None,
      Some(227),
      7,
      700,
      "handles lagging pay pay date where you are limited by pay dates",
    ),
    (
      monthly,
      "2025-01-01",
      "2025-09-16",
      "2024-11-15",
      "2025-01-05",
      None,
      Some(288),
      11,
      1003.33,
      "handles lagging pay pay date from the previous year",
    ),
  )
  test(
    "test tentativeRemainingPayPeriods, remainingPayPeriodsInteger, restOfYearIncome and ordinalEffectiveEndDate",
  ) {
    forAll(dataTable) {
      (
          frequency,
          startDateValue,
          endDateValue,
          recentPayPeriodEndValue,
          recentPayDateValue,
          expectedTentativePayPeriods,
          expectedEffectiveEndDate,
          expectedTruePayPeriods,
          expectedRestOfYearIncome,
          description,
      ) =>
        When(description)
        var graph = makeGraphWith(
          factDictionary,
          jobs -> Collection(uuidVector),
          payFrequency -> frequency,
          startDate -> Day(startDateValue),
          endDate -> Day(endDateValue),
          recentPayPeriodEnd -> Day(recentPayPeriodEndValue),
          recentPayDate -> Day(recentPayDateValue),
          averagePayPerPayPeriod -> Dollar(100),
          isPastJob -> false,
          isFutureJob -> false,
          isCurrentJob -> true,
        )

        val tentativePayPeriods = graph.get(tentativeRemainingPayPeriods)
        val trueRemainingPayPeriods = graph.get(remainingPayPeriodsInteger)
        val royIncome = graph.get(restOfYearIncome)
        val effectiveLastDay = graph.get(ordinalEffectiveEndDate)

        if (expectedTentativePayPeriods.isEmpty)
          assert(tentativePayPeriods.value.isEmpty)
        else
          assert(tentativePayPeriods.value.contains(expectedTentativePayPeriods.value))

        if (expectedEffectiveEndDate.isEmpty)
          assert(effectiveLastDay.value.isEmpty)
        else
          assert(effectiveLastDay.value.contains(expectedEffectiveEndDate.value))

        assert(trueRemainingPayPeriods.value.contains(expectedTruePayPeriods))
        assert(royIncome.value.contains(expectedRestOfYearIncome))
    }
  }

  val pension1Id = "8679f611-851e-44e8-9a31-d4ced931d1ed"
  val pension2Id = "30bb164e-feed-40f9-b800-4178a98ae79a"
  val pensions = Path("/pensions")
  val pensionsCollection = Collection(Vector(java.util.UUID.fromString(pension1Id)))

  test("Verifying monthly pension pay period calculations") {
    val graph = makeGraphWith(
      factDictionary,
      pensions -> pensionsCollection,
      Path("/primaryFilerDateOfBirth") -> Day("1985-01-28"),
      Path("/primaryFilerIsBlind") -> false,
      Path("/primaryFilerIsClaimedOnAnotherReturn") -> false,
      Path(s"/pensions/#${pension1Id}/w4pLine2bi") -> Dollar("50000.00"),
      Path(s"/pensions/#${pension1Id}/averagePayPerPayPeriod") -> Dollar("5000"),
      Path(s"/pensions/#${pension1Id}/averageWithholdingPerPayPeriod") -> Dollar("1000"),
      Path(s"/pensions/#${pension1Id}/yearToDateWithholding") -> Dollar("7200"),
      Path(s"/pensions/#${pension1Id}/payFrequency") -> Enum("monthly", "/payFrequencyOptions"),
      Path(s"/pensions/#${pension1Id}/income") -> Dollar("60000"),
      Path(s"/pensions/#${pension1Id}/startDate") -> Day("2025-01-01"),
      Path(s"/pensions/#${pension1Id}/mostRecentPayDate") -> Day("2025-08-01"),
      Path(s"/pensions/#${pension1Id}/endDate") -> Day("2025-12-31"),

      // Derived overrides
      Path("/adjustmentsToIncome") -> Dollar("0"),
      Path("/totalOtherIncome") -> Dollar("0"),
      Path("/totalCredits") -> Dollar("0"),
    )

    assert(graph.get(Path(s"/pensions/#${pension1Id}/remainingPayDates")).value.contains(4))
  }

  test("Verifying monthly pension pay period calculations - recent pay date on end month") {
    val graph = makeGraphWith(
      factDictionary,
      pensions -> pensionsCollection,
      Path("/primaryFilerDateOfBirth") -> Day("1985-01-28"),
      Path("/primaryFilerIsBlind") -> false,
      Path("/primaryFilerIsClaimedOnAnotherReturn") -> false,
      Path(s"/pensions/#${pension1Id}/w4pLine2bi") -> Dollar("50000.00"),
      Path(s"/pensions/#${pension1Id}/averagePayPerPayPeriod") -> Dollar("5000"),
      Path(s"/pensions/#${pension1Id}/averageWithholdingPerPayPeriod") -> Dollar("1000"),
      Path(s"/pensions/#${pension1Id}/yearToDateWithholding") -> Dollar("7200"),
      Path(s"/pensions/#${pension1Id}/payFrequency") -> Enum("monthly", "/payFrequencyOptions"),
      Path(s"/pensions/#${pension1Id}/income") -> Dollar("60000"),
      Path(s"/pensions/#${pension1Id}/startDate") -> Day("2025-01-01"),
      Path(s"/pensions/#${pension1Id}/mostRecentPayDate") -> Day("2025-08-01"),
      Path(s"/pensions/#${pension1Id}/endDate") -> Day("2025-08-20"),

      // Derived overrides
      Path("/adjustmentsToIncome") -> Dollar("0"),
      Path("/totalOtherIncome") -> Dollar("0"),
      Path("/totalCredits") -> Dollar("0"),
    )

    assert(graph.get(Path(s"/pensions/#${pension1Id}/remainingPayDates")).value.contains(1))
  }

  test("Verifying monthly pension pay period calculations - recent pay date on end month, when end month is december") {
    val graph = makeGraphWith(
      factDictionary,
      pensions -> pensionsCollection,
      Path("/primaryFilerDateOfBirth") -> Day("1985-01-28"),
      Path("/primaryFilerIsBlind") -> false,
      Path("/primaryFilerIsClaimedOnAnotherReturn") -> false,
      Path(s"/pensions/#${pension1Id}/w4pLine2bi") -> Dollar("50000.00"),
      Path(s"/pensions/#${pension1Id}/averagePayPerPayPeriod") -> Dollar("5000"),
      Path(s"/pensions/#${pension1Id}/averageWithholdingPerPayPeriod") -> Dollar("1000"),
      Path(s"/pensions/#${pension1Id}/yearToDateWithholding") -> Dollar("7200"),
      Path(s"/pensions/#${pension1Id}/payFrequency") -> Enum("monthly", "/payFrequencyOptions"),
      Path(s"/pensions/#${pension1Id}/income") -> Dollar("60000"),
      Path(s"/pensions/#${pension1Id}/startDate") -> Day("2025-01-01"),
      Path(s"/pensions/#${pension1Id}/mostRecentPayDate") -> Day("2025-12-01"),
      Path(s"/pensions/#${pension1Id}/endDate") -> Day("2025-12-31"),

      // Derived overrides
      Path("/adjustmentsToIncome") -> Dollar("0"),
      Path("/totalOtherIncome") -> Dollar("0"),
      Path("/totalCredits") -> Dollar("0"),
    )

    assert(graph.get(Path(s"/pensions/#${pension1Id}/remainingPayDates")).value.contains(0))
  }

  test("Verifying semimonthly pension pay period calculations") {
    val graph = makeGraphWith(
      factDictionary,
      pensions -> pensionsCollection,
      Path("/primaryFilerDateOfBirth") -> Day("1985-01-28"),
      Path("/primaryFilerIsBlind") -> false,
      Path("/primaryFilerIsClaimedOnAnotherReturn") -> false,
      Path(s"/pensions/#${pension1Id}/w4pLine2bi") -> Dollar("50000.00"),
      Path(s"/pensions/#${pension1Id}/averagePayPerPayPeriod") -> Dollar("5000"),
      Path(s"/pensions/#${pension1Id}/averageWithholdingPerPayPeriod") -> Dollar("1000"),
      Path(s"/pensions/#${pension1Id}/yearToDateWithholding") -> Dollar("7200"),
      Path(s"/pensions/#${pension1Id}/payFrequency") -> Enum("semiMonthly", "/payFrequencyOptions"),
      Path(s"/pensions/#${pension1Id}/income") -> Dollar("60000"),
      Path(s"/pensions/#${pension1Id}/startDate") -> Day("2025-01-01"),
      Path(s"/pensions/#${pension1Id}/mostRecentPayDate") -> Day("2025-08-11"),
      Path(s"/pensions/#${pension1Id}/endDate") -> Day("2025-12-31"),

      // Derived overrides
      Path("/adjustmentsToIncome") -> Dollar("0"),
      Path("/totalOtherIncome") -> Dollar("0"),
      Path("/totalCredits") -> Dollar("0"),
    )

    assert(graph.get(Path(s"/pensions/#${pension1Id}/remainingPayDates")).value.contains(9))
  }

  test("Verifying semimonthly pension pay period calculations - Recent and End are same month") {
    val graph = makeGraphWith(
      factDictionary,
      pensions -> pensionsCollection,
      Path("/primaryFilerDateOfBirth") -> Day("1985-01-28"),
      Path("/primaryFilerIsBlind") -> false,
      Path("/primaryFilerIsClaimedOnAnotherReturn") -> false,
      Path(s"/pensions/#${pension1Id}/w4pLine2bi") -> Dollar("50000.00"),
      Path(s"/pensions/#${pension1Id}/averagePayPerPayPeriod") -> Dollar("5000"),
      Path(s"/pensions/#${pension1Id}/averageWithholdingPerPayPeriod") -> Dollar("1000"),
      Path(s"/pensions/#${pension1Id}/yearToDateWithholding") -> Dollar("7200"),
      Path(s"/pensions/#${pension1Id}/payFrequency") -> Enum("semiMonthly", "/payFrequencyOptions"),
      Path(s"/pensions/#${pension1Id}/income") -> Dollar("60000"),
      Path(s"/pensions/#${pension1Id}/startDate") -> Day("2025-01-01"),
      Path(s"/pensions/#${pension1Id}/mostRecentPayDate") -> Day("2025-08-11"),
      Path(s"/pensions/#${pension1Id}/endDate") -> Day("2025-08-31"),

      // Derived overrides
      Path("/adjustmentsToIncome") -> Dollar("0"),
      Path("/totalOtherIncome") -> Dollar("0"),
      Path("/totalCredits") -> Dollar("0"),
    )

    assert(graph.get(Path(s"/pensions/#${pension1Id}/remainingPayDates")).value.contains(1))
  }

  test("Verifying semimonthly pension pay period calculations -- Recent and End are same month, December") {
    val graph = makeGraphWith(
      factDictionary,
      pensions -> pensionsCollection,
      Path(s"/pensions/#${pension1Id}/payFrequency") -> Enum("semiMonthly", "/payFrequencyOptions"),
      Path(s"/pensions/#${pension1Id}/startDate") -> Day("2025-01-01"),
      Path(s"/pensions/#${pension1Id}/mostRecentPayDate") -> Day("2025-12-11"),
      Path(s"/pensions/#${pension1Id}/endDate") -> Day("2025-12-31"),
    )

    assert(graph.get(Path(s"/pensions/#${pension1Id}/remainingPayDates")).value.contains(1))
  }

  val pensionDataTable = Table(
    (
      "frequencyValue",
      "startDateValue",
      "endDateValue",
      "recentPayDateValue",
      "expected true pay periods",
      "description",
    ),
    // Weekly Pay Periods
    (
      weekly,
      "2025-01-01",
      "2025-10-25",
      "2025-08-01",
      13,
      "working for a portion of the year with no eoy overlap on weekly pay",
    ),
    (
      weekly,
      "2025-02-01",
      "2025-12-31",
      "2025-02-18",
      45,
      "working for a portion of the year with eoy overlap on weekly pay",
    ),
    (
      weekly,
      "2025-01-01",
      "2025-12-31",
      "2025-08-01",
      21,
      "working for the whole year with eoy overlap on weekly pay",
    ),
    // Biweekly Pay Periods
    (
      biWeekly,
      "2025-01-01",
      "2025-10-25",
      "2025-08-01",
      7,
      "working for a portion of the year with no eoy overlap on biweekly pay",
    ),
    (
      biWeekly,
      "2025-02-01",
      "2025-12-31",
      "2025-02-14",
      22,
      "working for a portion of the year with eoy overlap on biweekly pay",
    ),
    (
      biWeekly,
      "2025-01-01",
      "2025-12-31",
      "2025-08-01",
      10,
      "working for the whole year with eoy overlap on biweekly pay",
    ),
  )

  test(
    "testing remainingPayDates for pensions",
  ) {
    forAll(pensionDataTable) {
      (
          frequency,
          startDateValue,
          endDateValue,
          recentPayDateValue,
          expectedTruePayPeriods,
          description,
      ) =>
        When(description)
        var graph = makeGraphWith(
          factDictionary,
          pensions -> pensionsCollection,
          Path(s"/pensions/#${pension1Id}/payFrequency") -> frequency,
          Path(s"/pensions/#${pension1Id}/startDate") -> Day(startDateValue),
          Path(s"/pensions/#${pension1Id}/mostRecentPayDate") -> Day(recentPayDateValue),
          Path(s"/pensions/#${pension1Id}/endDate") -> Day(endDateValue),
        )

        assert(graph.get(Path(s"/pensions/#${pension1Id}/remainingPayDates")).value.contains(expectedTruePayPeriods))
    }
  }

  val futureJobDataTable = Table(
    ("frequency", "startDateValue", "endDateValue", "expectedFractional", "description"),
    // Weekly pay frequency
    (weekly, "2025-10-01", "2025-12-05", Rational(2, 7), "Weekly pay freq"),
    // Biweekly pay frequency
    (biWeekly, "2025-10-01", "2025-12-05", Rational(9, 14), "Biweekly pay freq"),
    // Monthly pay frequency
    (monthly, "2025-10-01", "2025-12-05", Rational(0, 1), "Monthly end date is in December"),
    (monthly, "2025-10-31", "2025-11-20", Rational(65, 93), "Monthly at the margins"),
    // Semi-monthly pay frequency
    (semiMonthly, "2025-10-01", "2025-12-05", Rational(1, 3), "Semimonthly end date is in December"),
    (semiMonthly, "2025-10-14", "2025-11-15", Rational(2, 15), "Semimonthly at both margins"),
    (semiMonthly, "2025-10-15", "2025-11-16", Rational(2, 15), "Semimonthly at end date margin"),
  )
  test(
    "test fractionalRemainingPayPeriods for future jobs",
  ) {
    forAll(futureJobDataTable) {
      (
          frequency,
          startDateValue,
          endDateValue,
          expectedFractional,
          description,
      ) =>
        When(description)
        var graph = makeGraphWith(
          factDictionary,
          jobs -> Collection(uuidVector),
          payFrequency -> frequency,
          startDate -> Day(startDateValue),
          endDate -> Day(endDateValue),
          isPastJob -> false,
          isCurrentJob -> false,
          isFutureJob -> true,
        )

        val fractRemainingPayPeriods = graph.get(Path(s"/jobs/#${dummyUUID}/partialPayPeriods"))
        assert(fractRemainingPayPeriods.value.contains(expectedFractional))
    }
  }
}

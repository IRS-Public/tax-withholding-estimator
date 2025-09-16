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
  val remainingPayPeriods = Path(s"/jobs/#$dummyUUID/remainingPayPeriods")
  val fractionalRemainingPayPeriods = Path(s"/jobs/#$dummyUUID/fractionalRemainingPayPeriods")
  val ordinalEffectiveEndDate = Path(s"/jobs/#$dummyUUID/ordinalEffectiveEndDate")

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
      "expected true pay periods",
      "expected fractional pay periods",
      "expected effective end date",
      "description",
    ),
    // Weekly Pay Periods
    (
      weekly,
      "2025-01-01",
      "2025-10-25",
      "2025-07-26",
      "2025-08-01",
      13,
      13,
      Rational(91, 7),
      298,
      "working for a portion of the year with no eoy overlap on weekly pay",
    ),
    (
      weekly,
      "2025-02-01",
      "2025-12-31",
      "2025-02-14",
      "2025-02-18",
      46,
      45,
      Rational(315, 7),
      360,
      "working for a portion of the year with eoy overlap on weekly pay",
    ),
    (
      weekly,
      "2025-01-01",
      "2025-12-31",
      "2025-07-26",
      "2025-08-01",
      23,
      21,
      Rational(147, 7),
      354,
      "working for the whole year with eoy overlap on weekly pay",
    ),
    // Biweekly Pay Periods
    (
      biWeekly,
      "2025-01-01",
      "2025-10-25",
      "2025-07-26",
      "2025-08-01",
      7,
      7,
      Rational(91, 14),
      298,
      "working for a portion of the year with no eoy overlap on biweekly pay",
    ),
    (
      biWeekly,
      "2025-02-01",
      "2025-12-31",
      "2025-02-10",
      "2025-02-14",
      24,
      22,
      Rational(308, 14),
      349,
      "working for a portion of the year with eoy overlap on biweekly pay",
    ),
    (
      biWeekly,
      "2025-01-01",
      "2025-12-31",
      "2025-07-26",
      "2025-08-01",
      12,
      10,
      Rational(140, 14),
      347,
      "working for the whole year with eoy overlap on biweekly pay",
    ),
    // Semi Monthly Pay Periods
    (
      semiMonthly,
      "2025-02-01",
      "2025-12-31",
      "2025-02-15",
      "2025-02-19",
      20,
      20,
      Rational(300, 15),
      365,
      "working for a portion of the year with no eoy overlap on semimonthly pay",
    ),
    (
      semiMonthly,
      "2025-02-03",
      "2025-12-31",
      "2025-03-18",
      "2025-03-25",
      18,
      18,
      Rational(282, 15),
      365,
      "working for a portion of the year with eoy overlap on semimonthly pay",
    ),
    (
      semiMonthly,
      "2025-01-01",
      "2025-12-31",
      "2025-08-05",
      "2025-08-11",
      9,
      9,
      Rational(145, 15),
      365,
      "working for the whole year with eoy overlap on semimonthly pay",
    ),
    (
      semiMonthly,
      "2025-01-01",
      "2025-12-10",
      "2025-08-05",
      "2025-08-11",
      9,
      9,
      Rational(125, 15),
      344,
      "working part of the year with eoy overlap on semimonthly pay",
    ),
    (
      semiMonthly,
      "2025-01-01",
      "2025-12-15",
      "2025-12-14",
      "2025-12-15",
      1,
      1,
      Rational(1, 15),
      349,
      "working part of the year with final payment in next period/this year on semimonthly pay",
    ),
    (
      semiMonthly,
      "2025-01-01",
      "2025-12-15",
      "2025-12-14",
      "2025-12-16",
      0,
      0,
      Rational(0, 15),
      349,
      "working part of the year with final payment in next period/next year on semimonthly pay",
    ),
    (
      semiMonthly,
      "2025-01-01",
      "2025-10-15",
      "2025-09-30",
      "2025-10-02",
      1,
      1,
      Rational(15, 15),
      288,
      "working part of the year paid in arrears across month boundary on semimonthly pay",
    ),
    // Monthly Pay Periods
    (
      monthly,
      "2025-01-01",
      "2025-12-31",
      "2025-08-05",
      "2025-08-15",
      5,
      4,
      Rational(120, 30),
      337,
      "working for example when they have three remaining monthly pay periods",
    ),
    (
      monthly,
      "2025-01-01",
      "2025-12-31",
      "2025-11-10",
      "2025-11-12",
      2,
      1,
      Rational(30, 30),
      344,
      "working for a portion of the year with eoy overlap on monthly pay",
    ),
  )

  test(
    "test tentativeRemainingPayPeriods, remainingPayPeriods, fractionalRemainingPayPeriods, and ordinalEffectiveEndDate",
  ) {
    forAll(dataTable) {
      (
          frequency,
          startDateValue,
          endDateValue,
          recentPayPeriodEndValue,
          recentPayDateValue,
          expectedTentativePayPeriods,
          expectedTruePayPeriods,
          expectedFractionalPayPeriods,
          expectedEffectiveEndDate,
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
        )

        val tentativePayPeriods = graph.get(tentativeRemainingPayPeriods)
        val trueRemainingPayPeriods = graph.get(remainingPayPeriods)
        val fractRemainingPayPeriods = graph.get(fractionalRemainingPayPeriods)
        val effectiveLastDay = graph.get(ordinalEffectiveEndDate)

        assert(tentativePayPeriods.value.contains(expectedTentativePayPeriods))
        assert(trueRemainingPayPeriods.value.contains(expectedTruePayPeriods))
        assert(fractRemainingPayPeriods.value.contains(expectedFractionalPayPeriods))
        assert(effectiveLastDay.value.contains(expectedEffectiveEndDate))

    }
  }
}

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
      "expected effective end date",
      "expected true pay periods",
      "expected fractional pay periods",
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
      Rational(91, 7),
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
      Rational(315, 7),
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
      Rational(147, 7),
      "working for the whole year with eoy overlap on weekly pay",
    ),
    // Biweekly Pay Periods
    (
      biWeekly,
      "2025-01-01",
      "2025-10-25",
      "2025-07-26",
      "2025-08-01",
      Some(7),
      Some(298),
      7,
      Rational(91, 14),
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
      Rational(308, 14),
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
      Rational(140, 14),
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
      None,
      20,
      Rational(300, 15),
      "working for a portion of the year with no eoy overlap on semimonthly pay",
    ),
    (
      semiMonthly,
      "2025-02-03",
      "2025-12-31",
      "2025-03-18",
      "2025-03-25",
      None,
      None,
      18,
      Rational(282, 15),
      "working for a portion of the year with eoy overlap on semimonthly pay",
    ),
    (
      semiMonthly,
      "2025-01-01",
      "2025-12-31",
      "2025-08-05",
      "2025-08-11",
      None,
      None,
      9,
      Rational(145, 15),
      "working for the whole year with eoy overlap on semimonthly pay",
    ),
    (
      semiMonthly,
      "2025-01-01",
      "2025-12-10",
      "2025-08-05",
      "2025-08-11",
      None,
      None,
      9,
      Rational(125, 15),
      "working part of the year with eoy overlap on semimonthly pay",
    ),
    (
      semiMonthly,
      "2025-01-01",
      "2025-12-15",
      "2025-12-14",
      "2025-12-15",
      None,
      None,
      1,
      Rational(1, 15),
      "working part of the year with final payment in next period/this year on semimonthly pay",
    ),
    (
      semiMonthly,
      "2025-01-01",
      "2025-12-15",
      "2025-12-14",
      "2025-12-16",
      None,
      None,
      0,
      Rational(0, 15),
      "working part of the year with final payment in next period/next year on semimonthly pay",
    ),
    (
      semiMonthly,
      "2025-01-01",
      "2025-10-15",
      "2025-09-30",
      "2025-10-02",
      None,
      None,
      1,
      Rational(15, 15),
      "working part of the year paid in arrears across month boundary on semimonthly pay",
    ),
    (
      semiMonthly,
      "2025-01-01",
      "2025-12-03",
      "2025-08-05",
      "2025-08-11",
      None,
      None,
      8,
      Rational(103, 15),
      "working part of the year with one payment in final month on semimonthly pay",
    ),
    (
      semiMonthly,
      "2025-01-01",
      "2025-12-30",
      "2025-10-31",
      "2025-10-31",
      None,
      None,
      4,
      Rational(59, 15),
      "working part of the year with two payments in final month on semimonthly pay",
    ),
    // See bug #428 for discussion of this test data. The 69/15 value for /fractionalRemainingPayPeriods that we called
    // correct is based on the unwarranted assumptions that the user will be paid Dec 31, for work thru Dec 31.
    // We can activate this test once we understand what assumptions to make, and add that logic.
    // (Currently we compute 67/15.)
    // (
    //   semiMonthly,
    //   "2025-01-01",
    //   "2025-12-31",
    //   "2025-10-23",
    //   "2025-10-23",
    //   None,
    //   None,
    //   4,
    //   Rational(69, 15),
    //   "bug 428",
    // ),
    // Monthly Pay Periods
    (
      monthly,
      "2025-01-01",
      "2025-12-31",
      "2025-08-05",
      "2025-08-15",
      None,
      Some(337),
      4,
      Rational(120, 30),
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
      Rational(30, 30),
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
      Rational(0, 30),
      "ends in the same pay period on monthly pay",
    ),
    (
      monthly,
      "2025-01-01",
      "2025-12-31",
      "2025-06-25",
      "2025-07-02",
      None,
      Some(326),
      5,
      Rational(150, 30),
      "pay date is in future on monthly pay",
    ),
    (
      monthly,
      "2025-01-01",
      "2025-02-22",
      "2025-01-10",
      "2025-01-10",
      None,
      Some(53),
      2,
      Rational(43, 30),
      "need another paycheck to cover lag on monthly pay",
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
          expectedEffectiveEndDate,
          expectedTruePayPeriods,
          expectedFractionalPayPeriods,
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

        if (expectedTentativePayPeriods.isEmpty)
          assert(tentativePayPeriods.value.isEmpty)
        else
          assert(tentativePayPeriods.value.contains(expectedTentativePayPeriods.value))

        if (expectedEffectiveEndDate.isEmpty)
          assert(effectiveLastDay.value.isEmpty)
        else
          assert(effectiveLastDay.value.contains(expectedEffectiveEndDate.value))

        assert(trueRemainingPayPeriods.value.contains(expectedTruePayPeriods))
        assert(fractRemainingPayPeriods.value.contains(expectedFractionalPayPeriods))
    }
  }
}

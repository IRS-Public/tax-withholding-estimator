package gov.irs.twe.factDictionary

import gov.irs.factgraph.persisters.InMemoryPersister
import gov.irs.factgraph.types.intValue
import gov.irs.factgraph.types.Collection
import gov.irs.factgraph.types.Day
import gov.irs.factgraph.types.Dollar
import gov.irs.factgraph.types.Enum
import gov.irs.factgraph.types.Rational
import gov.irs.factgraph.FactDictionaryForTests
import gov.irs.factgraph.Graph
import gov.irs.factgraph.Path
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.Ignore

// Instead of updating this unit test, we should wait for other approved scenarios to create new tests for TY26, instead of modifying the below just to match our math
@Ignore
class TY25TaxCalculationsSpec extends AnyFunSuite with TableDrivenPropertyChecks {
  val factDictionary = setupFactDictionary()

  val filingStatus = Path("/filingStatus")
  val taxableIncome = Path("/taxableIncome")
  val roundedTaxableIncome = Path("/roundedTaxableIncome")
  val tentativeTaxFromTaxableIncome = Path("/tentativeTaxFromTaxableIncome")

  val single = Enum("single", "/filingStatusOptions")

  val dataTable = Table(
    ("status", "taxableIncome", "expectedRoundedIncome", "expectedTax"),
    // Not over $11,925: 10% of the taxable income
    (single, "0.0", "0.0", "0.0"),
    (single, "4.49", "2.5", "0.0"),
    (single, "5.0", "10.0", "1.0"),
    (single, "14.49", "10.0", "1.0"),
    (single, "15.0", "20.0", "2.0"),
    (single, "24.49", "20.0", "2.0"),
    (single, "25.0", "37.5", "4.0"),
    (single, "37.5", "37.5", "4.0"),
    (single, "49.49", "37.5", "4.0"),
    (single, "2975.0", "2987.5", "299.0"),
    (single, "2988.0", "2987.5", "299.0"),
    (single, "2999.49", "2987.5", "299.0"),
    (single, "3000.0", "3025.0", "303.0"),
    (single, "3025.0", "3025.0", "303.0"),
    (single, "3049.49", "3025.0", "303.0"),

    // Over $11,925 but not over $48,475: $1,192.50 plus 12% of the excess over $11,925
    (single, "20000.0", "20025.0", "2165.0"),

    // Over $48,475 but not over $103,350: $5,578.50 plus 22% of the excess over $48,475
    (single, "57050.0", "57075.0", "7471.0"),
    (single, "57075.0", "57075.0", "7471.0"),
    (single, "57099.49", "57075.0", "7471.0"),

    // Over $103,350 but not over $197,300: $17,651 plus 24% of the excess over $103,350
    (single, "105351.0", "105351.0", "18131.0"),

    // Over $197,300 but not over $250,525: $40,199 plus 32% of the excess over $197,300
    (single, "200000.0", "200000.0", "41063.0"),

    // Over $250,525 but not over $626,350: $57,231 plus 35% of the excess over $250,525
    (single, "260000.0", "260000.0", "60547.0"),

    // Over $626,350: $188,769.75 plus 37% of the excess over $626,350
    (single, "700000.0", "700000.0", "216020.0"),
  )

  test("test roundedTaxableIncome and tentativeTaxFromTaxableIncome") {
    forAll(dataTable) { (status, income, expectedRoundedIncome, expectedTax) =>
      val graph = makeGraphWith(
        factDictionary,
        filingStatus -> status,
        taxableIncome -> Dollar(income),
      )

      val roundedIncome = graph.get(roundedTaxableIncome)
      assert(roundedIncome.value.contains(Dollar(expectedRoundedIncome)))
      val taxAmount = graph.get(tentativeTaxFromTaxableIncome)
      assert(taxAmount.value.contains(Dollar(expectedTax)))

    }
  }

  val job1Id = "517466a2-3587-4c10-8b29-9b5a6d48d03d"
  val job2Id = "6862bbc2-d3b5-4dda-a747-44a8f882e015"
  val jobs = Path("/jobs")
  val jobsCollection = Collection(Vector(java.util.UUID.fromString(job1Id), java.util.UUID.fromString(job2Id)))

  val pension1Id = "8679f611-851e-44e8-9a31-d4ced931d1ed"
  val pension2Id = "30bb164e-feed-40f9-b800-4178a98ae79a"
  val pensions = Path("/pensions")
  val pensionsCollection = Collection(Vector(java.util.UUID.fromString(pension1Id)))
  val twoPensionsCollection = Collection(
    Vector(java.util.UUID.fromString(pension1Id), java.util.UUID.fromString(pension2Id)),
  )

  test("Form W4 Line 4a allocation") {
    val graph = makeGraphWith(
      factDictionary,
      jobs -> jobsCollection,
      Path(s"/jobs/#${job1Id}/writableStartDate") -> Day("2025-01-01"),
      Path(s"/jobs/#${job1Id}/writableEndDate") -> Day("2025-12-31"),
      Path(s"/jobs/#${job2Id}/writableStartDate") -> Day("2025-02-01"),
      Path(s"/jobs/#${job2Id}/writableEndDate") -> Day("2025-12-31"),
      // Derived overrides
      Path(s"/jobs/#${job1Id}/annualizedIncome") -> Dollar(1000),
      Path(s"/jobs/#${job2Id}/annualizedIncome") -> Dollar(2000),
      Path(s"/jobs/#${job1Id}/restOfYearStandardWithholding") -> Dollar(4000),
      Path(s"/jobs/#${job2Id}/restOfYearStandardWithholding") -> Dollar(5000),
      Path("/w4Line4NetChange") -> Dollar(100), // net change is positive: use Line 4a
    )

    // Two jobs at year end: full amount goes to year end job with highest income proportion.
    assert(graph.get(Path("/annualizedIncomeFromYearEndJobs")).value.contains(Dollar(3000)))
    assert(graph.get(Path(s"/yearEndJobs/#${job1Id}/proportionOfYearEndJobsIncome")).value.contains(BigDecimal(0.33)))
    assert(graph.get(Path(s"/yearEndJobs/#${job2Id}/proportionOfYearEndJobsIncome")).value.contains(BigDecimal(0.67)))
    assert(graph.get(Path(s"/yearEndJobs/#${job1Id}/w4Line4a")).value.contains(Dollar(0)))
    assert(graph.get(Path(s"/yearEndJobs/#${job2Id}/w4Line4a")).value.contains(Dollar(100)))

    // One job at year end and one ending earlier: full amount goes to year end job with highest income proportion.
    graph.set(Path(s"/jobs/#${job2Id}/writableEndDate"), Day("2025-12-30"))
    assert(graph.get(Path("/annualizedIncomeFromYearEndJobs")).value.contains(Dollar(1000)))
    assert(graph.get(Path(s"/yearEndJobs/#${job1Id}/proportionOfYearEndJobsIncome")).value.contains(BigDecimal(1.00)))
    assert(graph.get(Path(s"/yearEndJobs/#${job1Id}/w4Line4a")).value.contains(Dollar(100)))
    assert(graph.get(Path(s"/jobs/#${job2Id}/w4Line4a")).value.contains(Dollar(0)))

    // No jobs at year end: full amount goes to job with highest proportion of rest of year standard withholding.
    graph.set(Path(s"/jobs/#${job1Id}/writableEndDate"), Day("2025-12-30"))
    assert(graph.get(Path("/annualizedIncomeFromYearEndJobs")).value.contains(Dollar(0)))
    assert(graph.get(Path("/totalRestOfYearStandardWithholding")).value.contains(Dollar(9000)))
    assert(
      graph.get(Path(s"/jobs/#${job1Id}/proportionOfRestOfYearStandardWithholding")).value.contains(BigDecimal(0.44)),
    )
    assert(
      graph.get(Path(s"/jobs/#${job2Id}/proportionOfRestOfYearStandardWithholding")).value.contains(BigDecimal(0.56)),
    )
    assert(graph.get(Path(s"/jobs/#${job1Id}/w4Line4a")).value.contains(Dollar(0)))
    assert(graph.get(Path(s"/jobs/#${job2Id}/w4Line4a")).value.contains(Dollar(100)))
  }

  test("Form W4 Line 4b allocation") {
    val graph = makeGraphWith(
      factDictionary,
      jobs -> jobsCollection,
      Path(s"/jobs/#${job1Id}/writableStartDate") -> Day("2025-01-01"),
      Path(s"/jobs/#${job1Id}/writableEndDate") -> Day("2025-12-31"),
      Path(s"/jobs/#${job2Id}/writableStartDate") -> Day("2025-02-01"),
      Path(s"/jobs/#${job2Id}/writableEndDate") -> Day("2025-12-31"),
      // Derived overrides
      Path(s"/jobs/#${job1Id}/annualizedIncome") -> Dollar(1000),
      Path(s"/jobs/#${job2Id}/annualizedIncome") -> Dollar(2000),
      Path(s"/jobs/#${job1Id}/restOfYearStandardWithholding") -> Dollar(4000),
      Path(s"/jobs/#${job2Id}/restOfYearStandardWithholding") -> Dollar(5000),
      Path("/w4Line4NetChange") -> Dollar(-100), // net change is negative: use Line 4b
    )

    // Two jobs at year end: allocate amount in proportion to income.
    assert(graph.get(Path("/annualizedIncomeFromYearEndJobs")).value.contains(Dollar(3000)))
    assert(graph.get(Path(s"/yearEndJobs/#${job1Id}/proportionOfYearEndJobsIncome")).value.contains(BigDecimal(0.33)))
    assert(graph.get(Path(s"/yearEndJobs/#${job2Id}/proportionOfYearEndJobsIncome")).value.contains(BigDecimal(0.67)))
    assert(graph.get(Path(s"/yearEndJobs/#${job1Id}/w4Line4b")).value.contains(Dollar(33)))
    assert(graph.get(Path(s"/yearEndJobs/#${job2Id}/w4Line4b")).value.contains(Dollar(67)))

    // One job at year end and one ending earlier: allocate amount in proportion to income.
    graph.set(Path(s"/jobs/#${job2Id}/writableEndDate"), Day("2025-12-30"))
    assert(graph.get(Path("/annualizedIncomeFromYearEndJobs")).value.contains(Dollar(1000)))
    assert(graph.get(Path(s"/yearEndJobs/#${job1Id}/proportionOfYearEndJobsIncome")).value.contains(BigDecimal(1.00)))
    assert(graph.get(Path(s"/yearEndJobs/#${job1Id}/w4Line4b")).value.contains(Dollar(100)))
    assert(graph.get(Path(s"/jobs/#${job2Id}/w4Line4b")).value.contains(Dollar(0)))

    // No jobs at year end: allocate amount in proportion to rest of year standard withholding.
    graph.set(Path(s"/jobs/#${job1Id}/writableEndDate"), Day("2025-12-30"))
    assert(graph.get(Path("/annualizedIncomeFromYearEndJobs")).value.contains(Dollar(0)))
    assert(graph.get(Path("/totalRestOfYearStandardWithholding")).value.contains(Dollar(9000)))
    assert(
      graph.get(Path(s"/jobs/#${job1Id}/proportionOfRestOfYearStandardWithholding")).value.contains(BigDecimal(0.44)),
    )
    assert(
      graph.get(Path(s"/jobs/#${job2Id}/proportionOfRestOfYearStandardWithholding")).value.contains(BigDecimal(0.56)),
    )
    assert(graph.get(Path(s"/jobs/#${job1Id}/w4Line4b")).value.contains(Dollar(44)))
    assert(graph.get(Path(s"/jobs/#${job2Id}/w4Line4b")).value.contains(Dollar(56)))
  }

  // These tests are based on the "2197 Scenarios checks" worksheet of the "TWESprint1_2025_UAT_WHC2197_2200" spreadsheet.
  test("2197 Scenarios spreadsheet Column C") {
    val graph = makeGraphWith(
      factDictionary,
      filingStatus -> single,
      Path("/primaryFilerAge65OrOlder") -> false,
      Path("/primaryFilerIsBlind") -> false,
      Path("/primaryFilerIsClaimedOnAnotherReturn") -> false,
      jobs -> jobsCollection,
      Path(s"/jobs/#${job1Id}/isAllYear") -> false,
      Path(s"/jobs/#${job1Id}/writableStartDate") -> Day("2025-01-01"),
      Path(s"/jobs/#${job1Id}/writableEndDate") -> Day("2025-10-15"),
      Path(s"/jobs/#${job1Id}/payFrequency") -> Enum("monthly", "/payFrequencyOptions"),
      Path(s"/jobs/#${job1Id}/mostRecentPayPeriodEnd") -> Day("2025-09-30"),
      Path(s"/jobs/#${job1Id}/mostRecentPayDate") -> Day("2025-10-05"),
      Path(s"/jobs/#${job1Id}/averagePayPerPayPeriod") -> Dollar("4000"),
      Path(s"/jobs/#${job1Id}/yearToDateIncome") -> Dollar("40000"),
      Path(s"/jobs/#${job1Id}/averageWithholdingPerPayPeriod") -> Dollar("200"),
      Path(s"/jobs/#${job1Id}/yearToDateWithholding") -> Dollar("2000"),
      Path(s"/jobs/#${job1Id}/totalBonusReceived") -> Dollar("0"),
      Path(s"/jobs/#${job2Id}/isAllYear") -> true,
      Path(s"/jobs/#${job2Id}/payFrequency") -> Enum("monthly", "/payFrequencyOptions"),
      Path(s"/jobs/#${job2Id}/mostRecentPayPeriodEnd") -> Day("2025-09-30"),
      Path(s"/jobs/#${job2Id}/mostRecentPayDate") -> Day("2025-09-30"),
      Path(s"/jobs/#${job2Id}/averagePayPerPayPeriod") -> Dollar("2000"),
      Path(s"/jobs/#${job2Id}/yearToDateIncome") -> Dollar("18000"),
      Path(s"/jobs/#${job2Id}/averageWithholdingPerPayPeriod") -> Dollar("100"),
      Path(s"/jobs/#${job2Id}/yearToDateWithholding") -> Dollar("900"),
      Path(s"/jobs/#${job2Id}/totalBonusReceived") -> Dollar("0"),
      Path("/totalCtcAndOdc") -> Dollar("2000"),
      Path("/totalEstimatedTaxesPaid") -> Dollar("0"),
      Path("/netSelfEmploymentIncomeTotal") -> Dollar("0"),
      // Derived overrides
      Path(s"/jobs/#${job1Id}/preTaxDeductions") -> Dollar("0"),
      Path(s"/jobs/#${job2Id}/preTaxDeductions") -> Dollar("0"),
      Path("/adjustmentsToIncome") -> Dollar("0"),
      Path("/otherIncomeTotal") -> Dollar("0"),
      Path("/nonItemizerCharitableContributionDeductionAmount") -> Dollar("0"),
      Path(s"/jobs/#${job1Id}/isPastJob") -> false,
      Path(s"/jobs/#${job1Id}/isCurrentJob") -> true,
      Path(s"/jobs/#${job1Id}/isFutureJob") -> false,
      Path(s"/jobs/#${job2Id}/isPastJob") -> false,
      Path(s"/jobs/#${job2Id}/isCurrentJob") -> true,
      Path(s"/jobs/#${job2Id}/isFutureJob") -> false,
    )

    assert(graph.get(Path(s"/jobs/#${job1Id}/income")).value.contains(Dollar("42000")))
    assert(graph.get(Path(s"/jobs/#${job2Id}/income")).value.contains(Dollar("24000")))

    assert(graph.get(Path(s"/jobs/#${job1Id}/endOfYearProjectedWithholding")).value.contains(Dollar("2000")))
    assert(graph.get(Path(s"/jobs/#${job2Id}/endOfYearProjectedWithholding")).value.contains(Dollar("1200")))

    // In the spreadsheet this is actually 310.13 but since we use banker's rounding we round 310.125 down
    assert(graph.get(Path(s"/jobs/#${job1Id}/tentativeWithholdingAmount")).value.contains(Dollar("310.12")))
    assert(graph.get(Path(s"/jobs/#${job2Id}/tentativeWithholdingAmount")).value.contains(Dollar("75")))

    assert(graph.get(Path("/totalEndOfYearProjectedWithholding")).value.contains(Dollar("3200")))
    assert(graph.get(Path("/agi")).value.contains(Dollar("66000")))

    if (graph.get(Path("/usePreOb3StandardDeduction")).value.contains(true)) {
      // These are the original values from the QA spreadsheet.
      assert(graph.get(Path("/tentativeTaxFromTaxableIncome")).value.contains(Dollar("6140")))
      assert(graph.get(Path("/totalOwed")).value.contains(Dollar("4140")))
      assert(graph.get(Path("/withholdingGap")).value.contains(Dollar("940")))
      assert(graph.get(Path("/jobSelectedForExtraWithholding/w4Line4c")).value.contains(Dollar("338")))
    } else {
      // The following values now differ from the QA spreadsheet due to standard deduction changes.
      assert(graph.get(Path("/tentativeTaxFromTaxableIncome")).value.contains(Dollar("5975")))
      assert(graph.get(Path("/totalOwed")).value.contains(Dollar("3975")))
      assert(graph.get(Path("/withholdingGap")).value.contains(Dollar("775")))
      assert(graph.get(Path("/jobSelectedForExtraWithholding/w4Line4c")).value.contains(Dollar("232")))
    }
  }

  test("2197 Scenarios spreadsheet Column D") {
    val graph = makeGraphWith(
      factDictionary,
      filingStatus -> Enum("marriedFilingJointly", "/filingStatusOptions"),
      Path("/primaryFilerAge65OrOlder") -> false,
      Path("/primaryFilerIsBlind") -> false,
      Path("/primaryFilerIsClaimedOnAnotherReturn") -> false,
      Path("/secondaryFilerAge65OrOlder") -> false,
      Path("/secondaryFilerIsBlind") -> false,
      Path("/secondaryFilerIsClaimedOnAnotherReturn") -> false,
      jobs -> jobsCollection,
      Path(s"/jobs/#${job1Id}/writableStartDate") -> Day("2025-01-01"),
      Path(s"/jobs/#${job1Id}/writableEndDate") -> Day("2025-10-15"),
      Path(s"/jobs/#${job1Id}/isAllYear") -> false,
      Path(s"/jobs/#${job1Id}/payFrequency") -> Enum("monthly", "/payFrequencyOptions"),
      Path(s"/jobs/#${job1Id}/mostRecentPayPeriodEnd") -> Day("2025-09-30"),
      Path(s"/jobs/#${job1Id}/mostRecentPayDate") -> Day("2025-10-05"),
      Path(s"/jobs/#${job1Id}/averagePayPerPayPeriod") -> Dollar("4000"),
      Path(s"/jobs/#${job1Id}/yearToDateIncome") -> Dollar("40000"),
      Path(s"/jobs/#${job1Id}/averageWithholdingPerPayPeriod") -> Dollar("300"),
      Path(s"/jobs/#${job1Id}/yearToDateWithholding") -> Dollar("3000"),
      Path(s"/jobs/#${job1Id}/totalBonusReceived") -> Dollar("0"),
      Path(s"/jobs/#${job1Id}/filerAssignment") -> Enum("self", "/filerAssignment"),
      Path(s"/jobs/#${job1Id}/overtimeCompensation") -> Dollar("0"),
      Path(s"/jobs/#${job1Id}/qualifiedTipIncome") -> Dollar("0"),
      Path(s"/jobs/#${job2Id}/isAllYear") -> true,
      Path(s"/jobs/#${job2Id}/payFrequency") -> Enum("monthly", "/payFrequencyOptions"),
      Path(s"/jobs/#${job2Id}/mostRecentPayPeriodEnd") -> Day("2025-09-30"),
      Path(s"/jobs/#${job2Id}/mostRecentPayDate") -> Day("2025-09-30"),
      Path(s"/jobs/#${job2Id}/averagePayPerPayPeriod") -> Dollar("4000"),
      Path(s"/jobs/#${job2Id}/yearToDateIncome") -> Dollar("36000"),
      Path(s"/jobs/#${job2Id}/averageWithholdingPerPayPeriod") -> Dollar("100"),
      Path(s"/jobs/#${job2Id}/yearToDateWithholding") -> Dollar("900"),
      Path(s"/jobs/#${job2Id}/totalBonusReceived") -> Dollar("0"),
      Path(s"/jobs/#${job2Id}/filerAssignment") -> Enum("spouse", "/filerAssignment"),
      Path(s"/jobs/#${job2Id}/overtimeCompensation") -> Dollar("0"),
      Path(s"/jobs/#${job2Id}/qualifiedTipIncome") -> Dollar("0"),
      Path("/totalCtcAndOdc") -> Dollar("2000"),
      Path("/totalEstimatedTaxesPaid") -> Dollar("0"),
      Path("/netSelfEmploymentIncomeTotal") -> Dollar("0"),
      // Derived overrides
      Path(s"/jobs/#${job1Id}/preTaxDeductions") -> Dollar("0"),
      Path(s"/jobs/#${job2Id}/preTaxDeductions") -> Dollar("0"),
      Path("/adjustmentsToIncome") -> Dollar("0"),
      Path("/otherIncomeTotal") -> Dollar("0"),
      Path(s"/jobs/#${job1Id}/isPastJob") -> false,
      Path(s"/jobs/#${job1Id}/isCurrentJob") -> true,
      Path(s"/jobs/#${job1Id}/isFutureJob") -> false,
      Path(s"/jobs/#${job2Id}/isPastJob") -> false,
      Path(s"/jobs/#${job2Id}/isCurrentJob") -> true,
      Path(s"/jobs/#${job2Id}/isFutureJob") -> false,
      Path("/nonItemizerCharitableContributionDeductionAmount") -> Dollar("0"),
    )

    assert(graph.get(Path(s"/jobs/#${job1Id}/income")).value.contains(Dollar("42000")))
    assert(graph.get(Path(s"/jobs/#${job2Id}/income")).value.contains(Dollar("48000")))

    assert(graph.get(Path(s"/jobs/#${job1Id}/endOfYearProjectedWithholding")).value.contains(Dollar("3000")))
    assert(graph.get(Path(s"/jobs/#${job2Id}/endOfYearProjectedWithholding")).value.contains(Dollar("1200")))

    assert(graph.get(Path(s"/jobs/#${job1Id}/tentativeWithholdingAmount")).value.contains(Dollar("150")))
    assert(graph.get(Path(s"/jobs/#${job2Id}/tentativeWithholdingAmount")).value.contains(Dollar("150")))

    assert(graph.get(Path("/totalEndOfYearProjectedWithholding")).value.contains(Dollar("4200")))
    assert(graph.get(Path("/agi")).value.contains(Dollar("90000")))

    if (graph.get(Path("/usePreOb3StandardDeduction")).value.contains(true)) {
      // These are the original values from the QA spreadsheet.
      assert(graph.get(Path("/tentativeTaxFromTaxableIncome")).value.contains(Dollar("6726")))
      assert(graph.get(Path("/totalOwed")).value.contains(Dollar("4726")))
      assert(graph.get(Path("/withholdingGap")).value.contains(Dollar("526")))
      assert(graph.get(Path("/jobSelectedForExtraWithholding/w4Line4c")).value.contains(Dollar("125")))
    } else {
      // The following values now differ from the QA spreadsheet due to standard deduction changes.
      assert(graph.get(Path("/tentativeTaxFromTaxableIncome")).value.contains(Dollar("6546")))
      assert(graph.get(Path("/totalOwed")).value.contains(Dollar("4546")))
      assert(graph.get(Path("/withholdingGap")).value.contains(Dollar("346")))
      assert(graph.get(Path("/jobSelectedForExtraWithholding/w4Line4c")).value.contains(Dollar("40")))
    }
  }

  test("2197 Scenarios spreadsheet Column E") {
    val graph = makeGraphWith(
      factDictionary,
      filingStatus -> Enum("headOfHousehold", "/filingStatusOptions"),
      Path("/primaryFilerAge65OrOlder") -> false,
      Path("/primaryFilerIsBlind") -> false,
      Path("/primaryFilerIsClaimedOnAnotherReturn") -> false,
      jobs -> jobsCollection,
      Path(s"/jobs/#${job1Id}/writableStartDate") -> Day("2025-01-01"),
      Path(s"/jobs/#${job1Id}/writableEndDate") -> Day("2025-10-15"),
      Path(s"/jobs/#${job1Id}/isAllYear") -> false,
      Path(s"/jobs/#${job1Id}/payFrequency") -> Enum("monthly", "/payFrequencyOptions"),
      Path(s"/jobs/#${job1Id}/mostRecentPayPeriodEnd") -> Day("2025-09-30"),
      Path(s"/jobs/#${job1Id}/mostRecentPayDate") -> Day("2025-10-05"),
      Path(s"/jobs/#${job1Id}/averagePayPerPayPeriod") -> Dollar("4000"),
      Path(s"/jobs/#${job1Id}/yearToDateIncome") -> Dollar("40000"),
      Path(s"/jobs/#${job1Id}/averageWithholdingPerPayPeriod") -> Dollar("100"),
      Path(s"/jobs/#${job1Id}/yearToDateWithholding") -> Dollar("1000"),
      Path(s"/jobs/#${job1Id}/totalBonusReceived") -> Dollar("0"),
      Path(s"/jobs/#${job1Id}/filerAssignment") -> Enum("self", "/filerAssignment"),
      Path(s"/jobs/#${job1Id}/overtimeCompensation") -> Dollar("0"),
      Path(s"/jobs/#${job1Id}/qualifiedTipIncome") -> Dollar("0"),
      Path(s"/jobs/#${job2Id}/isAllYear") -> true,
      Path(s"/jobs/#${job2Id}/payFrequency") -> Enum("monthly", "/payFrequencyOptions"),
      Path(s"/jobs/#${job2Id}/mostRecentPayPeriodEnd") -> Day("2025-09-30"),
      Path(s"/jobs/#${job2Id}/mostRecentPayDate") -> Day("2025-09-30"),
      Path(s"/jobs/#${job2Id}/averagePayPerPayPeriod") -> Dollar("2000"),
      Path(s"/jobs/#${job2Id}/yearToDateIncome") -> Dollar("18000"),
      Path(s"/jobs/#${job2Id}/averageWithholdingPerPayPeriod") -> Dollar("100"),
      Path(s"/jobs/#${job2Id}/yearToDateWithholding") -> Dollar("900"),
      Path(s"/jobs/#${job2Id}/totalBonusReceived") -> Dollar("0"),
      Path(s"/jobs/#${job2Id}/filerAssignment") -> Enum("spouse", "/filerAssignment"),
      Path(s"/jobs/#${job2Id}/overtimeCompensation") -> Dollar("0"),
      Path(s"/jobs/#${job2Id}/qualifiedTipIncome") -> Dollar("0"),
      Path("/totalCtcAndOdc") -> Dollar("2000"),
      Path("/totalEstimatedTaxesPaid") -> Dollar("0"),
      Path("/netSelfEmploymentIncomeTotal") -> Dollar("0"),
      // Derived overrides
      Path(s"/jobs/#${job1Id}/preTaxDeductions") -> Dollar("0"),
      Path(s"/jobs/#${job2Id}/preTaxDeductions") -> Dollar("0"),
      Path("/adjustmentsToIncome") -> Dollar("0"),
      Path("/otherIncomeTotal") -> Dollar("0"),
      Path(s"/jobs/#${job1Id}/isPastJob") -> false,
      Path(s"/jobs/#${job1Id}/isCurrentJob") -> true,
      Path(s"/jobs/#${job1Id}/isFutureJob") -> false,
      Path(s"/jobs/#${job2Id}/isPastJob") -> false,
      Path(s"/jobs/#${job2Id}/isCurrentJob") -> true,
      Path(s"/jobs/#${job2Id}/isFutureJob") -> false,
      Path("/nonItemizerCharitableContributionDeductionAmount") -> Dollar("0"),
    )

    assert(graph.get(Path(s"/jobs/#${job1Id}/income")).value.contains(Dollar("42000")))
    assert(graph.get(Path(s"/jobs/#${job2Id}/income")).value.contains(Dollar("24000")))

    assert(graph.get(Path(s"/jobs/#${job1Id}/endOfYearProjectedWithholding")).value.contains(Dollar("1000")))
    assert(graph.get(Path(s"/jobs/#${job2Id}/endOfYearProjectedWithholding")).value.contains(Dollar("1200")))

    assert(graph.get(Path(s"/jobs/#${job1Id}/tentativeWithholdingAmount")).value.contains(Dollar("226.67")))
    assert(graph.get(Path(s"/jobs/#${job2Id}/tentativeWithholdingAmount")).value.contains(Dollar("12.50")))

    assert(graph.get(Path("/totalEndOfYearProjectedWithholding")).value.contains(Dollar("2200")))
    assert(graph.get(Path("/agi")).value.contains(Dollar("66000")))

    if (graph.get(Path("/usePreOb3StandardDeduction")).value.contains(true)) {
      // These are the original values from the QA spreadsheet.
      assert(graph.get(Path("/tentativeTaxFromTaxableIncome")).value.contains(Dollar("4883")))
      assert(graph.get(Path("/totalOwed")).value.contains(Dollar("2883")))
      assert(graph.get(Path("/withholdingGap")).value.contains(Dollar("683")))
      assert(graph.get(Path("/jobSelectedForExtraWithholding/w4Line4c")).value.contains(Dollar("315")))
    } else {
      // The following values now differ from the QA spreadsheet due to standard deduction changes.
      assert(graph.get(Path("/tentativeTaxFromTaxableIncome")).value.contains(Dollar("4745")))
      assert(graph.get(Path("/totalOwed")).value.contains(Dollar("2745")))
      assert(graph.get(Path("/withholdingGap")).value.contains(Dollar("545")))
      assert(graph.get(Path("/jobSelectedForExtraWithholding/w4Line4c")).value.contains(Dollar("231")))
    }
  }

  test("2197 Scenarios spreadsheet Column G") {
    val graph = makeGraphWith(
      factDictionary,
      filingStatus -> single,
      Path("/primaryFilerAge65OrOlder") -> false,
      Path("/primaryFilerIsBlind") -> false,
      Path("/primaryFilerIsClaimedOnAnotherReturn") -> false,
      jobs -> jobsCollection,
      Path(s"/jobs/#${job1Id}/writableStartDate") -> Day("2025-01-01"),
      Path(s"/jobs/#${job1Id}/writableEndDate") -> Day("2025-10-15"),
      Path(s"/jobs/#${job1Id}/isAllYear") -> false,
      Path(s"/jobs/#${job1Id}/payFrequency") -> Enum("semiMonthly", "/payFrequencyOptions"),
      Path(s"/jobs/#${job1Id}/mostRecentPayPeriodEnd") -> Day("2025-09-30"),
      Path(s"/jobs/#${job1Id}/mostRecentPayDate") -> Day("2025-10-02"),
      Path(s"/jobs/#${job1Id}/averagePayPerPayPeriod") -> Dollar("4000"),
      Path(s"/jobs/#${job1Id}/yearToDateIncome") -> Dollar("76000"),
      Path(s"/jobs/#${job1Id}/averageWithholdingPerPayPeriod") -> Dollar("300"),
      Path(s"/jobs/#${job1Id}/yearToDateWithholding") -> Dollar("5700"),
      Path(s"/jobs/#${job1Id}/totalBonusReceived") -> Dollar("0"),
      Path(s"/jobs/#${job1Id}/filerAssignment") -> Enum("self", "/filerAssignment"),
      Path(s"/jobs/#${job1Id}/overtimeCompensation") -> Dollar("0"),
      Path(s"/jobs/#${job1Id}/qualifiedTipIncome") -> Dollar("0"),
      Path(s"/jobs/#${job2Id}/isAllYear") -> true,
      Path(s"/jobs/#${job2Id}/payFrequency") -> Enum("semiMonthly", "/payFrequencyOptions"),
      Path(s"/jobs/#${job2Id}/mostRecentPayPeriodEnd") -> Day("2025-09-30"),
      Path(s"/jobs/#${job2Id}/mostRecentPayDate") -> Day("2025-09-30"),
      Path(s"/jobs/#${job2Id}/averagePayPerPayPeriod") -> Dollar("2000"),
      Path(s"/jobs/#${job2Id}/yearToDateIncome") -> Dollar("36000"),
      Path(s"/jobs/#${job2Id}/averageWithholdingPerPayPeriod") -> Dollar("400"),
      Path(s"/jobs/#${job2Id}/yearToDateWithholding") -> Dollar("7200"),
      Path(s"/jobs/#${job2Id}/totalBonusReceived") -> Dollar("0"),
      Path(s"/jobs/#${job2Id}/filerAssignment") -> Enum("self", "/filerAssignment"),
      Path(s"/jobs/#${job2Id}/overtimeCompensation") -> Dollar("0"),
      Path(s"/jobs/#${job2Id}/qualifiedTipIncome") -> Dollar("0"),
      Path("/totalCtcAndOdc") -> Dollar("4000"),
      Path("/totalEstimatedTaxesPaid") -> Dollar("0"),
      Path("/netSelfEmploymentIncomeTotal") -> Dollar("0"),
      // Derived overrides
      Path(s"/jobs/#${job1Id}/preTaxDeductions") -> Dollar("0"),
      Path(s"/jobs/#${job2Id}/preTaxDeductions") -> Dollar("0"),
      Path("/adjustmentsToIncome") -> Dollar("0"),
      Path("/otherIncomeTotal") -> Dollar("0"),
      Path(s"/jobs/#${job1Id}/isPastJob") -> false,
      Path(s"/jobs/#${job1Id}/isCurrentJob") -> true,
      Path(s"/jobs/#${job1Id}/isFutureJob") -> false,
      Path(s"/jobs/#${job2Id}/isPastJob") -> false,
      Path(s"/jobs/#${job2Id}/isCurrentJob") -> true,
      Path(s"/jobs/#${job2Id}/isFutureJob") -> false,
      Path("/nonItemizerCharitableContributionDeductionAmount") -> Dollar("0"),
    )

    assert(graph.get(Path(s"/jobs/#${job1Id}/income")).value.contains(Dollar("80000")))
    assert(graph.get(Path(s"/jobs/#${job2Id}/income")).value.contains(Dollar("48000")))

    assert(graph.get(Path(s"/jobs/#${job1Id}/endOfYearProjectedWithholding")).value.contains(Dollar("6000")))
    assert(graph.get(Path(s"/jobs/#${job2Id}/endOfYearProjectedWithholding")).value.contains(Dollar("9600")))

    assert(graph.get(Path(s"/jobs/#${job1Id}/tentativeWithholdingAmount")).value.contains(Dollar("530.58")))
    assert(graph.get(Path(s"/jobs/#${job2Id}/tentativeWithholdingAmount")).value.contains(Dollar("155.06")))

    assert(graph.get(Path("/totalEndOfYearProjectedWithholding")).value.contains(Dollar("15600")))
    assert(graph.get(Path("/agi")).value.contains(Dollar("128000")))

    if (graph.get(Path("/usePreOb3StandardDeduction")).value.contains(true)) {
      // These are the original values from the QA spreadsheet.
      assert(graph.get(Path("/tentativeTaxFromTaxableIncome")).value.contains(Dollar("19967")))
      assert(graph.get(Path("/totalOwed")).value.contains(Dollar("15967")))
      assert(graph.get(Path("/withholdingGap")).value.contains(Dollar("367")))
      assert(graph.get(Path("/jobSelectedForExtraWithholding/w4Line4c")).value.contains(Dollar("318")))
    } else {
      // The following values now differ from the QA spreadsheet due to standard deduction changes.
      assert(graph.get(Path("/tentativeTaxFromTaxableIncome")).value.contains(Dollar("19787")))
      assert(graph.get(Path("/totalOwed")).value.contains(Dollar("15787")))
      assert(graph.get(Path("/withholdingGap")).value.contains(Dollar("187")))
      assert(graph.get(Path("/jobSelectedForExtraWithholding/w4Line4c")).value.contains(Dollar("282")))
    }
  }

  test("2197 Scenarios spreadsheet Column H") {
    val graph = makeGraphWith(
      factDictionary,
      filingStatus -> Enum("marriedFilingJointly", "/filingStatusOptions"),
      Path(s"/netSelfEmploymentIncomeSelf") -> Dollar("10000"),
    )

    assert(graph.get(Path(s"/selfEmploymentTax")).value.contains(Dollar("1413")))

  }

  test("2197 Scenarios spreadsheet Column I") {
    val graph = makeGraphWith(
      factDictionary,
      filingStatus -> Enum("headOfHousehold", "/filingStatusOptions"),
      Path("/primaryFilerAge65OrOlder") -> false,
      Path("/primaryFilerIsBlind") -> false,
      Path("/primaryFilerIsClaimedOnAnotherReturn") -> false,
      jobs -> jobsCollection,
      Path(s"/jobs/#${job1Id}/writableStartDate") -> Day("2025-01-01"),
      Path(s"/jobs/#${job1Id}/writableEndDate") -> Day("2025-10-15"),
      Path(s"/jobs/#${job1Id}/isAllYear") -> false,
      Path(s"/jobs/#${job1Id}/payFrequency") -> Enum("semiMonthly", "/payFrequencyOptions"),
      Path(s"/jobs/#${job1Id}/mostRecentPayPeriodEnd") -> Day("2025-09-30"),
      Path(s"/jobs/#${job1Id}/mostRecentPayDate") -> Day("2025-10-02"),
      Path(s"/jobs/#${job1Id}/averagePayPerPayPeriod") -> Dollar("4000"),
      Path(s"/jobs/#${job1Id}/yearToDateIncome") -> Dollar("76000"),
      Path(s"/jobs/#${job1Id}/averageWithholdingPerPayPeriod") -> Dollar("300"),
      Path(s"/jobs/#${job1Id}/yearToDateWithholding") -> Dollar("5700"),
      Path(s"/jobs/#${job1Id}/totalBonusReceived") -> Dollar("0"),
      Path(s"/jobs/#${job2Id}/isAllYear") -> true,
      Path(s"/jobs/#${job2Id}/payFrequency") -> Enum("semiMonthly", "/payFrequencyOptions"),
      Path(s"/jobs/#${job2Id}/mostRecentPayPeriodEnd") -> Day("2025-09-30"),
      Path(s"/jobs/#${job2Id}/mostRecentPayDate") -> Day("2025-09-30"),
      Path(s"/jobs/#${job2Id}/averagePayPerPayPeriod") -> Dollar("2000"),
      Path(s"/jobs/#${job2Id}/yearToDateIncome") -> Dollar("36000"),
      Path(s"/jobs/#${job2Id}/averageWithholdingPerPayPeriod") -> Dollar("200"),
      Path(s"/jobs/#${job2Id}/yearToDateWithholding") -> Dollar("3600"),
      Path(s"/jobs/#${job2Id}/totalBonusReceived") -> Dollar("0"),
      Path("/totalCtcAndOdc") -> Dollar("4000"),
      Path("/totalEstimatedTaxesPaid") -> Dollar("0"),
      Path("/netSelfEmploymentIncomeTotal") -> Dollar("0"),
      // Derived overrides
      Path(s"/jobs/#${job1Id}/preTaxDeductions") -> Dollar("0"),
      Path(s"/jobs/#${job2Id}/preTaxDeductions") -> Dollar("0"),
      Path("/adjustmentsToIncome") -> Dollar("0"),
      Path("/otherIncomeTotal") -> Dollar("0"),
      // Path("/usePreOb3StandardDeduction") -> true,
      Path("/nonItemizerCharitableContributionDeductionAmount") -> Dollar("0"),
      Path(s"/jobs/#${job1Id}/isPastJob") -> false,
      Path(s"/jobs/#${job1Id}/isCurrentJob") -> true,
      Path(s"/jobs/#${job1Id}/isFutureJob") -> false,
      Path(s"/jobs/#${job2Id}/isPastJob") -> false,
      Path(s"/jobs/#${job2Id}/isCurrentJob") -> true,
      Path(s"/jobs/#${job2Id}/isFutureJob") -> false,
      Path(s"/netSelfEmploymentIncomeTotal") -> Dollar("0"),
    )

    assert(graph.get(Path(s"/jobs/#${job1Id}/income")).value.contains(Dollar("80000")))
    assert(graph.get(Path(s"/jobs/#${job2Id}/income")).value.contains(Dollar("48000")))

    assert(graph.get(Path(s"/jobs/#${job1Id}/endOfYearProjectedWithholding")).value.contains(Dollar("6000")))
    assert(graph.get(Path(s"/jobs/#${job2Id}/endOfYearProjectedWithholding")).value.contains(Dollar("4800")))

    assert(graph.get(Path(s"/jobs/#${job1Id}/tentativeWithholdingAmount")).value.contains(Dollar("389.38")))
    assert(graph.get(Path(s"/jobs/#${job2Id}/tentativeWithholdingAmount")).value.contains(Dollar("113.33")))

    assert(graph.get(Path("/totalEndOfYearProjectedWithholding")).value.contains(Dollar("10800")))
    assert(graph.get(Path("/agi")).value.contains(Dollar("128000")))

    if (graph.get(Path("/usePreOb3StandardDeduction")).value.contains(true)) {
      // These are the original values from the QA spreadsheet.
      assert(graph.get(Path("/tentativeTaxFromTaxableIncome")).value.contains(Dollar("16428")))
      assert(graph.get(Path("/totalOwed")).value.contains(Dollar("12428")))
      assert(graph.get(Path("/withholdingGap")).value.contains(Dollar("1628")))
      assert(graph.get(Path("/jobSelectedForExtraWithholding/w4Line4c")).value.contains(Dollar("412")))
    } else {
      // The following values now differ from the QA spreadsheet due to standard deduction changes.
      assert(graph.get(Path("/tentativeTaxFromTaxableIncome")).value.contains(Dollar("16158")))
      assert(graph.get(Path("/totalOwed")).value.contains(Dollar("12158")))
      assert(graph.get(Path("/withholdingGap")).value.contains(Dollar("1358")))
      assert(graph.get(Path("/jobSelectedForExtraWithholding/w4Line4c")).value.contains(Dollar("358")))
    }
  }

  test("2197 Scenarios spreadsheet Column L") {
    val graph = makeGraphWith(
      factDictionary,
      filingStatus -> Enum("headOfHousehold", "/filingStatusOptions"),
      Path(s"/netSelfEmploymentIncomeSelf") -> Dollar("20000"),
    )

    assert(graph.get(Path(s"/selfEmploymentTax")).value.contains(Dollar("2826")))
  }

  test("2197 Scenarios spreadsheet Column N") {
    val graph = makeGraphWith(
      factDictionary,
      filingStatus -> single,
      Path("/primaryFilerAge65OrOlder") -> false,
      Path("/primaryFilerIsBlind") -> false,
      Path("/primaryFilerIsClaimedOnAnotherReturn") -> false,
      jobs -> jobsCollection,
      Path(s"/jobs/#${job1Id}/writableStartDate") -> Day("2025-01-01"),
      Path(s"/jobs/#${job1Id}/writableEndDate") -> Day("2025-10-15"),
      Path(s"/jobs/#${job1Id}/isAllYear") -> false,
      Path(s"/jobs/#${job1Id}/mostRecentPayPeriodEnd") -> Day("2025-10-05"),
      Path(s"/jobs/#${job1Id}/mostRecentPayDate") -> Day("2025-10-08"),
      Path(s"/jobs/#${job1Id}/averagePayPerPayPeriod") -> Dollar("2000"),
      Path(s"/jobs/#${job1Id}/yearToDateIncome") -> Dollar("82000"),
      Path(s"/jobs/#${job1Id}/payFrequency") -> Enum("weekly", "/payFrequencyOptions"),
      Path(s"/jobs/#${job1Id}/averageWithholdingPerPayPeriod") -> Dollar("200"),
      Path(s"/jobs/#${job1Id}/yearToDateWithholding") -> Dollar("8200"),
      Path(s"/jobs/#${job1Id}/totalBonusReceived") -> Dollar("0"),
      Path(s"/jobs/#${job2Id}/isAllYear") -> true,
      Path(s"/jobs/#${job2Id}/mostRecentPayPeriodEnd") -> Day("2025-10-08"),
      Path(s"/jobs/#${job2Id}/mostRecentPayDate") -> Day("2025-10-08"),
      Path(s"/jobs/#${job2Id}/averagePayPerPayPeriod") -> Dollar("1000"),
      Path(s"/jobs/#${job2Id}/yearToDateIncome") -> Dollar("41000"),
      Path(s"/jobs/#${job2Id}/payFrequency") -> Enum("weekly", "/payFrequencyOptions"),
      Path(s"/jobs/#${job2Id}/averageWithholdingPerPayPeriod") -> Dollar("100"),
      Path(s"/jobs/#${job2Id}/yearToDateWithholding") -> Dollar("4100"),
      Path(s"/jobs/#${job2Id}/totalBonusReceived") -> Dollar("0"),
      Path("/totalCtcAndOdc") -> Dollar("6000"),
      Path("/totalEstimatedTaxesPaid") -> Dollar("0"),
      Path("/netSelfEmploymentIncomeTotal") -> Dollar("0"),
      // Derived overrides
      Path(s"/jobs/#${job1Id}/preTaxDeductions") -> Dollar("0"),
      Path(s"/jobs/#${job2Id}/preTaxDeductions") -> Dollar("0"),
      Path("/adjustmentsToIncome") -> Dollar("0"),
      Path("/otherIncomeTotal") -> Dollar("0"),
      Path("/nonItemizerCharitableContributionDeductionAmount") -> Dollar("0"),
      Path(s"/jobs/#${job1Id}/isPastJob") -> false,
      Path(s"/jobs/#${job1Id}/isCurrentJob") -> true,
      Path(s"/jobs/#${job1Id}/isFutureJob") -> false,
      Path(s"/jobs/#${job2Id}/isPastJob") -> false,
      Path(s"/jobs/#${job2Id}/isCurrentJob") -> true,
      Path(s"/jobs/#${job2Id}/isFutureJob") -> false,
    )

    assert(graph.get(Path(s"/jobs/#${job1Id}/income")).value.contains(Dollar("84857.14")))
    assert(graph.get(Path(s"/jobs/#${job2Id}/income")).value.contains(Dollar("53000")))

    assert(graph.get(Path(s"/jobs/#${job1Id}/endOfYearProjectedWithholding")).value.contains(Dollar("8400")))
    assert(graph.get(Path(s"/jobs/#${job2Id}/endOfYearProjectedWithholding")).value.contains(Dollar("5300")))

    assert(graph.get(Path(s"/jobs/#${job1Id}/tentativeWithholdingAmount")).value.contains(Dollar("278.73")))
    assert(graph.get(Path(s"/jobs/#${job2Id}/tentativeWithholdingAmount")).value.contains(Dollar("80.80")))

    assert(graph.get(Path("/totalEndOfYearProjectedWithholding")).value.contains(Dollar("13700")))
    assert(graph.get(Path("/agi")).value.contains(Dollar("137857")))

    if (graph.get(Path("/usePreOb3StandardDeduction")).value.contains(true)) {
      // These are the original values from the QA spreadsheet.
      assert(graph.get(Path("/tentativeTaxFromTaxableIncome")).value.contains(Dollar("22333")))
      assert(graph.get(Path("/totalOwed")).value.contains(Dollar("16333")))
      assert(graph.get(Path("/withholdingGap")).value.contains(Dollar("2633")))
      // This is actually 280 in the spreadsheet but when calculating it manually we get 283
      assert(graph.get(Path("/jobSelectedForExtraWithholding/w4Line4c")).value.contains(Dollar("283")))
    } else {
      // The following values now differ from the QA spreadsheet due to standard deduction changes.
      assert(graph.get(Path("/tentativeTaxFromTaxableIncome")).value.contains(Dollar("22153")))
      assert(graph.get(Path("/totalOwed")).value.contains(Dollar("16153")))
      assert(graph.get(Path("/withholdingGap")).value.contains(Dollar("2453")))
      assert(graph.get(Path("/jobSelectedForExtraWithholding/w4Line4c")).value.contains(Dollar("256")))
    }
  }

  test("2197 Scenarios spreadsheet Column O") {
    val graph = makeGraphWith(
      factDictionary,
      filingStatus -> Enum("marriedFilingJointly", "/filingStatusOptions"),
      Path(s"/netSelfEmploymentIncomeSelf") -> Dollar("7000"),
    )

    assert(graph.get(Path(s"/selfEmploymentTax")).value.contains(Dollar("989")))

  }

  test("2197 Scenarios spreadsheet Column P") {
    val graph = makeGraphWith(
      factDictionary,
      Path(s"/netSelfEmploymentIncomeSelf") -> Dollar("7000"),
    )

    assert(graph.get(Path(s"/selfEmploymentTax")).value.contains(Dollar("989")))
  }

  test("Scenario with three jobs") {
    val json = """ {
  "/jobs/#968b66ab-a22f-469b-93e7-d5f3e78cc36a/isAllYear": {
    "$type": "BooleanWrapper",
    "item": false
  },
  "/jobs/#b961fe59-0caf-4463-b8f6-e7955be1ae89/isAllYear": {
    "$type": "BooleanWrapper",
    "item": true
  },
  "/jobs/#968b66ab-a22f-469b-93e7-d5f3e78cc36a/writableEndDate": {
    "$type": "DayWrapper",
    "item": {
      "date": "2025-12-31"
    }
  },
  "/actualAmericanOpportunityTaxCreditAmount": {
    "$type": "DollarWrapper",
    "item": "0.00"
  },
  "/actualEarnedIncomeTaxCreditAmount": {
    "$type": "DollarWrapper",
    "item": "0.00"
  },
  "/actualChildAndDependentCareTaxCreditAmount": {
    "$type": "DollarWrapper",
    "item": "0.00"
  },
  "/jobs/#b961fe59-0caf-4463-b8f6-e7955be1ae89/payFrequency": {
    "$type": "EnumWrapper",
    "item": {
      "value": "monthly",
      "enumOptionsPath": "/payFrequencyOptions"
    }
  },
  "/jobs/#b961fe59-0caf-4463-b8f6-e7955be1ae89/totalBonusReceived": {
    "$type": "DollarWrapper",
    "item": "0.00"
  },
  "/jobs": {
    "$type": "CollectionWrapper",
    "item": {
      "items": [
        "968b66ab-a22f-469b-93e7-d5f3e78cc36a",
        "b961fe59-0caf-4463-b8f6-e7955be1ae89",
        "697b03f6-2f2b-4fe0-be56-356e7695a677"
      ]
    }
  },
  "/jobs/#968b66ab-a22f-469b-93e7-d5f3e78cc36a/payFrequency": {
    "$type": "EnumWrapper",
    "item": {
      "value": "monthly",
      "enumOptionsPath": "/payFrequencyOptions"
    }
  },
  "/jobs/#b961fe59-0caf-4463-b8f6-e7955be1ae89/averageWithholdingPerPayPeriod": {
    "$type": "DollarWrapper",
    "item": "91.00"
  },
  "/miscIncome": {
    "$type": "DollarWrapper",
    "item": "0.00"
  },
  "/actualElderlyAndDisabledTaxCreditAmount": {
    "$type": "DollarWrapper",
    "item": "0.00"
  },
  "/jobs/#b961fe59-0caf-4463-b8f6-e7955be1ae89/yearToDateWithholding": {
    "$type": "DollarWrapper",
    "item": "182.00"
  },
  "/jobs/#968b66ab-a22f-469b-93e7-d5f3e78cc36a/mostRecentPayDate": {
    "$type": "DayWrapper",
    "item": {
      "date": "2025-02-01"
    }
  },
  "/jobs/#b961fe59-0caf-4463-b8f6-e7955be1ae89/yearToDateIncome": {
    "$type": "DollarWrapper",
    "item": "4000.00"
  },
  "/jobs/#968b66ab-a22f-469b-93e7-d5f3e78cc36a/yearToDateIncome": {
    "$type": "DollarWrapper",
    "item": "10000.00"
  },
  "/jobs/#968b66ab-a22f-469b-93e7-d5f3e78cc36a/averageWithholdingPerPayPeriod": {
    "$type": "DollarWrapper",
    "item": "430.00"
  },
  "/jobs/#968b66ab-a22f-469b-93e7-d5f3e78cc36a/retirementPlanContributions": {
    "$type": "DollarWrapper",
    "item": "0.00"
  },
  "/jobs/#968b66ab-a22f-469b-93e7-d5f3e78cc36a/healthInsuranceContributions": {
    "$type": "DollarWrapper",
    "item": "0.00"
  },
  "/jobs/#968b66ab-a22f-469b-93e7-d5f3e78cc36a/hsaOrFsaContributions": {
    "$type": "DollarWrapper",
    "item": "0.00"
  },
  "/primaryFilerHasSSN": {
    "$type": "BooleanWrapper",
    "item": true
  },
  "/primaryFilerIsBlind": {
    "$type": "BooleanWrapper",
    "item": false
  },
  "/jobs/#b961fe59-0caf-4463-b8f6-e7955be1ae89/mostRecentPayPeriodEnd": {
    "$type": "DayWrapper",
    "item": {
      "date": "2025-01-31"
    }
  },
  "/jobs/#968b66ab-a22f-469b-93e7-d5f3e78cc36a/writableStartDate": {
    "$type": "DayWrapper",
    "item": {
      "date": "2025-01-01"
    }
  },
  "/jobs/#968b66ab-a22f-469b-93e7-d5f3e78cc36a/totalBonusReceived": {
    "$type": "DollarWrapper",
    "item": "0.00"
  },
  "/jobs/#b961fe59-0caf-4463-b8f6-e7955be1ae89/mostRecentPayDate": {
    "$type": "DayWrapper",
    "item": {
      "date": "2025-02-01"
    }
  },
  "/primaryFilerIsClaimedOnAnotherReturn": {
    "$type": "BooleanWrapper",
    "item": false
  },
  "/jobs/#968b66ab-a22f-469b-93e7-d5f3e78cc36a/mostRecentPayPeriodEnd": {
    "$type": "DayWrapper",
    "item": {
      "date": "2025-01-31"
    }
  },
  "/jobs/#968b66ab-a22f-469b-93e7-d5f3e78cc36a/yearToDateWithholding": {
    "$type": "DollarWrapper",
    "item": "860.00"
  },
  "/jobs/#968b66ab-a22f-469b-93e7-d5f3e78cc36a/averagePayPerPayPeriod": {
    "$type": "DollarWrapper",
    "item": "5000.00"
  },
  "/jobs/#b961fe59-0caf-4463-b8f6-e7955be1ae89/averagePayPerPayPeriod": {
    "$type": "DollarWrapper",
    "item": "2000.00"
  },
  "/filingStatus": {
    "$type": "EnumWrapper",
    "item": {
      "value": "single",
      "enumOptionsPath": "/filingStatusOptions"
    }
  },
  "/jobs/#b961fe59-0caf-4463-b8f6-e7955be1ae89/retirementPlanContributions": {
    "$type": "DollarWrapper",
    "item": "0.00"
  },
  "/jobs/#b961fe59-0caf-4463-b8f6-e7955be1ae89/healthInsuranceContributions": {
    "$type": "DollarWrapper",
    "item": "0.00"
  },
  "/jobs/#b961fe59-0caf-4463-b8f6-e7955be1ae89/hsaOrFsaContributions": {
    "$type": "DollarWrapper",
    "item": "0.00"
  },
  "/primaryFilerAge65OrOlder": {
    "$type": "BooleanWrapper",
    "item": false
  },
  "/jobs/#697b03f6-2f2b-4fe0-be56-356e7695a677/mostRecentPayPeriodEnd": {
    "$type": "DayWrapper",
    "item": {
      "date": "2025-01-31"
    }
  },
   "/jobs/#697b03f6-2f2b-4fe0-be56-356e7695a677/mostRecentPayDate": {
    "$type": "DayWrapper",
    "item": {
      "date": "2025-02-11"
    }
  },
  "/jobs/#697b03f6-2f2b-4fe0-be56-356e7695a677/totalBonusReceived": {
    "$type": "DollarWrapper",
    "item": "0.00"
  },
  "/jobs/#697b03f6-2f2b-4fe0-be56-356e7695a677/retirementPlanContributions": {
    "$type": "DollarWrapper",
    "item": "0.00"
  },
  "/jobs/#697b03f6-2f2b-4fe0-be56-356e7695a677/healthInsuranceContributions": {
    "$type": "DollarWrapper",
    "item": "0.00"
  },
  "/jobs/#697b03f6-2f2b-4fe0-be56-356e7695a677/hsaOrFsaContributions": {
    "$type": "DollarWrapper",
    "item": "0.00"
  },
  "/jobs/#697b03f6-2f2b-4fe0-be56-356e7695a677/isAllYear": {
    "$type": "BooleanWrapper",
    "item": true
  },
  "/jobs/#697b03f6-2f2b-4fe0-be56-356e7695a677/payFrequency": {
    "$type": "EnumWrapper",
    "item": {
      "value": "monthly",
      "enumOptionsPath": "/payFrequencyOptions"
    }
  },
  "/jobs/#697b03f6-2f2b-4fe0-be56-356e7695a677/averagePayPerPayPeriod": {
    "$type": "DollarWrapper",
    "item": "2000.00"
  },
  "/jobs/#697b03f6-2f2b-4fe0-be56-356e7695a677/averageWithholdingPerPayPeriod": {
    "$type": "DollarWrapper",
    "item": "100.00"
  },
  "/jobs/#697b03f6-2f2b-4fe0-be56-356e7695a677/yearToDateIncome": {
    "$type": "DollarWrapper",
    "item": "4000.00"
  },
  "/jobs/#697b03f6-2f2b-4fe0-be56-356e7695a677/yearToDateWithholding": {
    "$type": "DollarWrapper",
    "item": "200.00"
  },
  "/totalEstimatedTaxesPaid": {
    "$type": "DollarWrapper",
    "item": "0"
  }
}

    """
    val graph = Graph.apply(factDictionary, InMemoryPersister.apply(json))
    val _job1Id = "968b66ab-a22f-469b-93e7-d5f3e78cc36a"
    val _job2Id = "b961fe59-0caf-4463-b8f6-e7955be1ae89"
    val _job3Id = "697b03f6-2f2b-4fe0-be56-356e7695a677"
    // Derived overrides
    graph.set(Path("/adjustmentsToIncome"), Dollar(0))
    graph.set(Path("/otherIncomeTotal"), Dollar(0))
    graph.set(Path("/totalCredits"), Dollar(0))
    graph.set(Path("/nonItemizerCharitableContributionDeductionAmount"), Dollar("0"))
    graph.set(Path("/netSelfEmploymentIncomeTotal"), Dollar("0"))

    assert(graph.get(Path("/jobSelectedForExtraWithholding/w4Line4c")).value.contains(Dollar(817)))

    // Other job(s) should have zero on line 4c
    assert(graph.get(Path(s"/jobs/#${_job2Id}/w4Line4c")).value.contains(Dollar(0)))
    assert(graph.get(Path(s"/jobs/#${_job3Id}/w4Line4c")).value.contains(Dollar(0)))
  }

  test("Scenario where Form W4 Step 2 checkbox could apply") {
    // Using JSON here for ease of moving data in/out of the app, which has two benefits:
    // 1. ease of data entry using the app
    // 2. access to `factGraph.debugFact` to investigate test calculations.
    val json = """ {
  "/jobs/#968b66ab-a22f-469b-93e7-d5f3e78cc36a/isAllYear": {
    "$type": "BooleanWrapper",
    "item": true
  },
  "/actualAmericanOpportunityTaxCreditAmount": {
    "$type": "DollarWrapper",
    "item": "0.00"
  },
  "/actualEarnedIncomeTaxCreditAmount": {
    "$type": "DollarWrapper",
    "item": "0.00"
  },
  "/actualChildAndDependentCareTaxCreditAmount": {
    "$type": "DollarWrapper",
    "item": "0.00"
  },
  "/jobs/#b961fe59-0caf-4463-b8f6-e7955be1ae89/payFrequency": {
    "$type": "EnumWrapper",
    "item": {
      "value": "weekly",
      "enumOptionsPath": "/payFrequencyOptions"
    }
  },
  "/jobs/#b961fe59-0caf-4463-b8f6-e7955be1ae89/totalBonusReceived": {
    "$type": "DollarWrapper",
    "item": "0.00"
  },
  "/jobs": {
    "$type": "CollectionWrapper",
    "item": {
      "items": [
        "968b66ab-a22f-469b-93e7-d5f3e78cc36a",
        "b961fe59-0caf-4463-b8f6-e7955be1ae89"
      ]
    }
  },
  "/jobs/#968b66ab-a22f-469b-93e7-d5f3e78cc36a/payFrequency": {
    "$type": "EnumWrapper",
    "item": {
      "value": "monthly",
      "enumOptionsPath": "/payFrequencyOptions"
    }
  },
  "/jobs/#b961fe59-0caf-4463-b8f6-e7955be1ae89/averageWithholdingPerPayPeriod": {
    "$type": "DollarWrapper",
    "item": "93.00"
  },
  "/miscIncome": {
    "$type": "DollarWrapper",
    "item": "0.00"
  },
  "/actualElderlyAndDisabledTaxCreditAmount": {
    "$type": "DollarWrapper",
    "item": "0.00"
  },
  "/jobs/#b961fe59-0caf-4463-b8f6-e7955be1ae89/yearToDateWithholding": {
    "$type": "DollarWrapper",
    "item": "465.00"
  },
  "/jobs/#968b66ab-a22f-469b-93e7-d5f3e78cc36a/mostRecentPayDate": {
    "$type": "DayWrapper",
    "item": {
      "date": "2025-02-01"
    }
  },
  "/jobs/#b961fe59-0caf-4463-b8f6-e7955be1ae89/yearToDateIncome": {
    "$type": "DollarWrapper",
    "item": "5000.00"
  },
  "/jobs/#968b66ab-a22f-469b-93e7-d5f3e78cc36a/yearToDateIncome": {
    "$type": "DollarWrapper",
    "item": "10000.00"
  },
  "/jobs/#968b66ab-a22f-469b-93e7-d5f3e78cc36a/averageWithholdingPerPayPeriod": {
    "$type": "DollarWrapper",
    "item": "430.00"
  },
  "/jobs/#968b66ab-a22f-469b-93e7-d5f3e78cc36a/retirementPlanContributions": {
    "$type": "DollarWrapper",
    "item": "0.00"
  },
  "/jobs/#968b66ab-a22f-469b-93e7-d5f3e78cc36a/healthInsuranceContributions": {
    "$type": "DollarWrapper",
    "item": "0.00"
  },
  "/jobs/#968b66ab-a22f-469b-93e7-d5f3e78cc36a/hsaOrFsaContributions": {
    "$type": "DollarWrapper",
    "item": "0.00"
  },
  "/primaryFilerHasSSN": {
    "$type": "BooleanWrapper",
    "item": true
  },
  "/primaryFilerIsBlind": {
    "$type": "BooleanWrapper",
    "item": false
  },
  "/jobs/#b961fe59-0caf-4463-b8f6-e7955be1ae89/mostRecentPayPeriodEnd": {
    "$type": "DayWrapper",
    "item": {
      "date": "2025-01-31"
    }
  },
  "/jobs/#968b66ab-a22f-469b-93e7-d5f3e78cc36a/totalBonusReceived": {
    "$type": "DollarWrapper",
    "item": "0.00"
  },
  "/jobs/#b961fe59-0caf-4463-b8f6-e7955be1ae89/mostRecentPayDate": {
    "$type": "DayWrapper",
    "item": {
      "date": "2025-02-01"
    }
  },
  "/primaryFilerIsClaimedOnAnotherReturn": {
    "$type": "BooleanWrapper",
    "item": false
  },
  "/jobs/#968b66ab-a22f-469b-93e7-d5f3e78cc36a/mostRecentPayPeriodEnd": {
    "$type": "DayWrapper",
    "item": {
      "date": "2025-01-31"
    }
  },
  "/jobs/#b961fe59-0caf-4463-b8f6-e7955be1ae89/isAllYear": {
    "$type": "BooleanWrapper",
    "item": true
  },
  "/jobs/#968b66ab-a22f-469b-93e7-d5f3e78cc36a/yearToDateWithholding": {
    "$type": "DollarWrapper",
    "item": "860.00"
  },
  "/jobs/#968b66ab-a22f-469b-93e7-d5f3e78cc36a/averagePayPerPayPeriod": {
    "$type": "DollarWrapper",
    "item": "5000.00"
  },
  "/jobs/#b961fe59-0caf-4463-b8f6-e7955be1ae89/averagePayPerPayPeriod": {
    "$type": "DollarWrapper",
    "item": "1000.00"
  },
  "/filingStatus": {
    "$type": "EnumWrapper",
    "item": {
      "value": "single",
      "enumOptionsPath": "/filingStatusOptions"
    }
  },
  "/jobs/#b961fe59-0caf-4463-b8f6-e7955be1ae89/retirementPlanContributions": {
    "$type": "DollarWrapper",
    "item": "0.00"
  },
  "/jobs/#b961fe59-0caf-4463-b8f6-e7955be1ae89/healthInsuranceContributions": {
    "$type": "DollarWrapper",
    "item": "0.00"
  },
  "/jobs/#b961fe59-0caf-4463-b8f6-e7955be1ae89/hsaOrFsaContributions": {
    "$type": "DollarWrapper",
    "item": "0.00"
  },
  "/primaryFilerAge65OrOlder": {
    "$type": "BooleanWrapper",
    "item": false
  },
  "/totalEstimatedTaxesPaid": {
    "$type": "DollarWrapper",
    "item": "0"
  }
}
    """
    val graph = Graph.apply(factDictionary, InMemoryPersister.apply(json))
    val _job1Id = "968b66ab-a22f-469b-93e7-d5f3e78cc36a"
    val _job2Id = "b961fe59-0caf-4463-b8f6-e7955be1ae89"

    // Derived overrides
    graph.set(Path("/adjustmentsToIncome"), Dollar(0))
    graph.set(Path("/otherIncomeTotal"), Dollar(0))
    graph.set(Path("/totalCredits"), Dollar(0))
    graph.set(Path("/nonItemizerCharitableContributionDeductionAmount"), Dollar("0"))
    graph.set(Path("/netSelfEmploymentIncomeTotal"), Dollar("0"))

    graph.set(Path(s"/jobs/#${_job1Id}/isPastJob"), false)
    graph.set(Path(s"/jobs/#${_job1Id}/isCurrentJob"), true)
    graph.set(Path(s"/jobs/#${_job1Id}/isFutureJob"), false)
    graph.set(Path(s"/jobs/#${_job2Id}/isPastJob"), false)
    graph.set(Path(s"/jobs/#${_job2Id}/isCurrentJob"), true)
    graph.set(Path(s"/jobs/#${_job2Id}/isFutureJob"), false)

    assert(graph.get(Path(s"/jobs/#${_job1Id}/tentativeWithholdingAmount")).value.contains(Dollar("430.12")))
    assert(graph.get(Path(s"/jobs/#${_job2Id}/tentativeWithholdingAmount")).value.contains(Dollar("80.80")))
    assert(graph.get(Path("/jobSelectedForExtraWithholding/w4Line4c")).value.contains(Dollar(665)))

    // Other job(s) should have zero on line 4c
    assert(graph.get(Path(s"/jobs/#${_job2Id}/w4Line4c")).value.contains(Dollar(0)))
  }

  test("Scenario where Form W4 Multiple Jobs Worksheet could apply") {
    val json = """ {
  "/jobs/#b961fe59-0caf-4463-b8f6-e7955be1ae89/isAllYear": {
    "$type": "BooleanWrapper",
    "item": true
  },
  "/jobs/#968b66ab-a22f-469b-93e7-d5f3e78cc36a/isAllYear": {
    "$type": "BooleanWrapper",
    "item": true
  },
  "/actualAmericanOpportunityTaxCreditAmount": {
    "$type": "DollarWrapper",
    "item": "0.00"
  },
  "/actualEarnedIncomeTaxCreditAmount": {
    "$type": "DollarWrapper",
    "item": "0.00"
  },
  "/actualChildAndDependentCareTaxCreditAmount": {
    "$type": "DollarWrapper",
    "item": "0.00"
  },
  "/jobs/#b961fe59-0caf-4463-b8f6-e7955be1ae89/payFrequency": {
    "$type": "EnumWrapper",
    "item": {
      "value": "monthly",
      "enumOptionsPath": "/payFrequencyOptions"
    }
  },
  "/jobs/#b961fe59-0caf-4463-b8f6-e7955be1ae89/totalBonusReceived": {
    "$type": "DollarWrapper",
    "item": "0.00"
  },
  "/jobs": {
    "$type": "CollectionWrapper",
    "item": {
      "items": [
        "968b66ab-a22f-469b-93e7-d5f3e78cc36a",
        "b961fe59-0caf-4463-b8f6-e7955be1ae89"
      ]
    }
  },
  "/jobs/#968b66ab-a22f-469b-93e7-d5f3e78cc36a/payFrequency": {
    "$type": "EnumWrapper",
    "item": {
      "value": "monthly",
      "enumOptionsPath": "/payFrequencyOptions"
    }
  },
  "/jobs/#b961fe59-0caf-4463-b8f6-e7955be1ae89/averageWithholdingPerPayPeriod": {
    "$type": "DollarWrapper",
    "item": "91.00"
  },
  "/miscIncome": {
    "$type": "DollarWrapper",
    "item": "0.00"
  },
  "/actualElderlyAndDisabledTaxCreditAmount": {
    "$type": "DollarWrapper",
    "item": "0.00"
  },
  "/jobs/#b961fe59-0caf-4463-b8f6-e7955be1ae89/yearToDateWithholding": {
    "$type": "DollarWrapper",
    "item": "182.00"
  },
  "/jobs/#968b66ab-a22f-469b-93e7-d5f3e78cc36a/mostRecentPayDate": {
    "$type": "DayWrapper",
    "item": {
      "date": "2025-02-01"
    }
  },
  "/jobs/#b961fe59-0caf-4463-b8f6-e7955be1ae89/yearToDateIncome": {
    "$type": "DollarWrapper",
    "item": "4000.00"
  },
  "/jobs/#968b66ab-a22f-469b-93e7-d5f3e78cc36a/yearToDateIncome": {
    "$type": "DollarWrapper",
    "item": "10000.00"
  },
  "/jobs/#968b66ab-a22f-469b-93e7-d5f3e78cc36a/averageWithholdingPerPayPeriod": {
    "$type": "DollarWrapper",
    "item": "430.00"
  },
  "/jobs/#968b66ab-a22f-469b-93e7-d5f3e78cc36a/retirementPlanContributions": {
    "$type": "DollarWrapper",
    "item": "0.00"
  },
  "/jobs/#968b66ab-a22f-469b-93e7-d5f3e78cc36a/healthInsuranceContributions": {
    "$type": "DollarWrapper",
    "item": "0.00"
  },
  "/jobs/#968b66ab-a22f-469b-93e7-d5f3e78cc36a/hsaOrFsaContributions": {
    "$type": "DollarWrapper",
    "item": "0.00"
  },
  "/primaryFilerHasSSN": {
    "$type": "BooleanWrapper",
    "item": true
  },
  "/primaryFilerIsBlind": {
    "$type": "BooleanWrapper",
    "item": false
  },
  "/jobs/#b961fe59-0caf-4463-b8f6-e7955be1ae89/mostRecentPayPeriodEnd": {
    "$type": "DayWrapper",
    "item": {
      "date": "2025-01-31"
    }
  },
  "/jobs/#968b66ab-a22f-469b-93e7-d5f3e78cc36a/totalBonusReceived": {
    "$type": "DollarWrapper",
    "item": "0.00"
  },
  "/jobs/#b961fe59-0caf-4463-b8f6-e7955be1ae89/mostRecentPayDate": {
    "$type": "DayWrapper",
    "item": {
      "date": "2025-02-01"
    }
  },
  "/primaryFilerIsClaimedOnAnotherReturn": {
    "$type": "BooleanWrapper",
    "item": false
  },
  "/jobs/#968b66ab-a22f-469b-93e7-d5f3e78cc36a/mostRecentPayPeriodEnd": {
    "$type": "DayWrapper",
    "item": {
      "date": "2025-01-31"
    }
  },
  "/jobs/#968b66ab-a22f-469b-93e7-d5f3e78cc36a/yearToDateWithholding": {
    "$type": "DollarWrapper",
    "item": "860.00"
  },
  "/jobs/#968b66ab-a22f-469b-93e7-d5f3e78cc36a/averagePayPerPayPeriod": {
    "$type": "DollarWrapper",
    "item": "5000.00"
  },
  "/jobs/#b961fe59-0caf-4463-b8f6-e7955be1ae89/averagePayPerPayPeriod": {
    "$type": "DollarWrapper",
    "item": "2000.00"
  },
  "/filingStatus": {
    "$type": "EnumWrapper",
    "item": {
      "value": "single",
      "enumOptionsPath": "/filingStatusOptions"
    }
  },
  "/jobs/#b961fe59-0caf-4463-b8f6-e7955be1ae89/retirementPlanContributions": {
    "$type": "DollarWrapper",
    "item": "0.00"
  },
  "/jobs/#b961fe59-0caf-4463-b8f6-e7955be1ae89/healthInsuranceContributions": {
    "$type": "DollarWrapper",
    "item": "0.00"
  },
  "/jobs/#b961fe59-0caf-4463-b8f6-e7955be1ae89/hsaOrFsaContributions": {
    "$type": "DollarWrapper",
    "item": "0.00"
  },
  "/primaryFilerAge65OrOlder": {
    "$type": "BooleanWrapper",
    "item": false
  },
  "/totalEstimatedTaxesPaid": {
    "$type": "DollarWrapper",
    "item": "0"
  }
}
    """
    val graph = Graph.apply(factDictionary, InMemoryPersister.apply(json))
    val _job1Id = "968b66ab-a22f-469b-93e7-d5f3e78cc36a"
    val _job2Id = "b961fe59-0caf-4463-b8f6-e7955be1ae89"
    // Derived overrides
    graph.set(Path("/adjustmentsToIncome"), Dollar(0))
    graph.set(Path("/otherIncomeTotal"), Dollar(0))
    graph.set(Path("/totalCredits"), Dollar(0))
    graph.set(Path("/nonItemizerCharitableContributionDeductionAmount"), Dollar("0"))
    graph.set(Path("/netSelfEmploymentIncomeTotal"), Dollar("0"))

    assert(graph.get(Path("/jobSelectedForExtraWithholding/w4Line4c")).value.contains(Dollar(384)))

    // Other job(s) should have zero on line 4c
    assert(graph.get(Path(s"/jobs/#${_job2Id}/w4Line4c")).value.contains(Dollar(0)))
  }

  // test("2197 Scenarios spreadsheet Column N with a pension") {
  //   val graph = makeGraphWith(
  //     factDictionary,
  //     filingStatus -> single,
  //     Path("/primaryFilerDateOfBirth") -> Day("1985-01-28"),
  //     Path("/primaryFilerIsBlind") -> false,
  //     Path("/primaryFilerIsClaimedOnAnotherReturn") -> false,
  //     jobs -> jobsCollection,
  //     pensions -> pensionsCollection,
  //     Path(s"/jobs/#${job1Id}/startDate") -> Day("2025-01-01"),
  //     Path(s"/jobs/#${job1Id}/endDate") -> Day("2025-10-15"),
  //     Path(s"/jobs/#${job1Id}/mostRecentPayPeriodEnd") -> Day("2025-10-05"),
  //     Path(s"/jobs/#${job1Id}/mostRecentPayDate") -> Day("2025-10-08"),
  //     Path(s"/jobs/#${job1Id}/averagePayPerPayPeriod") -> Dollar("2000"),
  //     Path(s"/jobs/#${job1Id}/yearToDateIncome") -> Dollar("82000"),
  //     Path(s"/jobs/#${job1Id}/payFrequency") -> Enum("weekly", "/payFrequencyOptions"),
  //     Path(s"/jobs/#${job1Id}/averageWithholdingPerPayPeriod") -> Dollar("200"),
  //     Path(s"/jobs/#${job1Id}/yearToDateWithholding") -> Dollar("8200"),
  //     Path(s"/jobs/#${job1Id}/totalBonusReceived") -> Dollar("0"),
  //     Path(s"/jobs/#${job1Id}/preTaxDeductions") -> Dollar("0"),
  //     Path(s"/jobs/#${job2Id}/startDate") -> Day("2025-01-01"),
  //     Path(s"/jobs/#${job2Id}/endDate") -> Day("2025-12-31"),
  //     Path(s"/jobs/#${job2Id}/mostRecentPayPeriodEnd") -> Day("2025-10-08"),
  //     Path(s"/jobs/#${job2Id}/mostRecentPayDate") -> Day("2025-10-08"),
  //     Path(s"/jobs/#${job2Id}/averagePayPerPayPeriod") -> Dollar("1000"),
  //     Path(s"/jobs/#${job2Id}/yearToDateIncome") -> Dollar("41000"),
  //     Path(s"/jobs/#${job2Id}/payFrequency") -> Enum("weekly", "/payFrequencyOptions"),
  //     Path(s"/jobs/#${job2Id}/averageWithholdingPerPayPeriod") -> Dollar("100"),
  //     Path(s"/jobs/#${job2Id}/yearToDateWithholding") -> Dollar("4100"),
  //     Path(s"/jobs/#${job2Id}/totalBonusReceived") -> Dollar("0"),
  //     Path(s"/jobs/#${job2Id}/preTaxDeductions") -> Dollar("0"),
  //     Path("/totalCtcAndOdc") -> Dollar("6000"),
  //     Path("/totalEstimatedTaxesPaid") -> Dollar("0"),

  //     Path(s"/pensions/#${pension1Id}/averagePayPerPayPeriod") -> Dollar("5000"),
  //     Path(s"/pensions/#${pension1Id}/averageWithholdingPerPayPeriod") -> Dollar("400"),
  //     Path(s"/pensions/#${pension1Id}/yearToDateIncome") -> Dollar("5000"),
  //     Path(s"/pensions/#${pension1Id}/yearToDateWithholding") -> Dollar("3200"),
  //     Path(s"/pensions/#${pension1Id}/payFrequency") -> Enum("monthly", "/payFrequencyOptions"),

  //     // Derived overrides
  //     Path("/adjustmentsToIncome") -> Dollar("0"),
  //     Path("/otherIncomeTotal") -> Dollar("0"),
  //   )

  //   assert(graph.get(Path(s"/jobs/#${job1Id}/income")).value.contains(Dollar("84857.14")))
  //   assert(graph.get(Path(s"/jobs/#${job2Id}/income")).value.contains(Dollar("53000")))

  //   assert(graph.get(Path(s"/jobs/#${job1Id}/endOfYearProjectedWithholding")).value.contains(Dollar("8400")))
  //   assert(graph.get(Path(s"/jobs/#${job2Id}/endOfYearProjectedWithholding")).value.contains(Dollar("5300")))
  //   assert(graph.get(Path(s"/jobs/#${job1Id}/tentativeWithholdingAmount")).value.contains(Dollar("278.73")))
  //   assert(graph.get(Path(s"/jobs/#${job2Id}/tentativeWithholdingAmount")).value.contains(Dollar("80.80")))
  //   assert(graph.get(Path("/totalEndOfYearProjectedWithholding")).value.contains(Dollar("13700")))

  //   assert(graph.get(Path("/agi")).value.contains(Dollar("137857")))
  //   assert(graph.get(Path("/tentativeTaxFromTaxableIncome")).value.contains(Dollar("22333")))
  //   assert(graph.get(Path("/totalOwed")).value.contains(Dollar("16333")))

  //   assert(graph.get(Path("/withholdingGap")).value.contains(Dollar("2633")))
  //   assert(graph.get(Path("/jobSelectedForExtraWithholding/w4Line4c")).value.contains(Dollar("280")))
  // }

  test("Verifying single pension manual calculations from Pub 15T Worksheet 1B") {
    val graph = makeGraphWith(
      factDictionary,
      filingStatus -> single,
      pensions -> pensionsCollection,
      jobs -> Collection(Vector()),
      Path("/primaryFilerAge65OrOlder") -> false,
      Path("/primaryFilerIsBlind") -> false,
      Path("/primaryFilerIsClaimedOnAnotherReturn") -> false,
      Path(s"/pensions/#${pension1Id}/averagePayPerPayPeriod") -> Dollar("5000"),
      Path(s"/pensions/#${pension1Id}/averageWithholdingPerPayPeriod") -> Dollar("400"),
      Path(s"/pensions/#${pension1Id}/yearToDateIncome") -> Dollar("40000"),
      Path(s"/pensions/#${pension1Id}/yearToDateWithholding") -> Dollar("3200"),
      Path(s"/pensions/#${pension1Id}/payFrequency") -> Enum("monthly", "/payFrequencyOptions"),
      Path("/wantsStandardDeduction") -> true,
      Path("/totalEstimatedTaxesPaid") -> Dollar("0"),
      Path("/netSelfEmploymentIncomeTotal") -> Dollar("0"),

      // Derived overrides
      Path("/adjustmentsToIncome") -> Dollar("0"),
      Path("/otherIncomeTotal") -> Dollar("0"),
      Path("/totalCredits") -> Dollar("0"),
      Path(s"/pensions/#${pension1Id}/remainingPayDates") -> 4,
      Path("/nonItemizerCharitableContributionDeductionAmount") -> Dollar("0"),
      // Don't matters
      Path(s"/pensions/#${pension1Id}/startDate") -> Day("2025-01-01"),
      Path(s"/pensions/#${pension1Id}/endDate") -> Day("2025-10-15"),
    )

    assert(graph.get(Path(s"/pensions/#${pension1Id}/income")).value.contains(Dollar("60000")))
    assert(graph.get(Path(s"/pensions/#${pension1Id}/w4pLine4a")).value.contains(Dollar("0")))
    assert(graph.get(Path(s"/pensions/#${pension1Id}/w4pLine4b")).value.contains(Dollar("0")))
    assert(graph.get(Path(s"/pensions/#${pension1Id}/pub15Worksheet1bLine1g")).value.contains(Dollar("8600")))

    assert(graph.get(Path(s"/pensions/#${pension1Id}/adjustedAnnualPaymentAmount")).value.contains(Dollar("51400")))
    assert(graph.get(Path(s"/pensions/#${pension1Id}/pub15Worksheet1bLine2cOr2d")).value.contains(Dollar("51400")))
    assert(graph.get(Path(s"/pensions/#${pension1Id}/pub15Worksheet1bLine2j")).value.contains(Dollar("5162")))
    assert(graph.get(Path(s"/pensions/#${pension1Id}/tentativeWithholdingAmount")).value.contains(Dollar("430.17")))

    assert(graph.get(Path("/pensionsIncomeTotal")).value.contains(Dollar("60000")))
    assert(graph.get(Path("/taxableIncome")).value.contains(Dollar("44250")))
    assert(graph.get(Path("/roundedTaxableIncome")).value.contains(Dollar("44275")))
    assert(graph.get(Path("/tentativeTaxFromTaxableIncome")).value.contains(Dollar("5075")))
    assert(graph.get(Path("/withholdingGap")).value.contains(Dollar("275")))

//    assert(graph.get(Path(s"/pensions/#${pension1Id}/w4pLine4cRecommendation")).value.contains(Dollar("39")))

  }

  test("Verifying two pension manual calculations from Pub 15T Worksheet 1B") {
    val graph = makeGraphWith(
      factDictionary,
      filingStatus -> single,
      pensions -> twoPensionsCollection,
      jobs -> Collection(Vector()),
      Path("/primaryFilerAge65OrOlder") -> false,
      Path("/primaryFilerIsBlind") -> false,
      Path("/primaryFilerIsClaimedOnAnotherReturn") -> false,
      Path(s"/pensions/#${pension1Id}/averagePayPerPayPeriod") -> Dollar("5000"),
      Path(s"/pensions/#${pension1Id}/averageWithholdingPerPayPeriod") -> Dollar("400"),
      Path(s"/pensions/#${pension1Id}/yearToDateIncome") -> Dollar("40000"),
      Path(s"/pensions/#${pension1Id}/yearToDateWithholding") -> Dollar("3200"),
      Path(s"/pensions/#${pension1Id}/payFrequency") -> Enum("monthly", "/payFrequencyOptions"),
      Path(s"/pensions/#${pension2Id}/averagePayPerPayPeriod") -> Dollar("5000"),
      Path(s"/pensions/#${pension2Id}/averageWithholdingPerPayPeriod") -> Dollar("400"),
      Path(s"/pensions/#${pension2Id}/yearToDateIncome") -> Dollar("40000"),
      Path(s"/pensions/#${pension2Id}/yearToDateWithholding") -> Dollar("3200"),
      Path(s"/pensions/#${pension2Id}/payFrequency") -> Enum("monthly", "/payFrequencyOptions"),
      Path("/wantsStandardDeduction") -> true,
      Path("/totalEstimatedTaxesPaid") -> Dollar("0"),
      Path("/netSelfEmploymentIncomeTotal") -> Dollar("0"),

      // Derived overrides
      Path("/adjustmentsToIncome") -> Dollar("0"),
      Path("/otherIncomeTotal") -> Dollar("0"),
      Path("/totalCredits") -> Dollar("0"),
      Path(s"/pensions/#${pension1Id}/remainingPayDates") -> 4,
      Path(s"/pensions/#${pension2Id}/remainingPayDates") -> 4,
      Path("/nonItemizerCharitableContributionDeductionAmount") -> Dollar("0"),

      // Don't matters
      Path(s"/pensions/#${pension1Id}/startDate") -> Day("2025-01-01"),
      Path(s"/pensions/#${pension1Id}/endDate") -> Day("2025-10-15"),
      Path(s"/pensions/#${pension2Id}/startDate") -> Day("2025-01-01"),
      Path(s"/pensions/#${pension2Id}/endDate") -> Day("2025-10-15"),
    )

    assert(graph.get(Path(s"/pensions/#${pension1Id}/income")).value.contains(Dollar("60000")))
    assert(graph.get(Path(s"/pensions/#${pension1Id}/w4pLine4a")).value.contains(Dollar("0")))
    assert(graph.get(Path(s"/pensions/#${pension1Id}/w4pLine4b")).value.contains(Dollar("0")))
    assert(graph.get(Path(s"/pensions/#${pension1Id}/pub15Worksheet1bLine1g")).value.contains(Dollar("8600")))

    assert(graph.get(Path(s"/pensions/#${pension1Id}/adjustedAnnualPaymentAmount")).value.contains(Dollar("51400")))
    assert(graph.get(Path(s"/pensions/#${pension1Id}/pub15Worksheet1bLine2cOr2d")).value.contains(Dollar("51400")))
    assert(graph.get(Path(s"/pensions/#${pension1Id}/pub15Worksheet1bLine2j")).value.contains(Dollar("5162")))
    assert(graph.get(Path(s"/pensions/#${pension1Id}/tentativeWithholdingAmount")).value.contains(Dollar("430.17")))

    assert(graph.get(Path("/pensionsIncomeTotal")).value.contains(Dollar("120000")))
    assert(graph.get(Path("/taxableIncome")).value.contains(Dollar("104250")))
    assert(graph.get(Path("/roundedTaxableIncome")).value.contains(Dollar("104250")))
    assert(graph.get(Path("/tentativeTaxFromTaxableIncome")).value.contains(Dollar("17867")))
    assert(graph.get(Path("/pensionsTotalEndOfYearProjectedWithholding")).value.contains(Dollar("9600")))
    assert(graph.get(Path("/withholdingGap")).value.contains(Dollar("8267")))

    assert(graph.get(Path(s"/pensions/#${pension2Id}/tentativeWithholdingAmount")).value.contains(Dollar("430.17")))
//    assert(graph.get(Path("/totalCommittedWithholding")).value.contains(Dollar("9841.36")))
//    assert(graph.get(Path(s"/pensions/#${pension1Id}/w4pLine4cRecommendation")).value.contains(Dollar("2006")))

  }

  test("Verifying single pension with job income manual calculations from Pub 15T Worksheet 1B") {
    val graph = makeGraphWith(
      factDictionary,
      filingStatus -> single,
      pensions -> pensionsCollection,
      jobs -> Collection(Vector()),
      Path("/primaryFilerAge65OrOlder") -> false,
      Path("/primaryFilerIsBlind") -> false,
      Path("/primaryFilerIsClaimedOnAnotherReturn") -> false,
      Path(s"/pensions/#${pension1Id}/w4pLine2bi") -> Dollar("50000.00"),
      Path(s"/pensions/#${pension1Id}/averagePayPerPayPeriod") -> Dollar("5000"),
      Path(s"/pensions/#${pension1Id}/averageWithholdingPerPayPeriod") -> Dollar("1000"),
      Path(s"/pensions/#${pension1Id}/yearToDateWithholding") -> Dollar("7200"),
      Path(s"/pensions/#${pension1Id}/payFrequency") -> Enum("monthly", "/payFrequencyOptions"),
      Path(s"/pensions/#${pension1Id}/income") -> Dollar("60000"),
      Path("/totalEstimatedTaxesPaid") -> Dollar("0"),
      Path("/netSelfEmploymentIncomeTotal") -> Dollar("0"),

      // Derived overrides
      Path("/adjustmentsToIncome") -> Dollar("0"),
      Path("/otherIncomeTotal") -> Dollar("0"),
      Path("/totalCredits") -> Dollar("0"),
      Path(s"/pensions/#${pension1Id}/remainingPayDates") -> 8,
      // Don't matters
      Path(s"/pensions/#${pension1Id}/startDate") -> Day("2025-01-01"),
      Path(s"/pensions/#${pension1Id}/endDate") -> Day("2025-10-15"),
    )
    assert(graph.get(Path(s"/pensions/#${pension1Id}/adjustedAnnualPaymentAmount")).value.contains(Dollar("51400")))
    assert(graph.get(Path(s"/pensions/#${pension1Id}/pub15Worksheet1bLine2cOr2d")).value.contains(Dollar("51400")))
    assert(graph.get(Path(s"/pensions/#${pension1Id}/pub15Worksheet1bLine2j")).value.contains(Dollar("5162")))
    assert(graph.get(Path(s"/pensions/#${pension1Id}/tentativeWithholdingAmount")).value.contains(Dollar("430.17")))

    assert(graph.get(Path(s"/pensions/#${pension1Id}/committedWithholding")).value.contains(Dollar("10641.36")))
//    assert(graph.get(Path(s"/pensions/#${pension1Id}/w4pLine4cRecommendation")).value.contains(Dollar("0")))
  }

  test("Verifying future job income calculations") {
    val graph = makeGraphWith(
      factDictionary,
      filingStatus -> single,
      Path("/primaryFilerAge65OrOlder") -> false,
      Path("/primaryFilerIsBlind") -> false,
      Path("/primaryFilerIsClaimedOnAnotherReturn") -> false,
      jobs -> jobsCollection,
      Path(s"/jobs/#${job1Id}/isAllYear") -> false,
      Path(s"/jobs/#${job1Id}/writableStartDate") -> Day("2025-01-01"),
      Path(s"/jobs/#${job1Id}/writableEndDate") -> Day("2025-10-15"),
      Path(s"/jobs/#${job1Id}/payFrequency") -> Enum("monthly", "/payFrequencyOptions"),
      Path(s"/jobs/#${job1Id}/mostRecentPayPeriodEnd") -> Day("2025-09-30"),
      Path(s"/jobs/#${job1Id}/mostRecentPayDate") -> Day("2025-10-05"),
      Path(s"/jobs/#${job1Id}/averagePayPerPayPeriod") -> Dollar("4000"),
      Path(s"/jobs/#${job1Id}/yearToDateIncome") -> Dollar("40000"),
      Path(s"/jobs/#${job1Id}/averageWithholdingPerPayPeriod") -> Dollar("200"),
      Path(s"/jobs/#${job1Id}/yearToDateWithholding") -> Dollar("2000"),
      Path(s"/jobs/#${job1Id}/totalBonusReceived") -> Dollar("0"),
      Path(s"/jobs/#${job2Id}/isAllYear") -> true,
      Path(s"/jobs/#${job2Id}/payFrequency") -> Enum("monthly", "/payFrequencyOptions"),
      Path(s"/jobs/#${job2Id}/mostRecentPayPeriodEnd") -> Day("2025-09-30"),
      Path(s"/jobs/#${job2Id}/mostRecentPayDate") -> Day("2025-09-30"),
      Path(s"/jobs/#${job2Id}/averagePayPerPayPeriod") -> Dollar("2000"),
      Path(s"/jobs/#${job2Id}/yearToDateIncome") -> Dollar("18000"),
      Path(s"/jobs/#${job2Id}/averageWithholdingPerPayPeriod") -> Dollar("100"),
      Path(s"/jobs/#${job2Id}/yearToDateWithholding") -> Dollar("900"),
      Path(s"/jobs/#${job2Id}/totalBonusReceived") -> Dollar("0"),
      Path("/totalCtcAndOdc") -> Dollar("2000"),
      // Derived overrides
      Path(s"/jobs/#${job1Id}/preTaxDeductions") -> Dollar("0"),
      Path(s"/jobs/#${job2Id}/preTaxDeductions") -> Dollar("0"),
      Path("/adjustmentsToIncome") -> Dollar("0"),
      Path("/otherIncomeTotal") -> Dollar("0"),
      Path(s"/jobs/#${job1Id}/isPastJob") -> false,
      Path(s"/jobs/#${job1Id}/isCurrentJob") -> true,
      Path(s"/jobs/#${job1Id}/isFutureJob") -> false,
      Path(s"/jobs/#${job2Id}/isPastJob") -> false,
      Path(s"/jobs/#${job2Id}/isCurrentJob") -> true,
      Path(s"/jobs/#${job2Id}/isFutureJob") -> false,
    )
  }

  // This test validates some outputs.
  // Then I learned that the scenario involves self-employment tax and QBI, which we have not yet implemented.
  // We can return to this test when we have.
  // test("2197 Scenarios spreadsheet Column O") {
  //   val graph = makeGraphWith(
  //     factDictionary,
  //     filingStatus -> Enum("marriedFilingJointly", "/filingStatusOptions"),
  //     Path("/primaryFilerDateOfBirth") -> Day("1985-01-28"),
  //     Path("/primaryFilerIsBlind") -> false,
  //     Path("/primaryFilerIsClaimedOnAnotherReturn") -> false,
  //     Path("/secondaryFilerDateOfBirth") -> Day("1985-02-28"),
  //     Path("/secondaryFilerIsBlind") -> false,
  //     Path("/secondaryFilerIsClaimedOnAnotherReturn") -> false,
  //     jobs -> jobsCollection,
  //     Path(s"/jobs/#${job1Id}/startDate") -> Day("2025-01-01"),
  //     Path(s"/jobs/#${job1Id}/endDate") -> Day("2025-10-15"),
  //     Path(s"/jobs/#${job1Id}/mostRecentPayPeriodEnd") -> Day("2025-10-05"),
  //     Path(s"/jobs/#${job1Id}/mostRecentPayDate") -> Day("2025-10-05"),
  //     Path(s"/jobs/#${job1Id}/averagePayPerPayPeriod") -> Dollar("2000"),
  //     Path(s"/jobs/#${job1Id}/yearToDateIncome") -> Dollar("80000"),
  //     Path(s"/jobs/#${job1Id}/payFrequency") -> Enum("weekly", "/payFrequencyOptions"),
  //     Path(s"/jobs/#${job1Id}/averageWithholdingPerPayPeriod") -> Dollar("100"),
  //     Path(s"/jobs/#${job1Id}/yearToDateWithholding") -> Dollar("4000"),
  //     Path(s"/jobs/#${job1Id}/totalBonusReceived") -> Dollar("0"),
  //     Path(s"/jobs/#${job1Id}/preTaxDeductions") -> Dollar("0"),
  //     Path(s"/jobs/#${job2Id}/startDate") -> Day("2025-01-01"),
  //     Path(s"/jobs/#${job2Id}/endDate") -> Day("2025-12-31"),
  //     Path(s"/jobs/#${job2Id}/mostRecentPayPeriodEnd") -> Day("2025-10-02"),
  //     Path(s"/jobs/#${job2Id}/mostRecentPayDate") -> Day("2025-10-05"),
  //     Path(s"/jobs/#${job2Id}/averagePayPerPayPeriod") -> Dollar("1000"),
  //     Path(s"/jobs/#${job2Id}/yearToDateIncome") -> Dollar("40000"),
  //     Path(s"/jobs/#${job2Id}/payFrequency") -> Enum("weekly", "/payFrequencyOptions"),
  //     Path(s"/jobs/#${job2Id}/averageWithholdingPerPayPeriod") -> Dollar("100"),
  //     Path(s"/jobs/#${job2Id}/yearToDateWithholding") -> Dollar("4000"),
  //     Path(s"/jobs/#${job2Id}/totalBonusReceived") -> Dollar("0"),
  //     Path(s"/jobs/#${job2Id}/preTaxDeductions") -> Dollar("0"),
  //     Path("/totalCtcAndOdc") -> Dollar("4000"),
  //     Path("/qualifiedBusinessIncomeDeduction") -> Dollar("1302"),
  //     // Derived overrides
  //     Path("/totalNonJobsIncome") -> Dollar("5204"), // spreadsheet row 248
  //     Path("/adjustmentsToIncome") -> Dollar("495"), // row 275
  //     Path("/otherIncomeTotal") -> Dollar("7000"),
  //   )

  //   if (graph.get(Path("/usePreOb3StandardDeduction")).value.contains(true)) {
  //     cancel("Skipping test based on OB3 standard deduction numbers because you are using pre-OB3 facts")
  //   }

  //   // assert(graph.get(Path(s"/jobs/#${job1Id}/w4Line4a")).value.contains(Dollar("0")))
  //   // assert(graph.get(Path(s"/jobs/#${job2Id}/w4Line4a")).value.contains(Dollar("5204")))

  //   assert(graph.get(Path(s"/jobs/#${job1Id}/income")).value.contains(Dollar("82857.14")))
  //   assert(graph.get(Path(s"/jobs/#${job2Id}/income")).value.contains(Dollar("52000")))

  //   assert(graph.get(Path(s"/jobs/#${job1Id}/endOfYearProjectedWithholding")).value.contains(Dollar("4100")))
  //   assert(graph.get(Path(s"/jobs/#${job2Id}/endOfYearProjectedWithholding")).value.contains(Dollar("5200")))
  //   assert(graph.get(Path(s"/jobs/#${job1Id}/tentativeWithholdingAmount")).value.contains(Dollar("161.60")))
  //   // TODO: Investigate if we should use this value or 53.61, maybe both?
  //   assert(graph.get(Path(s"/jobs/#${job2Id}/tentativeWithholdingAmount")).value.contains(Dollar("42.31")))
  //   assert(graph.get(Path("/totalEndOfYearProjectedWithholding")).value.contains(Dollar("9300")))

  //   assert(graph.get(Path("/agi")).value.contains(Dollar("141362")))
  //   assert(graph.get(Path("/tentativeTaxFromTaxableIncome")).value.contains(Dollar("14041")))
  //   assert(graph.get(Path("/totalOwed")).value.contains(Dollar("10041")))
  //   assert(graph.get(Path("/withholdingGap")).value.contains(Dollar("741")))
  //   assert(graph.get(Path("/jobSelectedForExtraWithholding/w4Line4c")).value.contains(Dollar("122")))
  // }

  // These following scenarios are for scenarios where we need to reduce withholding
  // Right now they are only applied to a singular job, this will be updated as part of https://github.com/IRSDigitalService/tax-withholding-estimator/issues/841
  test("2200 Scenarios spreadsheet Column B") {
    val graph = makeGraphWith(
      factDictionary,
      filingStatus -> Enum("single", "/filingStatusOptions"),
      Path("/primaryFilerAge65OrOlder") -> false,
      Path("/primaryFilerIsBlind") -> false,
      Path("/primaryFilerIsClaimedOnAnotherReturn") -> false,
      Path("/totalEstimatedTaxesPaid") -> Dollar("10000"),
      Path("/netSelfEmploymentIncomeTotal") -> Dollar("0"),
      jobs -> jobsCollection,
      Path(s"/jobs/#${job1Id}/isAllYear") -> true,
      Path(s"/jobs/#${job1Id}/payFrequency") -> Enum("monthly", "/payFrequencyOptions"),
      Path(s"/jobs/#${job1Id}/mostRecentPayPeriodEnd") -> Day("2025-04-30"),
      Path(s"/jobs/#${job1Id}/mostRecentPayDate") -> Day("2025-04-30"),
      Path(s"/jobs/#${job1Id}/averagePayPerPayPeriod") -> Dollar("5000"),
      Path(s"/jobs/#${job1Id}/yearToDateIncome") -> Dollar("20000"),
      Path(s"/jobs/#${job1Id}/averageWithholdingPerPayPeriod") -> Dollar("1000"),
      Path(s"/jobs/#${job1Id}/yearToDateWithholding") -> Dollar("4000"),
      Path(s"/jobs/#${job1Id}/totalBonusReceived") -> Dollar("0"),
      Path(s"/jobs/#${job2Id}/isAllYear") -> true,
      Path(s"/jobs/#${job2Id}/payFrequency") -> Enum("biWeekly", "/payFrequencyOptions"),
      Path(s"/jobs/#${job2Id}/mostRecentPayPeriodEnd") -> Day("2025-05-10"),
      Path(s"/jobs/#${job2Id}/mostRecentPayDate") -> Day("2025-05-10"),
      Path(s"/jobs/#${job2Id}/averagePayPerPayPeriod") -> Dollar("1000"),
      Path(s"/jobs/#${job2Id}/yearToDateIncome") -> Dollar("10000"),
      Path(s"/jobs/#${job2Id}/averageWithholdingPerPayPeriod") -> Dollar("300"),
      Path(s"/jobs/#${job2Id}/yearToDateWithholding") -> Dollar("3000"),
      Path(s"/jobs/#${job2Id}/totalBonusReceived") -> Dollar("0"),
      // Derived overrides
      Path(s"/jobs/#${job1Id}/preTaxDeductions") -> Dollar("0"),
      Path(s"/jobs/#${job2Id}/preTaxDeductions") -> Dollar("0"),
      Path("/adjustmentsToIncome") -> Dollar("0"),
      Path("/otherIncomeTotal") -> Dollar("20000"),
      Path("/otherIncomeTotal") -> Dollar("20000"),
      Path("/nonItemizerCharitableContributionDeductionAmount") -> Dollar("0"),
    )

    assert(graph.get(Path(s"/jobs/#${job1Id}/income")).value.contains(Dollar("60000")))
    assert(graph.get(Path(s"/jobs/#${job2Id}/income")).value.contains(Dollar("26000")))

    assert(graph.get(Path(s"/jobs/#${job1Id}/endOfYearProjectedWithholding")).value.contains(Dollar("12000")))
    assert(graph.get(Path(s"/jobs/#${job2Id}/endOfYearProjectedWithholding")).value.contains(Dollar("7800")))
    assert(graph.get(Path("/totalEndOfYearProjectedWithholding")).value.contains(Dollar("19800")))

    assert(graph.get(Path(s"/jobs/#${job1Id}/standardAnnualWithholdingAmount")).value.contains(Dollar("5161.50")))
    // 430.13 is expected but since we are using banker's rounding and the value is 430.125 we round down
    assert(graph.get(Path(s"/jobs/#${job1Id}/tentativeWithholdingAmount")).value.contains(Dollar("430.12")))
    assert(graph.get(Path(s"/jobs/#${job2Id}/tentativeWithholdingAmount")).value.contains(Dollar("42.31")))

    assert(graph.get(Path("/agi")).value.contains(Dollar("106000")))

    if (graph.get(Path("/usePreOb3StandardDeduction")).value.contains(true)) {
      // These are the original values from the QA spreadsheet.
      assert(graph.get(Path("/tentativeTaxFromTaxableIncome")).value.contains(Dollar("14940")))
      assert(graph.get(Path("/totalOwed")).value.contains(Dollar("4940")))
      assert(graph.get(Path("/withholdingGap")).value.contains(Dollar("-14860")))
      assert(graph.get(Path("/jobSelectedForExtraWithholding/w4Line4c")).value.contains(Dollar("0")))
      // Rounding from tentativeWithholdingAmount causes this to differ from the expected 5162
      assert(graph.get(Path("/jobSelectedForExtraWithholding/w4Line3")).value.contains(Dollar("5161")))
    } else {
      // The following values now differ from the QA spreadsheet due to standard deduction changes.
      assert(graph.get(Path("/tentativeTaxFromTaxableIncome")).value.contains(Dollar("14775")))
      assert(graph.get(Path("/totalOwed")).value.contains(Dollar("4775")))
      assert(graph.get(Path("/withholdingGap")).value.contains(Dollar("-15025")))
      assert(graph.get(Path("/jobSelectedForExtraWithholding/w4Line4c")).value.contains(Dollar("0")))
      assert(graph.get(Path("/jobSelectedForExtraWithholding/w4Line3")).value.contains(Dollar("5161")))
    }
  }

  test("2200 Scenarios spreadsheet Column D") {
    val graph = makeGraphWith(
      factDictionary,
      filingStatus -> Enum("marriedFilingJointly", "/filingStatusOptions"),
      Path("/primaryFilerAge65OrOlder") -> false,
      Path("/primaryFilerIsBlind") -> false,
      Path("/primaryFilerIsClaimedOnAnotherReturn") -> false,
      Path("/secondaryFilerAge65OrOlder") -> false,
      Path("/secondaryFilerIsBlind") -> false,
      Path("/secondaryFilerIsClaimedOnAnotherReturn") -> false,
      jobs -> jobsCollection,
      Path(s"/jobs/#${job1Id}/isAllYear") -> true,
      Path(s"/jobs/#${job1Id}/payFrequency") -> Enum("monthly", "/payFrequencyOptions"),
      Path(s"/jobs/#${job1Id}/mostRecentPayPeriodEnd") -> Day("2025-04-30"),
      Path(s"/jobs/#${job1Id}/mostRecentPayDate") -> Day("2025-04-30"),
      Path(s"/jobs/#${job1Id}/averagePayPerPayPeriod") -> Dollar("4000"),
      Path(s"/jobs/#${job1Id}/yearToDateIncome") -> Dollar("16000"),
      Path(s"/jobs/#${job1Id}/averageWithholdingPerPayPeriod") -> Dollar("500"),
      Path(s"/jobs/#${job1Id}/yearToDateWithholding") -> Dollar("2000"),
      Path(s"/jobs/#${job1Id}/totalBonusReceived") -> Dollar("0"),
      Path(s"/jobs/#${job1Id}/filerAssignment") -> Enum("self", "/filerAssignment"),
      Path(s"/jobs/#${job1Id}/overtimeCompensation") -> Dollar("0"),
      Path(s"/jobs/#${job1Id}/qualifiedTipIncome") -> Dollar("0"),
      Path(s"/jobs/#${job2Id}/isAllYear") -> true,
      Path(s"/jobs/#${job2Id}/payFrequency") -> Enum("biWeekly", "/payFrequencyOptions"),
      Path(s"/jobs/#${job2Id}/mostRecentPayPeriodEnd") -> Day("2025-05-10"),
      Path(s"/jobs/#${job2Id}/mostRecentPayDate") -> Day("2025-05-10"),
      Path(s"/jobs/#${job2Id}/averagePayPerPayPeriod") -> Dollar("1000"),
      Path(s"/jobs/#${job2Id}/yearToDateIncome") -> Dollar("10000"),
      Path(s"/jobs/#${job2Id}/averageWithholdingPerPayPeriod") -> Dollar("300"),
      Path(s"/jobs/#${job2Id}/yearToDateWithholding") -> Dollar("3000"),
      Path(s"/jobs/#${job2Id}/totalBonusReceived") -> Dollar("0"),
      Path(s"/jobs/#${job2Id}/filerAssignment") -> Enum("spouse", "/filerAssignment"),
      Path(s"/jobs/#${job2Id}/overtimeCompensation") -> Dollar("0"),
      Path(s"/jobs/#${job2Id}/qualifiedTipIncome") -> Dollar("0"),
      Path("/totalEstimatedTaxesPaid") -> Dollar("0"),
      Path("/netSelfEmploymentIncomeTotal") -> Dollar("0"),
      // Derived overrides
      Path(s"/jobs/#${job1Id}/preTaxDeductions") -> Dollar("0"),
      Path(s"/jobs/#${job2Id}/preTaxDeductions") -> Dollar("0"),
      Path("/adjustmentsToIncome") -> Dollar("0"),
      Path("/otherIncomeTotal") -> Dollar("0"),
      Path("/nonItemizerCharitableContributionDeductionAmount") -> Dollar("0"),
    )

    assert(graph.get(Path(s"/jobs/#${job1Id}/income")).value.contains(Dollar("48000")))
    assert(graph.get(Path(s"/jobs/#${job2Id}/income")).value.contains(Dollar("26000")))

    assert(graph.get(Path(s"/jobs/#${job1Id}/endOfYearProjectedWithholding")).value.contains(Dollar("6000")))
    assert(graph.get(Path(s"/jobs/#${job2Id}/endOfYearProjectedWithholding")).value.contains(Dollar("7800")))
    assert(graph.get(Path("/totalEndOfYearProjectedWithholding")).value.contains(Dollar("13800")))

    assert(graph.get(Path(s"/jobs/#${job1Id}/standardAnnualWithholdingAmount")).value.contains(Dollar("1800")))
    assert(graph.get(Path(s"/jobs/#${job1Id}/tentativeWithholdingAmount")).value.contains(Dollar("150")))
    assert(graph.get(Path(s"/jobs/#${job2Id}/tentativeWithholdingAmount")).value.contains(Dollar("0")))

    assert(graph.get(Path("/agi")).value.contains(Dollar("74000")))

    if (graph.get(Path("/usePreOb3StandardDeduction")).value.contains(true)) {
      // These are the original values from the QA spreadsheet.
      assert(graph.get(Path("/tentativeTaxFromTaxableIncome")).value.contains(Dollar("4806")))
      assert(graph.get(Path("/totalOwed")).value.contains(Dollar("4806")))
      assert(graph.get(Path("/withholdingGap")).value.contains(Dollar("-8994")))
      assert(graph.get(Path("/jobSelectedForExtraWithholding/w4Line4c")).value.contains(Dollar("0")))
      assert(graph.get(Path("/jobSelectedForExtraWithholding/w4Line3")).value.contains(Dollar("1800")))
    } else {
      // The following values now differ from the QA spreadsheet due to standard deduction changes.
      assert(graph.get(Path("/tentativeTaxFromTaxableIncome")).value.contains(Dollar("4626")))
      assert(graph.get(Path("/totalOwed")).value.contains(Dollar("4626")))
      assert(graph.get(Path("/withholdingGap")).value.contains(Dollar("-9174")))
      assert(graph.get(Path("/jobSelectedForExtraWithholding/w4Line4c")).value.contains(Dollar("0")))
      assert(graph.get(Path("/jobSelectedForExtraWithholding/w4Line3")).value.contains(Dollar("1800")))
    }
  }

  test("when you need to reduce withholding but haven't already paid enough for the year") {
    val graph = makeGraphWith(
      factDictionary,
      filingStatus -> Enum("single", "/filingStatusOptions"),
      Path("/primaryFilerAge65OrOlder") -> false,
      Path("/primaryFilerIsBlind") -> false,
      Path("/primaryFilerIsClaimedOnAnotherReturn") -> false,
      jobs -> jobsCollection,
      Path(s"/jobs/#${job1Id}/isAllYear") -> true,
      Path(s"/jobs/#${job1Id}/payFrequency") -> Enum("monthly", "/payFrequencyOptions"),
      Path(s"/jobs/#${job1Id}/mostRecentPayPeriodEnd") -> Day("2025-04-30"),
      Path(s"/jobs/#${job1Id}/mostRecentPayDate") -> Day("2025-04-30"),
      Path(s"/jobs/#${job1Id}/averagePayPerPayPeriod") -> Dollar("10000"),
      Path(s"/jobs/#${job1Id}/yearToDateIncome") -> Dollar("40000"),
      Path(s"/jobs/#${job1Id}/averageWithholdingPerPayPeriod") -> Dollar("3000"),
      Path(s"/jobs/#${job1Id}/yearToDateWithholding") -> Dollar("12000"),
      Path(s"/jobs/#${job1Id}/totalBonusReceived") -> Dollar("0"),
      Path(s"/jobs/#${job2Id}/isAllYear") -> true,
      Path(s"/jobs/#${job2Id}/payFrequency") -> Enum("biWeekly", "/payFrequencyOptions"),
      Path(s"/jobs/#${job2Id}/mostRecentPayPeriodEnd") -> Day("2025-05-10"),
      Path(s"/jobs/#${job2Id}/mostRecentPayDate") -> Day("2025-05-10"),
      Path(s"/jobs/#${job2Id}/averagePayPerPayPeriod") -> Dollar("1000"),
      Path(s"/jobs/#${job2Id}/yearToDateIncome") -> Dollar("10000"),
      Path(s"/jobs/#${job2Id}/averageWithholdingPerPayPeriod") -> Dollar("300"),
      Path(s"/jobs/#${job2Id}/yearToDateWithholding") -> Dollar("3000"),
      Path(s"/jobs/#${job2Id}/totalBonusReceived") -> Dollar("0"),
      Path("/totalEstimatedTaxesPaid") -> Dollar("0"),
      Path("/netSelfEmploymentIncomeTotal") -> Dollar("0"),
      // Derived overrides
      Path(s"/jobs/#${job1Id}/preTaxDeductions") -> Dollar("0"),
      Path(s"/jobs/#${job2Id}/preTaxDeductions") -> Dollar("0"),
      Path("/adjustmentsToIncome") -> Dollar("0"),
      Path("/otherIncomeTotal") -> Dollar("0"),
      Path("/nonItemizerCharitableContributionDeductionAmount") -> Dollar("0"),
    )

    assert(graph.get(Path(s"/jobs/#${job1Id}/income")).value.contains(Dollar("120000")))
    assert(graph.get(Path(s"/jobs/#${job2Id}/income")).value.contains(Dollar("26000")))

    assert(graph.get(Path(s"/jobs/#${job1Id}/endOfYearProjectedWithholding")).value.contains(Dollar("36000")))
    assert(graph.get(Path(s"/jobs/#${job2Id}/endOfYearProjectedWithholding")).value.contains(Dollar("7800")))
    assert(graph.get(Path("/totalEndOfYearProjectedWithholding")).value.contains(Dollar("43800")))

    assert(graph.get(Path(s"/jobs/#${job1Id}/standardAnnualWithholdingAmount")).value.contains(Dollar("18047")))
    assert(graph.get(Path(s"/jobs/#${job1Id}/tentativeWithholdingAmount")).value.contains(Dollar("1503.92")))
    assert(graph.get(Path(s"/jobs/#${job2Id}/tentativeWithholdingAmount")).value.contains(Dollar("42.31")))

    assert(graph.get(Path("/agi")).value.contains(Dollar("146000")))

    if (graph.get(Path("/usePreOb3StandardDeduction")).value.contains(true)) {
      // These are only included in case we wanted to compare it with the older numbers
      assert(graph.get(Path("/tentativeTaxFromTaxableIncome")).value.contains(Dollar("24287")))
      assert(graph.get(Path("/totalOwed")).value.contains(Dollar("24287")))
      assert(graph.get(Path("/withholdingGap")).value.contains(Dollar("-19513")))
      assert(graph.get(Path("/taxGap")).value.contains(Dollar("-3679.01")))
      assert(graph.get(Path("/jobSelectedForExtraWithholding/w4Line4c")).value.contains(Dollar("0")))
      // There are some roundings for intermediate values that cause this to be 5519 instead of 5518
      assert(graph.get(Path("/jobSelectedForExtraWithholding/w4Line3")).value.contains(Dollar("5519")))
    } else {
      // The following values now differ from the QA spreadsheet due to standard deduction changes.
      assert(graph.get(Path("/tentativeTaxFromTaxableIncome")).value.contains(Dollar("24107")))
      assert(graph.get(Path("/totalOwed")).value.contains(Dollar("24107")))
      assert(graph.get(Path("/withholdingGap")).value.contains(Dollar("-19693")))
      assert(graph.get(Path("/taxGap")).value.contains(Dollar("-3859.01")))
      assert(graph.get(Path("/jobSelectedForExtraWithholding/w4Line4c")).value.contains(Dollar("0")))
      assert(graph.get(Path("/jobSelectedForExtraWithholding/w4Line3")).value.contains(Dollar("5789")))
    }
  }
}

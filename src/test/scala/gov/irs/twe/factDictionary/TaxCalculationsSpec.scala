package gov.irs.twe.factDictionary

import gov.irs.factgraph.types.Collection
import gov.irs.factgraph.types.Day
import gov.irs.factgraph.types.Dollar
import gov.irs.factgraph.types.Enum
import gov.irs.factgraph.FactDictionaryForTests
import gov.irs.factgraph.Path
import gov.irs.twe.FileLoaderHelper
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.prop.TableDrivenPropertyChecks

class TaxCalculationsSpec extends AnyFunSuite with TableDrivenPropertyChecks {
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

  // These tests are based on the "2197 Scenarios checks" worksheet of the "TWESprint1_2025_UAT_WHC2197_2200" spreadsheet.
  test("2197 Scenarios spreadsheet Column C") {
    val graph = makeGraphWith(
      factDictionary,
      filingStatus -> single,
      Path("/primaryFilerDateOfBirth") -> Day("1985-01-28"),
      Path("/primaryFilerIsBlind") -> false,
      Path("/primaryFilerIsClaimedOnAnotherReturn") -> false,
      jobs -> jobsCollection,
      Path(s"/jobs/#${job1Id}/startDate") -> Day("2025-01-01"),
      Path(s"/jobs/#${job1Id}/endDate") -> Day("2025-10-15"),
      Path(s"/jobs/#${job1Id}/payFrequency") -> Enum("monthly", "/payFrequencyOptions"),
      Path(s"/jobs/#${job1Id}/mostRecentPayPeriodEnd") -> Day("2025-09-30"),
      Path(s"/jobs/#${job1Id}/mostRecentPayDate") -> Day("2025-10-05"),
      Path(s"/jobs/#${job1Id}/averagePayPerPayPeriod") -> Dollar("4000"),
      Path(s"/jobs/#${job1Id}/yearToDateIncome") -> Dollar("40000"),
      Path(s"/jobs/#${job1Id}/averageWithholdingPerPayPeriod") -> Dollar("200"),
      Path(s"/jobs/#${job1Id}/yearToDateWithholding") -> Dollar("2000"),
      Path(s"/jobs/#${job1Id}/totalBonusReceived") -> Dollar("0"),
      Path(s"/jobs/#${job1Id}/preTaxDeductions") -> Dollar("0"),
      Path(s"/jobs/#${job2Id}/startDate") -> Day("2025-01-01"),
      Path(s"/jobs/#${job2Id}/endDate") -> Day("2025-12-31"),
      Path(s"/jobs/#${job2Id}/payFrequency") -> Enum("monthly", "/payFrequencyOptions"),
      Path(s"/jobs/#${job2Id}/mostRecentPayPeriodEnd") -> Day("2025-09-30"),
      Path(s"/jobs/#${job2Id}/mostRecentPayDate") -> Day("2025-09-30"),
      Path(s"/jobs/#${job2Id}/averagePayPerPayPeriod") -> Dollar("2000"),
      Path(s"/jobs/#${job2Id}/yearToDateIncome") -> Dollar("18000"),
      Path(s"/jobs/#${job2Id}/averageWithholdingPerPayPeriod") -> Dollar("100"),
      Path(s"/jobs/#${job2Id}/yearToDateWithholding") -> Dollar("900"),
      Path(s"/jobs/#${job2Id}/totalBonusReceived") -> Dollar("0"),
      Path(s"/jobs/#${job2Id}/preTaxDeductions") -> Dollar("0"),
      Path("/actualChildTaxCreditAmount") -> Dollar("2000"),
      // Derived overrides
      Path("/adjustmentsToIncome") -> Dollar("0"),
      Path("/totalOtherIncome") -> Dollar("0"),
    )

    assert(graph.get(Path(s"/jobs/#${job1Id}/income")).value.contains(Dollar("42000")))
    assert(graph.get(Path(s"/jobs/#${job2Id}/income")).value.contains(Dollar("24000")))

    assert(graph.get(Path(s"/jobs/#${job1Id}/endOfYearProjectedWithholding")).value.contains(Dollar("2000")))
    assert(graph.get(Path(s"/jobs/#${job2Id}/endOfYearProjectedWithholding")).value.contains(Dollar("1200")))
    assert(graph.get(Path("/totalEndOfYearProjectedWithholding")).value.contains(Dollar("3200")))

    assert(graph.get(Path("/agi")).value.contains(Dollar("66000")))
    assert(graph.get(Path("/tentativeTaxFromTaxableIncome")).value.contains(Dollar("6140")))
    assert(graph.get(Path("/totalTax")).value.contains(Dollar("4140")))

    assert(graph.get(Path("/withholdingGap")).value.contains(Dollar("940")))
    assert(graph.get(Path("/jobSelectedForExtraWithholding/w4Line4c")).value.contains(Dollar("338")))
  }

  test("2197 Scenarios spreadsheet Column D") {
    val graph = makeGraphWith(
      factDictionary,
      filingStatus -> Enum("marriedFilingJointly", "/filingStatusOptions"),
      Path("/primaryFilerDateOfBirth") -> Day("1985-01-28"),
      Path("/primaryFilerIsBlind") -> false,
      Path("/primaryFilerIsClaimedOnAnotherReturn") -> false,
      Path("/secondaryFilerDateOfBirth") -> Day("1985-02-28"),
      Path("/secondaryFilerIsBlind") -> false,
      Path("/secondaryFilerIsClaimedOnAnotherReturn") -> false,
      jobs -> jobsCollection,
      Path(s"/jobs/#${job1Id}/startDate") -> Day("2025-01-01"),
      Path(s"/jobs/#${job1Id}/endDate") -> Day("2025-10-15"),
      Path(s"/jobs/#${job1Id}/payFrequency") -> Enum("monthly", "/payFrequencyOptions"),
      Path(s"/jobs/#${job1Id}/mostRecentPayPeriodEnd") -> Day("2025-09-30"),
      Path(s"/jobs/#${job1Id}/mostRecentPayDate") -> Day("2025-10-05"),
      Path(s"/jobs/#${job1Id}/averagePayPerPayPeriod") -> Dollar("4000"),
      Path(s"/jobs/#${job1Id}/yearToDateIncome") -> Dollar("40000"),
      Path(s"/jobs/#${job1Id}/averageWithholdingPerPayPeriod") -> Dollar("300"),
      Path(s"/jobs/#${job1Id}/yearToDateWithholding") -> Dollar("3000"),
      Path(s"/jobs/#${job1Id}/totalBonusReceived") -> Dollar("0"),
      Path(s"/jobs/#${job1Id}/preTaxDeductions") -> Dollar("0"),
      Path(s"/jobs/#${job2Id}/startDate") -> Day("2025-01-01"),
      Path(s"/jobs/#${job2Id}/endDate") -> Day("2025-12-31"),
      Path(s"/jobs/#${job2Id}/payFrequency") -> Enum("monthly", "/payFrequencyOptions"),
      Path(s"/jobs/#${job2Id}/mostRecentPayPeriodEnd") -> Day("2025-09-30"),
      Path(s"/jobs/#${job2Id}/mostRecentPayDate") -> Day("2025-09-30"),
      Path(s"/jobs/#${job2Id}/averagePayPerPayPeriod") -> Dollar("4000"),
      Path(s"/jobs/#${job2Id}/yearToDateIncome") -> Dollar("36000"),
      Path(s"/jobs/#${job2Id}/averageWithholdingPerPayPeriod") -> Dollar("100"),
      Path(s"/jobs/#${job2Id}/yearToDateWithholding") -> Dollar("900"),
      Path(s"/jobs/#${job2Id}/totalBonusReceived") -> Dollar("0"),
      Path(s"/jobs/#${job2Id}/preTaxDeductions") -> Dollar("0"),
      Path("/actualChildTaxCreditAmount") -> Dollar("2000"),
      // Derived overrides
      Path("/adjustmentsToIncome") -> Dollar("0"),
      Path("/totalOtherIncome") -> Dollar("0"),
    )

    assert(graph.get(Path(s"/jobs/#${job1Id}/income")).value.contains(Dollar("42000")))
    assert(graph.get(Path(s"/jobs/#${job2Id}/income")).value.contains(Dollar("48000")))

    assert(graph.get(Path(s"/jobs/#${job1Id}/endOfYearProjectedWithholding")).value.contains(Dollar("3000")))
    assert(graph.get(Path(s"/jobs/#${job2Id}/endOfYearProjectedWithholding")).value.contains(Dollar("1200")))
    assert(graph.get(Path("/totalEndOfYearProjectedWithholding")).value.contains(Dollar("4200")))

    assert(graph.get(Path("/agi")).value.contains(Dollar("90000")))
    assert(graph.get(Path("/tentativeTaxFromTaxableIncome")).value.contains(Dollar("6726")))
    assert(graph.get(Path("/totalTax")).value.contains(Dollar("4726")))

    assert(graph.get(Path("/withholdingGap")).value.contains(Dollar("526")))
    assert(graph.get(Path("/jobSelectedForExtraWithholding/w4Line4c")).value.contains(Dollar("125")))
  }

  test("2197 Scenarios spreadsheet Column E") {
    val graph = makeGraphWith(
      factDictionary,
      filingStatus -> Enum("headOfHousehold", "/filingStatusOptions"),
      Path("/primaryFilerDateOfBirth") -> Day("1985-01-28"),
      Path("/primaryFilerIsBlind") -> false,
      Path("/primaryFilerIsClaimedOnAnotherReturn") -> false,
      jobs -> jobsCollection,
      Path(s"/jobs/#${job1Id}/startDate") -> Day("2025-01-01"),
      Path(s"/jobs/#${job1Id}/endDate") -> Day("2025-10-15"),
      Path(s"/jobs/#${job1Id}/payFrequency") -> Enum("monthly", "/payFrequencyOptions"),
      Path(s"/jobs/#${job1Id}/mostRecentPayPeriodEnd") -> Day("2025-09-30"),
      Path(s"/jobs/#${job1Id}/mostRecentPayDate") -> Day("2025-10-05"),
      Path(s"/jobs/#${job1Id}/averagePayPerPayPeriod") -> Dollar("4000"),
      Path(s"/jobs/#${job1Id}/yearToDateIncome") -> Dollar("40000"),
      Path(s"/jobs/#${job1Id}/averageWithholdingPerPayPeriod") -> Dollar("100"),
      Path(s"/jobs/#${job1Id}/yearToDateWithholding") -> Dollar("1000"),
      Path(s"/jobs/#${job1Id}/totalBonusReceived") -> Dollar("0"),
      Path(s"/jobs/#${job1Id}/preTaxDeductions") -> Dollar("0"),
      Path(s"/jobs/#${job2Id}/startDate") -> Day("2025-01-01"),
      Path(s"/jobs/#${job2Id}/endDate") -> Day("2025-12-31"),
      Path(s"/jobs/#${job2Id}/payFrequency") -> Enum("monthly", "/payFrequencyOptions"),
      Path(s"/jobs/#${job2Id}/mostRecentPayPeriodEnd") -> Day("2025-09-30"),
      Path(s"/jobs/#${job2Id}/mostRecentPayDate") -> Day("2025-09-30"),
      Path(s"/jobs/#${job2Id}/averagePayPerPayPeriod") -> Dollar("2000"),
      Path(s"/jobs/#${job2Id}/yearToDateIncome") -> Dollar("18000"),
      Path(s"/jobs/#${job2Id}/averageWithholdingPerPayPeriod") -> Dollar("100"),
      Path(s"/jobs/#${job2Id}/yearToDateWithholding") -> Dollar("900"),
      Path(s"/jobs/#${job2Id}/totalBonusReceived") -> Dollar("0"),
      Path(s"/jobs/#${job2Id}/preTaxDeductions") -> Dollar("0"),
      Path("/actualChildTaxCreditAmount") -> Dollar("2000"),
      // Derived overrides
      Path("/adjustmentsToIncome") -> Dollar("0"),
      Path("/totalOtherIncome") -> Dollar("0"),
    )

    assert(graph.get(Path(s"/jobs/#${job1Id}/income")).value.contains(Dollar("42000")))
    assert(graph.get(Path(s"/jobs/#${job2Id}/income")).value.contains(Dollar("24000")))

    assert(graph.get(Path(s"/jobs/#${job1Id}/endOfYearProjectedWithholding")).value.contains(Dollar("1000")))
    assert(graph.get(Path(s"/jobs/#${job2Id}/endOfYearProjectedWithholding")).value.contains(Dollar("1200")))
    assert(graph.get(Path("/totalEndOfYearProjectedWithholding")).value.contains(Dollar("2200")))

    assert(graph.get(Path("/agi")).value.contains(Dollar("66000")))
    assert(graph.get(Path("/tentativeTaxFromTaxableIncome")).value.contains(Dollar("4883")))
    assert(graph.get(Path("/totalTax")).value.contains(Dollar("2883")))

    assert(graph.get(Path("/withholdingGap")).value.contains(Dollar("683")))
    assert(graph.get(Path("/jobSelectedForExtraWithholding/w4Line4c")).value.contains(Dollar("315")))
  }

  test("2197 Scenarios spreadsheet Column G") {
    val graph = makeGraphWith(
      factDictionary,
      filingStatus -> single,
      Path("/primaryFilerDateOfBirth") -> Day("1985-01-28"),
      Path("/primaryFilerIsBlind") -> false,
      Path("/primaryFilerIsClaimedOnAnotherReturn") -> false,
      jobs -> jobsCollection,
      Path(s"/jobs/#${job1Id}/startDate") -> Day("2025-01-01"),
      Path(s"/jobs/#${job1Id}/endDate") -> Day("2025-10-15"),
      Path(s"/jobs/#${job1Id}/payFrequency") -> Enum("semiMonthly", "/payFrequencyOptions"),
      Path(s"/jobs/#${job1Id}/mostRecentPayPeriodEnd") -> Day("2025-09-30"),
      Path(s"/jobs/#${job1Id}/mostRecentPayDate") -> Day("2025-10-02"),
      Path(s"/jobs/#${job1Id}/averagePayPerPayPeriod") -> Dollar("4000"),
      Path(s"/jobs/#${job1Id}/yearToDateIncome") -> Dollar("76000"),
      Path(s"/jobs/#${job1Id}/averageWithholdingPerPayPeriod") -> Dollar("300"),
      Path(s"/jobs/#${job1Id}/yearToDateWithholding") -> Dollar("5700"),
      Path(s"/jobs/#${job1Id}/totalBonusReceived") -> Dollar("0"),
      Path(s"/jobs/#${job1Id}/preTaxDeductions") -> Dollar("0"),
      Path(s"/jobs/#${job2Id}/startDate") -> Day("2025-01-01"),
      Path(s"/jobs/#${job2Id}/endDate") -> Day("2025-12-31"),
      Path(s"/jobs/#${job2Id}/payFrequency") -> Enum("semiMonthly", "/payFrequencyOptions"),
      Path(s"/jobs/#${job2Id}/mostRecentPayPeriodEnd") -> Day("2025-09-30"),
      Path(s"/jobs/#${job2Id}/mostRecentPayDate") -> Day("2025-09-30"),
      Path(s"/jobs/#${job2Id}/averagePayPerPayPeriod") -> Dollar("2000"),
      Path(s"/jobs/#${job2Id}/yearToDateIncome") -> Dollar("36000"),
      Path(s"/jobs/#${job2Id}/averageWithholdingPerPayPeriod") -> Dollar("400"),
      Path(s"/jobs/#${job2Id}/yearToDateWithholding") -> Dollar("7200"),
      Path(s"/jobs/#${job2Id}/totalBonusReceived") -> Dollar("0"),
      Path(s"/jobs/#${job2Id}/preTaxDeductions") -> Dollar("0"),
      Path("/actualChildTaxCreditAmount") -> Dollar("4000"),
      // Derived overrides
      Path("/adjustmentsToIncome") -> Dollar("0"),
      Path("/totalOtherIncome") -> Dollar("0"),
    )

    assert(graph.get(Path(s"/jobs/#${job1Id}/income")).value.contains(Dollar("80000")))
    assert(graph.get(Path(s"/jobs/#${job2Id}/income")).value.contains(Dollar("48000")))

    assert(graph.get(Path(s"/jobs/#${job1Id}/endOfYearProjectedWithholding")).value.contains(Dollar("6000")))
    assert(graph.get(Path(s"/jobs/#${job2Id}/endOfYearProjectedWithholding")).value.contains(Dollar("9600")))
    assert(graph.get(Path("/totalEndOfYearProjectedWithholding")).value.contains(Dollar("15600")))

    assert(graph.get(Path("/agi")).value.contains(Dollar("128000")))
    assert(graph.get(Path("/tentativeTaxFromTaxableIncome")).value.contains(Dollar("19967")))
    assert(graph.get(Path("/totalTax")).value.contains(Dollar("15967")))

    assert(graph.get(Path("/withholdingGap")).value.contains(Dollar("367")))
    assert(graph.get(Path("/jobSelectedForExtraWithholding/w4Line4c")).value.contains(Dollar("318")))
  }

  test("2197 Scenarios spreadsheet Column N") {
    val graph = makeGraphWith(
      factDictionary,
      filingStatus -> single,
      Path("/primaryFilerDateOfBirth") -> Day("1985-01-28"),
      Path("/primaryFilerIsBlind") -> false,
      Path("/primaryFilerIsClaimedOnAnotherReturn") -> false,
      jobs -> jobsCollection,
      Path(s"/jobs/#${job1Id}/startDate") -> Day("2025-01-01"),
      Path(s"/jobs/#${job1Id}/endDate") -> Day("2025-10-15"),
      Path(s"/jobs/#${job1Id}/mostRecentPayPeriodEnd") -> Day("2025-10-05"),
      Path(s"/jobs/#${job1Id}/mostRecentPayDate") -> Day("2025-10-08"),
      Path(s"/jobs/#${job1Id}/averagePayPerPayPeriod") -> Dollar("2000"),
      Path(s"/jobs/#${job1Id}/yearToDateIncome") -> Dollar("82000"),
      Path(s"/jobs/#${job1Id}/payFrequency") -> Enum("weekly", "/payFrequencyOptions"),
      Path(s"/jobs/#${job1Id}/averageWithholdingPerPayPeriod") -> Dollar("200"),
      Path(s"/jobs/#${job1Id}/yearToDateWithholding") -> Dollar("8200"),
      Path(s"/jobs/#${job1Id}/totalBonusReceived") -> Dollar("0"),
      Path(s"/jobs/#${job1Id}/preTaxDeductions") -> Dollar("0"),
      Path(s"/jobs/#${job2Id}/startDate") -> Day("2025-01-01"),
      Path(s"/jobs/#${job2Id}/endDate") -> Day("2025-12-31"),
      Path(s"/jobs/#${job2Id}/mostRecentPayPeriodEnd") -> Day("2025-10-08"),
      Path(s"/jobs/#${job2Id}/mostRecentPayDate") -> Day("2025-10-08"),
      Path(s"/jobs/#${job2Id}/averagePayPerPayPeriod") -> Dollar("1000"),
      Path(s"/jobs/#${job2Id}/yearToDateIncome") -> Dollar("41000"),
      Path(s"/jobs/#${job2Id}/payFrequency") -> Enum("weekly", "/payFrequencyOptions"),
      Path(s"/jobs/#${job2Id}/averageWithholdingPerPayPeriod") -> Dollar("100"),
      Path(s"/jobs/#${job2Id}/yearToDateWithholding") -> Dollar("4100"),
      Path(s"/jobs/#${job2Id}/totalBonusReceived") -> Dollar("0"),
      Path(s"/jobs/#${job2Id}/preTaxDeductions") -> Dollar("0"),
      Path("/actualChildTaxCreditAmount") -> Dollar("6000"),
      // Derived overrides
      Path("/adjustmentsToIncome") -> Dollar("0"),
      Path("/totalOtherIncome") -> Dollar("0"),
    )

    assert(graph.get(Path(s"/jobs/#${job1Id}/income")).value.contains(Dollar("84857.14")))
    assert(graph.get(Path(s"/jobs/#${job2Id}/income")).value.contains(Dollar("53000")))

    assert(graph.get(Path(s"/jobs/#${job1Id}/endOfYearProjectedWithholding")).value.contains(Dollar("8400")))
    assert(graph.get(Path(s"/jobs/#${job2Id}/endOfYearProjectedWithholding")).value.contains(Dollar("5300")))
    assert(graph.get(Path("/totalEndOfYearProjectedWithholding")).value.contains(Dollar("13700")))

    assert(graph.get(Path("/agi")).value.contains(Dollar("137857")))
    assert(graph.get(Path("/tentativeTaxFromTaxableIncome")).value.contains(Dollar("22333")))
    assert(graph.get(Path("/totalTax")).value.contains(Dollar("16333")))

    assert(graph.get(Path("/withholdingGap")).value.contains(Dollar("2633")))
    assert(graph.get(Path("/jobSelectedForExtraWithholding/w4Line4c")).value.contains(Dollar("280")))
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
  //     Path("/actualChildTaxCreditAmount") -> Dollar("4000"),
  //     Path("/qualifiedBusinessIncomeDeduction") -> Dollar("1302"),
  //     // Derived overrides
  //     Path("/totalNonJobsIncome") -> Dollar("5204"), // spreadsheet row 248
  //     Path("/adjustmentsToIncome") -> Dollar("495"), // row 275
  //     Path("/totalOtherIncome") -> Dollar("7000"),
  //   )

  //   // assert(graph.get(Path(s"/jobs/#${job1Id}/w4Line4a")).value.contains(Dollar("0")))
  //   // assert(graph.get(Path(s"/jobs/#${job2Id}/w4Line4a")).value.contains(Dollar("5204")))

  //   assert(graph.get(Path(s"/jobs/#${job1Id}/income")).value.contains(Dollar("82857.14")))
  //   assert(graph.get(Path(s"/jobs/#${job2Id}/income")).value.contains(Dollar("52000")))

  //   assert(graph.get(Path(s"/jobs/#${job1Id}/endOfYearProjectedWithholding")).value.contains(Dollar("4100")))
  //   assert(graph.get(Path(s"/jobs/#${job2Id}/endOfYearProjectedWithholding")).value.contains(Dollar("5200")))
  //   assert(graph.get(Path("/totalEndOfYearProjectedWithholding")).value.contains(Dollar("9300")))

  //   assert(graph.get(Path("/agi")).value.contains(Dollar("141362")))
  //   assert(graph.get(Path("/tentativeTaxFromTaxableIncome")).value.contains(Dollar("14041")))
  //   assert(graph.get(Path("/totalTax")).value.contains(Dollar("10041")))
  //   assert(graph.get(Path("/withholdingGap")).value.contains(Dollar("741")))
  //   assert(graph.get(Path("/jobSelectedForExtraWithholding/w4Line4c")).value.contains(Dollar("122")))
  // }
}

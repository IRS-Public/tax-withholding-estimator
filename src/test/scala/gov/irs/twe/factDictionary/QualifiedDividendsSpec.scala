package gov.irs.twe.factDictionary

import gov.irs.factgraph.types.Dollar
import gov.irs.factgraph.types.Enum
import gov.irs.factgraph.Path
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.prop.TableDrivenPropertyChecks

class QualifiedDividendsSpec extends AnyFunSuite with TableDrivenPropertyChecks {
  val factDictionary = setupFactDictionary()
  val single = Enum("single", "/filingStatusOptions")
  val mfj = Enum("marriedFilingJointly", "/filingStatusOptions")
  val qss = Enum("qualifiedSurvivingSpouse", "/filingStatusOptions")
  val hoh = Enum("headOfHousehold", "/filingStatusOptions")
  val mfs = Enum("marriedFilingSeparately", "/filingStatusOptions")
  // Set up table of data to run and validate all qualified dividends scenarios
  val dataTable = Table(
    // Header values for test table -- these are not reused anywhere
    ("status", "taxableIncome", "qualDiv", "ltCapitalGains", "amtAt0", "totalAt15", "totalAt20", "totalTax"),

    // Single tp with qualified dividends taxed at 0%
    (single, "40000", "4000", "1000", "5000", "0", "0", "0"),
    // Single tp with qualified dividends, income at 0% threshhold
    (single, "48350", "5000", "0", "5000", "0", "0", "0"),
    // Single tp with qualified dividends taxed at 0% and 15%
    (single, "53000", "5000", "3000", "3350", "697.50", "0", "697.50"),
    // Single tp with qualified dividends, income at 15% threshhold
    (single, "533400", "15000", "0", "0", "2250", "0", "2250"),
    // Single tp with qualified dividends taxed at 15%
    (single, "87000", "10000", "2000", "0", "1800", "0", "1800"),
    // Single tp with qualified dividends taxed at 15% and 20%
    (single, "540000", "14000", "1000", "0", "1260", "1320", "2580"),
    // Single tp with qualified dividends taxed at 20%,
    (single, "625000", "20000", "5000", "0", "0", "5000", "5000"),

    // MFS tp with qualified dividends taxed at 0%
    (mfs, "45000", "5000", "0", "5000", "0", "0", "0"),
    // MFS tp with qualified dividends, income at 0% threshhold
    (mfs, "48350", "5000", "0", "5000", "0", "0", "0"),
    // MFS tp with qualified dividends taxed at 0% and 15%
    (mfs, "53000", "5000", "3000", "3350", "697.50", "0", "697.50"),
    // MFS tp with qualified dividends taxed at 15%
    (mfs, "87000", "10000", "2000", "0", "1800", "0", "1800"),
    // MFS tp with qualified dividends, income at 15% threshhold
    (mfs, "300000", "15000", "0", "0", "2250", "0", "2250"),
    // // MFS tp with qualified dividends taxed at 15% and 20%
    (mfs, "307500", "14000", "1000", "0", "1125", "1500", "2625"),
    // // MFS tp with qualified dividends taxed at 20%,
    (mfs, "400000", "20000", "5000", "0", "0", "5000", "5000"),

    // MFJ tp with qualified dividends taxed at 0%
    (mfj, "90000", "3000", "7000", "10000", "0", "0", "0"),
    // MFS tp with qualified dividends, income at 0% threshhold
    (mfj, "96700", "10000", "0", "10000", "0", "0", "0"),
    // MFJ tp with qualified dividends taxed at 0% and 15%
    (mfj, "105000", "14500", "500", "6700", "1245", "0", "1245"),
    // MFJ tp with qualified dividends taxed at 15%
    (mfj, "170000", "19000", "1000", "0", "3000", "0", "3000"),
    // MFJ tp with qualified dividends, income at 15% threshold
    (mfj, "600050", "25000", "0", "0", "3750", "0", "3750"),
    // MFJ tp with qualified dividends taxed at 15% and 20%
    (mfj, "615000", "15000", "10000", "0", "1507.50", "2990", "4497.50"), // run through this
    // MFJ tp with qualified dividends taxed at 20%
    (mfj, "785000", "15000", "20000", "0", "0", "7000", "7000"),

    // QSS tp with qualified dividends taxed at 0%
    (qss, "90000", "3000", "7000", "10000", "0", "0", "0"),
    // QSS tp with qualified dividends taxed at 0% and 15%
    (qss, "105000", "14500", "500", "6700", "1245", "0", "1245"),
    // QSS tp with qualified dividends taxed at 15%
    (qss, "170000", "19000", "1000", "0", "3000", "0", "3000"),
    // QSS tp with qualified dividends taxed at 15% and 20%
    (qss, "615000", "15000", "10000", "0", "1507.50", "2990", "4497.50"), // run through this
    // QSS tp with qualified dividends taxed at 20%
    (qss, "785000", "15000", "20000", "0", "0", "7000", "7000"),

    // HoH tp with qualified dividends taxed at 0%
    (hoh, "62000", "2000", "5000", "7000", "0", "0", "0"),
    // HoH tp with qualified dividends, income at 0% threshold
    (hoh, "64750", "2000", "5000", "7000", "0", "0", "0"),
    // HoH tp with qualified dividends taxed at 0% and 15%
    (hoh, "70000", "10000", "0", "4750", "787.50", "0", "787.50"),
    // HoH tp with qualified dividends taxed at 15%
    (hoh, "115000", "15000", "0", "0", "2250", "0", "2250"),
    // HoH tp with qualified dividends, income at 15% threshold
    (hoh, "566700", "15000", "0", "0", "2250", "0", "2250"),
    // HoH tp with qualified dividends taxed at 15% and 20%
    (hoh, "580000", "10000", "10000", "0", "1005", "2660", "3665"),
    // HoH tp with qualified dividends taxed at 20%
    (hoh, "680000", "30000", "0", "0", "0", "6000", "6000"),
  )

  // Set up shorthand for facts to check against our table data
  val filingStatus = Path("/filingStatus")
  val taxableIncome = Path("/taxableIncome")
  val qualifiedDividends = Path("/qualifiedDividendsIncome")
  val longTermCapitalGains = Path("/longTermCapitalGainsIncome")
  val amountAt0 = Path("/amountOfDividendsTaxedAt0")
  val totalQualifiedDividends15Tax = Path("/totalQualifiedDividends15Tax")
  val totalQualifiedDividends20Tax = Path("/totalQualifiedDividends20Tax")
  val totalQualifiedDividendsTax = Path("/totalQualifiedDividendsWithholding")

  test("Qualified Dividends calculates appropriate tax withholding for single, mfj, and hoh filers") {
    forAll(dataTable) { (status, income, qDiv, ltGains, totalAt0, taxDueAt15, taxDueAt20, totalTax) =>
      //  Pass in necessary overrides to calculate qualified dividends withholding.
      val graph = makeGraphWith(
        factDictionary,
        filingStatus -> status,
        taxableIncome -> Dollar(income),
        qualifiedDividends -> Dollar(qDiv),
        longTermCapitalGains -> Dollar(ltGains),
      )

      // Get calculated values based on test data
      val taxTotal = graph.get(totalQualifiedDividendsTax)
      val amountTaxedAt0 = graph.get(amountAt0)
      val taxTotal15 = graph.get(totalQualifiedDividends15Tax)
      val taxTotal20 = graph.get(totalQualifiedDividends20Tax)

      assert(amountTaxedAt0.value.contains(Dollar(totalAt0)))
      assert(taxTotal15.value.contains(Dollar(taxDueAt15)))
      assert(taxTotal20.value.contains(Dollar(taxDueAt20)))
      assert(taxTotal.value.contains(Dollar(totalTax)))
    }
  }
}

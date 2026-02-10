package gov.irs.twe.factDictionary

import gov.irs.factgraph.types.Collection
import gov.irs.factgraph.types.Day
import gov.irs.factgraph.types.Dollar
import gov.irs.factgraph.types.Enum
import gov.irs.factgraph.FactDictionaryForTests
import gov.irs.factgraph.Path
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.prop.TableDrivenPropertyChecks

class IncomeSpec extends AnyFunSuite with TableDrivenPropertyChecks {
  val factDictionary = setupFactDictionary()

  val jobId = "11111111-1111-447e-a94d-0944cc8e3b6b"
  val jobsCollection = Collection(Vector(java.util.UUID.fromString(jobId)))

  val filingStatus = Path("/filingStatus")
  val taxableIncome = Path("/taxableIncome")
  val roundedTaxableIncome = Path("/roundedTaxableIncome")
  val tentativeTaxFromTaxableIncome = Path("/tentativeTaxFromTaxableIncome")
  val jobs = Path("/jobs")
  val adjustedAnnualWageAmount = Path(s"/jobs/#${jobId}/adjustedAnnualWageAmount")
  val standardAnnualWithholdingAmount = Path(s"/jobs/#${jobId}/standardAnnualWithholdingAmount")
  val single = Enum("single", "/filingStatusOptions")
  val mfs = Enum("marriedFilingSeparately", "/filingStatusOptions")
  val mfj = Enum("marriedFilingJointly", "/filingStatusOptions")
  val qss = Enum("qualifiedSurvivingSpouse", "/filingStatusOptions")
  val hoh = Enum("headOfHousehold", "/filingStatusOptions")

  val dataTable = Table(
    ("status", "aawa", "expectedStandardAnnualWithholdingAmount"),
    // Less than $7,500: no withholding
    (single, "7499", "0.00"),
    // Less than $7,500: no withholding
    (mfs, "7499", "0.00"),
    // At least $7,500 but less than $19,900: $0 plus 10% of the excess over $7,500
    (single, "7500", "0.00"),
    // At least $7,500 but less than $19,900: $0 plus 10% of the excess over $7,500
    (single, "7510", "1.00"),
    // At least $7,500 but less than $19,900: $0 plus 10% of the excess over $7,500
    (mfs, "19899", "1239.90"),
    // At least $19,900 but less than $57,900: $1,240 plus 12% of the excess over $19,900
    (single, "19900", "1240.00"),
    // At least $19,900 but less than $57,900: $1,240 plus 12% of the excess over $19,900
    (single, "19910", "1241.20"),
    // At least $19,900 but less than $57,900: $1,240 plus 12% of the excess over $19,900
    (mfs, "57899", "5799.88"),
    // At least $57,900 but less than $113,200: $5,800.00 plus 22% of the excess over $57,900
    (mfs, "57900", "5800.00"),
    // At least $57,900 but less than $113,200: $5,800.00 plus 22% of the excess over $57,900
    (mfs, "57905", "5801.10"),
    // At least $57,900 but less than $113,200: $5,800.00 plus 22% of the excess over $57,900
    (single, "113199", "17965.78"),
    // At least $113,200 but less than $209,275: $17,966 plus 24% of the excess over $113,200
    (mfs, "113200", "17966.00"),
    // At least $113,200 but less than $209,275: $17,966 plus 24% of the excess over $113,200
    (mfs, "113210", "17968.40"),
    // At least $113,200 but less than $209,275: $17,966 plus 24% of the excess over $113,200
    (single, "209274", "41023.76"),
    // At least $209,275 but less than $263,725: $41,024.00 plus 32% of the excess over $209,275
    (single, "209275", "41024.00"),
    // At least $209,275 but less than $263,725: $41,024.00 plus 32% of the excess over $209,275
    (single, "209276", "41024.32"),
    // At least $209,275 but less than $263,725: $41,024.00 plus 32% of the excess over $209,275
    (mfs, "263724", "58447.68"),
    // At least $263,725 but less than $648,100: $58,448 plus 35% of the excess over $263,725
    (single, "263725", "58448.00"),
    // At least $263,725 but less than $648,100: $58,448 plus 35% of the excess over $263,725
    (single, "555555", "160588.50"),
    // At least $263,725 but less than $648,100: $58,448 plus 35% of the excess over $263,725
    (mfs, "648099", "192978.90"),
    // $648,100 and above: $192,979.25 plus 37% of the excess over $648,100
    (single, "648100", "192979.25"),
    // $648,100 and above: $192,979.25 plus 37% of the excess over $648,100
    (mfs, "1000000", "323182.25"),
    // Less than $19,300: no withholding
    (mfj, "19299", "0.00"),
    // At least $19,300, but less than $44,100: $0 plus 10% of the excess over $19,300
    (mfj, "19300", "0.00"),
    // At least $19,300, but less than $44,100: $0 plus 10% of the excess over $19,300
    (qss, "25000", "570.00"),
    // At least $19,300, but less than $44,100: $0 plus 10% of the excess over $19,300
    (mfj, "44099", "2479.90"),
    // At least $44,100 but less than $120,100: $2,480 plus 12% of the excess over $44,100
    (qss, "44100", "2480.00"),
    // At least $44,100 but less than $120,100: $2,480 plus 12% of the excess over $44,100
    (mfj, "73000", "5948.00"),
    // At least $44,100 but less than $120,100: $2,480 plus 12% of the excess over $44,100
    (mfj, "120099", "11599.88"),
    // At least $120,100 but less than $230,700: $11,600 plus 22% of the excess over $120,100
    (mfj, "120100", "11600.00"),
    // At least $120,100 but less than $230,700: $11,600 plus 22% of the excess over $120,100
    (mfj, "183210", "25484.20"),
    // At least $120,100 but less than $230,700: $11,600 plus 22% of the excess over $120,100
    (qss, "230699", "35931.78"),
    // At least $230,700 but less than $422,850: $35,932 plus 24% of the excess over $230,700
    (mfj, "230700", "35932.00"),
    // At least $230,700 but less than $422,850: $35,932 plus 24% of the excess over $230,700
    (qss, "333333", "60563.92"),
    // At least $230,700 but less than $422,850: $35,932 plus 24% of the excess over $230,700
    (mfj, "422849", "82047.76"),
    // At least $422,850 but less than $531,750: $82,048 plus 32% of the excess over $422,850
    (qss, "422850", "82048.00"),
    // At least $422,850 but less than $531,750: $82,048 plus 32% of the excess over $422,850
    (mfj, "498765", "106340.80"),
    // At least $422,850 but less than $531,750: $82,048 plus 32% of the excess over $422,850
    (mfj, "531749", "116895.68"),
    // At least $531,750 but less than $788,000: $116,896 plus 35% of the excess over $531,750
    (mfj, "531750", "116896.00"),
    // At least $531,750 but less than $788,000: $116,896 plus 35% of the excess over $531,750
    (mfj, "677777", "168005.45"),
    // At least $531,750 but less than $788,000: $116,896 plus 35% of the excess over $531,750
    (qss, "787999", "206583.15"),
    // $788,000 and above: $206,583.50 plus 37% of the excess over $788,000
    (mfj, "788000", "206583.50"),
    // $788,000 and above: $206,583.50 plus 37% of the excess over $788,000
    (qss, "900000", "248023.50"),
    // Less than $15,550: no withholding
    (hoh, "15549", "0.00"),
    // At least $15,550 but less than $33,250: $0 plus 10% of the excess over $15,550
    (hoh, "15550", "0.00"),
    // At least $15,550 but less than $33,250: $0 plus 10% of the excess over $15,550
    (hoh, "15560", "1.00"),
    // At least $15,550 but less than $33,250: $0 plus 10% of the excess over $15,550
    (hoh, "33249", "1769.90"),
    // At least $33,250 but less than $83,000: $1,770 plus 12% of the excess over $33,250
    (hoh, "33250", "1770.00"),
    // At least $33,250 but less than $83,000: $1,770 plus 12% of the excess over $33,250
    (hoh, "55000", "4380.00"),
    // At least $33,250 but less than $83,000: $1,770 plus 12% of the excess over $33,250
    (hoh, "82999", "7739.88"),
    // At least $83,000 but less than $121,250: $7,740 plus 22% of the excess over $83,000
    (hoh, "83000", "7740.00"),
    // At least $83,000 but less than $121,250: $7,740 plus 22% of the excess over $83,000
    (hoh, "100000", "11480.00"),
    // At least $83,000 but less than $121,250: $7,740 plus 22% of the excess over $83,000
    (hoh, "121249", "16154.78"),
    // At least $121,250 but less than $217,300: $16,155 plus 24% of the excess over $121,250
    (hoh, "121250", "16155.00"),
    // At least $121,250 but less than $217,300: $16,155 plus 24% of the excess over $121,250
    (hoh, "153210", "23825.40"),
    // At least $121,250 but less than $217,300: $16,155 plus 24% of the excess over $121,250
    (hoh, "217299", "39206.76"),
    // At least $217,300 but less than $271,750: $39,207 plus 32% of the excess over $217,300
    (hoh, "217300", "39207.00"),
    // At least $217,300 but less than $271,750: $39,207 plus 32% of the excess over $217,300
    (hoh, "225225", "41743.00"),
    // At least $217,300 but less than $271,750: $39,207 plus 32% of the excess over $217,300
    (hoh, "271749", "56630.68"),
    // At least $271,750 but less than $656,150: $56,631 plus 35% of the excess over $271,750
    (hoh, "271750", "56631.00"),
    // At least $271,750 but less than $656,150: $56,631 plus 35% of the excess over $271,750
    (hoh, "555555", "155962.75"),
    // At least $271,750 but less than $656,150: $56,631 plus 35% of the excess over $271,750
    (hoh, "656149", "191170.65"),
    // $656,150 and above: $191,171.00 plus 37% of the excess over $656,150
    (hoh, "656150", "191171.00"),
    // $656,150 and above: $191,171.00 plus 37% of the excess over $656,150
    (hoh, "1000000", "318395.50"),
  )

  test("test standardAnnualWithholdingAmount") {
    forAll(dataTable) { (status, aawa, expectedStandardAnnualWithholdingAmount) =>
      val graph = makeGraphWith(
        factDictionary,
        filingStatus -> status,
        jobs -> jobsCollection,
        adjustedAnnualWageAmount -> Dollar(aawa),
      )

      val actual = graph.get(standardAnnualWithholdingAmount)
      assert(actual.value.contains(Dollar(expectedStandardAnnualWithholdingAmount)))
    }
  }

  test("a jobs income includes bonuses") {
    val allYear = Path(s"/jobs/#$jobId/isAllYear")
    val recentPayPeriodEnd = Path(s"/jobs/#$jobId/mostRecentPayPeriodEnd")
    val recentPayDate = Path(s"/jobs/#$jobId/mostRecentPayDate")
    val payFreq = Path(s"/jobs/#$jobId/payFrequency")
    val weekly = Enum("weekly", "/payFrequencyOptions")

    val amountLastPaycheck = Path(s"/jobs/#$jobId/amountLastPaycheck")
    val mostRecentPayPeriodBonusAmount = Path(s"/jobs/#$jobId/mostRecentPayPeriodBonusAmount")
    val totalFutureBonus = Path(s"/jobs/#$jobId/totalFutureBonus")
    val yearToDateIncome = Path(s"/jobs/#$jobId/yearToDateIncome")

    val graph = makeGraphWith(
      factDictionary,
      filingStatus -> single,
      jobs -> jobsCollection,
      allYear -> true,
      recentPayPeriodEnd -> Day("2026-02-04"),
      recentPayDate -> Day("2026-02-04"),
      Path("/today") -> Day("2026-02-04"),
      payFreq -> weekly,
      amountLastPaycheck -> Dollar(2000),
      mostRecentPayPeriodBonusAmount -> Dollar(1000),
      totalFutureBonus -> Dollar(40000),
      yearToDateIncome -> Dollar(7000),
    )
    assert(graph.get(Path(s"/jobs/#$jobId/isCurrentJob")).value.contains(true))
    assert(graph.get(Path(s"/jobs/#$jobId/totalBonusReceived")).value.contains(Dollar(41000)))
    assert(graph.get(Path(s"/jobs/#$jobId/income")).value.contains(Dollar(94000)))
  }
}

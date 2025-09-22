package gov.irs.twe.factDictionary

import gov.irs.factgraph.types.Collection
import gov.irs.factgraph.types.Dollar
import gov.irs.factgraph.types.Enum
import gov.irs.factgraph.FactDictionaryForTests
import gov.irs.factgraph.Path
import gov.irs.twe.FileLoaderHelper
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
    ///////////////////////////////////////////////////////////////////////////////
    // Single, MFS
    // Less than $6,400: no withholding
    (single, "6399", "0"),
    (mfs, "6399", "0"),
    // At least $6,400 but less than $18,325: $0 plus 10% of the excess over $6,400
    (single, "6400", "0"),
    (single, "6410", "1"),
    (mfs, "18324", "1192.40"),
    // At least $18,325 but less than $54,875: $1,192.50 plus 12% of the excess over $18,325
    (single, "18325", "1192.50"),
    (single, "18335", "1193.70"),
    (mfs, "54874", "5578.38"),
    // At least $54,875 but less than $109,750: $5,578.50 plus 22% of the excess over $54,875
    (mfs, "54875", "5578.50"),
    (mfs, "54880", "5579.60"),
    (single, "109749", "17650.78"),
    // At least $109,750 but less than $203,700: $17,651 plus 24% of the excess over $109,750
    (mfs, "109750", "17651"),
    (mfs, "183210", "35281.40"),
    (single, "203699", "40198.76"),
    // At least $203,700 but less than $256,925: $40,199 plus 32% of the excess over $203,700
    (single, "203700", "40199"),
    (single, "203749", "40214.68"),
    (mfs, "225225", "47087"),
    (mfs, "256924", "57230.68"),
    // At least $256,925 but less than $632,750: $57,231 plus 35% of the excess over $256,925
    (single, "256925", "57231"),
    (single, "555555", "161751.50"),
    (mfs, "632749", "188769.40"),
    // $632,750 and above: $188,769.75 plus 37% of the excess over $632,750
    (single, "632750", "188769.75"),
    (mfs, "1000000", "324652.25"),
    ///////////////////////////////////////////////////////////////////////////////
    // MFJ, QSS
    // Less than $17,100: no withholding
    (mfj, "17099", "0"),
    // At least $17,100, but less than $40,950: $0 plus 10% of the excess over $17,100
    (mfj, "17100", "0"),
    (qss, "25000", "790"),
    (mfj, "40949", "2384.90"),
    // At least $40,950 but less than $114,050: $2,385 plus 12% of the excess over $40,950
    (qss, "40950", "2385"),
    (mfj, "73000", "6231"),
    (mfj, "114049", "11156.88"),
    // At least $114,050 but less than $223,800: $11,157 plus 22% of the excess over $114,050
    (mfj, "114050", "11157"),
    (mfj, "183210", "26372.20"),
    (qss, "223799", "35301.78"),
    // At least $223,800 but less than $411,700: $35,302 plus 24% of the excess over $223,800
    (mfj, "223800", "35302"),
    (qss, "333333", "61589.92"),
    (mfj, "411699", "80397.76"),
    // At least $411,700 but less than $518,150: $80,398 plus 32% of the excess over $411,700
    (qss, "411700", "80398"),
    (mfj, "498765", "108258.80"),
    (mfj, "518149", "114461.68"),
    // At least $518,150 but less than $768,700: $114,462 plus 35% of the excess over $518,150
    (mfj, "518150", "114462"),
    (mfj, "677777", "170331.45"),
    (qss, "768699", "202154.15"),
    // $768,700 and above: $202,154.50 plus 37% of the excess over $768,700
    (mfj, "768700", "202154.50"),
    (qss, "900000", "250735.50"),
    ///////////////////////////////////////////////////////////////////////////////
    // HoH
    // Less than $13,900: no withholding
    (hoh, "13899", "0"),
    // At least $13,900 but less than $30,900: $0 plus 10% of the excess over $13,900
    (hoh, "13900", "0"),
    (hoh, "13910", "1"),
    (hoh, "30899", "1699.90"),
    // At least $30,900 but less than $78,750: $1,700 plus 12% of the excess over $30,900
    (hoh, "30900", "1700"),
    (hoh, "55000", "4592"),
    (hoh, "78749", "7441.88"),
    // At least $78,750 but less than $117,250: $7,442 plus 22% of the excess over $78,750
    (hoh, "78750", "7442"),
    (hoh, "100000", "12117"),
    (hoh, "117249", "15911.78"),
    // At least $117,250 but less than $211,200: $15,912 plus 24% of the excess over $117,250
    (hoh, "117250", "15912"),
    (hoh, "153210", "24542.40"),
    (hoh, "211199", "38459.76"),
    // At least $211,200 but less than $264,400: $38,460 plus 32% of the excess over $211,200
    (hoh, "211200", "38460"),
    (hoh, "225225", "42948"),
    (hoh, "264399", "55483.68"),
    // At least $264,400 but less than $640,250: $55,484 plus 35% of the excess over $264,400
    (hoh, "264400", "55484"),
    (hoh, "555555", "157388.25"),
    (hoh, "640249", "187031.15"),
    // $640,250 and above: $187,031.50 plus 37% of the excess over $640,250
    (hoh, "640250", "187031.50"),
    (hoh, "1000000", "320139"),
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
}

package gov.irs.twe.factDictionary

import gov.irs.factgraph.types.Dollar
import gov.irs.factgraph.types.Enum
import gov.irs.factgraph.Path
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.prop.TableDrivenPropertyChecks

class QBIDeductionSpec extends AnyFunSuite with TableDrivenPropertyChecks {
  val factDictionary = setupFactDictionary()
  val single = Enum("single", "/filingStatusOptions")
  val mfs = Enum("marriedFilingSeparately", "/filingStatusOptions")
  val mfj = Enum("marriedFilingJointly", "/filingStatusOptions")
  val qss = Enum("qualifiedSurvivingSpouse", "/filingStatusOptions")
  val hoh = Enum("headOfHousehold", "/filingStatusOptions")

  test("test OB3 70105: QBI eligibility and floor") {
    val dataTable = Table(
      ("tentativeQualifiedBusinessIncomeDeduction", "qualifiedBusinessIncomeDeduction"),
      ("100", "0"),
      ("999", "0"),
      ("1000", "1000"),
      ("2500", "2500"),
      ("5000", "5000"),
    )
    forAll(dataTable) { (tentativeQualifiedBusinessIncomeDeduction, qualifiedBusinessIncomeDeduction) =>
      val graph = makeGraphWith(
        factDictionary,
        Path("/tentativeQualifiedBusinessIncomeDeduction") -> Dollar(tentativeQualifiedBusinessIncomeDeduction),
      )

      val actualQBID = graph.get("/qualifiedBusinessIncomeDeduction")

      assert(actualQBID.value.contains(Dollar(qualifiedBusinessIncomeDeduction)))
    }
    println(s"Completed ${dataTable.length} tests for calculating QBI floor")
  }
  test("test QBI derivation") {
    val dataTable = Table(
      ("status", "taxableIncomeForQBIDeduction", "qualifiedBusinessIncome", "netCapitalGains", "expectedQBID"),
      (single, "0", "0", "0", "0"),
      (single, "100000", "50000", "40000", "10000"),
      (single, "100000", "50000", "60000", "8000"),
      (single, "100000", "50000", "94000", "1200"),
      (single, "100000", "50000", "98000", "0"),
      (single, "185000", "184200", "1200", "36760"),
      (single, "185000", "183332", "5000", "36000"),
      (single, "190000", "189400", "1200", "37760"),
      (single, "190000", "188750", "5000", "37000"),
      (single, "195000", "194600", "1200", "38760"),
      (single, "195000", "194167", "5000", "38000"),
      (single, "200000", "199800", "1200", "39760"),
      (single, "205000", "205000", "1200", "40760"),
      (single, "206000", "206000", "1200", "40960"),
      (single, "210000", "210000", "1200", "41760"),
      (single, "222300", "100000", "180000", "8460"),
      (single, "247000", "100000", "0", "20000"),
      (single, "247300", "100000", "0", "20000"),
      (single, "247300", "109611", "54416", "21922.2"),
      (single, "272300", "97199", "77670", "19439.8"),
      (single, "297300", "63439", "69029", "12687.8"),
      (single, "300000", "200000", "0", "40000"),
      (single, "300000", "500000", "0", "60000"),
      (single, "322300", "105640", "28786", "21128"),
      (single, "347300", "79148", "60731", "15829.6"),
      (single, "350000", "400000", "0", "70000"),
      (single, "372300", "124741", "72677", "24948.2"),
      (single, "397300", "53240", "42858", "10648"),
      (single, "410000", "500000", "0", "82000"),
      (single, "422300", "67854", "22845", "13570.8"),
      (single, "447300", "106629", "57364", "0"),
      (single, "450000", "500000", "0", "0"),
      (single, "472300", "119262", "59918", "0"),
      (single, "490000", "500000", "0", "0"),
      (single, "497300", "49119", "27077", "0"),
      (single, "522300", "78729", "59742", "0"),
      (single, "547300", "91020", "80049", "0"),
      (single, "572300", "76136", "82921", "0"),
      (single, "597300", "120899", "62302", "0"),
      (single, "622300", "47497", "10368", "0"),
      (mfj, "0", "0", "0", "0"),
      (mfj, "100000", "50000", "40000", "10000"),
      (mfj, "110000", "50000", "0", "10000"),
      (mfj, "120000", "80000", "60000", "12000"),
      (mfj, "150000", "100000", "80000", "14000"),
      (mfj, "150000", "100000", "10000", "20000"),
      (mfj, "150000", "70000", "0", "14000"),
      (mfj, "150000", "200000", "60000", "18000"),
      (mfj, "160000", "150000", "70000", "18000"),
      (mfj, "180000", "150000", "40000", "28000"),
      (mfj, "180000", "200000", "0", "36000"),
      (mfj, "190000", "100000", "0", "20000"),
      (mfj, "190000", "100000", "5000", "20000"),
      (mfj, "195000", "150000", "0", "30000"),
      (mfj, "197000", "300000", "100000", "19400"),
      (mfj, "197300", "250000", "0", "39460"),
      (mfj, "200000", "50000", "0", "10000"),
      (mfj, "207300", "100000", "0", "20000"),
      (mfj, "207300", "200000", "100000", "21460"),
      (mfj, "217300", "150000", "10000", "30000"),
      (mfj, "242300", "209707", "83552", "0"),
      (mfj, "267300", "262362", "81577", "0"),
      (mfj, "292300", "192001", "63487", "0"),
      (mfj, "317300", "155672", "35500", "0"),
      (mfj, "342300", "204448", "61790", "0"),
      (mfj, "367300", "205552", "83097", "0"),
      (mfj, "392300", "266656", "99279", "0"),
    )
    forAll(dataTable) {
      (status, taxableIncomeForQBIDeduction, qualifiedBusinessIncome, netCapitalGains, expectedQBID) =>
        val graph = makeGraphWith(
          factDictionary,
          Path("/filingStatus") -> status,
          Path("/qualifiedBusinessIncome") -> Dollar(qualifiedBusinessIncome),
          Path("/netCapitalGains") -> Dollar(netCapitalGains),
          Path("/taxableIncomeForQBIDeduction") -> Dollar(taxableIncomeForQBIDeduction),
        )

        val actualQBID = graph.get("/qualifiedBusinessIncomeDeduction")
        assert(actualQBID.value.contains(Dollar(expectedQBID)))
    }
    println(s"Completed ${dataTable.length} tests for deriving QBI")
  }
}

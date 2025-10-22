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

  test("test OB3 70105: QBI floor") {
    //    Note: this test should break when we implement Form 8995 properly and change the derivation logic
    val dataTable = Table(
      ("estimatedQualifiedBusinessIncomeDeduction", "qualifiedBusinessIncomeDeduction"),
      ("100", "0"),
      ("999", "0"),
      ("1000", "1000"),
      ("2500", "2500"),
      ("5000", "5000"),
    )
    forAll(dataTable) { (estimatedQualifiedBusinessIncomeDeduction, qualifiedBusinessIncomeDeduction) =>
      val graph = makeGraphWith(
        factDictionary,
        Path("/estimatedQualifiedBusinessIncomeDeduction") -> Dollar(estimatedQualifiedBusinessIncomeDeduction),
      )

      val actualQBID = graph.get("/qualifiedBusinessIncomeDeduction")

      assert(actualQBID.value.contains(Dollar(qualifiedBusinessIncomeDeduction)))
    }
    println(s"Completed ${dataTable.length} tests for calculating QBI floor")
  }
}

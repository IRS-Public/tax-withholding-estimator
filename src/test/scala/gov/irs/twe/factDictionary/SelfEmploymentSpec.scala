package gov.irs.twe.factDictionary

import gov.irs.factgraph.types.Dollar
import gov.irs.factgraph.types.Enum
import gov.irs.factgraph.FactDictionaryForTests
import gov.irs.factgraph.Path
import gov.irs.twe.FileLoaderHelper
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.prop.TableDrivenPropertyChecks

class SelfEmploymentSpec extends AnyFunSuite with TableDrivenPropertyChecks {
  val factDictionary = setupFactDictionary()
  val netEarningsFromSelfEmployment = Path("/netSelfEmploymentIncome")
  val selfEmploymentTaxResult = Path("/selfEmploymentTax")

  val dataTable = Table(
    ("netIncome", "expectedSelfEmploymentTax"),
    ///////////////////////////////////////////////////////////////////////////////
    ("50000", "7064.78"),
    ("30000", "4238.86"),
    ("10000", "1412.96"),
    // Test income beyond 2025 threshold $176,100
    ("190000", "26846.14"),
  )

  test("test selfEmploymentTaxResult") {
    forAll(dataTable) { (netIncome, expectedSelfEmploymentTax) =>
      val graph = makeGraphWith(
        factDictionary,
        netEarningsFromSelfEmployment -> Dollar(netIncome),
      )

      val actual = graph.get(selfEmploymentTaxResult)
      assert(actual.value.contains(Dollar(expectedSelfEmploymentTax)))
    }
  }

  val dataTableAboveThresholds = Table(
    ("netIncome", "expectedSelfEmploymentTax", "filingStatus"),
    ///////////////////////////////////////////////////////////////////////////////
    ("300000", "30564.3", "single"),
    ("270000", "29067.40", "marriedFilingJointly"),
    ("150000", "21316.04", "marriedFilingSeparately"),
  )
  test("test selfEmploymentTaxResult for all filing statuses") {
    forAll(dataTableAboveThresholds) { (netIncome, expectedSelfEmploymentTax, filingStatus) =>
      val graph = makeGraphWith(
        factDictionary,
        Path("/filingStatus") -> Enum(filingStatus, "/filingStatusOptions"),
        netEarningsFromSelfEmployment -> Dollar(netIncome),
      )

      val actual = graph.get(selfEmploymentTaxResult)
      assert(actual.value.contains(Dollar(expectedSelfEmploymentTax)))
    }
  }
}

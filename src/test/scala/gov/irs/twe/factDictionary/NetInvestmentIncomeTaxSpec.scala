package gov.irs.twe.factDictionary

import gov.irs.factgraph.types.{ Dollar, Dollar as status, Enum }
import gov.irs.factgraph.FactDictionaryForTests
import gov.irs.factgraph.Path
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.prop.TableDrivenPropertyChecks

class NetInvestmentIncomeTaxSpec extends AnyFunSuite with TableDrivenPropertyChecks {
  val factDictionary = setupFactDictionary()
  val single = Enum("single", "/filingStatusOptions")
  val mfs = Enum("marriedFilingSeparately", "/filingStatusOptions")
  val mfj = Enum("marriedFilingJointly", "/filingStatusOptions")
  val qss = Enum("qualifiedSurvivingSpouse", "/filingStatusOptions")
  val hoh = Enum("headOfHousehold", "/filingStatusOptions")

  // this table accounts for totalJobIncome as a lump fact, eventually it will be split by self and spouse
  val dataTable = Table(
    (
      "status",
      "agi",
      "totalInvestmentIncomeForm8960Line8",
      "netInvestmentIncomeTax",
    ),
    (single, "190000", "10000", "0"),
    (single, "200000", "50000", "0"),
    (single, "220000", "0", "0"),
    (single, "220000", "5000", "190"),
    (hoh, "220000", "30000", "760"),
    (hoh, "300000", "15000", "570"),
    (hoh, "300000", "150000", "3800"),
    (hoh, "500000", "20000", "760"),
    (mfj, "240000", "20000", "0"),
    (mfj, "250000", "30000", "0"),
    (mfj, "270000", "0", "0"),
    (qss, "270000", "10000", "380"),
    (qss, "270000", "40000", "760"),
    (qss, "400000", "5000", "190"),
    (qss, "400000", "200000", "5700"),
    (mfs, "120000", "5000", "0"),
    (mfs, "130000", "0", "0"),
    (mfs, "130000", "2000", "76"),
    (mfs, "150000", "30000", "950"),
    (mfs, "200000", "10000", "380"),
  )

  test("test net investment income scenarios") {
    forAll(dataTable) {
      (
          status,
          agi,
          totalInvestmentIncomeForm8960Line8,
          netInvestmentIncomeTax,
      ) =>
        val graph = makeGraphWith(
          factDictionary,
          Path("/filingStatus") -> status,
          Path("/agi") -> Dollar(agi),
          Path("/totalInvestmentIncomeForm8960Line8") -> Dollar(totalInvestmentIncomeForm8960Line8),
        )
        val actual = graph.get("/netInvestmentIncomeTax")

        assert(actual.value.contains(Dollar(netInvestmentIncomeTax)))

    }
    println(s"Completed ${dataTable.length} tests for calculating net investment income tax ")
  }
}

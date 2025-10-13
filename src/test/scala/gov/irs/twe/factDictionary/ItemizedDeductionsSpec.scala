package gov.irs.twe.factDictionary

import gov.irs.factgraph.types.Dollar
import gov.irs.factgraph.types.Enum
import gov.irs.factgraph.Path
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.prop.TableDrivenPropertyChecks

class ItemizedDeductionsSpec extends AnyFunSuite with TableDrivenPropertyChecks {
  val factDictionary = setupFactDictionary()
  val single = Enum("single", "/filingStatusOptions")
  val mfs = Enum("marriedFilingSeparately", "/filingStatusOptions")
  val mfj = Enum("marriedFilingJointly", "/filingStatusOptions")
  val qss = Enum("qualifiedSurvivingSpouse", "/filingStatusOptions")
  val hoh = Enum("headOfHousehold", "/filingStatusOptions")

  val dataTable = Table(
    ("status", "income", "itemizedDeductionTotal", "expectedItemizedDeduction"),
    ///////////////////////////////////////////////////////////////////////////////
    /////////// SINGLE/////////////////////////////////////////////////////////
    // low income, no itemized deductions
    (single, "10000", "0", "0"),
    // low income, full deduction
    (single, "10000", "1000", "1000"),
    // high income, no itemized deductions
    (single, "700000", "0", "0"),
    // high income but below threshold = full deduction
    (single, "625000", "5000", "5000"),
    // high income at threshold = full deduction
    (single, "626350", "5000", "5000"),
    // slightly above threshold, but not enough to trigger reduction
    (single, "626351", "5000", "5000"),
    // slightly above threshold with minimal reduction
    (single, "626364", "5000", "4999"),
    // well above threshold with no itemized
    (single, "1000000", "0", "0"),
    // well above threshold with itemized less than excess - 2/37 * itemized total (5000)
    (single, "1000000", "5000", "4730"),
    // well above threshold with itemized less than excess - 2/37 * itemized total (1000)
    (single, "1000000", "10000", "9459"),
    // slightly above threshold with itemized greater than excess - 2/37 * excess total (100)
    (single, "626450", "1000", "995"),

    /////////// MFS/////////////////////////////////////////////////////////
    // low income, no itemized deductions
    (hoh, "10000", "0", "0"),
    // low income, full deduction
    (hoh, "10000", "1000", "1000"),
    // high income, no itemized deductions
    (hoh, "700000", "0", "0"),
    // high income but below threshold = full deduction
    (hoh, "625000", "5000", "5000"),
    // high income at threshold = full deduction
    (hoh, "626350", "5000", "5000"),
    // slightly above threshold, but not enough to trigger reduction
    (hoh, "626351", "5000", "5000"),
    // slightly above threshold with minimal reduction
    (hoh, "626364", "5000", "4999"),
    // well above threshold with no itemized
    (hoh, "1000000", "0", "0"),
    // well above threshold with itemized less than excess - 2/37 * itemized total (5000)
    (hoh, "1000000", "5000", "4730"),
    // well above threshold with itemized less than excess - 2/37 * itemized total (1000)
    (hoh, "1000000", "10000", "9459"),
    // slightly above threshold with itemized greater than excess - 2/37 * excess total (100)
    (hoh, "626450", "1000", "995"),

    /////////// MFS/////////////////////////////////////////////////////////
    // low income, no itemized deductions
    (mfs, "10000", "0", "0"),
    // low income, full deduction
    (mfs, "10000", "1000", "1000"),
    // high income, no itemized deductions
    (mfs, "700000", "0", "0"),
    // high income but below threshold = full deduction
    (mfs, "300000", "5000", "5000"),
    // high income at threshold = full deduction
    (mfs, "375800", "5000", "5000"),
    // slightly above threshold, but not enough to trigger reduction
    (mfs, "375801", "5000", "5000"),
    // slightly above threshold with minimal reduction
    (mfs, "375814", "5000", "4999"),
    // well above threshold with no itemized
    (mfs, "1000000", "0", "0"),
    // well above threshold with itemized less than excess - 2/37 * itemized total (5000)
    (mfs, "1000000", "5000", "4730"),
    // well above threshold with itemized less than excess - 2/37 * itemized total (1000)
    (mfs, "1000000", "10000", "9459"),
    // slightly above threshold with itemized greater than excess - 2/37 * excess total (100)
    (mfs, "375914", "1000", "994"),

    /////////// QSS/////////////////////////////////////////////////////////
    // low income, no itemized deductions
    (qss, "10000", "0", "0"),
    // low income, full deduction
    (qss, "10000", "1000", "1000"),
    // high income, no itemized deductions
    (qss, "851600", "0", "0"),
    // high income but below threshold = full deduction
    (qss, "750600", "5000", "5000"),
    // high income at threshold = full deduction
    (qss, "751600", "5000", "5000"),
    // slightly above threshold, but not enough to trigger reduction
    (qss, "751601", "5000", "5000"),
    // slightly above threshold with minimal reduction
    (qss, "751614", "5000", "4999"),
    // well above threshold with no itemized
    (qss, "1000000", "0", "0"),
    // well above threshold with itemized less than excess - 2/37 * itemized total (5000)
    (qss, "1000000", "5000", "4730"),
    // well above threshold with itemized less than excess - 2/37 * itemized total (1000)
    (qss, "1000000", "10000", "9459"),
    // slightly above threshold with itemized greater than excess - 2/37 * excess total (100)
    (qss, "751700", "1000", "995"),

    /////////// MFJ/////////////////////////////////////////////////////////
    // low income, no itemized deductions
    (mfj, "10000", "0", "0"),
    // low income, full deduction
    (mfj, "10000", "1000", "1000"),
    // high income, no itemized deductions
    (mfj, "851600", "0", "0"),
    // high income but below threshold = full deduction
    (mfj, "750600", "5000", "5000"),
    // high income at threshold = full deduction
    (mfj, "751600", "5000", "5000"),
    // slightly above threshold, but not enough to trigger reduction
    (mfj, "751601", "5000", "5000"),
    // slightly above threshold with minimal reduction
    (mfj, "751614", "5000", "4999"),
    // well above threshold with no itemized
    (mfj, "1000000", "0", "0"),
    // well above threshold with itemized less than excess - 2/37 * itemized total (5000)
    (mfj, "1000000", "5000", "4730"),
    // well above threshold with itemized less than excess - 2/37 * itemized total (1000)
    (mfj, "1000000", "10000", "9459"),
    // slightly above threshold with itemized greater than excess - 2/37 * excess total (100)
    (mfj, "751700", "1000", "995"),
  )

  test("test OB3: 70111 itemized deduction limitations calculation reduction correctly") {
    forAll(dataTable) { (status, income, itemizedDeductionTotal, expectedItemized) =>
      val graph = makeGraphWith(
        factDictionary,
        Path("/filingStatus") -> status,
        Path("/taxableIncomeExcludingItemizedDeductions") -> Dollar(income),
        Path("/itemizedDeductionTotal") -> Dollar(itemizedDeductionTotal),
      )

      val actual = graph.get("/itemizedDeduction")

      assert(actual.value.contains(Dollar(expectedItemized)))
    }
    println(s"Completed ${dataTable.length} tests for calculating itemized deduction limits")
  }
}

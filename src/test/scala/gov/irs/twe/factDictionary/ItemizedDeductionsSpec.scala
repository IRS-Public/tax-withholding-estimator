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
    (single, "657000", "5000", "4730"),
    // well above threshold with no itemized
    (single, "1000000", "0", "0"),
    // well above threshold with itemized less than excess - 2/37 * itemized total (5000)
    (single, "1000000", "5000", "4730"),
    // well above threshold with itemized less than excess - 2/37 * itemized total (1000)
    (single, "1000000", "10000", "9459"),
    // slightly above threshold with itemized greater than excess - 2/37 * excess total (100)
    (single, "657000", "1000", "946"),

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
    (hoh, "657000", "5000", "4730"),
    // well above threshold with no itemized
    (hoh, "1000000", "0", "0"),
    // well above threshold with itemized less than excess - 2/37 * itemized total (5000)
    (hoh, "1000000", "5000", "4730"),
    // well above threshold with itemized less than excess - 2/37 * itemized total (1000)
    (hoh, "1000000", "10000", "9459"),
    // slightly above threshold with itemized greater than excess - 2/37 * excess total (100)
    (hoh, "657000", "1000", "946"),

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
    (mfs, "657000", "5000", "4730"),
    // well above threshold with no itemized
    (mfs, "1000000", "0", "0"),
    // well above threshold with itemized less than excess - 2/37 * itemized total (5000)
    (mfs, "1000000", "5000", "4730"),
    // well above threshold with itemized less than excess - 2/37 * itemized total (1000)
    (mfs, "1000000", "10000", "9459"),
    // slightly above threshold with itemized greater than excess - 2/37 * excess total (100)
    (mfs, "657000", "1000", "946"),

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
    (qss, "771614", "5000", "4842"),
    // well above threshold with no itemized
    (qss, "1000000", "0", "0"),
    // well above threshold with itemized less than excess - 2/37 * itemized total (5000)
    (qss, "1000000", "5000", "4730"),
    // well above threshold with itemized less than excess - 2/37 * itemized total (1000)
    (qss, "1000000", "10000", "9459"),
    // slightly above threshold with itemized greater than excess - 2/37 * excess total (100)
    (qss, "771614", "1000", "946"),

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
    (mfj, "771614", "5000", "4842"),
    // well above threshold with no itemized
    (mfj, "1000000", "0", "0"),
    // well above threshold with itemized less than excess - 2/37 * itemized total (5000)
    (mfj, "1000000", "5000", "4730"),
    // well above threshold with itemized less than excess - 2/37 * itemized total (1000)
    (mfj, "1000000", "10000", "9459"),
    // slightly above threshold with itemized greater than excess - 2/37 * excess total (100)
    (mfj, "771614", "1000", "946"),
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
  }

  test("test OB3 70120: simple SALT cap") {
    val dataTable = Table(
      ("stateAndLocalTaxPayments", "expectedStateAndLocalTaxPayments"),
      ("10000", "10000"),
      ("30000", "30000"),
      ("40400", "40400"),
      ("50000", "40400"),
    )
    forAll(dataTable) { (stateAndLocalTaxPayments, expectedStateAndLocalTaxPayments) =>
      val graph = makeGraphWith(
        factDictionary,
        Path("/stateAndLocalTaxPayments") -> Dollar(stateAndLocalTaxPayments),
      )

      val actualSALT = graph.get("/stateAndLocalTaxDeduction")

      assert(actualSALT.value.contains(Dollar(expectedStateAndLocalTaxPayments)))
    }
  }

  test("test OB3 70120: SALT phase down for high earners") {
    val dataTable = Table(
      ("status", "agi", "stateAndLocalTaxPayments", "expectedStateAndLocalTaxPaymentsTotal"),
      // Initial Examples (No Phase-out)
      (single, "100000", "10000", "10000"), // Below AGI threshold, below SALT Cap
      (mfs, "100000", "30000", "20200"), // Below AGI threshold, capped at MFS limit ($20,200)

      // **NON-MFS (Threshold: $505,000, Cap: $40,400, Floor: $10,000)**
      // Just under threshold: Should deduct full cap or actual payments
      (single, "500000", "50000", "40400"), // Capped at $40,400
      (single, "500000", "30000", "30000"), // Deduct full $30,000

      // AGI exceeds threshold, phase-down begins (Reduction: $13,500)
      // AGI: $550,000. Reduction = (550,000 - 505,000) * 0.30 = $13,500
      (single, "550000", "40400", "26900"), // $40,400 - $13,500 = $26,900
      (single, "550000", "50000", "26900"), // Cap of $40,400 used for phase-down base

      // AGI exceeds threshold, deduction is phased down to above the floor (Reduction: $28,500)
      // AGI: $600,000. Reduction = (600,000 - 505,000) * 0.30 = $28,500
      (single, "600000", "40400", "11900"), // $40,400 - $28,500 = $11,900

      // AGI exceeds threshold, deduction is phased down below the floor (Reduction: $39,000)
      // AGI: $635,000. Reduction = (635,000 - 505,000) * 0.30 = $39,000
      (single, "635000", "40400", "10000"), // Max($10,000, $40,400 - $39,000 = $1,400) = $10,000
      (single, "650000", "40400", "10000"), // AGI where deduction hits minimum floor and stays there

      // **MFS (Threshold: $505,000, Cap: $40,400, Floor: $5,000)**
      // AGI: $550,000. Reduction = $13,500
      (mfs, "550000", "20200", "5000"),

      // AGI exceeds threshold, deduction is phased down below the floor (Reduction: $28,500)
      // AGI: $600,000. Reduction = $28,500
      (mfs, "600000", "20200", "5000"), // Max($5,000, $20,200 - $28,500 = -$8,300) = $5,000
    )
    forAll(dataTable) { (status, agi, stateAndLocalTaxPayments, expectedStateAndLocalTaxPaymentsTotal) =>
      val graph = makeGraphWith(
        factDictionary,
        Path("/filingStatus") -> status,
        Path("/agi") -> Dollar(agi),
        Path("/stateAndLocalTaxPayments") -> Dollar(stateAndLocalTaxPayments),
      )

      val actualSALTDeduction = graph.get("/stateAndLocalTaxDeduction")

      assert(actualSALTDeduction.value.contains(Dollar(expectedStateAndLocalTaxPaymentsTotal)))
    }
  }
  test("test OB3: qualified mortgage insurance premium phaseout") {
    val dataTable = Table(
      ("status", "agi", "premiums", "expectedDeduction"),
      (single, "95000", "2000", "2000.00"),
      (single, "100000", "2000", "2000.00"),
      (single, "100001", "2000", "2000.00"),
      (single, "101000", "2000", "1800.00"),
      (single, "101001", "2000", "1800.00"),
      (single, "102000", "2000", "1600.00"),
      (single, "102001", "2000", "1600.00"),
      (single, "105000", "2000", "1000.00"),
      (single, "105001", "2000", "1000.00"),
      (single, "109000", "2000", "200.00"),
      (single, "109001", "2000", "200.00"),
      (single, "110000", "2000", "0.00"),
      (single, "110001", "2000", "0.00"),
      (mfs, "49000", "3000", "3000"),
      (mfs, "50000", "3000", "3000"),
      (mfs, "50001", "3000", "3000"),
      (mfs, "50500", "3000", "2700"),
      (mfs, "50501", "3000", "2700"),
      (mfs, "51001", "3000", "2400"),
      (mfs, "54500", "3000", "300"),
      (mfs, "54501", "3000", "300"),
      (mfs, "55000", "3000", "0"),
      (mfs, "60000", "3000", "0"),
    )

    forAll(dataTable) { (status, agi, premiums, expectedDeduction) =>
      val graph = makeGraphWith(
        factDictionary,
        Path("/filingStatus") -> status,
        Path("/agi") -> Dollar(agi),
        Path("/qualifiedMortgageInsurancePremiums") -> Dollar(premiums),
      )

      val actual = graph.get("/qualifiedMortgageInsurancePremiumDeductionTotal")
      assert(actual.value.contains(Dollar(expectedDeduction)))
    }
  }

}

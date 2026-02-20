package gov.irs.twe.factDictionary

import gov.irs.factgraph.types.Collection
import gov.irs.factgraph.types.Dollar
import gov.irs.factgraph.types.Enum
import gov.irs.factgraph.FactDictionaryForTests
import gov.irs.factgraph.Path
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.prop.TableDrivenPropertyChecks
import scala.sys.BooleanProp

class socialSecurityBenefitsSpec extends AnyFunSuite with TableDrivenPropertyChecks {
  val factDictionary = setupFactDictionary()
  val filingStatus = Path("/filingStatus")

  val single = Enum("single", "/filingStatusOptions")
  val mfs = Enum("marriedFilingSeparately", "/filingStatusOptions")
  val mfj = Enum("marriedFilingJointly", "/filingStatusOptions")
  val qss = Enum("qualifiedSurvivingSpouse", "/filingStatusOptions")
  val hoh = Enum("headOfHousehold", "/filingStatusOptions")

  // Social Securty Benefits calculation values
  val magiSSABenefits = Path("/pub915MAGIForBenefitsTaxableAmountLine8")
  val benefitsTaxableAmount = Path("/ssaBenefitsTaxableAmount")
  val benefitsTotal = Path("/socialSecurityBenefitsIncome")

  val isSeparateLivingTogether = Path("/isMFSLivedTogether")

  val dataTable = Table(
    (
      "filingStatus",
      "isMFSLivedTogether",
      "socialSecurityBenefitsIncome",
      "pub915MAGIForBenefitsTaxableAmountLine8",
      "expectedSSABenefitsTaxableAmount",
    ),
    // Single, SSA AGI more than 34K. Taxable amount up to 85% of benefits total.
    (single, false, "6000.00", "38000.00", "5100.00"),

    // Single, SSA AGI more than 25K, less than 34K. Taxable amount up to 50% of benefits total.
    // example from Pub915 page 7
    (single, false, "5980.00", "31980.00", "2990.00"),

    // Single, SSA AGI more than 25K, less than 34K. Taxable amount < 50% of benefits total.
    (single, false, "6000.00", "27000.00", "1000.00"),

    // Single, SSA AGI less than 25K. Taxable amount = $0.00
    (single, false, "5980.00", "24980.00", "0.00"),

    // MFJ, SSA AGI less than $32000.00. Taxable amount = $0.00
    // example from Pub915 page 8
    (mfj, false, "5600.00", "31550.00", "0.00"),

    // MFJ, SSA AGI > 44K, Portion 1 (between 32K and 44K = 12K) is taxed at 50% but not to exceed 50% of SSA benefits total.
    // Portion 2 (exceeding 44K) is taxed at 85%, but total = Portion 1 + Portion 2 not to exceed 85% of SSA benefits total
    // example from Pub915 page 9
    (mfj, false, "10000.00", "45500.00", "6275.00"),

    // MFS living together. All SSA benefits are taxed at up to 85%, no lower/upper limits for SSA AGI apply/
    // example from Pub915 page 10
    (mfs, true, "4000.00", "10000.00", "3400.00"),

    // MFS living separately. The same calculation as for Single
    (mfs, false, "4000.00", "10000.00", "0.00"),

    // Head of Household, same calculation as for Single. SSA AGI more than 25K, less than 34K. Taxable amount < 50% of benefits total.
    (hoh, false, "6000.00", "27000.00", "1000.00"),

    // Qualified surviving spouse, same calculation as for Single. SSA AGI more than 25K, less than 34K. Taxable amount up to 50% of benefits total.
    (qss, false, "5980.00", "31980.00", "2990.00"),
  )

  test("test expectedSSABenefitsTaxableAmount") {
    forAll(dataTable) {
      (
          status,
          isMFSLivedTogether,
          socialSecurityBenefitsIncome,
          pub915MAGIForBenefitsTaxableAmountLine8,
          expectedSSABenefitsTaxableAmount,
      ) =>
        val graph = makeGraphWith(
          factDictionary,
          filingStatus -> status,
          isSeparateLivingTogether -> isMFSLivedTogether,
          benefitsTotal -> Dollar(socialSecurityBenefitsIncome),
          magiSSABenefits -> Dollar(pub915MAGIForBenefitsTaxableAmountLine8),
        )

        val actual = graph.get(benefitsTaxableAmount)
        assert(actual.value.contains(Dollar(expectedSSABenefitsTaxableAmount)))
    }
  }
}

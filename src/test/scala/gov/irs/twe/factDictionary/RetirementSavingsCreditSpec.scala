package gov.irs.twe.factDictionary

import gov.irs.factgraph.{ FactDictionary, Path }
import gov.irs.factgraph.types.{ Dollar, Enum }
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.prop.TableDrivenPropertyChecks
import scala.math.Numeric.Implicits.infixNumericOps

class RetirementSavingsCreditSpec extends AnyFunSuite with TableDrivenPropertyChecks {
  val factDictionary: FactDictionary = setupFactDictionary()
  val single = Enum("single", "/filingStatusOptions")
  val mfs = Enum("marriedFilingSeparately", "/filingStatusOptions")
  val mfj = Enum("marriedFilingJointly", "/filingStatusOptions")
  val qss = Enum("qualifiedSurvivingSpouse", "/filingStatusOptions")
  val hoh = Enum("headOfHousehold", "/filingStatusOptions")

  test(
    "/isEligibleForRetirementSavingsContributionsCredit when agi is less than or equal to the corresponding threshold",
  ) {
    val parameterizedTests = Table(
      ("filingStatus", "thresholdFactPath"),
      (single, "/retirementSavingsContributionsTaxCreditIncomeThresholdSingle"),
      (mfs, "/retirementSavingsContributionsTaxCreditIncomeThresholdSingle"),
      (qss, "/retirementSavingsContributionsTaxCreditIncomeThresholdSingle"),
      (hoh, "/retirementSavingsContributionsTaxCreditIncomeThresholdHOH"),
      (mfj, "/retirementSavingsContributionsTaxCreditIncomeThresholdMFJ"),
    )

    forAll(parameterizedTests) {
      (
          filingStatus,
          thresholdFactPath,
      ) =>
        // given
        val graph = makeGraphWith(
          factDictionary,
          Path("/filingStatus") -> filingStatus,
        )
        val threshold = graph.get(thresholdFactPath).get.asInstanceOf[Dollar]
        val agi = threshold
        graph.set("/agi", agi)

        // when
        val isEligibleForRetirementSavingsContributionsCredit =
          graph.get("/isEligibleForRetirementSavingsContributionsCredit")

        // then
        assert(isEligibleForRetirementSavingsContributionsCredit.complete)
        assert(isEligibleForRetirementSavingsContributionsCredit.hasValue)
        assert(isEligibleForRetirementSavingsContributionsCredit.get == true)
    }
  }

  test(
    "NOT /isEligibleForRetirementSavingsContributionsCredit when agi is greater than the corresponding threshold",
  ) {
    val parameterizedTests = Table(
      ("filingStatus", "thresholdFactPath"),
      (single, "/retirementSavingsContributionsTaxCreditIncomeThresholdSingle"),
      (mfs, "/retirementSavingsContributionsTaxCreditIncomeThresholdSingle"),
      (qss, "/retirementSavingsContributionsTaxCreditIncomeThresholdSingle"),
      (hoh, "/retirementSavingsContributionsTaxCreditIncomeThresholdHOH"),
      (mfj, "/retirementSavingsContributionsTaxCreditIncomeThresholdMFJ"),
    )

    forAll(parameterizedTests) {
      (
          filingStatus,
          thresholdFactPath,
      ) =>
        // given
        val graph = makeGraphWith(
          factDictionary,
          Path("/filingStatus") -> filingStatus,
        )
        val threshold = graph.get(thresholdFactPath).get.asInstanceOf[Dollar]
        val agi = threshold + Dollar(1)
        graph.set("/agi", agi)

        // when
        val isEligibleForRetirementSavingsContributionsCredit =
          graph.get("/isEligibleForRetirementSavingsContributionsCredit")

        // then
        assert(isEligibleForRetirementSavingsContributionsCredit.complete)
        assert(isEligibleForRetirementSavingsContributionsCredit.hasValue)
        assert(isEligibleForRetirementSavingsContributionsCredit.get == false)
    }
  }
}

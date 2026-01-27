package gov.irs.twe.factDictionary

import gov.irs.factgraph.{ FactDictionary, Path }
import gov.irs.factgraph.types.{ Dollar, Enum }
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.prop.TableDrivenPropertyChecks
import scala.math.Numeric.Implicits.infixNumericOps

class ElderlyAndDisabledCreditSpec extends AnyFunSuite with TableDrivenPropertyChecks {
  val factDictionary: FactDictionary = setupFactDictionary()
  val single = Enum("single", "/filingStatusOptions")
  val mfs = Enum("marriedFilingSeparately", "/filingStatusOptions")
  val mfj = Enum("marriedFilingJointly", "/filingStatusOptions")
  val qss = Enum("qualifiedSurvivingSpouse", "/filingStatusOptions")
  val hoh = Enum("headOfHousehold", "/filingStatusOptions")

  test("MFJ /isTentativelyEligibleForEdc with agi below \"both qualified\" threshold") {
    // given
    val graph = makeGraphWith(
      factDictionary,
      Path("/filingStatus") -> mfj,
    )
    val edcAgiThresholdMFJBothQualified = graph.get("/edcAgiThresholdMFJBothQualified").get.asInstanceOf[Dollar]
    val agi = edcAgiThresholdMFJBothQualified - Dollar(1)
    graph.set("/agi", agi)

    // when
    val isTentativelyEligibleForEdc = graph.get("/isTentativelyEligibleForEdc")

    // then
    assert(isTentativelyEligibleForEdc.complete)
    assert(isTentativelyEligibleForEdc.hasValue)
    assert(isTentativelyEligibleForEdc.get == true)
  }

  test("MFJ /isTentativelyEligibleForEdc with agi at or above the \"both qualified\" threshold") {
    // given
    val graph = makeGraphWith(
      factDictionary,
      Path("/filingStatus") -> mfj,
    )
    val edcAgiThresholdMFJBothQualified = graph.get("/edcAgiThresholdMFJBothQualified").get.asInstanceOf[Dollar]
    val agi = edcAgiThresholdMFJBothQualified
    graph.set("/agi", agi)

    // when
    val isTentativelyEligibleForEdc = graph.get("/isTentativelyEligibleForEdc")

    // then
    assert(isTentativelyEligibleForEdc.complete)
    assert(isTentativelyEligibleForEdc.hasValue)
    assert(isTentativelyEligibleForEdc.get == false)
  }

  test(
    "MFJ /isEligibleForEdc with agi below \"both qualified\" threshold when both filers self-certify eligibility",
  ) {
    // given
    val graph = makeGraphWith(
      factDictionary,
      Path("/filingStatus") -> mfj,
    )
    val edcAgiThresholdMFJBothQualified = graph.get("/edcAgiThresholdMFJBothQualified").get.asInstanceOf[Dollar]
    val agi = edcAgiThresholdMFJBothQualified - Dollar(1)
    graph.set("/agi", agi)
    graph.set("/primaryFilerIsClaimingEdc", true)
    graph.set("/secondaryFilerIsClaimingEdc", true)

    // when
    val isEligibleForEdc = graph.get("/isEligibleForEdc")

    // then
    assert(isEligibleForEdc.complete)
    assert(isEligibleForEdc.hasValue)
    assert(isEligibleForEdc.get == true)
  }

  test(
    "MFJ /isEligibleForEdc with agi at or above \"both qualified\" threshold when both filers self-certify eligibility",
  ) {
    // given
    val graph = makeGraphWith(
      factDictionary,
      Path("/filingStatus") -> mfj,
    )
    val edcAgiThresholdMFJBothQualified = graph.get("/edcAgiThresholdMFJBothQualified").get.asInstanceOf[Dollar]
    val agi = edcAgiThresholdMFJBothQualified
    graph.set("/agi", agi)
    graph.set("/primaryFilerIsClaimingEdc", true)
    graph.set("/secondaryFilerIsClaimingEdc", true)

    // when
    val isEligibleForEdc = graph.get("/isEligibleForEdc")

    // then
    assert(isEligibleForEdc.complete)
    assert(isEligibleForEdc.hasValue)
    assert(isEligibleForEdc.get == false)
  }

  test(
    "MFJ /isEligibleForEdc with agi at or above \"one qualified\" threshold when one filer self-certifies eligibility",
  ) {
    // given
    val graph = makeGraphWith(
      factDictionary,
      Path("/filingStatus") -> mfj,
    )
    val edcAgiThresholdMFJOneQualified = graph.get("/edcAgiThresholdMFJOneQualified").get.asInstanceOf[Dollar]
    val agi = edcAgiThresholdMFJOneQualified
    graph.set("/agi", agi)
    graph.set("/primaryFilerIsClaimingEdc", true)
    graph.set("/secondaryFilerIsClaimingEdc", false)

    // when
    val isEligibleForEdc = graph.get("/isEligibleForEdc")

    // then
    assert(isEligibleForEdc.complete)
    assert(isEligibleForEdc.hasValue)
    assert(isEligibleForEdc.get == false)
  }

  test(
    "MFJ /isEligibleForEdc with agi below \"one qualified\" threshold when one filer self-certifies eligibility",
  ) {
    // given
    val graph = makeGraphWith(
      factDictionary,
      Path("/filingStatus") -> mfj,
    )
    val edcAgiThresholdMFJOneQualified = graph.get("/edcAgiThresholdMFJOneQualified").get.asInstanceOf[Dollar]
    val agi = edcAgiThresholdMFJOneQualified - Dollar(1)
    graph.set("/agi", agi)
    graph.set("/primaryFilerIsClaimingEdc", true)
    graph.set("/secondaryFilerIsClaimingEdc", false)

    // when
    val isEligibleForEdc = graph.get("/isEligibleForEdc")

    // then
    assert(isEligibleForEdc.complete)
    assert(isEligibleForEdc.hasValue)
    assert(isEligibleForEdc.get == true)
  }

  test("Single/HOH/QSS eligibility with agi below threshold") {
    val dataTable = Table(
      "filingStatus",
      single,
      hoh,
      qss,
    )

    forAll(dataTable) { (filingStatus) =>
      // given
      val graph = makeGraphWith(
        factDictionary,
        Path("/filingStatus") -> filingStatus,
      )
      val edcAgiThresholdSingle = graph.get("/edcAgiThresholdSingle").get.asInstanceOf[Dollar]
      val agi = edcAgiThresholdSingle - Dollar(1)
      graph.set("/agi", agi)

      // when
      val isTentativelyEligibleForEdc = graph.get("/isTentativelyEligibleForEdc")
      val isEligibleForEdc = graph.get("/isEligibleForEdc")

      // then
      assert(isTentativelyEligibleForEdc.complete)
      assert(isTentativelyEligibleForEdc.hasValue)
      assert(isTentativelyEligibleForEdc.get == true)

      assert(isEligibleForEdc.complete)
      assert(isEligibleForEdc.hasValue)
      assert(isEligibleForEdc.get == true)
    }
  }

  test("Single/HOH/QSS eligibility with agi at or above threshold") {
    val dataTable = Table(
      "filingStatus",
      single,
      hoh,
      qss,
    )

    forAll(dataTable) { filingStatus =>
      // given
      val graph = makeGraphWith(
        factDictionary,
        Path("/filingStatus") -> filingStatus,
      )
      val edcAgiThresholdSingle = graph.get("/edcAgiThresholdSingle").get.asInstanceOf[Dollar]
      val agi = edcAgiThresholdSingle
      graph.set("/agi", agi)

      // when
      val isTentativelyEligibleForEdc = graph.get("/isTentativelyEligibleForEdc")
      val isEligibleForEdc = graph.get("/isEligibleForEdc")

      // then
      assert(isTentativelyEligibleForEdc.complete)
      assert(isTentativelyEligibleForEdc.hasValue)
      assert(isTentativelyEligibleForEdc.get == false)

      assert(isEligibleForEdc.complete)
      assert(isEligibleForEdc.hasValue)
      assert(isEligibleForEdc.get == false)
    }
  }

  test("MFS (lived apart all year) eligibility with agi below threshold") {
    // given
    val graph = makeGraphWith(
      factDictionary,
      Path("/filingStatus") -> mfs,
      Path("/isMFSLivedTogether") -> false,
    )
    val edcAgiThresholdMFS = graph.get("/edcAgiThresholdMFS").get.asInstanceOf[Dollar]
    val agi = edcAgiThresholdMFS - Dollar(1)
    graph.set("/agi", agi)

    // when
    val isTentativelyEligibleForEdc = graph.get("/isTentativelyEligibleForEdc")
    val isEligibleForEdc = graph.get("/isEligibleForEdc")

    // then
    assert(isTentativelyEligibleForEdc.complete)
    assert(isTentativelyEligibleForEdc.hasValue)
    assert(isTentativelyEligibleForEdc.get == true)

    assert(isEligibleForEdc.complete)
    assert(isEligibleForEdc.hasValue)
    assert(isEligibleForEdc.get == true)
  }

  test("MFS (lived apart all year) eligibility with agi at or above threshold") {
    // given
    val graph = makeGraphWith(
      factDictionary,
      Path("/filingStatus") -> mfs,
      Path("/isMFSLivedTogether") -> false,
    )
    val edcAgiThresholdMFS = graph.get("/edcAgiThresholdMFS").get.asInstanceOf[Dollar]
    val agi = edcAgiThresholdMFS
    graph.set("/agi", agi)

    // when
    val isTentativelyEligibleForEdc = graph.get("/isTentativelyEligibleForEdc")
    val isEligibleForEdc = graph.get("/isEligibleForEdc")

    // then
    assert(isTentativelyEligibleForEdc.complete)
    assert(isTentativelyEligibleForEdc.hasValue)
    assert(isTentativelyEligibleForEdc.get == false)

    assert(isEligibleForEdc.complete)
    assert(isEligibleForEdc.hasValue)
    assert(isEligibleForEdc.get == false)
  }
}

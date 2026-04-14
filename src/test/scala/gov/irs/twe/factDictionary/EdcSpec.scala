package gov.irs.twe.factDictionary

import gov.irs.factgraph.types.{ Dollar, Enum }
import gov.irs.factgraph.Path
import org.scalatest.funsuite.AnyFunSuite

class EdcSpec extends AnyFunSuite {
  private val factDictionary = setupFactDictionary()
  private val single = Enum("single", "/filingStatusOptions")
  private val mfs = Enum("marriedFilingSeparately", "/filingStatusOptions")
  private val mfj = Enum("marriedFilingJointly", "/filingStatusOptions")
  private val qss = Enum("qualifiedSurvivingSpouse", "/filingStatusOptions")
  private val hoh = Enum("headOfHousehold", "/filingStatusOptions")

  private val mfsAndLivedTogetherAgiLimit = 12500
  private val mfjAndBothQualifyAgiLimit = 25000
  private val otherFilingStatusAgiLimit = 17500

  test("/flowIsEligibleForEDC is true when AGI is below the limit for Single filers") {
    // given
    val graph = makeGraphWith(
      factDictionary,
      Path("/filingStatus") -> single,
      Path("/agi") -> Dollar(otherFilingStatusAgiLimit - 1),
    )

    // when
    val flowIsEligibleForEDC = graph.get("/flowIsEligibleForEDC")

    // then
    assert(flowIsEligibleForEDC.complete)
    assert(flowIsEligibleForEDC.hasValue)
    assert(flowIsEligibleForEDC.get == true)
  }

  test("/flowIsEligibleForEDC is false when AGI is at-or-above the limit for Single filers") {
    // given
    val graph = makeGraphWith(
      factDictionary,
      Path("/filingStatus") -> single,
      Path("/agi") -> Dollar(otherFilingStatusAgiLimit),
    )

    // when
    val flowIsEligibleForEDC = graph.get("/flowIsEligibleForEDC")

    // then
    assert(flowIsEligibleForEDC.complete)
    assert(flowIsEligibleForEDC.hasValue)
    assert(flowIsEligibleForEDC.get == false)
  }

  test("/flowIsEligibleForEDC is true when AGI is below the limit for MFS filers who did not live together") {
    // given
    val graph = makeGraphWith(
      factDictionary,
      Path("/filingStatus") -> mfs,
      Path("/agi") -> Dollar(mfsAndLivedTogetherAgiLimit - 1),
      Path("/isMFSLivedTogether") -> false,
    )

    // when
    val flowIsEligibleForEDC = graph.get("/flowIsEligibleForEDC")

    // then
    assert(flowIsEligibleForEDC.complete)
    assert(flowIsEligibleForEDC.hasValue)
    assert(flowIsEligibleForEDC.get == true)
  }

  test("/flowIsEligibleForEDC is false when AGI is at-or-above the limit for MFS filers who did not live together") {
    // given
    val graph = makeGraphWith(
      factDictionary,
      Path("/filingStatus") -> mfs,
      Path("/agi") -> Dollar(mfsAndLivedTogetherAgiLimit),
      Path("/isMFSLivedTogether") -> false,
    )

    // when
    val flowIsEligibleForEDC = graph.get("/flowIsEligibleForEDC")

    // then
    assert(flowIsEligibleForEDC.complete)
    assert(flowIsEligibleForEDC.hasValue)
    assert(flowIsEligibleForEDC.get == false)
  }

  test("/flowIsEligibleForEDC is false regardless of AGI for MFS filers who lived together") {
    // given
    val graph = makeGraphWith(
      factDictionary,
      Path("/filingStatus") -> mfs,
      Path("/agi") -> Dollar(0),
      Path("/isMFSLivedTogether") -> true,
    )

    // when
    val flowIsEligibleForEDC = graph.get("/flowIsEligibleForEDC")

    // then
    assert(flowIsEligibleForEDC.complete)
    assert(flowIsEligibleForEDC.hasValue)
    assert(flowIsEligibleForEDC.get == false)
  }

  test("/flowIsEligibleForEDC is true when AGI is below the limit for MFJ filers") {
    // given
    val graph = makeGraphWith(
      factDictionary,
      Path("/filingStatus") -> mfj,
      Path("/agi") -> Dollar(mfjAndBothQualifyAgiLimit - 1),
    )

    // when
    val flowIsEligibleForEDC = graph.get("/flowIsEligibleForEDC")

    // then
    assert(flowIsEligibleForEDC.complete)
    assert(flowIsEligibleForEDC.hasValue)
    assert(flowIsEligibleForEDC.get == true)
  }

  test("/flowIsEligibleForEDC is false when AGI is at-or-above the limit for MFJ filers") {
    // given
    val graph = makeGraphWith(
      factDictionary,
      Path("/filingStatus") -> mfj,
      Path("/agi") -> Dollar(mfjAndBothQualifyAgiLimit),
    )

    // when
    val flowIsEligibleForEDC = graph.get("/flowIsEligibleForEDC")

    // then
    assert(flowIsEligibleForEDC.complete)
    assert(flowIsEligibleForEDC.hasValue)
    assert(flowIsEligibleForEDC.get == false)
  }

  test("/flowIsEligibleForEDC is true when AGI is below the limit for QSS") {
    // given
    val graph = makeGraphWith(
      factDictionary,
      Path("/filingStatus") -> qss,
      Path("/agi") -> Dollar(otherFilingStatusAgiLimit - 1),
    )

    // when
    val flowIsEligibleForEDC = graph.get("/flowIsEligibleForEDC")

    // then
    assert(flowIsEligibleForEDC.complete)
    assert(flowIsEligibleForEDC.hasValue)
    assert(flowIsEligibleForEDC.get == true)
  }

  test("/flowIsEligibleForEDC is false when AGI is at-or-above the limit for QSS") {
    // given
    val graph = makeGraphWith(
      factDictionary,
      Path("/filingStatus") -> qss,
      Path("/agi") -> Dollar(otherFilingStatusAgiLimit),
    )

    // when
    val flowIsEligibleForEDC = graph.get("/flowIsEligibleForEDC")

    // then
    assert(flowIsEligibleForEDC.complete)
    assert(flowIsEligibleForEDC.hasValue)
    assert(flowIsEligibleForEDC.get == false)
  }

  test("/flowIsEligibleForEDC is true when AGI is below the limit for HOH filers") {
    // given
    val graph = makeGraphWith(
      factDictionary,
      Path("/filingStatus") -> hoh,
      Path("/agi") -> Dollar(otherFilingStatusAgiLimit - 1),
    )

    // when
    val flowIsEligibleForEDC = graph.get("/flowIsEligibleForEDC")

    // then
    assert(flowIsEligibleForEDC.complete)
    assert(flowIsEligibleForEDC.hasValue)

    assert(flowIsEligibleForEDC.get == true)
  }

  test("/flowIsEligibleForEDC is false when AGI is at-or-above the limit for HOH filers") {
    // given
    val graph = makeGraphWith(
      factDictionary,
      Path("/filingStatus") -> hoh,
      Path("/agi") -> Dollar(otherFilingStatusAgiLimit),
    )

    // when
    val flowIsEligibleForEDC = graph.get("/flowIsEligibleForEDC")

    // then
    assert(flowIsEligibleForEDC.complete)
    assert(flowIsEligibleForEDC.hasValue)
    assert(flowIsEligibleForEDC.get == false)
  }
}

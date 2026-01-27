package gov.irs.twe.factDictionary

import gov.irs.factgraph.types.Dollar
import gov.irs.factgraph.FactDictionary
import org.scalatest.funsuite.AnyFunSuite
import scala.math.Numeric.Implicits.infixNumericOps

class AdoptionCreditSpec extends AnyFunSuite {
  val factDictionary: FactDictionary = setupFactDictionary()

  test("The taxpayer IS NOT considered eligible when their income is above the top-end of the phaseout threshold") {
    // given
    val graph = makeGraphWith(factDictionary)
    val adoptionMagiPhaseoutThresholdEnd = graph.get("/adoptionMagiPhaseoutThresholdEnd").get.asInstanceOf[Dollar]
    val agi = adoptionMagiPhaseoutThresholdEnd + Dollar(1)
    graph.set("/agi", agi)

    // when
    val isEligibleForAdoptionCredit = graph.get("/isEligibleForAdoptionCredit")

    // then
    assert(isEligibleForAdoptionCredit.complete)
    assert(isEligibleForAdoptionCredit.hasValue)
    assert(isEligibleForAdoptionCredit.get == false)
  }

  test(
    "The taxpayer IS considered eligible when their income is equal or below the top-end of the phaseout threshold",
  ) {
    // given
    val graph = makeGraphWith(factDictionary)
    val adoptionMagiPhaseoutThresholdEnd = graph.get("/adoptionMagiPhaseoutThresholdEnd").get.asInstanceOf[Dollar]
    val agi = adoptionMagiPhaseoutThresholdEnd
    graph.set("/agi", agi)

    // when
    val isEligibleForAdoptionCredit = graph.get("/isEligibleForAdoptionCredit")

    // then
    assert(isEligibleForAdoptionCredit.complete)
    assert(isEligibleForAdoptionCredit.hasValue)
    assert(isEligibleForAdoptionCredit.get == true)
  }
}

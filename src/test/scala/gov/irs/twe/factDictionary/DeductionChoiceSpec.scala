package gov.irs.twe.factDictionary

import gov.irs.factgraph.Path
import org.scalatest.funsuite.AnyFunSuite

class DeductionChoiceSpec extends AnyFunSuite {
  val factDictionary = setupFactDictionary()

  test("Deduction preference is properly reflected when the Taxpayer has not yet made a choice") {
    // given: the Taxpayer has not yet made a deduction choice
    val graph = makeGraphWith(factDictionary)

    // when: inspecting the deduction choice facts
    val wantsStandardDeduction = graph.get("/wantsStandardDeduction")
    val wantsItemizedDeduction = graph.get("/wantsItemizedDeduction")

    // then: /wantsStandardDeduction (writable) is incomplete with a placeholder value ("Pending" state)
    assert(!wantsStandardDeduction.complete)
    assert(wantsStandardDeduction.value.get === true)

    // and: /wantsItemizedDeduction (derived) is incomplete but false
    assert(!wantsItemizedDeduction.complete)
    assert(wantsItemizedDeduction.value.get == false)
  }

  test("Deduction preference is properly reflected when the Taxpayer prefers the standard deduction") {
    // given: the Taxpayer prefers the standard deduction
    val graph = makeGraphWith(factDictionary)

    val wantsStandardDeduction = true
    graph.set(Path("/wantsStandardDeduction"), wantsStandardDeduction)

    // when: inspecting the itemized deduction derived fact
    val wantsItemizedDeduction = graph.get("/wantsItemizedDeduction")

    // then: /wantsItemizedDeduction is false (inverse of the standard deduction choice)
    assert(wantsItemizedDeduction.complete)
    assert(wantsItemizedDeduction.value.get == !wantsStandardDeduction)
  }

  test("Deduction preference is properly reflected when the Taxpayer prefers to itemize deductions") {
    // given: the Taxpayer prefers to itemize deductions
    val graph = makeGraphWith(factDictionary)

    val wantsStandardDeduction = false
    graph.set(Path("/wantsStandardDeduction"), wantsStandardDeduction)

    // when: inspecting the itemized deduction derived fact
    val wantsItemizedDeduction = graph.get("/wantsItemizedDeduction")

    // then: /wantsItemizedDeduction is true (inverse of the standard deduction choice)
    assert(wantsItemizedDeduction.complete)
    assert(wantsItemizedDeduction.value.get == !wantsStandardDeduction)
  }
}

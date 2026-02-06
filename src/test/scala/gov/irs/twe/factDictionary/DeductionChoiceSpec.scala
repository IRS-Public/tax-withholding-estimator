package gov.irs.twe.factDictionary

import gov.irs.factgraph.Path
import org.scalatest.funsuite.AnyFunSuite

class DeductionChoiceSpec extends AnyFunSuite {
  val factDictionary = setupFactDictionary()

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

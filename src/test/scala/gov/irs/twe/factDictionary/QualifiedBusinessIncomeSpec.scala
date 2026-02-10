package gov.irs.twe.factDictionary

import gov.irs.factgraph.types.Dollar
import gov.irs.factgraph.Path
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.prop.TableDrivenPropertyChecks

class QualifiedBusinessIncomeSpec extends AnyFunSuite with TableDrivenPropertyChecks {
  private val factDictionary = setupFactDictionary()

  test("/standardOrItemizedDeductionForQBIDeduction always has a value") {
    val standardDeductionValue = 15000
    val itemizedDeductionsGreaterThanStandardDeduction = standardDeductionValue + 500
    val itemizedDeductionsLessThanStandardDeduction = standardDeductionValue - 500

    val parameterizedTests = Table(
      (
        // Whether the taxpayer elected the standard or itemized deduction in the flow
        // /wantsItemizedDeduction is the derived inverse of this fact, and a dependency of /standardOrItemizedDeductionForQBIDeduction
        "wantsStandardDeduction",
        "itemizedDeductionTotalForQBI",
        "expectedStandardOrItemizedDeductionForQBIDeduction",
      ),
      (
        // Just wants the standard deduction
        true,
        0,
        standardDeductionValue,
      ),
      (
        // [theoretically not possible] wants the standard deduction but managed to itemize it out of favor
        true,
        itemizedDeductionsGreaterThanStandardDeduction,
        // Note:
        //   All other standard/itemized deduction calculations use the most advantageous.
        //   This one depends on taxpayer choice via /wantsItemizedDeduction
        //   But again, theoretically not possible.
        standardDeductionValue,
      ),
      (
        // wants to itemize but does not have enough itemized deductions to exceed the standard deduction
        false,
        itemizedDeductionsLessThanStandardDeduction,
        standardDeductionValue,
      ),
      (
        // wants to itemize, and itemized deductions are equivalent to the standard deduction
        false,
        standardDeductionValue,
        standardDeductionValue,
      ),
      (
        // wants to itemize, and itemized deductions are greater than the standard deduction
        false,
        itemizedDeductionsGreaterThanStandardDeduction,
        itemizedDeductionsGreaterThanStandardDeduction,
      ),
    )

    forAll(parameterizedTests) {
      (
          wantsStandardDeduction,
          itemizedDeductionTotalForQBI,
          expectedStandardOrItemizedDeductionForQBIDeduction,
      ) =>
        // given
        val graph = makeGraphWith(
          factDictionary,
          Path("/wantsStandardDeduction") -> wantsStandardDeduction,
          Path("/itemizedDeductionTotalForQBI") -> Dollar(itemizedDeductionTotalForQBI),
          // To bypass complex setup, we can set the standard deduction to an arbitrary value.
          // The actual value doesn't matter, just the state of the derived facts relative to it.
          Path("/standardDeduction") -> Dollar(standardDeductionValue),
        )

        // when
        val actualStandardOrItemizedDeductionForQBIDeduction = graph.get("/standardOrItemizedDeductionForQBIDeduction")

        // then

        // Not important for the fact to be complete, but it must have a value.
        assert(actualStandardOrItemizedDeductionForQBIDeduction.hasValue)
        assert(
          actualStandardOrItemizedDeductionForQBIDeduction.value.contains(
            Dollar(expectedStandardOrItemizedDeductionForQBIDeduction),
          ),
        )
    }
  }
}

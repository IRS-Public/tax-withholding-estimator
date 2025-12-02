package gov.irs.twe.factDictionary

import gov.irs.factgraph.types.intValue
import gov.irs.factgraph.types.Collection
import gov.irs.factgraph.types.Dollar
import gov.irs.factgraph.types.Enum
import gov.irs.factgraph.FactDictionaryForTests
import gov.irs.factgraph.Path
import gov.irs.twe.FileLoaderHelper
import org.scalatest.exceptions.DuplicateTestNameException
import org.scalatest.funspec.AnyFunSpec
import scala.math.Numeric.Implicits.infixNumericOps

class StandardDeductionSpec extends AnyFunSpec {
  describe("Standard Deduction amounts") {
    // Begin tax year sensitive data.
    // Hopefully you will only need to update these values for a change in tax year.
    // <TaxYear>2025</TaxYear> TY25 TY2025
    object standardDeduction:
      val single = 16100
      val mfs = 16100
      val qss = 32200
      val hoh = 24150
      val mfj = 32200
    object additionalStandardDeduction:
      val single = 2050
      val mfs = 1650
      val qss = 1650
      val hoh = 2050
      val mfj = 1650
    object dependentStandardDeduction:
      // cannot exceed the greater of ...
      val baseLimit = 1350 // ... and ...
      val addToIncome = 450 // ... plus the dependent's earned income.
    // End tax year sensitive data.

    val factDictionary = setupFactDictionary()
    val filingStatusList = List(
      (
        Enum("single", "/filingStatusOptions"),
        Dollar(standardDeduction.single),
        Dollar(additionalStandardDeduction.single),
      ),
      (
        Enum("marriedFilingSeparately", "/filingStatusOptions"),
        Dollar(standardDeduction.mfs),
        Dollar(additionalStandardDeduction.mfs),
      ),
      (
        Enum("qualifiedSurvivingSpouse", "/filingStatusOptions"),
        Dollar(standardDeduction.qss),
        Dollar(additionalStandardDeduction.qss),
      ),
      (
        Enum("headOfHousehold", "/filingStatusOptions"),
        Dollar(standardDeduction.hoh),
        Dollar(additionalStandardDeduction.hoh),
      ),
      (
        Enum("marriedFilingJointly", "/filingStatusOptions"),
        Dollar(standardDeduction.mfj),
        Dollar(additionalStandardDeduction.mfj),
      ),
    )
    val totalIncomeList =
      List(
        // no income
        Dollar(0),
        // highest income to get base limit
        Dollar(dependentStandardDeduction.baseLimit - dependentStandardDeduction.addToIncome),
        // lowest income to exceed base limit
        Dollar(dependentStandardDeduction.baseLimit - dependentStandardDeduction.addToIncome + 1),
      )
    val booleanList = List(true, false)

    for (filingStatus, standardDeduction, additionalStandardDeduction) <- filingStatusList
    do
      val isMfj = (filingStatus == Enum("marriedFilingJointly", "/filingStatusOptions"))
      for
        filerIsClaimed <- booleanList
        filerIsBlind <- booleanList
        filerIsSenior <- booleanList
        // hack: when not MFJ, pretend there is only one value for spouse attributes (and ignore it in the logic)
        spouseIsClaimed <- booleanList.filter(b => b || isMfj)
        spouseIsBlind <- booleanList.filter(b => b || isMfj)
        spouseIsSenior <- booleanList.filter(b => b || isMfj)
        totalIncome <- totalIncomeList
      do
        var testTitle =
          s"${filingStatus}, claimed: ${filerIsClaimed}, blind: ${filerIsBlind}, senior: ${filerIsSenior}"
        if (isMfj) {
          testTitle =
            s"${testTitle}, spouse claimed: ${spouseIsClaimed}, spouse blind: ${spouseIsBlind}, spouse senior: ${spouseIsSenior}"
        }
        testTitle = s"${testTitle}, income: ${totalIncome}"

        it(testTitle) {
          val graph = makeGraphWith(
            factDictionary,
            Path("/filingStatus") -> filingStatus,
            Path("/primaryFilerIsClaimedOnAnotherReturn") -> filerIsClaimed,
            Path("/primaryFilerIsBlind") -> filerIsBlind,
            Path("/primaryFilerAge65OrOlder") -> filerIsSenior,
            Path("/incomeTotal") -> totalIncome,
          )

          if (graph.get(Path("/usePreOb3StandardDeduction")).value.contains(true)) {
            cancel("Skipping test based on OB3 standard deduction numbers because you are using pre-OB3 facts")
          }

          val baseAmount = if (filerIsClaimed || (isMfj && spouseIsClaimed)) {
            Dollar(
              scala.math
                .max(
                  dependentStandardDeduction.baseLimit,
                  totalIncome.intValue + dependentStandardDeduction.addToIncome,
                ),
            )
          } else {
            standardDeduction
          }

          var multiplier = 0
          if (filerIsBlind) multiplier = multiplier + 1
          if (filerIsSenior) multiplier = multiplier + 1
          if (isMfj) {
            if (spouseIsBlind) multiplier = multiplier + 1
            if (spouseIsSenior) multiplier = multiplier + 1
          }

          if (isMfj) {
            graph.set(Path("/secondaryFilerIsClaimedOnAnotherReturn"), spouseIsClaimed)
            graph.set(Path("/secondaryFilerIsBlind"), spouseIsBlind)
            graph.set(Path("/secondaryFilerAge65OrOlder"), spouseIsSenior)
          }

          val actual = graph.get(Path("/standardDeduction"))
          assert(actual.value.contains(baseAmount + Dollar(multiplier) * additionalStandardDeduction))
        }
  }
}

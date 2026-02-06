package gov.irs.twe.factDictionary

import gov.irs.factgraph.{ FactDictionary, Path }
import gov.irs.factgraph.types.Enum
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.prop.TableDrivenPropertyChecks

class EitcSpec extends AnyFunSuite with TableDrivenPropertyChecks {
  val factDictionary: FactDictionary = setupFactDictionary()
  val single = Enum("single", "/filingStatusOptions")
  val mfj = Enum("marriedFilingJointly", "/filingStatusOptions")

  test(
    "Flow shows EITC section (/flowShouldShowEitcSection) when any of the questions within the section will be shown",
  ) {
    val parameterizedTests = Table(
      (
        "flowShouldAskForEitcQualifyingChildren",
        "flowShouldAskWhetherPrimaryFilerAge25OrOlderForEitc",
        "flowShouldAskWhetherSecondaryFilerAge25OrOlderForEitc",
        "expectedFlowShouldShowEitcSection",
      ),
      (true, true, true, true),
      (true, false, false, true),
      (false, true, false, true),
      (false, false, true, true),
      (false, false, false, false),
    )

    forAll(parameterizedTests) {
      (
          flowShouldAskForEitcQualifyingChildren,
          flowShouldAskWhetherPrimaryFilerAge25OrOlderForEitc,
          flowShouldAskWhetherSecondaryFilerAge25OrOlderForEitc,
          expectedFlowShouldShowEitcSection,
      ) =>
        // given
        val graph = makeGraphWith(
          factDictionary,
          Path(
            "/flowShouldAskForEitcQualifyingChildren",
          ) -> flowShouldAskForEitcQualifyingChildren,
          Path(
            "/flowShouldAskWhetherPrimaryFilerAge25OrOlderForEitc",
          ) -> flowShouldAskWhetherPrimaryFilerAge25OrOlderForEitc,
          Path(
            "/flowShouldAskWhetherSecondaryFilerAge25OrOlderForEitc",
          ) -> flowShouldAskWhetherSecondaryFilerAge25OrOlderForEitc,
        )

        // when
        val flowShouldShowEitcSection = graph.get("/flowShouldShowEitcSection")

        // then
        assert(flowShouldShowEitcSection.complete)
        assert(flowShouldShowEitcSection.hasValue)
        assert(flowShouldShowEitcSection.get == expectedFlowShouldShowEitcSection)
    }
  }

  test(
    "Flow asks for EITC qualifying children (/flowShouldAskForEitcQualifyingChildren) when the prerequisites conditions are met",
  ) {
    val parameterizedTests = Table(
      (
        "primaryFilerIsClaimingDependents",
        "expectedFlowShouldAskForEitcQualifyingChildren",
      ),
      (true, true),
      (false, false),
    )

    forAll(parameterizedTests) {
      (
          primaryFilerIsClaimingDependents,
          expectedFlowShouldAskForEitcQualifyingChildren,
      ) =>
        // given
        val graph = makeGraphWith(
          factDictionary,
          Path("/maybeEligibleForEitcBaseDependingOnNumberOfQualifyingChildren") -> true,
          Path("/primaryFilerIsClaimingDependents") -> primaryFilerIsClaimingDependents,
        )

        // when
        val flowShouldAskForEitcQualifyingChildren = graph.get("/flowShouldAskForEitcQualifyingChildren")

        // then
        assert(flowShouldAskForEitcQualifyingChildren.complete)
        assert(flowShouldAskForEitcQualifyingChildren.hasValue)
        assert(flowShouldAskForEitcQualifyingChildren.get == expectedFlowShouldAskForEitcQualifyingChildren)
    }
  }

  test(
    "Flow asks if primary filer is over 25 (/flowShouldAskWhetherPrimaryFilerAge25OrOlderForEitc) when the prerequisites conditions are met",
  ) {
    val parameterizedTests = Table(
      (
        "filingStatus",
        "primaryFilerAge65OrOlder",
        "primaryFilerIsClaimedOnAnotherReturn",
        "secondaryFilerIsClaimedOnAnotherReturn",
        "primaryFilerIsClaimingDependents",
        "eitcQualifyingChildren",
        "expectedFlowShouldAskWhetherPrimaryFilerAge25OrOlderForEitc",
      ),
      // "true" cases
      (single, false, false, false, false, None, true),
      (single, false, false, false, true, Some(0), true),
      (mfj, false, false, false, false, None, true),

      // "false" cases
      // - primary filer must be under 65
      (single, true, false, false, false, None, false),
      // - neither filer can be claimed on another return
      (single, false, true, false, false, None, false),
      (mfj, false, true, false, false, None, false),
      (mfj, false, false, true, false, None, false),
      // - if claiming dependents, must enter "0" for qualifying children for the question to show up
      (single, false, false, false, true, None, false),
      // - don't need to ask if has qualified children
      (single, false, false, false, true, Some(1), false),
    )

    forAll(parameterizedTests) {
      (
          filingStatus,
          primaryFilerAge65OrOlder,
          primaryFilerIsClaimedOnAnotherReturn,
          secondaryFilerIsClaimedOnAnotherReturn,
          primaryFilerIsClaimingDependents,
          eitcQualifyingChildren,
          expectedFlowShouldAskWhetherPrimaryFilerAge25OrOlderForEitc,
      ) =>
        // given
        val graph = makeGraphWith(
          factDictionary,
          Path("/maybeEligibleForEitcBase") -> true,
          Path("/filingStatus") -> filingStatus,
          Path("/primaryFilerAge65OrOlder") -> primaryFilerAge65OrOlder,
          Path("/primaryFilerIsClaimedOnAnotherReturn") -> primaryFilerIsClaimedOnAnotherReturn,
          Path("/secondaryFilerIsClaimedOnAnotherReturn") -> secondaryFilerIsClaimedOnAnotherReturn,
          Path("/primaryFilerIsClaimingDependents") -> primaryFilerIsClaimingDependents,
        )
        eitcQualifyingChildren.foreach(numberEntered => graph.set(Path("/eitcQualifyingChildren"), numberEntered))

        // when
        val flowShouldAskWhetherPrimaryFilerAge25OrOlderForEitc =
          graph.get("/flowShouldAskWhetherPrimaryFilerAge25OrOlderForEitc")

        // then
        assert(flowShouldAskWhetherPrimaryFilerAge25OrOlderForEitc.complete)
        assert(flowShouldAskWhetherPrimaryFilerAge25OrOlderForEitc.hasValue)
        assert(
          flowShouldAskWhetherPrimaryFilerAge25OrOlderForEitc.get == expectedFlowShouldAskWhetherPrimaryFilerAge25OrOlderForEitc,
        )
    }
  }

  test(
    "Flow asks if secondary filer is over 25 (/flowShouldAskWhetherSecondaryFilerAge25OrOlderForEitc) when the prerequisites conditions are met",
  ) {
    val parameterizedTests = Table(
      (
        "filingStatus",
        "secondaryFilerAge65OrOlder",
        "primaryFilerIsClaimedOnAnotherReturn",
        "secondaryFilerIsClaimedOnAnotherReturn",
        "primaryFilerAge25OrOlderForEitc",
        "primaryFilerIsClaimingDependents",
        "eitcQualifyingChildren",
        "expectedFlowShouldAskWhetherSecondaryFilerAge25OrOlderForEitc",
      ),
      // "true" cases
      (mfj, false, false, false, false, false, None, true),
      (mfj, false, false, false, false, true, Some(0), true),

      // "false" cases
      // - must be MFJ
      (single, false, false, false, false, false, None, false),
      // - secondary filer must be under 65
      (mfj, true, false, false, false, false, None, false),
      // - neither filer can be claimed on another return
      (mfj, false, true, false, false, false, None, false),
      (mfj, false, false, true, false, false, None, false),
      // - don't need to ask the question if the primary filer already qualified
      (mfj, false, false, false, true, false, None, false),
      // - don't need to ask if has qualified children
      (mfj, false, false, false, false, true, Some(1), false),
    )

    forAll(parameterizedTests) {
      (
          filingStatus,
          secondaryFilerAge65OrOlder,
          primaryFilerIsClaimedOnAnotherReturn,
          secondaryFilerIsClaimedOnAnotherReturn,
          primaryFilerAge25OrOlderForEitc,
          primaryFilerIsClaimingDependents,
          eitcQualifyingChildren,
          expectedFlowShouldAskWhetherSecondaryFilerAge25OrOlderForEitc,
      ) =>
        // given
        val graph = makeGraphWith(
          factDictionary,
          Path("/maybeEligibleForEitcBase") -> true,
          Path("/filingStatus") -> filingStatus,
          Path("/secondaryFilerAge65OrOlder") -> secondaryFilerAge65OrOlder,
          Path("/primaryFilerIsClaimedOnAnotherReturn") -> primaryFilerIsClaimedOnAnotherReturn,
          Path("/secondaryFilerIsClaimedOnAnotherReturn") -> secondaryFilerIsClaimedOnAnotherReturn,
          Path("/primaryFilerAge25OrOlderForEitc") -> primaryFilerAge25OrOlderForEitc,
          Path("/primaryFilerIsClaimingDependents") -> primaryFilerIsClaimingDependents,
        )
        eitcQualifyingChildren.foreach(numberEntered => graph.set(Path("/eitcQualifyingChildren"), numberEntered))

        // when
        val flowShouldAskWhetherSecondaryFilerAge25OrOlderForEitc =
          graph.get("/flowShouldAskWhetherSecondaryFilerAge25OrOlderForEitc")

        // then
        assert(flowShouldAskWhetherSecondaryFilerAge25OrOlderForEitc.complete)
        assert(flowShouldAskWhetherSecondaryFilerAge25OrOlderForEitc.hasValue)
        assert(
          flowShouldAskWhetherSecondaryFilerAge25OrOlderForEitc.get == expectedFlowShouldAskWhetherSecondaryFilerAge25OrOlderForEitc,
        )
    }
  }
}

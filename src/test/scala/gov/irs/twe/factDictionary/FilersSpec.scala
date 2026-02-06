package gov.irs.twe.factDictionary

import gov.irs.factgraph.Path
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.prop.TableDrivenPropertyChecks

class FilersSpec extends AnyFunSuite with TableDrivenPropertyChecks {
  val factDictionary = setupFactDictionary()

  test("test filer blindness conditional evaluations") {
    val dataTable = Table(
      (
        // The value to insert into the fact graph. "None" will skip insertion resulting in an incomplete writable fact
        "primaryFilerIsBlind",
        // The value to insert into the fact graph. "None" will skip insertion resulting in an incomplete writable fact
        "secondaryFilerIsBlind",
        "expectedOnlyPrimaryFilerIsBlind",
        "expectedOnlySecondaryFilerIsBlind",
        "expectedPrimaryAndSecondaryFilersAreBlind",
      ),
      (
        Some(true),
        None,
        true,
        false,
        false,
      ),
      (
        Some(true),
        Some(false),
        true,
        false,
        false,
      ),
      (
        None,
        Some(true),
        false,
        true,
        false,
      ),
      (
        Some(false),
        Some(true),
        false,
        true,
        false,
      ),
      (
        Some(true),
        Some(true),
        false,
        false,
        true,
      ),
      (
        Some(false),
        Some(false),
        false,
        false,
        false,
      ),
      (
        None,
        None,
        false,
        false,
        false,
      ),
    )

    forAll(dataTable) {
      (
          primaryFilerIsBlind,
          secondaryFilerIsBlind,
          expectedOnlyPrimaryFilerIsBlind,
          expectedOnlySecondaryFilerIsBlind,
          expectedPrimaryAndSecondaryFilersAreBlind,
      ) =>
        val graph = makeGraphWith(factDictionary)
        primaryFilerIsBlind.foreach(it => graph.set(Path("/primaryFilerIsBlind"), it))
        secondaryFilerIsBlind.foreach(it => graph.set(Path("/secondaryFilerIsBlind"), it))

        val actualOnlyPrimaryFilerIsBlind = graph.get("/onlyPrimaryFilerIsBlind")
        val actualOnlySecondaryFilerIsBlind = graph.get("/onlySecondaryFilerIsBlind")
        val actualPrimaryAndSecondaryFilersAreBlind = graph.get("/primaryAndSecondaryFilersAreBlind")

        assert(actualOnlyPrimaryFilerIsBlind.complete)
        assert(actualOnlyPrimaryFilerIsBlind.value.contains(expectedOnlyPrimaryFilerIsBlind))

        assert(actualOnlySecondaryFilerIsBlind.complete)
        assert(actualOnlySecondaryFilerIsBlind.value.contains(expectedOnlySecondaryFilerIsBlind))

        assert(actualPrimaryAndSecondaryFilersAreBlind.complete)
        assert(actualPrimaryAndSecondaryFilersAreBlind.value.contains(expectedPrimaryAndSecondaryFilersAreBlind))
    }
  }

  test("test filer dependent status conditional evaluations") {
    val dataTable = Table(
      (
        // The value to insert into the fact graph. "None" will skip insertion resulting in an incomplete writable fact
        "primaryFilerIsClaimedOnAnotherReturn",
        // The value to insert into the fact graph. "None" will skip insertion resulting in an incomplete writable fact
        "secondaryFilerIsClaimedOnAnotherReturn",
        "expectedOnlyPrimaryFilerIsClaimedOnAnotherReturn",
        "expectedOnlySecondaryFilerIsClaimedOnAnotherReturn",
        "expectedPrimaryAndSecondaryFilersAreClaimedOnAnotherReturn",
        "expectedNeitherFilerIsClaimedOnAnotherReturn",
      ),
      (
        Some(true),
        None,
        true,
        false,
        false,
        false,
      ),
      (
        Some(true),
        Some(false),
        true,
        false,
        false,
        false,
      ),
      (
        None,
        Some(true),
        false,
        true,
        false,
        false,
      ),
      (
        Some(false),
        Some(true),
        false,
        true,
        false,
        false,
      ),
      (
        Some(true),
        Some(true),
        false,
        false,
        true,
        false,
      ),
      (
        Some(false),
        Some(false),
        false,
        false,
        false,
        true,
      ),
      (
        None,
        None,
        false,
        false,
        false,
        true,
      ),
    )

    forAll(dataTable) {
      (
          primaryFilerIsClaimedOnAnotherReturn,
          secondaryFilerIsClaimedOnAnotherReturn,
          expectedOnlyPrimaryFilerIsClaimedOnAnotherReturn,
          expectedOnlySecondaryFilerIsClaimedOnAnotherReturn,
          expectedPrimaryAndSecondaryFilersAreClaimedOnAnotherReturn,
          expectedNeitherFilerIsClaimedOnAnotherReturn,
      ) =>
        val graph = makeGraphWith(factDictionary)
        primaryFilerIsClaimedOnAnotherReturn.foreach(it => graph.set(Path("/primaryFilerIsClaimedOnAnotherReturn"), it))
        secondaryFilerIsClaimedOnAnotherReturn.foreach(it =>
          graph.set(Path("/secondaryFilerIsClaimedOnAnotherReturn"), it),
        )

        val actualOnlyPrimaryFilerIsClaimedOnAnotherReturn = graph.get("/onlyPrimaryFilerIsClaimedOnAnotherReturn")
        val actualOnlySecondaryFilerIsClaimedOnAnotherReturn = graph.get("/onlySecondaryFilerIsClaimedOnAnotherReturn")
        val actualPrimaryAndSecondaryFilersAreClaimedOnAnotherReturn =
          graph.get("/primaryAndSecondaryFilersAreClaimedOnAnotherReturn")
        val actualNeitherFilerIsClaimedOnAnotherReturn = graph.get("/neitherFilerIsClaimedOnAnotherReturn")

        assert(actualOnlyPrimaryFilerIsClaimedOnAnotherReturn.complete)
        assert(
          actualOnlyPrimaryFilerIsClaimedOnAnotherReturn.value.contains(
            expectedOnlyPrimaryFilerIsClaimedOnAnotherReturn,
          ),
        )

        assert(actualOnlySecondaryFilerIsClaimedOnAnotherReturn.complete)
        assert(
          actualOnlySecondaryFilerIsClaimedOnAnotherReturn.value.contains(
            expectedOnlySecondaryFilerIsClaimedOnAnotherReturn,
          ),
        )

        assert(actualPrimaryAndSecondaryFilersAreClaimedOnAnotherReturn.complete)
        assert(
          actualPrimaryAndSecondaryFilersAreClaimedOnAnotherReturn.value.contains(
            expectedPrimaryAndSecondaryFilersAreClaimedOnAnotherReturn,
          ),
        )

        assert(actualNeitherFilerIsClaimedOnAnotherReturn.complete)
        assert(
          actualNeitherFilerIsClaimedOnAnotherReturn.value.contains(
            expectedNeitherFilerIsClaimedOnAnotherReturn,
          ),
        )
    }
  }

  test("test filer 'over 65' conditional evaluations") {
    val dataTable = Table(
      (
        // The value to insert into the fact graph. "None" will skip insertion resulting in an incomplete writable fact
        "primaryFilerAge65OrOlder",
        // The value to insert into the fact graph. "None" will skip insertion resulting in an incomplete writable fact
        "secondaryFilerAge65OrOlder",
        "expectedOnlyPrimaryFilerAge65OrOlder",
        "expectedOnlySecondaryFilerAge65OrOlder",
        "expectedPrimaryAndSecondaryFilersAge65OrOlder",
      ),
      (
        Some(true),
        None,
        true,
        false,
        false,
      ),
      (
        Some(true),
        Some(false),
        true,
        false,
        false,
      ),
      (
        None,
        Some(true),
        false,
        true,
        false,
      ),
      (
        Some(false),
        Some(true),
        false,
        true,
        false,
      ),
      (
        Some(true),
        Some(true),
        false,
        false,
        true,
      ),
      (
        Some(false),
        Some(false),
        false,
        false,
        false,
      ),
      (
        None,
        None,
        false,
        false,
        false,
      ),
    )

    forAll(dataTable) {
      (
          primaryFilerAge65OrOlder,
          secondaryFilerAge65OrOlder,
          expectedOnlyPrimaryFilerAge65OrOlder,
          expectedOnlySecondaryFilerAge65OrOlder,
          expectedPrimaryAndSecondaryFilersAge65OrOlder,
      ) =>
        val graph = makeGraphWith(factDictionary)
        primaryFilerAge65OrOlder.foreach(it => graph.set(Path("/primaryFilerAge65OrOlder"), it))
        secondaryFilerAge65OrOlder.foreach(it => graph.set(Path("/secondaryFilerAge65OrOlder"), it))

        val actualOnlyPrimaryFilerAge65OrOlder = graph.get("/onlyPrimaryFilerAge65OrOlder")
        val actualOnlySecondaryFilerAge65OrOlder = graph.get("/onlySecondaryFilerAge65OrOlder")
        val actualPrimaryAndSecondaryFilersAge65OrOlder =
          graph.get("/primaryAndSecondaryFilersAge65OrOlder")

        assert(actualOnlyPrimaryFilerAge65OrOlder.complete)
        assert(
          actualOnlyPrimaryFilerAge65OrOlder.value.contains(expectedOnlyPrimaryFilerAge65OrOlder),
        )

        assert(actualOnlySecondaryFilerAge65OrOlder.complete)
        assert(
          actualOnlySecondaryFilerAge65OrOlder.value.contains(expectedOnlySecondaryFilerAge65OrOlder),
        )

        assert(actualPrimaryAndSecondaryFilersAge65OrOlder.complete)
        assert(
          actualPrimaryAndSecondaryFilersAge65OrOlder.value.contains(
            expectedPrimaryAndSecondaryFilersAge65OrOlder,
          ),
        )
    }
  }
}

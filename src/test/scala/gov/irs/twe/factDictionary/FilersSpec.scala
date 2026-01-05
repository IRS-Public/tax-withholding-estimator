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
        // expected value. "None" means that we expect the fact to be incomplete
        "expectedOnlyPrimaryFilerIsBlind",
        // expected value. "None" means that we expect the fact to be incomplete
        "expectedOnlySecondaryFilerIsBlind",
        // expected value. "None" means that we expect the fact to be incomplete
        "expectedPrimaryAndSecondaryFilersAreBlind",
      ),
      (Some(true), None, Some(true), Some(false), None),
      (Some(true), Some(false), Some(true), Some(false), Some(false)),
      (None, Some(true), Some(false), Some(true), None),
      (Some(false), Some(true), Some(false), Some(true), Some(false)),
      (Some(true), Some(true), Some(false), Some(false), Some(true)),
      (Some(false), Some(false), Some(false), Some(false), Some(false)),
      (
        None,
        None,
        None,
        None,
        None,
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

        if (expectedOnlyPrimaryFilerIsBlind.isEmpty)
          assert(!actualOnlyPrimaryFilerIsBlind.complete)
          assert(actualOnlyPrimaryFilerIsBlind.value.isEmpty)
        else {
          assert(actualOnlyPrimaryFilerIsBlind.complete)
          assert(actualOnlyPrimaryFilerIsBlind.value.contains(expectedOnlyPrimaryFilerIsBlind.get))
        }

        if (expectedOnlySecondaryFilerIsBlind.isEmpty)
          assert(!actualOnlySecondaryFilerIsBlind.complete)
          assert(actualOnlySecondaryFilerIsBlind.value.isEmpty)
        else
          assert(actualOnlySecondaryFilerIsBlind.complete)
          assert(actualOnlySecondaryFilerIsBlind.value.contains(expectedOnlySecondaryFilerIsBlind.get))

        if (expectedPrimaryAndSecondaryFilersAreBlind.isEmpty)
          assert(!actualPrimaryAndSecondaryFilersAreBlind.complete)
          assert(actualPrimaryAndSecondaryFilersAreBlind.value.isEmpty)
        else
          assert(actualPrimaryAndSecondaryFilersAreBlind.complete)
          assert(actualPrimaryAndSecondaryFilersAreBlind.value.contains(expectedPrimaryAndSecondaryFilersAreBlind.get))
    }
  }

  test("test filer dependent status conditional evaluations") {
    val dataTable = Table(
      (
        // The value to insert into the fact graph. "None" will skip insertion resulting in an incomplete writable fact
        "primaryFilerIsClaimedOnAnotherReturn",
        // The value to insert into the fact graph. "None" will skip insertion resulting in an incomplete writable fact
        "secondaryFilerIsClaimedOnAnotherReturn",
        // expected value. "None" means that we expect the fact to be incomplete
        "expectedOnlyPrimaryFilerIsClaimedOnAnotherReturn",
        // expected value. "None" means that we expect the fact to be incomplete
        "expectedOnlySecondaryFilerIsClaimedOnAnotherReturn",
        // expected value. "None" means that we expect the fact to be incomplete
        "expectedPrimaryAndSecondaryFilersAreClaimedOnAnotherReturn",
      ),
      (Some(true), None, Some(true), Some(false), None),
      (Some(true), Some(false), Some(true), Some(false), Some(false)),
      (None, Some(true), Some(false), Some(true), None),
      (Some(false), Some(true), Some(false), Some(true), Some(false)),
      (Some(true), Some(true), Some(false), Some(false), Some(true)),
      (Some(false), Some(false), Some(false), Some(false), Some(false)),
      (
        None,
        None,
        None,
        None,
        None,
      ),
    )

    forAll(dataTable) {
      (
          primaryFilerIsClaimedOnAnotherReturn,
          secondaryFilerIsClaimedOnAnotherReturn,
          expectedOnlyPrimaryFilerIsClaimedOnAnotherReturn,
          expectedOnlySecondaryFilerIsClaimedOnAnotherReturn,
          expectedPrimaryAndSecondaryFilersAreClaimedOnAnotherReturn,
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

        if (expectedOnlyPrimaryFilerIsClaimedOnAnotherReturn.isEmpty)
          assert(!actualOnlyPrimaryFilerIsClaimedOnAnotherReturn.complete)
          assert(actualOnlyPrimaryFilerIsClaimedOnAnotherReturn.value.isEmpty)
        else {
          assert(actualOnlyPrimaryFilerIsClaimedOnAnotherReturn.complete)
          assert(
            actualOnlyPrimaryFilerIsClaimedOnAnotherReturn.value.contains(
              expectedOnlyPrimaryFilerIsClaimedOnAnotherReturn.get,
            ),
          )
        }

        if (expectedOnlySecondaryFilerIsClaimedOnAnotherReturn.isEmpty)
          assert(!actualOnlySecondaryFilerIsClaimedOnAnotherReturn.complete)
          assert(actualOnlySecondaryFilerIsClaimedOnAnotherReturn.value.isEmpty)
        else
          assert(actualOnlySecondaryFilerIsClaimedOnAnotherReturn.complete)
          assert(
            actualOnlySecondaryFilerIsClaimedOnAnotherReturn.value.contains(
              expectedOnlySecondaryFilerIsClaimedOnAnotherReturn.get,
            ),
          )

        if (expectedPrimaryAndSecondaryFilersAreClaimedOnAnotherReturn.isEmpty)
          assert(!actualPrimaryAndSecondaryFilersAreClaimedOnAnotherReturn.complete)
          assert(actualPrimaryAndSecondaryFilersAreClaimedOnAnotherReturn.value.isEmpty)
        else
          assert(actualPrimaryAndSecondaryFilersAreClaimedOnAnotherReturn.complete)
          assert(
            actualPrimaryAndSecondaryFilersAreClaimedOnAnotherReturn.value.contains(
              expectedPrimaryAndSecondaryFilersAreClaimedOnAnotherReturn.get,
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
        // expected value. "None" means that we expect the fact to be incomplete
        "expectedOnlyPrimaryFilerAge65OrOlder",
        // expected value. "None" means that we expect the fact to be incomplete
        "expectedOnlySecondaryFilerAge65OrOlder",
        // expected value. "None" means that we expect the fact to be incomplete
        "expectedPrimaryAndSecondaryFilersAge65OrOlder",
      ),
      (Some(true), None, Some(true), Some(false), None),
      (Some(true), Some(false), Some(true), Some(false), Some(false)),
      (None, Some(true), Some(false), Some(true), None),
      (Some(false), Some(true), Some(false), Some(true), Some(false)),
      (Some(true), Some(true), Some(false), Some(false), Some(true)),
      (Some(false), Some(false), Some(false), Some(false), Some(false)),
      (
        None,
        None,
        None,
        None,
        None,
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

        val actualOnlyPrimaryFilerIsClaimedOnAnotherReturn = graph.get("/onlyPrimaryFilerAge65OrOlder")
        val actualOnlySecondaryFilerIsClaimedOnAnotherReturn = graph.get("/onlySecondaryFilerAge65OrOlder")
        val actualPrimaryAndSecondaryFilersAreClaimedOnAnotherReturn =
          graph.get("/primaryAndSecondaryFilersAge65OrOlder")

        if (expectedOnlyPrimaryFilerAge65OrOlder.isEmpty)
          assert(!actualOnlyPrimaryFilerIsClaimedOnAnotherReturn.complete)
          assert(actualOnlyPrimaryFilerIsClaimedOnAnotherReturn.value.isEmpty)
        else {
          assert(actualOnlyPrimaryFilerIsClaimedOnAnotherReturn.complete)
          assert(
            actualOnlyPrimaryFilerIsClaimedOnAnotherReturn.value.contains(expectedOnlyPrimaryFilerAge65OrOlder.get),
          )
        }

        if (expectedOnlySecondaryFilerAge65OrOlder.isEmpty)
          assert(!actualOnlySecondaryFilerIsClaimedOnAnotherReturn.complete)
          assert(actualOnlySecondaryFilerIsClaimedOnAnotherReturn.value.isEmpty)
        else
          assert(actualOnlySecondaryFilerIsClaimedOnAnotherReturn.complete)
          assert(
            actualOnlySecondaryFilerIsClaimedOnAnotherReturn.value.contains(expectedOnlySecondaryFilerAge65OrOlder.get),
          )

        if (expectedPrimaryAndSecondaryFilersAge65OrOlder.isEmpty)
          assert(!actualPrimaryAndSecondaryFilersAreClaimedOnAnotherReturn.complete)
          assert(actualPrimaryAndSecondaryFilersAreClaimedOnAnotherReturn.value.isEmpty)
        else
          assert(actualPrimaryAndSecondaryFilersAreClaimedOnAnotherReturn.complete)
          assert(
            actualPrimaryAndSecondaryFilersAreClaimedOnAnotherReturn.value.contains(
              expectedPrimaryAndSecondaryFilersAge65OrOlder.get,
            ),
          )
    }
  }
}

package gov.irs.twe.factDictionary

import gov.irs.factgraph.types.Dollar
import gov.irs.factgraph.types.Enum
import gov.irs.factgraph.FactDictionaryForTests
import gov.irs.factgraph.Path
import gov.irs.twe.FileLoaderHelper
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.prop.TableDrivenPropertyChecks

class TaxCalculationsSpec extends AnyFunSuite with TableDrivenPropertyChecks {
  val factDictionary = setupFactDictionary()

  val filingStatus = Path("/filingStatus")
  val taxableIncome = Path("/taxableIncome")
  val roundedTaxableIncome = Path("/roundedTaxableIncome")
  val tentativeTaxFromTaxableIncome = Path("/tentativeTaxFromTaxableIncome")

  val single = Enum("single", "/filingStatusOptions")

  val dataTable = Table(
    ("status", "taxableIncome", "expectedRoundedIncome", "expectedTax"),
    // Not over $11,925: 10% of the taxable income
    (single, "0.0", "0.0", "0.0"),
    (single, "4.49", "2.5", "0.0"),
    (single, "5.0", "10.0", "1.0"),
    (single, "14.49", "10.0", "1.0"),
    (single, "15.0", "20.0", "2.0"),
    (single, "24.49", "20.0", "2.0"),
    (single, "25.0", "37.5", "4.0"),
    (single, "37.5", "37.5", "4.0"),
    (single, "49.49", "37.5", "4.0"),
    (single, "2975.0", "2987.5", "299.0"),
    (single, "2988.0", "2987.5", "299.0"),
    (single, "2999.49", "2987.5", "299.0"),
    (single, "3000.0", "3025.0", "303.0"),
    (single, "3025.0", "3025.0", "303.0"),
    (single, "3049.49", "3025.0", "303.0"),

    // Over $11,925 but not over $48,475: $1,192.50 plus 12% of the excess over $11,925
    (single, "20000.0", "20025.0", "2165.0"),

    // Over $48,475 but not over $103,350: $5,578.50 plus 22% of the excess over $48,475
    (single, "57050.0", "57075.0", "7471.0"),
    (single, "57075.0", "57075.0", "7471.0"),
    (single, "57099.49", "57075.0", "7471.0"),

    // Over $103,350 but not over $197,300: $17,651 plus 24% of the excess over $103,350
    (single, "105351.0", "105351.0", "18131.0"),

    // Over $197,300 but not over $250,525: $40,199 plus 32% of the excess over $197,300
    (single, "200000.0", "200000.0", "41063.0"),

    // Over $250,525 but not over $626,350: $57,231 plus 35% of the excess over $250,525
    (single, "260000.0", "260000.0", "60547.0"),

    // Over $626,350: $188,769.75 plus 37% of the excess over $626,350
    (single, "700000.0", "700000.0", "216020.0"),
  )

  test("test roundedTaxableIncome and tentativeTaxFromTaxableIncome") {
    forAll(dataTable) { (status, income, expectedRoundedIncome, expectedTax) =>
      val graph = makeGraphWith(
        factDictionary,
        filingStatus -> status,
        taxableIncome -> Dollar(income),
      )

      val roundedIncome = graph.get(roundedTaxableIncome)
      assert(roundedIncome.value.contains(Dollar(expectedRoundedIncome)))
      val taxAmount = graph.get(tentativeTaxFromTaxableIncome)
      assert(taxAmount.value.contains(Dollar(expectedTax)))

    }
  }

  // Example of a singular test not using TableDrivenPropertyChecks
  // test("test roundedTaxableIncome and tentativeTaxFromTaxableIncome") {
  //   val graph = makeGraphWith(factDictionary,
  //     filingStatus -> Enum("single", "/filingStatusOptions"),
  //     taxableIncome -> Dollar(3000.00)
  //   )

  //   graph.save()
  //   val roundedIncome = graph.get(roundedTaxableIncome)
  //   assert(roundedIncome.value.contains(Dollar(3025.00)))
  //   val taxAmount = graph.get(tentativeTaxFromTaxableIncome)
  //   assert(taxAmount.value.contains(Dollar(303.00)))
  // }
}

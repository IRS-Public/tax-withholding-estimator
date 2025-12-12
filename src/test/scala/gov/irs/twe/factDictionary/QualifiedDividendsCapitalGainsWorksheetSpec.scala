package gov.irs.twe.factDictionary

import gov.irs.factgraph.types.Dollar
import gov.irs.factgraph.types.Enum
import gov.irs.factgraph.Path
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.prop.TableDrivenPropertyChecks

class QualifiedDividendsCapitalGainsWorksheetSpec extends AnyFunSuite with TableDrivenPropertyChecks {
  val factDictionary = setupFactDictionary()
  val single = Enum("single", "/filingStatusOptions")
  val mfj = Enum("marriedFilingJointly", "/filingStatusOptions")
  val qss = Enum("qualifiedSurvivingSpouse", "/filingStatusOptions")
  val hoh = Enum("headOfHousehold", "/filingStatusOptions")
  val mfs = Enum("marriedFilingSeparately", "/filingStatusOptions")
  // Set up shorthand for facts to check against our table data
  val filingStatus = Path("/filingStatus")
  val taxableIncome = Path("/taxableIncome")
  val qualifiedDividends = Path("/qualifiedDividendsIncome")
  val longTermCapitalGains = Path("/longTermCapitalGainsIncome")
  val amountAt0 = Path("/amountOfNetGainsTaxedAt0")
  val netGainTaxAmountWith15PercentTax = Path("/netGainTaxAmountWith15PercentTax")
  val netGainTaxAmountWith20PercentTax = Path("/netGainTaxAmountWith20PercentTax")
  val tentativeTaxFromTaxableIncome = Path("/tentativeTaxFromTaxableIncome")
  val tentativeTaxFromTaxableIncomeWithoutNetGains = Path("/tentativeTaxFromTaxableIncomeWithoutNetGains")
  val tentativeTaxFromTaxableIncomeWithNetCapitalGains = Path("/tentativeTaxFromTaxableIncomeWithNetCapitalGains")

  test("Calculates tentative tax with net capital gains for single, mfj, and hoh filers") {
    val dataTable = Table(
      ("status", "taxableIncome", "qualDiv", "ltCapitalGains", "amtAt0", "totalAt15", "totalAt20", "totalTax"),

      // Single tp with qualified dividends taxed at 0%
      (single, "40000", "4000", "1000", "5000", "0", "0", "3955.00"),
      // Single tp with qualified dividends, income at 0% threshhold
      (single, "48350", "5000", "0", "5000", "0", "0", "4957.00"),
      // Single tp with qualified dividends taxed at 0% and 15%
      (single, "53000", "5000", "3000", "4450", "532.5", "0", "5687.50"),
      // Single tp with qualified dividends, income at 15% threshhold
      (single, "533400", "15000", "0", "0", "2250", "0", "152459.00"),
      // Single tp with qualified dividends taxed at 15%
      (single, "87000", "10000", "2000", "0", "1800", "0", "13018.00"),
      // Single tp with qualified dividends taxed at 15% and 20%
      (single, "540000", "14000", "1000", "0", "2250", "0", "154769.00"),
      // Single tp with qualified dividends taxed at 20%,
      (single, "625000", "20000", "5000", "0", "0", "5000", "183769.00"),
      // MFS tp with qualified dividends taxed at 0%
      (mfs, "45000", "5000", "0", "5000", "0", "0", "4555.00"),
      // MFS tp with qualified dividends, income at 0% threshhold
      (mfs, "48350", "5000", "0", "5000", "0", "0", "4957.00"),
      // MFS tp with qualified dividends taxed at 0% and 15%
      (mfs, "53000", "5000", "3000", "4450", "532.5", "0", "5687.50"),
      // MFS tp with qualified dividends taxed at 15%
      (mfs, "87000", "10000", "2000", "0", "1800", "0", "13018.00"),
      // MFS tp with qualified dividends, income at 15% threshhold
      (mfs, "300000", "15000", "0", "0", "2250", "0", "70769.00"),
      // MFS tp with qualified dividends taxed at 15% and 20%
      (mfs, "307500", "14000", "1000", "0", "2152.5", "130", "73426.50"),
      // MFS tp with qualified dividends taxed at 20%,
      (mfs, "400000", "20000", "5000", "0", "0", "5000", "105019.00"),
      // MFJ tp with qualified dividends taxed at 0%
      (mfj, "90000", "3000", "7000", "10000", "0", "0", "9107.00"),
      // MFS tp with qualified dividends, income at 0% threshhold
      (mfj, "96700", "10000", "0", "10000", "0", "0", "9911.00"),
      // MFJ tp with qualified dividends taxed at 0% and 15%
      (mfj, "105000", "14500", "500", "8900", "915", "0", "11222.00"),
      // MFJ tp with qualified dividends taxed at 15%
      (mfj, "170000", "19000", "1000", "0", "3000", "0", "25424.00"),
      // MFJ tp with qualified dividends, income at 15% threshold
      (mfj, "600050", "25000", "0", "0", "3750", "0", "142556.00"),
      // MFJ tp with qualified dividends taxed at 15% and 20%
      (mfj, "615000", "15000", "10000", "0", "3555", "260", "147854.00"),
      // MFJ tp with qualified dividends taxed at 20%
      (mfj, "785000", "15000", "20000", "0", "0", "7000", "207039.00"),
      // QSS tp with qualified dividends taxed at 0%
      (qss, "90000", "3000", "7000", "10000", "0", "0", "9107.00"),
      // QSS tp with qualified dividends taxed at 0% and 15%
      (qss, "105000", "14500", "500", "8900", "915", "0", "11222.00"),
      // QSS tp with qualified dividends taxed at 15%
      (qss, "170000", "19000", "1000", "0", "3000", "0", "25424.00"),
      // QSS tp with qualified dividends taxed at 15% and 20%
      (qss, "615000", "15000", "10000", "0", "3555", "260", "147854.00"),
      // QSS tp with qualified dividends taxed at 20%
      (qss, "785000", "15000", "20000", "0", "0", "7000", "207039.00"),
      // HoH tp with qualified dividends taxed at 0%
      (hoh, "62000", "2000", "5000", "7000", "0", "0", "6249.00"),
      // HoH tp with qualified dividends, income at 0% threshold
      (hoh, "64750", "2000", "5000", "7000", "0", "0", "6579.00"),
      // HoH tp with qualified dividends taxed at 0% and 15%
      (hoh, "70000", "10000", "0", "6200", "570", "0", "7419.00"),
      // HoH tp with qualified dividends taxed at 15%
      (hoh, "115000", "15000", "0", "0", "2250", "0", "17151.00"),
      // HoH tp with qualified dividends, income at 15% threshold
      (hoh, "566700", "15000", "0", "0", "2250", "0", "162306.00"),
      // HoH tp with qualified dividends taxed at 15% and 20%
      (hoh, "580000", "10000", "10000", "0", "2940", "80", "165981.00"),
      // HoH tp with qualified dividends taxed at 20%
      (hoh, "680000", "30000", "0", "0", "0", "6000", "200649.00"),
    )
    forAll(dataTable) { (status, income, qDiv, ltGains, totalAt0, taxDueAt15, taxDueAt20, totalTax) =>
      //  Pass in necessary overrides to calculate qualified dividends withholding.
      val graph = makeGraphWith(
        factDictionary,
        filingStatus -> status,
        taxableIncome -> Dollar(income),
        qualifiedDividends -> Dollar(qDiv),
        longTermCapitalGains -> Dollar(ltGains),
      )

      // Get calculated values based on test data
      val taxTotal = graph.get(tentativeTaxFromTaxableIncomeWithNetCapitalGains)
      val amountTaxedAt0 = graph.get(amountAt0)
      val taxTotal15 = graph.get(netGainTaxAmountWith15PercentTax)
      val taxTotal20 = graph.get(netGainTaxAmountWith20PercentTax)

      assert(amountTaxedAt0.value.contains(Dollar(totalAt0)))
      assert(taxTotal15.value.contains(Dollar(taxDueAt15)))
      assert(taxTotal20.value.contains(Dollar(taxDueAt20)))
      assert(taxTotal.value.contains(Dollar(totalTax)))
    }
  }

  test("Tentative tax calculation always exclude net capital gain for single, mfj, and hoh filers with QDCG") {
    val dataTable = Table(
      ("status", "taxableIncome", "qualDiv", "ltCapitalGains", "amtAt0", "totalAt15", "totalAt20", "totalTax"),

      // Single tp with qualified dividends taxed at 0%
      (single, "40000", "4000", "1000", "5000", "0", "0", "3955.00"),
      // Single tp with qualified dividends, income at 0% threshhold
      (single, "48350", "5000", "0", "5000", "0", "0", "4957.00"),
      // Single tp with qualified dividends taxed at 0% and 15%
      (single, "53000", "5000", "3000", "4450", "532.5", "0", "5687.50"),
      // Single tp with qualified dividends, income at 15% threshhold
      (single, "533400", "15000", "0", "0", "2250", "0", "152459.00"),
      // Single tp with qualified dividends taxed at 15%
      (single, "87000", "10000", "2000", "0", "1800", "0", "13018.00"),
      // Single tp with qualified dividends taxed at 15% and 20%
      (single, "540000", "14000", "1000", "0", "2250", "0", "154769.00"),
      // Single tp with qualified dividends taxed at 20%,
      (single, "625000", "20000", "5000", "0", "0", "5000", "183769.00"),
      // MFS tp with qualified dividends taxed at 0%
      (mfs, "45000", "5000", "0", "5000", "0", "0", "4555.00"),
      // MFS tp with qualified dividends, income at 0% threshhold
      (mfs, "48350", "5000", "0", "5000", "0", "0", "4957.00"),
      // MFS tp with qualified dividends taxed at 0% and 15%
      (mfs, "53000", "5000", "3000", "4450", "532.5", "0", "5687.50"),
      // MFS tp with qualified dividends taxed at 15%
      (mfs, "87000", "10000", "2000", "0", "1800", "0", "13018.00"),
      // MFS tp with qualified dividends, income at 15% threshhold
      (mfs, "300000", "15000", "0", "0", "2250", "0", "70769.00"),
      // MFS tp with qualified dividends taxed at 15% and 20%
      (mfs, "307500", "14000", "1000", "0", "2152.5", "130", "73426.50"),
      // MFS tp with qualified dividends taxed at 20%,
      (mfs, "400000", "20000", "5000", "0", "0", "5000", "105019.00"),
      // MFJ tp with qualified dividends taxed at 0%
      (mfj, "90000", "3000", "7000", "10000", "0", "0", "9107.00"),
      // MFS tp with qualified dividends, income at 0% threshhold
      (mfj, "96700", "10000", "0", "10000", "0", "0", "9911.00"),
      // MFJ tp with qualified dividends taxed at 0% and 15%
      (mfj, "105000", "14500", "500", "8900", "915", "0", "11222.00"),
      // MFJ tp with qualified dividends taxed at 15%
      (mfj, "170000", "19000", "1000", "0", "3000", "0", "25424.00"),
      // MFJ tp with qualified dividends, income at 15% threshold
      (mfj, "600050", "25000", "0", "0", "3750", "0", "142556.00"),
      // MFJ tp with qualified dividends taxed at 15% and 20%
      (mfj, "615000", "15000", "10000", "0", "3555", "260", "147854.00"),
      // MFJ tp with qualified dividends taxed at 20%
      (mfj, "785000", "15000", "20000", "0", "0", "7000", "207039.00"),
      // QSS tp with qualified dividends taxed at 0%
      (qss, "90000", "3000", "7000", "10000", "0", "0", "9107.00"),
      // QSS tp with qualified dividends taxed at 0% and 15%
      (qss, "105000", "14500", "500", "8900", "915", "0", "11222.00"),
      // QSS tp with qualified dividends taxed at 15%
      (qss, "170000", "19000", "1000", "0", "3000", "0", "25424.00"),
      // QSS tp with qualified dividends taxed at 15% and 20%
      (qss, "615000", "15000", "10000", "0", "3555", "260", "147854.00"),
      // QSS tp with qualified dividends taxed at 20%
      (qss, "785000", "15000", "20000", "0", "0", "7000", "207039.00"),
      // HoH tp with qualified dividends taxed at 0%
      (hoh, "62000", "2000", "5000", "7000", "0", "0", "6249.00"),
      // HoH tp with qualified dividends, income at 0% threshold
      (hoh, "64750", "2000", "5000", "7000", "0", "0", "6579.00"),
      // HoH tp with qualified dividends taxed at 0% and 15%
      (hoh, "70000", "10000", "0", "6200", "570", "0", "7419.00"),
      // HoH tp with qualified dividends taxed at 15%
      (hoh, "115000", "15000", "0", "0", "2250", "0", "17151.00"),
      // HoH tp with qualified dividends, income at 15% threshold
      (hoh, "566700", "15000", "0", "0", "2250", "0", "162306.00"),
      // HoH tp with qualified dividends taxed at 15% and 20%
      (hoh, "580000", "10000", "10000", "0", "2940", "80", "165981.00"),
      // HoH tp with qualified dividends taxed at 20%
      (hoh, "680000", "30000", "0", "0", "0", "6000", "200649.00"),
    )
    forAll(dataTable) { (status, income, qDiv, ltGains, totalAt0, taxDueAt15, taxDueAt20, totalTax) =>
      //  Pass in necessary overrides to calculate qualified dividends withholding.
      val graph = makeGraphWith(
        factDictionary,
        filingStatus -> status,
        taxableIncome -> Dollar(income),
        qualifiedDividends -> Dollar(qDiv),
        longTermCapitalGains -> Dollar(ltGains),
      )

      // Get calculated values based on test data
      val amountTaxedAt0 = graph.get(amountAt0)
      val taxTotal15 = graph.get(netGainTaxAmountWith15PercentTax)
      val taxTotal20 = graph.get(netGainTaxAmountWith20PercentTax)
      val tentativeTax = graph.get(tentativeTaxFromTaxableIncome)
      val taxWithoutNetGains = graph.get(tentativeTaxFromTaxableIncomeWithoutNetGains)
      val taxWithNetGains = graph.get(tentativeTaxFromTaxableIncomeWithNetCapitalGains)

      assert(amountTaxedAt0.value.contains(Dollar(totalAt0)))
      assert(taxTotal15.value.contains(Dollar(taxDueAt15)))
      assert(taxTotal20.value.contains(Dollar(taxDueAt20)))
      assert(taxWithNetGains.value.contains(Dollar(totalTax)))
      assert(tentativeTax.value.contains(taxWithNetGains.value.get))
      assert(tentativeTax.value.get != taxWithoutNetGains.value.get)
      assert(taxWithNetGains.value.get != taxWithoutNetGains.value.get)
    }
  }

  test("Tentative tax calculation always include net capital loss for single, mfj, and hoh filers with QDCG") {
    val dataTable = Table(
      ("status", "taxableIncome", "qualDiv", "ltCapitalGains", "amtAt0", "totalAt15", "totalAt20", "totalTax"),

      // Single tp with qualified dividends taxed at 0%
      (single, "40000", "4000", "-50000", "4000.00", "0.00", "0.00", "4075.00"),
      // Single tp with qualified dividends, income at 0% threshhold
      (single, "48350", "5000", "-50000", "5000.00", "0.00", "0.00", "4957.00"),
      // Single tp with qualified dividends taxed at 0% and 15%
      (single, "53000", "5000", "-50000", "1450.00", "532.50", "0.00", "6047.50"),
      // Single tp with qualified dividends, income at 15% threshhold
      (single, "533400", "15000", "-50000", "0.00", "2250.00", "0.00", "152459.00"),
      // Single tp with qualified dividends taxed at 15%
      (single, "87000", "10000", "-50000", "0.00", "1500.00", "0.00", "13158.00"),
      // Single tp with qualified dividends taxed at 15% and 20%
      (single, "540000", "14000", "-50000", "0.00", "2100.00", "0.00", "154969.00"),
      // Single tp with qualified dividends taxed at 20%,
      (single, "625000", "20000", "-50000", "0.00", "0.00", "4000.00", "184519.00"),
      // MFS tp with qualified dividends taxed at 0%
      (mfs, "45000", "5000", "-50000", "5000.00", "0.00", "0.00", "4555.00"),
      // MFS tp with qualified dividends, income at 0% threshhold
      (mfs, "48350", "5000", "-50000", "5000.00", "0.00", "0.00", "4957.00"),
      // MFS tp with qualified dividends taxed at 0% and 15%
      (mfs, "53000", "5000", "-50000", "1450.00", "532.50", "0.00", "6047.50"),
      // MFS tp with qualified dividends taxed at 15%
      (mfs, "87000", "10000", "-50000", "0.00", "1500.00", "0.00", "13158.00"),
      // MFS tp with qualified dividends, income at 15% threshhold
      (mfs, "300000", "15000", "-50000", "0.00", "2250.00", "0.00", "70769.00"),
      // MFS tp with qualified dividends taxed at 15% and 20%
      (mfs, "307500", "14000", "-50000", "0.00", "2002.50", "130.00", "73626.50"),
      // MFS tp with qualified dividends taxed at 20%,
      (mfs, "400000", "20000", "-50000", "0.00", "0.00", "4000.00", "105769.00"),
      // MFJ tp with qualified dividends taxed at 0%
      (mfj, "90000", "3000", "-50000", "3000.00", "0.00", "0.00", "9947.00"),
      // MFS tp with qualified dividends, income at 0% threshhold
      (mfj, "96700", "10000", "-50000", "10000.00", "0.00", "0.00", "9911.00"),
      // MFJ tp with qualified dividends taxed at 0% and 15%
      (mfj, "105000", "14500", "-50000", "8400.00", "915.00", "0.00", "11282.00"),
      // MFJ tp with qualified dividends taxed at 15%
      (mfj, "170000", "19000", "-50000", "0.00", "2850.00", "0.00", "25494.00"),
      // MFJ tp with qualified dividends, income at 15% threshold
      (mfj, "600050", "25000", "-50000", "0.00", "3750.00", "0.00", "142556.00"),
      // MFJ tp with qualified dividends taxed at 15% and 20%
      (mfj, "615000", "15000", "-50000", "0.00", "2055.00", "260.00", "149854.00"),
      // MFJ tp with qualified dividends taxed at 20%
      (mfj, "785000", "15000", "-50000", "0.00", "0.00", "3000.00", "210065.00"),
      // QSS tp with qualified dividends taxed at 0%
      (qss, "90000", "3000", "-50000", "3000.00", "0.00", "0.00", "9947.00"),
      // QSS tp with qualified dividends taxed at 0% and 15%
      (qss, "105000", "14500", "-50000", "8400.00", "915.00", "0.00", "11282.00"),
      // QSS tp with qualified dividends taxed at 15%
      (qss, "170000", "19000", "-50000", "0.00", "2850.00", "0.00", "25494.00"),
      // QSS tp with qualified dividends taxed at 15% and 20%
      (qss, "615000", "15000", "-50000", "0.00", "2055.00", "260.00", "149854.00"),
      // QSS tp with qualified dividends taxed at 20%
      (qss, "785000", "15000", "-50000", "0.00", "0.00", "3000.00", "210065.00"),
      // HoH tp with qualified dividends taxed at 0%
      (hoh, "62000", "2000", "-50000", "2000.00", "0.00", "0.00", "6849.00"),
      // HoH tp with qualified dividends, income at 0% threshold
      (hoh, "64750", "2000", "-50000", "2000.00", "0.00", "0.00", "7179.00"),
      // HoH tp with qualified dividends taxed at 0% and 15%
      (hoh, "70000", "10000", "-50000", "6200.00", "570.00", "0.00", "7419.00"),
      // HoH tp with qualified dividends taxed at 15%
      (hoh, "115000", "15000", "-50000", "0.00", "2250.00", "0.00", "17151.00"),
      // HoH tp with qualified dividends, income at 15% threshold
      (hoh, "566700", "15000", "-50000", "0.00", "2250.00", "0.00", "162306.00"),
      // HoH tp with qualified dividends taxed at 15% and 20%
      (hoh, "580000", "10000", "-50000", "0.00", "1440.00", "80.00", "167981.00"),
      // HoH tp with qualified dividends taxed at 20%
      (hoh, "680000", "30000", "-50000", "0.00", "0.00", "6000.00", "200649.00"),
    )
    forAll(dataTable) { (status, income, qDiv, ltGains, totalAt0, taxDueAt15, taxDueAt20, totalTax) =>
      //  Pass in necessary overrides to calculate qualified dividends withholding.
      val graph = makeGraphWith(
        factDictionary,
        filingStatus -> status,
        taxableIncome -> Dollar(income),
        qualifiedDividends -> Dollar(qDiv),
        longTermCapitalGains -> Dollar(ltGains),
      )

      // Get calculated values based on test data
      val amountTaxedAt0 = graph.get(amountAt0)
      val taxTotal15 = graph.get(netGainTaxAmountWith15PercentTax)
      val taxTotal20 = graph.get(netGainTaxAmountWith20PercentTax)
      val tentativeTax = graph.get(tentativeTaxFromTaxableIncome)
      val taxWithoutNetGains = graph.get(tentativeTaxFromTaxableIncomeWithoutNetGains)
      val taxWithNetGains = graph.get(tentativeTaxFromTaxableIncomeWithNetCapitalGains)

      assert(amountTaxedAt0.value.contains(Dollar(totalAt0)))
      assert(taxTotal15.value.contains(Dollar(taxDueAt15)))
      assert(taxTotal20.value.contains(Dollar(taxDueAt20)))
      assert(taxWithNetGains.value.contains(Dollar(totalTax)))
      assert(tentativeTax.value.contains(taxWithNetGains.value.get))
    }
  }
  test("Calculates appropriate tax withholding for single, mfj, and hoh filers without QDCG") {
    val dataTable = Table(
      ("status", "taxableIncome", "qualDiv", "ltCapitalGains", "amtAt0", "totalAt15", "totalAt20", "totalTax"),
      (single, "40000", "0", "0", "0", "0", "0", "4555.00"),
      (single, "48350", "0", "0", "0", "0", "0", "5557.00"),
      (single, "53000", "0", "0", "0", "0", "0", "6378.00"),
      (single, "533400", "0", "0", "0", "0", "0", "155459.00"),
      (single, "87000", "0", "0", "0", "0", "0", "13858.00"),
      (single, "540000", "0", "0", "0", "0", "0", "157769.00"),
      (single, "625000", "0", "0", "0", "0", "0", "187519.00"),
      (mfs, "45000", "0", "0", "0", "0", "0", "5155.00"),
      (mfs, "48350", "0", "0", "0", "0", "0", "5557.00"),
      (mfs, "53000", "0", "0", "0", "0", "0", "6378.00"),
      (mfs, "87000", "0", "0", "0", "0", "0", "13858.00"),
      (mfs, "300000", "0", "0", "0", "0", "0", "73769.00"),
      (mfs, "307500", "0", "0", "0", "0", "0", "76394.00"),
      (mfs, "400000", "0", "0", "0", "0", "0", "109082.00"),
      (mfj, "90000", "0", "0", "0", "0", "0", "10307.00"),
      (mfj, "96700", "0", "0", "0", "0", "0", "11111.00"),
      (mfj, "105000", "0", "0", "0", "0", "0", "12524.00"),
      (mfj, "170000", "0", "0", "0", "0", "0", "26824.00"),
      (mfj, "600050", "0", "0", "0", "0", "0", "147556.00"),
      (mfj, "615000", "0", "0", "0", "0", "0", "152789.00"),
      (mfj, "785000", "0", "0", "0", "0", "0", "212615.00"),
      (qss, "90000", "0", "0", "0", "0", "0", "10307.00"),
      (qss, "105000", "0", "0", "0", "0", "0", "12524.00"),
      (qss, "170000", "0", "0", "0", "0", "0", "26824.00"),
      (qss, "615000", "0", "0", "0", "0", "0", "152789.00"),
      (qss, "785000", "0", "0", "0", "0", "0", "212615.00"),
      (hoh, "62000", "0", "0", "0", "0", "0", "7089.00"),
      (hoh, "64750", "0", "0", "0", "0", "0", "7419.00"),
      (hoh, "70000", "0", "0", "0", "0", "0", "8307.00"),
      (hoh, "115000", "0", "0", "0", "0", "0", "18387.00"),
      (hoh, "566700", "0", "0", "0", "0", "0", "165306.00"),
      (hoh, "580000", "0", "0", "0", "0", "0", "169961.00"),
      (hoh, "680000", "0", "0", "0", "0", "0", "205749.00"),
    )
    forAll(dataTable) { (status, income, qDiv, ltGains, totalAt0, taxDueAt15, taxDueAt20, totalTax) =>
      //  Pass in necessary overrides to calculate qualified dividends withholding.
      val graph = makeGraphWith(
        factDictionary,
        filingStatus -> status,
        taxableIncome -> Dollar(income),
        qualifiedDividends -> Dollar(qDiv),
        longTermCapitalGains -> Dollar(ltGains),
      )

      // Get calculated values based on test data
      val tentativeTax = graph.get(tentativeTaxFromTaxableIncome)
      val taxWithoutNetGains = graph.get(tentativeTaxFromTaxableIncomeWithoutNetGains)
      val taxWithNetGains = graph.get(tentativeTaxFromTaxableIncomeWithNetCapitalGains)
      val amountTaxedAt0 = graph.get(amountAt0)
      val taxTotal15 = graph.get(netGainTaxAmountWith15PercentTax)
      val taxTotal20 = graph.get(netGainTaxAmountWith20PercentTax)

      assert(amountTaxedAt0.value.contains(Dollar(totalAt0)))
      assert(taxTotal15.value.contains(Dollar(taxDueAt15)))
      assert(taxTotal20.value.contains(Dollar(taxDueAt20)))
      assert(tentativeTax.value.contains(Dollar(totalTax)))
      assert(tentativeTax.value.contains(taxWithoutNetGains.value.get))
      assert(tentativeTax.value.contains(taxWithNetGains.value.get))
    }
  }
}

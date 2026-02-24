package gov.irs.twe.factDictionary

import gov.irs.factgraph.types.Dollar
import gov.irs.factgraph.types.Enum
import gov.irs.factgraph.Path
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.prop.TableDrivenPropertyChecks

class OB3DeductionSpec extends AnyFunSuite with TableDrivenPropertyChecks {
  val factDictionary = setupFactDictionary()
  val single = Enum("single", "/filingStatusOptions")
  val mfs = Enum("marriedFilingSeparately", "/filingStatusOptions")
  val mfj = Enum("marriedFilingJointly", "/filingStatusOptions")
  val qss = Enum("qualifiedSurvivingSpouse", "/filingStatusOptions")
  val hoh = Enum("headOfHousehold", "/filingStatusOptions")

  test("test OB3 70424: non-itemizer charitable deduction") {
    object standardDeduction:
      val single = "15750"
      val mfs = "15750"
      val qss = "31500"
      val hoh = "23625"
      val mfj = "31500"
    val dataTable = Table(
      (
        "status",
        "standardDeduction",
        "itemizedDeductionTotal",
        "magiForOB3BelowLineDeductions",
        "nonItemizerCharitableContributionDeductionAmount",
        "nonItemizerCharitableContributionDeduction",
      ),
      // SINGLE
      // single standard is larger than itemized
      (single, standardDeduction.single, "0", "50000", "500", "500"),
      (single, standardDeduction.single, "0", "50000", "1000", "1000"),
      (single, standardDeduction.single, "0", "50000", "2000", "1000"),
      (single, standardDeduction.single, "15000", "50000", "1000", "1000"),
      (single, standardDeduction.single, "15000", "50000", "2000", "1000"),
      (single, standardDeduction.single, "15749", "50000", "2000", "1000"),
      // single itemized is larger than standard, no deduction
      (single, standardDeduction.single, "16755", "50000", "1000", "0"),
      (single, standardDeduction.single, "16755", "50000", "2000", "0"),

      // MFS
      // MFS standard is larger than itemized
      (mfs, standardDeduction.mfs, "0", "50000", "500", "500"),
      (mfs, standardDeduction.mfs, "0", "50000", "1000", "1000"),
      (mfs, standardDeduction.mfs, "0", "50000", "2000", "1000"),
      (mfs, standardDeduction.mfs, "15000", "50000", "1000", "1000"),
      (mfs, standardDeduction.mfs, "15000", "50000", "2000", "1000"),
      (mfs, standardDeduction.mfs, "15749", "50000", "2000", "1000"),

      // MFS itemized is larger than standard, no deduction
      (mfs, standardDeduction.mfs, "16755", "50000", "1000", "0"),
      (mfs, standardDeduction.mfs, "16755", "50000", "2000", "0"),

      // QSS
      // QSS standard is larger than itemized
      (qss, standardDeduction.qss, "0", "50000", "500", "500"),
      (qss, standardDeduction.qss, "0", "50000", "1000", "1000"),
      (qss, standardDeduction.qss, "0", "50000", "2000", "1000"),
      (qss, standardDeduction.qss, "31000", "50000", "1000", "1000"),
      (qss, standardDeduction.qss, "31000", "50000", "2000", "1000"),
      (qss, standardDeduction.qss, "31499", "50000", "2000", "1000"),

      // QSS itemized is larger than standard, no deduction
      (qss, standardDeduction.qss, "32500", "50000", "1000", "0"),
      (qss, standardDeduction.qss, "32600", "50000", "2000", "0"),

      // HOH
      // HOH standard is larger than itemized
      (hoh, standardDeduction.hoh, "0", "50000", "500", "500"),
      (hoh, standardDeduction.hoh, "0", "50000", "1000", "1000"),
      (hoh, standardDeduction.hoh, "0", "50000", "2000", "1000"),
      (hoh, standardDeduction.hoh, "23600", "50000", "1000", "1000"),
      (hoh, standardDeduction.hoh, "23600", "50000", "2000", "1000"),
      (hoh, standardDeduction.hoh, "23600", "50000", "2000", "1000"),
      (hoh, standardDeduction.hoh, "23624", "50000", "2000", "1000"),

      // HOH itemized is larger than standard, no deduction
      (hoh, standardDeduction.hoh, "25625", "50000", "1000", "0"),
      (hoh, standardDeduction.hoh, "25630", "50000", "2000", "0"),

      // MFJ
      // MFJ standard is larger than itemized
      (mfj, standardDeduction.mfj, "0", "50000", "500", "500"),
      (mfj, standardDeduction.mfj, "0", "50000", "1000", "1000"),
      (mfj, standardDeduction.mfj, "0", "50000", "2000", "2000"),
      (mfj, standardDeduction.mfj, "31000", "50000", "2000", "2000"),
      (mfj, standardDeduction.mfj, "31000", "50000", "3000", "2000"),
      (mfj, standardDeduction.mfj, "31499", "50000", "3000", "2000"),

      // MFJ itemized is larger than standard, no deduction
      (mfj, standardDeduction.mfj, "33500", "50000", "1000", "0"),
      (mfj, standardDeduction.mfj, "33600", "50000", "2000", "0"),
    )
    forAll(dataTable) {
      (
          status,
          standardDeduction,
          itemizedDeductionTotal,
          magiForOB3BelowLineDeductions,
          nonItemizerCharitableContributionDeductionAmount,
          nonItemizerCharitableContributionDeduction,
      ) =>
        val graph = makeGraphWith(
          factDictionary,
          Path("/filingStatus") -> status,
          Path("/itemizedDeductionTotal") -> Dollar(itemizedDeductionTotal),
          Path("/magiForOB3BelowLineDeductions") -> Dollar(magiForOB3BelowLineDeductions),
          Path("/nonItemizerCharitableContributionDeductionAmount") -> Dollar(
            nonItemizerCharitableContributionDeductionAmount,
          ),
          Path("/standardDeduction") -> Dollar(standardDeduction),
        )

        val actual = graph.get("/nonItemizerCharitableContributionDeduction")

        assert(actual.value.contains(Dollar(nonItemizerCharitableContributionDeduction)))
    }
  }

  test("test OB3 70203: car loan interest limit and phase out") {
    val dataTable = Table(
      (
        "status",
        "magiForOB3BelowLineDeductions",
        "personalVehicleLoanInterestAmount",
        "qualifiedPersonalVehicleLoanInterestDeduction",
      ),
      //      Single, $100,000 threshold start, $150,000 threshold end, each $10,000 more of MAGI reduces deduction by $2,000
      (single, "50000", "0", "0"),
      (single, "50000", "5000", "5000"),
      (single, "50000", "9999", "9999"),
      (single, "50000", "10000", "10000"),
      (single, "100000", "10000", "10000"),
      (single, "110000", "10000", "8000"),
      (single, "120000", "10000", "6000"),
      (single, "130000", "10000", "4000"),
      (single, "140000", "10000", "2000"),
      (single, "150000", "10000", "0"),
      //      MFS, $100,000 threshold start, $150,000 threshold end, each $10,000 more of MAGI reduces deduction by $2,000
      (mfs, "50000", "0", "0"),
      (mfs, "50000", "5000", "5000"),
      (mfs, "50000", "9999", "9999"),
      (mfs, "50000", "10000", "10000"),
      (mfs, "100000", "10000", "10000"),
      (mfs, "110000", "10000", "8000"),
      (mfs, "120000", "10000", "6000"),
      (mfs, "130000", "10000", "4000"),
      (mfs, "140000", "10000", "2000"),
      (mfs, "150000", "10000", "0"),
      //      HOH, $100,000 threshold start, $150,000 threshold end, each $10,000 more of MAGI reduces deduction by $2,000
      (hoh, "50000", "0", "0"),
      (hoh, "50000", "5000", "5000"),
      (hoh, "50000", "9999", "9999"),
      (hoh, "50000", "10000", "10000"),
      (hoh, "100000", "10000", "10000"),
      (hoh, "110000", "10000", "8000"),
      (hoh, "120000", "10000", "6000"),
      (hoh, "130000", "10000", "4000"),
      (hoh, "140000", "10000", "2000"),
      (hoh, "150000", "10000", "0"),
      //      QSS, $100,000 threshold start, $150,000 threshold end, each $10,000 more of MAGI reduces deduction by $2,000
      (qss, "50000", "0", "0"),
      (qss, "50000", "5000", "5000"),
      (qss, "50000", "9999", "9999"),
      (qss, "50000", "10000", "10000"),
      (qss, "100000", "10000", "10000"),
      (qss, "110000", "10000", "8000"),
      (qss, "120000", "10000", "6000"),
      (qss, "130000", "10000", "4000"),
      (qss, "140000", "10000", "2000"),
      (qss, "150000", "10000", "0"),
      //      MFJ, $200,000 threshold start, $250,000 threshold end, each $10,000 more of MAGI reduces deduction by $2,000
      (mfj, "50000", "0", "0"),
      (mfj, "50000", "5000", "5000"),
      (mfj, "50000", "9999", "9999"),
      (mfj, "50000", "10000", "10000"),
      (mfj, "100000", "10000", "10000"),
      (mfj, "200000", "10000", "10000"),
      (mfj, "210000", "10000", "8000"),
      (mfj, "220000", "10000", "6000"),
      (mfj, "230000", "10000", "4000"),
      (mfj, "240000", "10000", "2000"),
      (mfj, "250000", "10000", "0"),
    )
    forAll(dataTable) {
      (
          status,
          magiForOB3BelowLineDeductions,
          personalVehicleLoanInterestAmount,
          qualifiedPersonalVehicleLoanInterestDeduction,
      ) =>
        val graph = makeGraphWith(
          factDictionary,
          Path("/filingStatus") -> status,
          Path("/magiForOB3BelowLineDeductions") -> Dollar(magiForOB3BelowLineDeductions),
          Path("/personalVehicleLoanInterestAmount") -> Dollar(personalVehicleLoanInterestAmount),
        )

        val actual = graph.get("/qualifiedPersonalVehicleLoanInterestDeduction")

        assert(actual.value.contains(Dollar(qualifiedPersonalVehicleLoanInterestDeduction)))
    }
  }

  test("test OB3 70103: senior deduction eligibility") {
    val dataTable = Table(
      (
        "status",
        "primaryFilerAge65OrOlder",
        "primaryTaxpayerElectsForSeniorDeduction",
        "secondaryFilerAge65OrOlder",
        "secondaryTaxpayerElectsForSeniorDeduction",
        "eligibleTaxpayers",
      ),
      // neither are 65 or older and they don't see the senior deduction election option
      (single, false, None, None, None, 0),
      (mfs, false, None, None, None, 0),
      (hoh, false, None, None, None, 0),
      (qss, false, None, None, None, 0),
      (mfj, false, None, None, None, 0),

      // primary over 65, secondary isn't
      (single, true, true, None, None, 1),
      (mfs, true, true, None, None, 1),
      (hoh, true, true, None, None, 1),
      (qss, true, true, None, None, 1),
      (mfj, true, true, None, None, 1),

      // primary is under 65, secondary is over 65. Only applies to MFJ
      (single, false, None, None, None, 0),
      (mfs, false, None, None, None, 0),
      (hoh, false, None, None, None, 0),
      (qss, false, None, None, None, 0),
      (mfj, false, None, true, true, 1),

      // both primary and secondary are over 65 but only primary elects for senior deduction
      (single, true, true, None, None, 1),
      (mfs, true, true, None, None, 1),
      (hoh, true, true, None, None, 1),
      (qss, true, true, None, None, 1),
      (mfj, true, true, true, false, 1),

      // both primary and secondary are over 65 but only secondary elects for senior deduction
      (single, true, false, None, None, 0),
      (mfs, true, false, None, None, 0),
      (hoh, true, false, None, None, 0),
      (qss, true, false, None, None, 0),
      (mfj, true, false, true, true, 1),

      // both primary and secondary are over 65 and both elect for senior deduction
      (single, true, true, None, None, 1),
      (mfs, true, true, None, None, 1),
      (hoh, true, true, None, None, 1),
      (qss, true, true, None, None, 1),
      (mfj, true, true, true, true, 2),
    )
    forAll(dataTable) {
      (
          status,
          primaryFilerAge65OrOlder,
          primaryTaxpayerElectsForSeniorDeduction,
          secondaryFilerAge65OrOlder,
          secondaryTaxpayerElectsForSeniorDeduction,
          eligibleTaxpayers,
      ) =>
        val primary65Older: Boolean =
          if (primaryFilerAge65OrOlder.equals(None) || secondaryFilerAge65OrOlder.equals(false)) false else true
        val secondary65Older: Boolean =
          if (secondaryFilerAge65OrOlder.equals(None) || secondaryFilerAge65OrOlder.equals(false)) false else true
        val primaryElect: Boolean =
          if (
            primaryTaxpayerElectsForSeniorDeduction
              .equals(None) || primaryTaxpayerElectsForSeniorDeduction.equals(false)
          ) false
          else true
        val secondaryElect: Boolean =
          if (
            secondaryTaxpayerElectsForSeniorDeduction
              .equals(None) || secondaryTaxpayerElectsForSeniorDeduction.equals(false)
          ) false
          else true
        val graph = makeGraphWith(
          factDictionary,
          Path("/filingStatus") -> status,
          Path("/primaryFilerAge65OrOlder") -> primary65Older,
          Path("/secondaryFilerAge65OrOlder") -> secondary65Older,
          Path("/primaryTaxpayerElectsForSeniorDeduction") -> primaryElect,
          Path("/secondaryTaxpayerElectsForSeniorDeduction") -> secondaryElect,
        )

        val actual = graph.get("/eligibleTaxpayersForSeniorDeduction")

        assert(actual.value.contains(eligibleTaxpayers))
    }
  }

  test("test OB3 70103: senior deduction phase out and limit") {
    val dataTable = Table(
      (
        "status",
        "magiForOB3BelowLineDeductions",
        "eligibleTaxpayersForSeniorDeduction",
        "expectedSeniorDeduction",
      ),
      //      Single, $75,000 threshold start, $175,000 threshold end, each $10,000 more of MAGI reduces deduction by $600
      (single, "74000", 0, "0"),
      (single, "74000", 1, "6000"),
      (single, "75000", 1, "6000"),
      (single, "80000", 1, "5700"),
      (single, "90000", 1, "5100"),
      (single, "100000", 1, "4500"),
      (single, "110000", 1, "3900"),
      (single, "120000", 1, "3300"),
      (single, "130000", 1, "2700"),
      (single, "140000", 1, "2100"),
      (single, "150000", 1, "1500"),
      (single, "160000", 1, "900"),
      (single, "170000", 1, "300"),
      (single, "175000", 1, "0"),
      (single, "175000", 0, "0"),

      //      Single, $75,000 threshold start, $175,000 threshold end, each $10,000 more of MAGI reduces deduction by $600
      (single, "74000", 0, "0"),
      (single, "74000", 1, "6000"),
      (single, "75000", 1, "6000"),
      (single, "80000", 1, "5700"),
      (single, "90000", 1, "5100"),
      (single, "100000", 1, "4500"),
      (single, "110000", 1, "3900"),
      (single, "120000", 1, "3300"),
      (single, "130000", 1, "2700"),
      (single, "140000", 1, "2100"),
      (single, "150000", 1, "1500"),
      (single, "160000", 1, "900"),
      (single, "170000", 1, "300"),
      (single, "175000", 1, "0"),
      (single, "175000", 0, "0"),

      //      MFS, $75,000 threshold start, $175,000 threshold end, each $10,000 more of MAGI reduces deduction by $600
      (mfs, "74000", 0, "0"),
      (mfs, "74000", 1, "6000"),
      (mfs, "75000", 1, "6000"),
      (mfs, "80000", 1, "5700"),
      (mfs, "90000", 1, "5100"),
      (mfs, "100000", 1, "4500"),
      (mfs, "110000", 1, "3900"),
      (mfs, "120000", 1, "3300"),
      (mfs, "130000", 1, "2700"),
      (mfs, "140000", 1, "2100"),
      (mfs, "150000", 1, "1500"),
      (mfs, "160000", 1, "900"),
      (mfs, "170000", 1, "300"),
      (mfs, "175000", 1, "0"),
      (mfs, "175000", 0, "0"),

      //      HOH, $75,000 threshold start, $175,000 threshold end, each $10,000 more of MAGI reduces deduction by $600
      (hoh, "74000", 0, "0"),
      (hoh, "74000", 1, "6000"),
      (hoh, "75000", 1, "6000"),
      (hoh, "80000", 1, "5700"),
      (hoh, "90000", 1, "5100"),
      (hoh, "100000", 1, "4500"),
      (hoh, "110000", 1, "3900"),
      (hoh, "120000", 1, "3300"),
      (hoh, "130000", 1, "2700"),
      (hoh, "140000", 1, "2100"),
      (hoh, "150000", 1, "1500"),
      (hoh, "160000", 1, "900"),
      (hoh, "170000", 1, "300"),
      (hoh, "175000", 1, "0"),
      (hoh, "175000", 0, "0"),

      //      QSS, $75,000 threshold start, $175,000 threshold end, each $10,000 more of MAGI reduces deduction by $600
      (qss, "74000", 0, "0"),
      (qss, "74000", 1, "6000"),
      (qss, "75000", 1, "6000"),
      (qss, "80000", 1, "5700"),
      (qss, "90000", 1, "5100"),
      (qss, "100000", 1, "4500"),
      (qss, "110000", 1, "3900"),
      (qss, "120000", 1, "3300"),
      (qss, "130000", 1, "2700"),
      (qss, "140000", 1, "2100"),
      (qss, "150000", 1, "1500"),
      (qss, "160000", 1, "900"),
      (qss, "170000", 1, "300"),
      (qss, "175000", 1, "0"),
      (qss, "175000", 0, "0"),

      //      MFJ, $150,000 threshold start, $250,000 threshold end, each $10,000 more of MAGI reduces deduction by $600 (per taxpayer)
      (mfj, "140000", 0, "0"),
      //      1 eligible taxpayer
      (mfj, "140000", 1, "6000"),
      (mfj, "150000", 1, "6000"),
      (mfj, "160000", 1, "5400"),
      (mfj, "170000", 1, "4800"),
      (mfj, "180000", 1, "4200"),
      (mfj, "190000", 1, "3600"),
      (mfj, "200000", 1, "3000"),
      (mfj, "210000", 1, "2400"),
      (mfj, "220000", 1, "1800"),
      (mfj, "230000", 1, "1200"),
      (mfj, "240000", 1, "600"),
      (mfj, "250000", 1, "0"),
      (mfj, "260000", 1, "0"),
      //      2 eligible taxpayers
      (mfj, "140000", 2, "12000"),
      (mfj, "150000", 2, "12000"),
      (mfj, "160000", 2, "10800"),
      (mfj, "170000", 2, "9600"),
      (mfj, "180000", 2, "8400"),
      (mfj, "190000", 2, "7200"),
      (mfj, "200000", 2, "6000"),
      (mfj, "210000", 2, "4800"),
      (mfj, "220000", 2, "3600"),
      (mfj, "230000", 2, "2400"),
      (mfj, "240000", 2, "1200"),
      (mfj, "250000", 2, "0"),
      (mfj, "260000", 2, "0"),
    )
    forAll(dataTable) {
      (
          status,
          magiForOB3BelowLineDeductions,
          eligibleTaxpayersForSeniorDeduction,
          expectedSeniorDeduction,
      ) =>
        val graph = makeGraphWith(
          factDictionary,
          Path("/filingStatus") -> status,
          Path("/eligibleTaxpayersForSeniorDeduction") -> eligibleTaxpayersForSeniorDeduction,
          Path("/magiForOB3BelowLineDeductions") -> Dollar(magiForOB3BelowLineDeductions),
        )

        val actual = graph.get("/seniorDeduction")

        assert(actual.value.contains(Dollar(expectedSeniorDeduction)))
    }
  }
}

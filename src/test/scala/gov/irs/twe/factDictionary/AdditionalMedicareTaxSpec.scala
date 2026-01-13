package gov.irs.twe.factDictionary

import gov.irs.factgraph.types.{ Dollar, Enum }
import gov.irs.factgraph.Path
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.prop.TableDrivenPropertyChecks

class AdditionalMedicareTaxSpec extends AnyFunSuite with TableDrivenPropertyChecks {
  val factDictionary = setupFactDictionary()
  val single = Enum("single", "/filingStatusOptions")
  val mfs = Enum("marriedFilingSeparately", "/filingStatusOptions")
  val mfj = Enum("marriedFilingJointly", "/filingStatusOptions")
  val qss = Enum("qualifiedSurvivingSpouse", "/filingStatusOptions")
  val hoh = Enum("headOfHousehold", "/filingStatusOptions")
  val netEarningsFromSelfEmployment = Path("/netSelfEmploymentIncome")
  val selfEmploymentTaxResult = Path("/selfEmploymentTax")

  // this table accounts for totalJobIncome as a lump fact, eventually it will be split by self and spouse
  val dataTable = Table(
    (
      "status",
      "netSelfEmploymentIncomeSelf",
      "netSelfEmploymentIncomeSpouse",
      "jobsIncomeSelf",
      "jobsIncomeSpouse",
      "expectedScheduleSELine6Self",
      "expectedScheduleSELine6Spouse",
      "expectedAdditionalMedicareTaxOnMedicareWagesForm5969Line7",
      "expectedAdditionalMedicareTaxOnSelfEmploymentIncomeForm8959Line13",
      "expectedAdditionalMedicareTax",
    ),
    ////////////////////////////// BASE///////////////////////////////////////////
    (single, "20000", "0", "0", "0", "18470", "0", "0", "0", "0"),
    (single, "24210", "0", "200000", "0", "22358", "0", "0", "201", "201"),
    (single, "25000", "0", "251912", "0", "23088", "0", "467", "208", "675"),

    ////////////////////////////// NON-MFJ///////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////// WAGES ONLY, NO SE INCOME///////////////////////////////////////////

    (single, "0", "0", "20000", "0", "0", "0", "0", "0", "0"),
    (qss, "0", "0", "20000", "0", "0", "0", "0", "0", "0"),
    (mfs, "0", "0", "20000", "0", "0", "0", "0", "0", "0"),
    (hoh, "0", "0", "20000", "0", "0", "0", "0", "0", "0"),
    (qss, "0", "0", "20000", "0", "0", "0", "0", "0", "0"),
    (single, "20000", "0", "0", "0", "18470", "0", "0", "0", "0"),
    (single, "74000", "0", "0", "0", "68339", "0", "0", "0", "0"),
    (single, "176100", "0", "0", "0", "162628", "0", "0", "0", "0"),
    (single, "180000", "0", "0", "0", "166230", "0", "0", "0", "0"),
    (single, "150000", "0", "0", "0", "138525", "0", "0", "0", "0"),
    (single, "200000", "0", "0", "0", "184700", "0", "0", "0", "0"),
    (single, "235200", "0", "0", "0", "217207", "0", "0", "155", "155"),
    (mfs, "20000", "0", "0", "0", "18470", "0", "0", "0", "0"),
    (mfs, "74000", "0", "0", "0", "68339", "0", "0", "0", "0"),
    (mfs, "176100", "0", "0", "0", "162628", "0", "0", "339", "339"),
    (mfs, "180000", "0", "0", "0", "166230", "0", "0", "371", "371"),
    (mfs, "150000", "0", "0", "0", "138525", "0", "0", "122", "122"),
    (mfs, "200000", "0", "0", "0", "184700", "0", "0", "537", "537"),
    (mfs, "235200", "0", "0", "0", "217207", "0", "0", "830", "830"),
    (hoh, "20000", "0", "0", "0", "18470", "0", "0", "0", "0"),
    (hoh, "74000", "0", "0", "0", "68339", "0", "0", "0", "0"),
    (hoh, "176100", "0", "0", "0", "162628", "0", "0", "0", "0"),
    (hoh, "180000", "0", "0", "0", "166230", "0", "0", "0", "0"),
    (hoh, "150000", "0", "0", "0", "138525", "0", "0", "0", "0"),
    (hoh, "200000", "0", "0", "0", "184700", "0", "0", "0", "0"),
    (hoh, "235200", "0", "0", "0", "217207", "0", "0", "155", "155"),
    (qss, "20000", "0", "0", "0", "18470", "0", "0", "0", "0"),
    (qss, "74000", "0", "0", "0", "68339", "0", "0", "0", "0"),
    (qss, "176100", "0", "0", "0", "162628", "0", "0", "0", "0"),
    (qss, "180000", "0", "0", "0", "166230", "0", "0", "0", "0"),
    (qss, "150000", "0", "0", "0", "138525", "0", "0", "0", "0"),
    (qss, "200000", "0", "0", "0", "184700", "0", "0", "0", "0"),
    (qss, "235200", "0", "0", "0", "217207", "0", "0", "155", "155"),

    ////////////////////////////// NO WAGES, SE INCOME ONLY///////////////////////////////////////////
    (single, "15000", "0", "74000", "0", "13853", "0", "0", "0", "0"),
    (single, "24210", "0", "150000", "0", "22358", "0", "0", "0", "0"),
    (single, "24210", "0", "200000", "0", "22358", "0", "0", "201", "201"),
    (single, "25000", "0", "235200", "0", "23088", "0", "317", "208", "525"),
    (mfs, "15000", "0", "74000", "0", "13853", "0", "0", "0", "0"),
    (mfs, "24210", "0", "150000", "0", "22358", "0", "225", "201", "426"),
    (mfs, "24210", "0", "200000", "0", "22358", "0", "675", "201", "876"),
    (mfs, "25000", "0", "235200", "0", "23088", "0", "992", "208", "1200"),
    (hoh, "15000", "0", "74000", "0", "13853", "0", "0", "0", "0"),
    (hoh, "24210", "0", "150000", "0", "22358", "0", "0", "0", "0"),
    (hoh, "24210", "0", "200000", "0", "22358", "0", "0", "201", "201"),
    (hoh, "25000", "0", "235200", "0", "23088", "0", "317", "208", "525"),
    (qss, "15000", "0", "74000", "0", "13853", "0", "0", "0", "0"),
    (qss, "24210", "0", "150000", "0", "22358", "0", "0", "0", "0"),
    (qss, "24210", "0", "200000", "0", "22358", "0", "0", "201", "201"),
    (qss, "25000", "0", "235200", "0", "23088", "0", "317", "208", "525"),

//    ////////////////////////////// MFJ///////////////////////////////////////////
//
//    ////////////////////////////// WAGES ONLY, NO SE INCOME///////////////////////////////////////////
    (mfj, "0", "0", "20000", "0", "0", "0", "0", "0", "0"),
//    ////////////////////////////// WAGES AND SELF SE INCOME, NO SPOUSE SE INCOME///////////////////////////////////////////
    (mfj, "15000", "0", "74000", "0", "13853", "0", "0", "0", "0"),
    (mfj, "24210", "0", "150000", "0", "22358", "0", "0", "0", "0"),
    (mfj, "24210", "0", "200000", "0", "22358", "0", "0", "0", "0"),
    (mfj, "25000", "0", "235200", "0", "23088", "0", "0", "75", "75"),
    ////////////////////////////// WAGES AND SPOUSE SE INCOME, NO SELF SE INCOME///////////////////////////////////////////
    (mfj, "0", "15000", "0", "74000", "0", "13852.50", "0", "0", "0"),
    (mfj, "0", "24210", "0", "150000", "0", "22357.94", "0", "0", "0"),
    (mfj, "0", "24210", "0", "200000", "0", "22357.94", "0", "0", "0"),
    (mfj, "0", "25000", "0", "235200", "0", "23087.50", "0", "75", "75"),
    ////////////////////////////// WAGES AND SELF SE INCOME AND SPOUSE SE INCOME///////////////////////////////////////////
    (mfj, "10000", "15000", "74000", "0", "9235", "13852.50", "0", "0", "0"),
    (mfj, "20000", "15000", "0", "74000", "18470", "13852.50", "0", "0", "0"),
    (mfj, "10000", "24210", "150000", "0", "9235", "22357.94", "0", "0", "0"),
    (mfj, "10000", "24210", "0", "200000", "9235", "22357.94", "0", "0", "0"),
    (mfj, "10000", "25000", "235200", "0", "9235", "23087.50", "0", "158", "158"),
    (mfj, "100000", "150000", "0", "235200", "92350", "138525", "0", "1945", "1945"),
    ////////////////////////////// NO WAGES AND SELF SE INCOME AND SPOUSE SE INCOME///////////////////////////////////////////
    (mfj, "10000", "15000", "0", "0", "9235", "13852.50", "0", "0", "0"),
    (mfj, "100000", "15000", "0", "0", "92350", "13852.50", "0", "0", "0"),
    (mfj, "100000", "65000", "0", "0", "92350", "60027.50", "0", "0", "0"),
    (mfj, "100000", "76100", "0", "0", "92350", "70278.35", "0", "0", "0"),
    (mfj, "100000", "80000", "0", "0", "92350", "73880", "0", "0", "0"),
    (mfj, "100000", "150000", "0", "0", "92350", "138525", "0", "0", "0"),
  )

  test("test self employment tax and deduction scenarios") {
    forAll(dataTable) {
      (
          status,
          netSelfEmploymentIncomeSelf,
          netSelfEmploymentIncomeSpouse,
          jobsIncomeSelf,
          jobsIncomeSpouse,
          expectedScheduleSELine6Self,
          expectedScheduleSELine6Spouse,
          expectedAdditionalMedicareTaxOnMedicareWagesForm5969Line7,
          expectedAdditionalMedicareTaxOnSelfEmploymentIncomeForm8959Line13,
          expectedAdditionalMedicareTax,
      ) =>
        val graph = makeGraphWith(
          factDictionary,
          Path("/filingStatus") -> status,
          Path("/netSelfEmploymentIncomeSelf") -> Dollar(netSelfEmploymentIncomeSelf),
          Path("/netSelfEmploymentIncomeSpouse") -> Dollar(netSelfEmploymentIncomeSpouse),
          Path("/jobsIncomeSelf") -> Dollar(jobsIncomeSelf),
          Path("/jobsIncomeSpouse") -> Dollar(jobsIncomeSpouse),
        )
        val actualScheduleSELine6Self = graph.get("/scheduleSELine6Self")
        val actualScheduleSELine6Spouse = graph.get("/scheduleSELine6Spouse")
        val actualAdditionalMedicareTaxOnMedicareWagesForm5969Line7 =
          graph.get("/additionalMedicareTaxOnMedicareWagesForm5969Line7")
        val actualAdditionalMedicareTaxOnSelfEmploymentIncomeForm8959Line13 =
          graph.get("/additionalMedicareTaxOnSelfEmploymentIncomeForm8959Line13")
        val actualAdditionalMedicareTax = graph.get("/additionalMedicareTax")

        assert(actualScheduleSELine6Self.value.contains(Dollar(expectedScheduleSELine6Self)))
        assert(actualScheduleSELine6Spouse.value.contains(Dollar(expectedScheduleSELine6Spouse)))
        assert(
          actualAdditionalMedicareTaxOnMedicareWagesForm5969Line7.value.contains(
            Dollar(expectedAdditionalMedicareTaxOnMedicareWagesForm5969Line7),
          ),
        )
        assert(
          actualAdditionalMedicareTaxOnSelfEmploymentIncomeForm8959Line13.value.contains(
            Dollar(expectedAdditionalMedicareTaxOnSelfEmploymentIncomeForm8959Line13),
          ),
        )
        assert(actualAdditionalMedicareTax.value.contains(Dollar(expectedAdditionalMedicareTax)))

    }
  }
}

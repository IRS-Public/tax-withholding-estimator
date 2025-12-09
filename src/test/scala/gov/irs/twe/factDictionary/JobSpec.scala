package gov.irs.twe.factDictionary

import gov.irs.factgraph.types.Collection
import gov.irs.factgraph.types.Dollar
import gov.irs.factgraph.types.Enum
import gov.irs.factgraph.FactDictionaryForTests
import gov.irs.factgraph.Path
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.prop.TableDrivenPropertyChecks

class JobSpec extends AnyFunSuite with TableDrivenPropertyChecks {
  val factDictionary = setupFactDictionary()

  val jobs = Path("/jobs")
  val jobId = "11111111-1111-447e-a94d-0944cc8e3b6b"
  val jobsCollection = Collection(Vector(java.util.UUID.fromString(jobId)))

  val inconsistentPay = Path(s"/jobs/#${jobId}/inconsistentPay")
  val paycheck1 = Path(s"/jobs/#${jobId}/pastPaycheckIncome1")
  val paycheck2 = Path(s"/jobs/#${jobId}/pastPaycheckIncome2")
  val paycheck3 = Path(s"/jobs/#${jobId}/pastPaycheckIncome3")
  val amountWithheldLastPaycheckPath = Path(s"/jobs/#${jobId}/amountWithheldLastPaycheck")

  val inconsistentPayAveragePath = Path(s"/jobs/#${jobId}/inconsistentPayAverage")
  val averagePayPerPayPeriodPath = Path(s"/jobs/#${jobId}/averagePayPerPayPeriod")
  val averageWithholdingPerPayPeriodPath = Path(s"/jobs/#${jobId}/averageWithholdingPerPayPeriod")

  test("does not calculate inconsistent income with only 1 paycheck") {
    val graph = makeGraphWith(
      factDictionary,
      jobs -> jobsCollection,
      inconsistentPay -> true,
      paycheck1 -> Dollar("100"),
    )

    val averagePay = graph.get(averagePayPerPayPeriodPath).value
    assert(averagePay.isEmpty)
  }

  test("calculates average income for 2 inconsistent paychecks") {
    val graph = makeGraphWith(
      factDictionary,
      jobs -> jobsCollection,
      inconsistentPay -> true,
      paycheck1 -> Dollar("100"),
      paycheck2 -> Dollar("200"),
    )

    val averagePay = graph.get(averagePayPerPayPeriodPath).value
    assert(averagePay.contains(Dollar(150)))
  }

  test("calculates average income for 3 inconsistent paychecks") {
    val graph = makeGraphWith(
      factDictionary,
      jobs -> jobsCollection,
      inconsistentPay -> true,
      paycheck1 -> Dollar("100"),
      paycheck2 -> Dollar("200"),
      paycheck3 -> Dollar("400"),
    )

    val averagePay = graph.get(averagePayPerPayPeriodPath).value
    assert(averagePay.contains(Dollar(233.33)))
  }

  test("applies percentage withheld from each paycheck across the average for 2 inconsistent paychecks") {
    val graph = makeGraphWith(
      factDictionary,
      jobs -> jobsCollection,
      inconsistentPay -> true,
      paycheck1 -> Dollar("100"),
      paycheck2 -> Dollar("200"),
      amountWithheldLastPaycheckPath -> Dollar("10"),
    )

    val averageWithholdingPerPayPeriod = graph.get(averageWithholdingPerPayPeriodPath).value
    assert(averageWithholdingPerPayPeriod.contains(Dollar(15)))
  }

  test("applies percentage withheld from each paycheck across the average for 3 inconsistent paychecks") {
    val graph = makeGraphWith(
      factDictionary,
      jobs -> jobsCollection,
      inconsistentPay -> true,
      paycheck1 -> Dollar("100"),
      paycheck2 -> Dollar("200"),
      paycheck2 -> Dollar("400"),
      amountWithheldLastPaycheckPath -> Dollar("10"),
    )

    val averageWithholdingPerPayPeriod = graph.get(averageWithholdingPerPayPeriodPath).value
    assert(averageWithholdingPerPayPeriod.contains(Dollar(25)))
  }
}

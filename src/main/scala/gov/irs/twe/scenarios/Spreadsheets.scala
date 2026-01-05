package gov.irs.twe.scenarios

import com.github.tototoshi.csv.*
import gov.irs.factgraph.{ types, FactDefinition, Graph }
import gov.irs.factgraph.compnodes.{ EnumNode, MultiEnumNode }
import gov.irs.factgraph.types.{ Day, Dollar }
import gov.irs.twe.loadTweFactDictionary
import java.nio.file.Paths

val INPUT_NAME_COL = 0
val INPUT_VALUE_COL = 1 // This will end up being dynamic for multi-scenario sheets

val PREVIOUS_SELF_JOB_ID = "9A21FD95-1CE1-4AEE-957C-109443A646BC"
val PREVIOUS_SPOUSE_JOB_ID = "78CCB2A9-6A0C-4918-B88C-8A1A87CE1FC8"
val JOB_1_ID = "A3006AF1-A040-4235-9D31-68C5830C55FD"
val JOB_2_ID = "8955625F-6317-451B-BCE9-48893D60E766"
val JOB_3_ID = "20B48125-6DB6-4719-8FD3-96C9DAA17E57" // Spouse job 1
val JOB_4_ID = "9141223F-AF3D-42EF-8AA7-3EC454D5CCBC" // Spouse job 2
val ALL_JOBS = List(PREVIOUS_SELF_JOB_ID, PREVIOUS_SPOUSE_JOB_ID, JOB_1_ID, JOB_2_ID, JOB_3_ID, JOB_4_ID)

@main def convertSpreadsheet(file: String): Unit = {
  val path = Paths.get(file)
  val reader = CSVReader.open(path.toFile)
  val rows = reader.all()

  val spreadsheetData: Map[String, String] = rows.foldLeft(Map()) { (dict, row) =>
    val inputName = row(INPUT_NAME_COL)
    val inputValue = row(INPUT_VALUE_COL)
    dict + (inputName -> inputValue)
  }
  reader.close()

  // Get each row of the scenario and map it to a specific fact
  val spreadsheetFacts = SHEET_ROW_FACT_MAPPINGS.map((sheetKey, factName) => factName -> spreadsheetData(sheetKey))

  // Create the fact graph
  val tweFactDictionary = loadTweFactDictionary()
  val factGraph = Graph(tweFactDictionary.factDictionary)

  // Add the 5 jobs to the fact graph
  ALL_JOBS.foreach(job => factGraph.addToCollection("/jobs", job))
  factGraph.set(s"/jobs/#$PREVIOUS_SELF_JOB_ID/filerAssignment", new types.Enum(Some("self"), "/filerAssignmentOption"))
  factGraph.set(
    s"/jobs/#$PREVIOUS_SPOUSE_JOB_ID/filerAssignment",
    new types.Enum(Some("spouse"), "/filerAssignmentOption"),
  )
  factGraph.set(s"/jobs/#$JOB_1_ID/filerAssignment", new types.Enum(Some("self"), "/filerAssignmentOption"))
  factGraph.set(s"/jobs/#$JOB_2_ID/filerAssignment", new types.Enum(Some("self"), "/filerAssignmentOption"))
  factGraph.set(s"/jobs/#$JOB_3_ID/filerAssignment", new types.Enum(Some("spouse"), "/filerAssignmentOption"))
  factGraph.set(s"/jobs/#$JOB_4_ID/filerAssignment", new types.Enum(Some("spouse"), "/filerAssignmentOption"))

  // Set dummy dates for the past jobs
  // This is sort of a hack; we should just ask for spreadsheets that include "previous jobs" as regular jobs
  List(PREVIOUS_SELF_JOB_ID, PREVIOUS_SPOUSE_JOB_ID).foreach(jobId => {
    factGraph.set(s"/jobs/#$jobId/writableStartDate", Day("2026-01-01"))
    factGraph.set(s"/jobs/#$jobId/writableEndDate", Day("2026-01-15"))
    factGraph.set(s"/jobs/#$jobId/payFrequency", types.Enum(Some("weekly"), "/payFrequencyOptions"))
    factGraph.set(s"/jobs/#$jobId/mostRecentPayPeriodEnd", Day("2026-01-15"))
    factGraph.set(s"/jobs/#$jobId/mostRecentPayDate", Day("2026-01-15"))
    factGraph.set(s"/jobs/#$jobId/amountLastPaycheck", Dollar("0"))
    factGraph.set(s"/jobs/#$jobId/amountWithheldLastPaycheck", Dollar("0"))
  })

  // These are facts that we need to set that aren't in the spreadsheets
  factGraph.set("/secondaryFilerIsClaimedOnAnotherReturn", false)

  // For each fact, convert the value to its fact type and set it in the fact graph
  spreadsheetFacts.foreach { (factPath, value) =>
    val definition = tweFactDictionary.factDictionary.getDefinition(factPath)

    val convertedValue = definition.typeNode match {
      case "BooleanNode" => convertBoolean(value)
      case "DayNode"     => convertDate(value)
      case "EnumNode"    => convertEnum(value, definition)
      case "DollarNode"  => Dollar(value.replace("$", ""))
      case "IntNode"     => value.toInt
      case _             => value
    }

    factGraph.set(factPath, convertedValue)
  }

  // Manually setting social security until it's implemented
  factGraph.set("/socialSecurityBenefitsIncome", Dollar(0))
  factGraph.set("/totalTaxesPaidOnSocialSecurityIncome", Dollar(0))

  // Calculated facts
  factGraph.set("/primaryFilerAge25OrOlderForEitc", spreadsheetData("User Age").toInt >= 25)
  val spouseAge = spreadsheetData("Spouse Age").toInt
  if (spouseAge > 0) { factGraph.set("/secondaryFilerAge25OrOlderForEitc", spouseAge >= 25) }

  // Remove jobs that have no income
  ALL_JOBS.foreach(jobId => {
    val income = factGraph.get(s"/jobs/#$jobId/income")
    if (!income.hasValue || income.get == 0) {
      factGraph.delete(s"/jobs/#$jobId")
    }
  })

  factGraph.save()
  println(factGraph.persister.toJson(2))
}

def convertBoolean(raw: String): Boolean = {
  raw match {
    case "0" => false
    case "1" => true
    case _   => throw Exception(s"Unexpected value $raw for boolean")
  }
}

def convertDate(raw: String): Day = {
  val split = raw.split("/")
  val year = split(2).toInt
  val month = split(0).toInt
  val day = split(1).toInt
  Day(f"$year-$month%02d-$day%02d")
}

def convertEnum(value: String, factDefinition: FactDefinition): types.Enum = {
  val factPath = factDefinition.path.toString
  val optionsEnumPath = factDefinition.value match
    case value: EnumNode      => value.enumOptionsPath
    case value: MultiEnumNode => value.enumOptionsPath.toString
    case _                    => throw Exception(s"Fact $factPath is not an enum")

  optionsEnumPath match {
    case "/filingStatusOptions" =>
      value match {
        case "1" => new types.Enum(Some("single"), "/filingStatusOptions")
        case "2" => new types.Enum(Some("marriedFilingJointly"), "/filingStatusOptions")
        case "3" => new types.Enum(Some("marriedFilingSeparately"), "/filingStatusOptions")
        case "4" => new types.Enum(Some("headOfHousehold"), "/filingStatusOptions")
        case "5" => new types.Enum(Some("qualifiedSurvivingSpouse"), "/filingStatusOptions")
        case _   => throw Exception(s"$value is not a known enum for /filingStatusOptions")
      }
    case "/payFrequencyOptions" =>
      value match {
        case "1" => new types.Enum(Some("weekly"), "/payFrequencyOptions")
        case "2" => new types.Enum(Some("biWeekly"), "/payFrequencyOptions")
        case "3" => new types.Enum(Some("semiMonthly"), "/payFrequencyOptions")
        case "4" => new types.Enum(Some("monthly"), "/payFrequencyOptions")
        case _   => throw Exception(s"$value is not a known enum for /payFrequencyOptions")
      }
    case _ => throw Exception(s"Unknown enum path: $factPath")
  }
}

val SHEET_ROW_FACT_MAPPINGS = Map(
  "DateRun" -> "/overrideDate",
  "F-Status (1=S; 2=MJ; 3=MS; 4=HH; 5=QW)" -> "/filingStatus",
  "65 or Older (1=yes)" -> "/primaryFilerAge65OrOlder",
  "Spouse 65 or Older (1=yes)" -> "/secondaryFilerAge65OrOlder",
  "Blind (1=yes)" -> "/primaryFilerIsBlind",
  "Spouse Blind (1=yes)" -> "/secondaryFilerIsBlind",
  "Valid SSN (self)" -> "/primaryFilerHasSSN",
  "Valid SSN (spouse)" -> "/secondaryFilerHasSSN",
  "Claimed as a Dependent" -> "/primaryFilerIsClaimedOnAnotherReturn",
  "Plan to claim dependents?" -> "/primaryFilerIsClaimingDependents",
  // Previous jobs
  "Previous job income-User" -> s"/jobs/#$PREVIOUS_SELF_JOB_ID/yearToDateIncome",
  "Withholding from previous job-User" -> s"/jobs/#$PREVIOUS_SELF_JOB_ID/yearToDateWithholding",
  // Job 1
  "Job start1" -> s"/jobs/#$JOB_1_ID/writableStartDate",
  "Job end1" -> s"/jobs/#$JOB_1_ID/writableEndDate",
  "payFrequency1 (1=W; 2=BW; 3=SM; 4=M)" -> s"/jobs/#$JOB_1_ID/payFrequency",
  "recentPPd End1" -> s"/jobs/#$JOB_1_ID/mostRecentPayPeriodEnd",
  "recentPayDate1" -> s"/jobs/#$JOB_1_ID/mostRecentPayDate",
  "paymentPerPPd1" -> s"/jobs/#$JOB_1_ID/amountLastPaycheck",
  "paymentYTD1" -> s"/jobs/#$JOB_1_ID/yearToDateIncome",
  "taxWhPerPPd1" -> s"/jobs/#$JOB_1_ID/amountWithheldLastPaycheck",
  "taxWhYTD1" -> s"/jobs/#$JOB_1_ID/yearToDateWithholding",
  // Job 2
  "Job start2" -> s"/jobs/#$JOB_2_ID/writableStartDate",
  "Job end2" -> s"/jobs/#$JOB_2_ID/writableEndDate",
  "payFrequency2 (1=W; 2=BW; 3=SM; 4=M)" -> s"/jobs/#$JOB_2_ID/payFrequency",
  "recentPPd End2" -> s"/jobs/#$JOB_2_ID/mostRecentPayPeriodEnd",
  "recentPayDate2" -> s"/jobs/#$JOB_2_ID/mostRecentPayDate",
  "paymentPerPPd2" -> s"/jobs/#$JOB_2_ID/amountLastPaycheck",
  "paymentYTD2" -> s"/jobs/#$JOB_2_ID/yearToDateIncome",
  "taxWhPerPPd2" -> s"/jobs/#$JOB_2_ID/amountWithheldLastPaycheck",
  "taxWhYTD2" -> s"/jobs/#$JOB_2_ID/yearToDateWithholding",
  // Job 3
  "Job start3" -> s"/jobs/#$JOB_3_ID/writableStartDate",
  "Job end3" -> s"/jobs/#$JOB_3_ID/writableEndDate",
  "payFrequency3 (1=W; 2=BW; 3=SM; 4=M)" -> s"/jobs/#$JOB_3_ID/payFrequency",
  "recentPPd End3" -> s"/jobs/#$JOB_3_ID/mostRecentPayPeriodEnd",
  "recentPayDate3" -> s"/jobs/#$JOB_3_ID/mostRecentPayDate",
  "paymentPerPPd3" -> s"/jobs/#$JOB_3_ID/amountLastPaycheck",
  "paymentYTD3" -> s"/jobs/#$JOB_3_ID/yearToDateIncome",
  "taxWhPerPPd3" -> s"/jobs/#$JOB_3_ID/amountWithheldLastPaycheck",
  "taxWhYTD3" -> s"/jobs/#$JOB_3_ID/yearToDateWithholding",
  // Job 4
  "Job start4" -> s"/jobs/#$JOB_4_ID/writableStartDate",
  "Job end4" -> s"/jobs/#$JOB_4_ID/writableEndDate",
  "payFrequency4 (1=W; 2=BW; 3=SM; 4=M)" -> s"/jobs/#$JOB_4_ID/payFrequency",
  "recentPPd End4" -> s"/jobs/#$JOB_4_ID/mostRecentPayPeriodEnd",
  "recentPayDate4" -> s"/jobs/#$JOB_4_ID/mostRecentPayDate",
  "paymentPerPPd4" -> s"/jobs/#$JOB_4_ID/amountLastPaycheck",
  "paymentYTD4" -> s"/jobs/#$JOB_4_ID/yearToDateIncome",
  "taxWhPerPPd4" -> s"/jobs/#$JOB_4_ID/amountWithheldLastPaycheck",
  "taxWhYTD4" -> s"/jobs/#$JOB_4_ID/yearToDateWithholding",
  // Supported Credits and Deductions
  "Car loan interest" -> "/personalVehicleLoanInterestAmount",
  "qualChildrenCDCC" -> "/ctcEligibleDependents",
  "How many QC for EITC" -> "/eitcQualifyingChildren",
)

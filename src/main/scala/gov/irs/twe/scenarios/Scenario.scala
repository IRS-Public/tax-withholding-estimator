package gov.irs.twe.scenarios

import com.github.tototoshi.csv.*
import gov.irs.factgraph.{ types, FactDefinition, Graph }
import gov.irs.factgraph.compnodes.{ EnumNode, MultiEnumNode }
import gov.irs.factgraph.types.{ Day, Dollar }
import gov.irs.twe.loadTweFactDictionary

val INPUT_NAME_COL = 0

val PREVIOUS_SELF_JOB_ID = "9A21FD95-1CE1-4AEE-957C-109443A646BC"
val PREVIOUS_SPOUSE_JOB_ID = "78CCB2A9-6A0C-4918-B88C-8A1A87CE1FC8"
val JOB_1_ID = "A3006AF1-A040-4235-9D31-68C5830C55FD"
val JOB_2_ID = "8955625F-6317-451B-BCE9-48893D60E766"
val JOB_3_ID = "20B48125-6DB6-4719-8FD3-96C9DAA17E57" // Spouse job 1
val JOB_4_ID = "9141223F-AF3D-42EF-8AA7-3EC454D5CCBC" // Spouse job 2
val ALL_JOBS = List(PREVIOUS_SELF_JOB_ID, PREVIOUS_SPOUSE_JOB_ID, JOB_1_ID, JOB_2_ID, JOB_3_ID, JOB_4_ID)

val SS_ID = "9f5e25b9-5f6c-4c93-b327-27b1c21a4ff3"
val SS_SPOUSE_ID = "2e4b8107-f72b-4ea0-8081-8012d256373f"
val ALL_SS_SOURCES = List(SS_ID, SS_SPOUSE_ID)

@main def convertSpreadsheet(file: String): Unit = {
  val path = os.Path(file)
  val scenario = loadScenarioByColumnLetter(path, "B")
  println(scenario.graphToJson())
}

case class Scenario(csv: Map[String, String], graph: Graph) {
  def getFact(path: String): Any = {
    this.graph.get(path).get
  }

  def getInput(rowName: String): String = {
    this.csv(rowName).replace("$", "").strip()
  }

  def graphToJson(): String = {
    this.graph.persister.toJson(2)
  }
}

def loadScenarioByColumnLetter(path: os.ReadablePath, columnLetter: String): Scenario = {
  val reader = CSVReader.open(path.toString)
  val rows = reader.all()
  reader.close()

  // Convert column letter into a row index
  var column = 0
  val length = columnLetter.length
  for (i <- 0.until(length))
    column += ((columnLetter(i).toInt - 64) * Math.pow(26, length - i - 1)).toInt

  parseScenario(rows, column - 1)
}

def loadScenarioByName(path: os.ReadablePath, scenarioName: String): Scenario = {
  val reader = CSVReader.open(path.toString)
  val rows = reader.all()
  reader.close()

  val namesRow = rows(1)
  val col = namesRow.indexOf(scenarioName)
  parseScenario(rows, col)
}

private def parseScenario(rows: List[List[String]], scenarioColumn: Int): Scenario = {
  // We don't support SS benefit YTD or SS withholding YTD
  val socialSecurityOverrideFields =
    List("Start date", "End date", "SS monthly benefit", "SS monthly withholding")

  val csv: Map[String, String] = rows.foldLeft(Map()) { (dict, row) =>
    var inputName = row(INPUT_NAME_COL)
    // work around for handling Social security labels, since they aren't unique
    if (socialSecurityOverrideFields.contains(inputName) && dict.contains(inputName)) {
      inputName = inputName + "2"
    }
    val inputValue = row(scenarioColumn)
    dict + (inputName -> inputValue)
  }

  // Get each row of the scenario and map it to a specific fact
  var spreadsheetFacts = SHEET_ROW_FACT_MAPPINGS.map((sheetKey, factName) => factName -> csv(sheetKey))

  // convert SS monthly withholding to a percent and handle as an enum
  val monthlyBenefit1 = csv("SS monthly benefit").replace("$", "").replace(",", "").toDouble
  val monthlyWithholding1 = csv("SS monthly withholding").replace("$", "").replace(",", "").toDouble
  val withholdingEnum1 =
    if (monthlyWithholding1 == 0.0) "zero"
    else
      monthlyWithholding1 / monthlyBenefit1 match {
        case 0.07 => "seven"
        case 0.1  => "ten"
        case 0.12 => "twelve"
        case 0.22 => "twentyTwo"
        case _    => throw Exception("Invalid ratio for self social security tax withholdings")
      }
  spreadsheetFacts = spreadsheetFacts + (s"/socialSecuritySources/#$SS_ID/withheldRate" -> withholdingEnum1)

  val monthlyBenefit2 = csv("SS monthly benefit2").replace("$", "").replace(",", "").toDouble
  val monthlyWithholding2 = csv("SS monthly withholding2").replace("$", "").replace(",", "").toDouble
  val withholdingEnum2 =
    if (monthlyWithholding2 == 0.0) "zero"
    else
      monthlyWithholding2 / monthlyBenefit2 match {
        case 0.07 => "seven"
        case 0.1  => "ten"
        case 0.12 => "twelve"
        case 0.22 => "twentyTwo"
        case _    => throw Exception("Invalid ratio for spouse social security tax withholdings")
      }
  spreadsheetFacts = spreadsheetFacts + (s"/socialSecuritySources/#$SS_SPOUSE_ID/withheldRate" -> withholdingEnum2)

  // Create the fact graph
  val tweFactDictionary = loadTweFactDictionary()
  val factGraph = Graph(tweFactDictionary.factDictionary)

  // Add the 5 jobs to the fact graph
  ALL_JOBS.foreach(job => factGraph.addToCollection("/jobs", job))

  // Set social security collection
  ALL_SS_SOURCES.foreach(source => factGraph.addToCollection("/socialSecuritySources", source))

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
      case "DollarNode"  => Dollar(value.replace("$", "").strip())
      case "IntNode"     => value.toInt
      case _             => value
    }

    factGraph.set(factPath, convertedValue)
  }

  // Stopgap to add senior deduction, we should update the FG to automate this
  if (factGraph.get("/primaryFilerAge65OrOlder").value.get == true) {
    factGraph.set("/primaryTaxpayerElectsForSeniorDeduction", true)
  }
  if (factGraph.get("/secondaryFilerAge65OrOlder").value.get == true) {
    factGraph.set("/secondaryTaxpayerElectsForSeniorDeduction", true)
  }

  // Calculated facts
  factGraph.set("/primaryFilerAge25OrOlderForEitc", csv("User Age").toInt >= 25)
  val spouseAge = csv("Spouse Age").toInt
  if (spouseAge > 0) {
    factGraph.set("/secondaryFilerAge25OrOlderForEitc", spouseAge >= 25)
  }

  // Remove jobs that have no income
  ALL_JOBS.foreach(jobId => {
    val income = factGraph.get(s"/jobs/#$jobId/income")
    if (!income.hasValue || income.get == 0) {
      factGraph.delete(s"/jobs/#$jobId")
    }
  })

  factGraph.save()
  Scenario(csv, factGraph)
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
    case "/socialSecurityWithheldTaxesOptions" =>
      value match {
        case "zero"      => new types.Enum(Some("zero"), "/socialSecurityWithheldTaxesOptions")
        case "seven"     => new types.Enum(Some("seven"), "/socialSecurityWithheldTaxesOptions")
        case "ten"       => new types.Enum(Some("ten"), "/socialSecurityWithheldTaxesOptions")
        case "twelve"    => new types.Enum(Some("twelve"), "/socialSecurityWithheldTaxesOptions")
        case "twentyTwo" => new types.Enum(Some("twentyTwo"), "/socialSecurityWithheldTaxesOptions")
        case _           => throw Exception(s"$value is not a known enum for /socialSecurityWithheldTaxesOptions")
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
  "Claimed as a Dependent" -> "/primaryFilerIsClaimedOnAnotherReturn",
  "Plan to claim dependents?" -> "/primaryFilerIsClaimingDependents",
  // Previous jobs
  "Previous job income-User" -> s"/jobs/#$PREVIOUS_SELF_JOB_ID/yearToDateIncome",
  "Withholding from previous job-User" -> s"/jobs/#$PREVIOUS_SELF_JOB_ID/yearToDateWithholding",
  "Previous job income-Spouse" -> s"/jobs/#$PREVIOUS_SPOUSE_JOB_ID/yearToDateIncome",
  "Withholding from previous job-Spouse" -> s"/jobs/#$PREVIOUS_SPOUSE_JOB_ID/yearToDateWithholding",
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
  // Self employment income
  "selfEmploymentAmount-User" -> s"/grossSelfEmploymentIncomeSelf",
  "selfEmploymentAmount-Spouse" -> s"/grossSelfEmploymentIncomeSpouse",
  // Social Security #1
  "Start date" -> s"/socialSecuritySources/#$SS_ID/startDate",
  "End date" -> s"/socialSecuritySources/#$SS_ID/endDate",
  "SS monthly benefit" -> s"/socialSecuritySources/#$SS_ID/monthlyIncome",
  "SS monthly withholding" -> s"/socialSecuritySources/#$SS_ID/withheldRate",
  // Social Security #2
  "Start date2" -> s"/socialSecuritySources/#$SS_SPOUSE_ID/startDate",
  "End date2" -> s"/socialSecuritySources/#$SS_SPOUSE_ID/endDate",
  "SS monthly benefit2" -> s"/socialSecuritySources/#$SS_SPOUSE_ID/monthlyIncome",
  "SS monthly withholding2" -> s"/socialSecuritySources/#$SS_SPOUSE_ID/withheldRate",
  // Supported Credits and Deductions
  "studentLoanInterest" -> "/studentLoanInterestAmount",
  "Car loan interest" -> "/personalVehicleLoanInterestAmount",
  "qualChildrenCDCC" -> "/ctcEligibleDependents",
  "How many QC for EITC" -> "/eitcQualifyingChildren",
  // Itemized Deductions
  "Interest you Paid" -> "/qualifiedMortgageInterestAndInvestmentInterestExpenses",
  "SALT you paid" -> "/stateAndLocalTaxPayments",
  "MedicalExpenses" -> "/medicalAndDentalExpenses",
  "Gifts to Charity" -> "/charitableContributions",
  "Casualty Lossess" -> "/casualtyLossesTotal",
  "Other Itemized Deductions" -> "/otherDeductionsTotal",
)

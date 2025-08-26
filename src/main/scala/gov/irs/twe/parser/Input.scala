package gov.irs.twe.parser

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.exceptions.InvalidFormConfig
import gov.irs.twe.Log

case class HtmlOption(name: String, value: String)

enum Input {
  case select(options: List[HtmlOption], optionsPath: Option[String])
  case text
  case int
  case boolean(question: String)
  case dollar
  case date(question: String)

  def typeString: String = this match {
    case Input.text         => "text"
    case Input.int          => "int"
    case Input.boolean(_)   => "boolean"
    case Input.dollar       => "dollar"
    case Input.select(_, _) => "select"
    case Input.date(_)      => "date"
  }

  def html(path: String): xml.Elem =
    this match {
      case Input.boolean(question) =>
        <fieldset class="usa-fieldset">
          <legend class="usa-legend">{question}</legend>
          <div class="usa-radio">
            <input id={s"${path}-yes"} class="usa-radio__input usa-radio__input--tile" type="radio" value="true" name={
          path
        } required="true" aria-invalid="false"/>
            <label for={s"${path}-yes"} class="usa-radio__label">Yes</label>
          </div>
          <div class="usa-radio">
            <input id={s"${path}-no"} class="usa-radio__input usa-radio__input--tile" type="radio" value="false" name={
          path
        } required="true" aria-invalid="false"/>
            <label for={s"${path}-no"} class="usa-radio__label">No</label>
          </div>
        </fieldset>
      case Input.select(options, optionsPath) =>
        <select id={path} class="usa-select" optionsPath={
          optionsPath.getOrElse("")
        } required="true" aria-invalid="false">
        <option value={""} disabled="true" selected="true">
          {"-- Select one --"}
        </option>{
          options.map(option => <option value={option.value}>
          {option.name}
        </option>)
        }
      </select>
      case Input.dollar =>
        <div class="usa-input-group">
          <div class="usa-input-prefix" aria-hidden="true">
            <svg aria-hidden="true" role="img" focusable="false" class="usa-icon">
              <use href="/resources/uswds-3.13.0/img/sprite.svg#attach_money"></use>
            </svg>
          </div>
          <input class="usa-input" id={path} type="text" inputmode="numeric" name={
          path
        } autocomplete="off" required="true"/>
        </div>
      case Input.date(question) =>
        <fieldset class="usa-fieldset">
          <legend class="usa-legend">{question}</legend>
          <div class="usa-memorable-date">
            <div class="usa-form-group usa-form-group--month usa-form-group--select">
              <label class="usa-label" for={s"${path}-month"}>Month</label>
              <select
                class="usa-select"
                id={s"${path}-month"}
                name={s"${path}-month"}
                aria-invalid="false"
                required="true"
                >
                <option value="">- Select -</option>
                <option value="01">January</option>
                <option value="02">February</option>
                <option value="03">March</option>
                <option value="04">April</option>
                <option value="05">May</option>
                <option value="06">June</option>
                <option value="07">July</option>
                <option value="08">August</option>
                <option value="09">September</option>
                <option value="10">October</option>
                <option value="11">November</option>
                <option value="12">December</option>
              </select>
            </div>
            <div class="usa-form-group usa-form-group--day">
              <label class="usa-label" for={s"${path}-day"}>Day</label>
              <input
                class="usa-input"
                id={s"${path}-day"}
                name={s"${path}-day"}
                maxlength="2"
                pattern="[0-9]*"
                inputmode="numeric"
                value=""
                required="true"
                aria-invalid="false"
              />
            </div>
            <div class="usa-form-group usa-form-group--year">
              <label class="usa-label" for={s"${path}-year"}>Year</label>
              <input
                class="usa-input"
                id={s"${path}-year"}
                name={s"${path}-year"}
                minlength="4"
                maxlength="4"
                pattern="[0-9]*"
                inputmode="numeric"
                value=""
                required="true"
                aria-invalid="false"
              />
            </div>
          </div>
        </fieldset>
      case Input.text =>
        <input id={path} class="usa-input" type="text" name={
          path
        } autocomplete="off" required="true" aria-invalid="false"/>
      case Input.int =>
        <input id={path} class="usa-input" type="text" name={
          path
        } autocomplete="off" required="true" aria-invalid="false"/>
    }
}

object Input {
  def extractFromFgSet(node: xml.Node, factDictionary: FactDictionary): Input = {
    val path = node \@ "path"
    val question = node \ "question"
    // Handle the <select> as a special case
    val selectNode = node \ "select"
    if (selectNode.nonEmpty) {
      val optionsPath = Option(selectNode \@ "options-path").filter(_.nonEmpty)
      val options = (selectNode \ "option").map { node =>
        val name = node.text
        var value = node \@ "value"
        if (value == "") value = name

        HtmlOption(name, value)
      }.toList
      // TODO validate that the options match the num path

      if (options.isEmpty) {
        Log.warn(s"Empty options for fg-set: $path")
      }
      return Input.select(options, optionsPath)
    }

    // Otherwise parse the <input>
    val inputNode = node \ "input"
    if (inputNode.isEmpty) {
      throw InvalidFormConfig(s"Missing an input for question $path")
    }

    inputNode \@ "type" match {
      case "text"    => Input.text
      case "int"     => Input.int
      case "boolean" => Input.boolean(question.text.trim)
      case "dollar"  => Input.dollar
      case "date"    => Input.date(question.text.trim)
      case x         => throw InvalidFormConfig(s"Unexpected input type \"$x\" for question $path")
    }
  }
}

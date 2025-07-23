package gov.irs.twe.parser

object Utils {
  def optionString(string: String): Option[String] = {
    if (string.isEmpty)  None else Option(string)
  }
}

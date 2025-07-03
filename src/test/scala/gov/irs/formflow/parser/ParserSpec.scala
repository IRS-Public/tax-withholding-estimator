package gov.irs.formflow.parser

type Content = xml.Elem

class ParserSpec {
  def Generate(flow: Flow): Content = {
    flow.sections
  }
}

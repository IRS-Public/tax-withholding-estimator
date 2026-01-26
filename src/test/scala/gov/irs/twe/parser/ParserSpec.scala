package gov.irs.twe.parser

import gov.irs.twe.exceptions.InvalidFormConfig
import gov.irs.twe.loadTweFactDictionary
import gov.irs.twe.parser.Flow
import org.scalatest.funspec.AnyFunSpec
import scala.io.Source

class ParserSpec extends AnyFunSpec {

  describe("flow parsing") {
    it("should parse the flow XML files and validate all fact names") {
      val FlowResourceRoot = "twe/flow"

      // Load the flow index.xml file (same as main.scala does)
      val flowFile = Source.fromResource(s"$FlowResourceRoot/index.xml").getLines().mkString("\n")
      val flowConfig = xml.XML.loadString(flowFile)
      val children = flowConfig \\ "FlowConfig" \ "_"

      // Resolve modules (same logic as main.scala)
      def resolveModule(node: xml.Node): xml.NodeSeq = {
        val src = node \@ "src"
        val resolvedSrc = src.replaceAll("^\\./", "")
        val moduleFile = Source.fromResource(s"$FlowResourceRoot/$resolvedSrc").getLines().mkString("\n")

        val flowConfigModule = xml.XML.loadString(moduleFile)
        if (flowConfigModule.label != "FlowConfig") {
          throw InvalidFormConfig(s"Module file $src does not have a top-level FlowConfig")
        }

        flowConfigModule \ "_"
      }

      val resolvedChildren = children.map(child =>
        child.label match {
          case "module" => resolveModule(child)
          case _        => child
        },
      )

      val resolvedConfig = <FlowConfig>{resolvedChildren}</FlowConfig>

      // Load the fact dictionary
      val tweFactDictionary = loadTweFactDictionary()

      // Parse the flow - this will throw InvalidFormConfig if any fact names are invalid
      val flow = Flow.fromXmlConfig(resolvedConfig, tweFactDictionary.factDictionary)

      // If we get here, the flow parsed successfully with all valid fact names
      assert(flow.pages.nonEmpty, "Flow should contain at least one page")
    }
  }

}

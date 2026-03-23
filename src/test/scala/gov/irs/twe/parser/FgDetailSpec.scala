package gov.irs.twe.parser

import gov.irs.twe.exceptions.InvalidFormConfig
import gov.irs.twe.loadTweFactDictionary
import org.scalatest.funspec.AnyFunSpec
import scala.xml.Elem

class FgDetailSpec extends AnyFunSpec {
  private val factDictionary = loadTweFactDictionary().factDictionary
  private val flowParser = FlowParser(factDictionary)

  describe("FgDetail.fromXml") {
    it("parses summary and children from XML") {
      val xml = <fg-detail>
        <summary>Estimated tips and overtime</summary>
        <p>Content here.</p>
      </fg-detail>
      val fd = FgDetail.fromXml(xml, flowParser)
      assert(fd.summary == "Estimated tips and overtime")
      assert(fd.children.size == 1)
      assert(fd.children.head.isInstanceOf[HtmlLeafNode])
    }

    it("preserves fg-show and other tags in summary as HTML") {
      val xml = <fg-detail>
        <summary>Income in <fg-show path="/taxYear"/></summary>
        <p>Body.</p>
      </fg-detail>
      val fd = FgDetail.fromXml(xml, flowParser)
      assert(fd.summary.contains("Income in <fg-show path=\"/taxYear\"/>"))
      assert(fd.children.size == 1)
      assert(fd.children.head.isInstanceOf[HtmlLeafNode])
    }

    it("parses icon=\"chevron\" as useChevron true, otherwise false") {
      val withChevron = <fg-detail icon="chevron">
        <summary>Chevron accordion</summary>
        <p>Body.</p>
      </fg-detail>
      val fd1 = FgDetail.fromXml(withChevron, flowParser)
      assert(fd1.useChevron)

      val withoutIcon = <fg-detail>
        <summary>Default accordion</summary>
        <p>Body.</p>
      </fg-detail>
      val fd2 = FgDetail.fromXml(withoutIcon, flowParser)
      assert(!fd2.useChevron)
    }

    it("parses open=\"true\" as open true, otherwise false") {
      val openTrue = FgDetail.fromXml(
        <fg-detail open="true"><summary>Open by default</summary><p>Content</p></fg-detail>,
        flowParser,
      )
      assert(openTrue.open)
      val openFalse = FgDetail.fromXml(
        <fg-detail><summary>Closed by default</summary><p>Content</p></fg-detail>,
        flowParser,
      )
      assert(!openFalse.open)
      val openExplicitFalse = FgDetail.fromXml(
        <fg-detail open="false"><summary>Closed</summary><p>Content</p></fg-detail>,
        flowParser,
      )
      assert(!openExplicitFalse.open)
    }

    it("parses heading-tag h3 when present, defaults to h4 when missing") {
      val withTag = FgDetail.fromXml(
        <fg-detail heading-tag="h3"><summary>X</summary><p>Content</p></fg-detail>,
        flowParser,
      )
      assert(withTag.headingTag == "h3")
      val default =
        FgDetail.fromXml(<fg-detail><summary>Y</summary><p>Content</p></fg-detail>, flowParser)
      assert(default.headingTag == "h4")
    }

    it("parses if-true as Condition; throws if both if-true and if-false") {
      val fd = FgDetail.fromXml(
        <fg-detail if-true="/primaryFilerIsBlind"><summary>X</summary><p>Content</p></fg-detail>,
        flowParser,
      )
      assert(fd.condition.contains(Condition("/primaryFilerIsBlind", ConditionOperator.isTrue)))
      assertThrows[InvalidFormConfig](
        FgDetail.fromXml(
          <fg-detail if-true="/primaryFilerIsBlind" if-false="/primaryFilerAge65OrOlder"><summary>Both</summary><p>Content</p></fg-detail>,
          flowParser,
        ),
      )
    }
  }
}

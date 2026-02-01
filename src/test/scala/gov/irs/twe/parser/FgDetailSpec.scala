package gov.irs.twe.parser

import gov.irs.twe.exceptions.InvalidFormConfig
import gov.irs.twe.loadTweFactDictionary
import org.scalatest.funspec.AnyFunSpec

class FgDetailSpec extends AnyFunSpec {

  val factDictionary = loadTweFactDictionary().factDictionary
  val pageRoute = "/income"

  describe("FgDetail.parse") {
    it("parses summary and children from XML") {
      val xml = <fg-detail>
        <summary>Estimated tips and overtime</summary>
        <p>Content here.</p>
      </fg-detail>
      val node = Section.processNode(xml, pageRoute, factDictionary)
      val fd = node match { case SectionNode.fgDetail(x) => x; case _ => fail("expected fgDetail") }
      assert(fd.summary == "Estimated tips and overtime")
      assert(fd.children.nonEmpty)
    }

    it("preserves fg-show and other tags in summary as HTML") {
      val xml = <fg-detail>
        <summary>Income in <fg-show path="/taxYear"/></summary>
        <p>Body.</p>
      </fg-detail>
      val node = Section.processNode(xml, pageRoute, factDictionary)
      val fd = node match { case SectionNode.fgDetail(x) => x; case _ => fail("expected fgDetail") }
      assert(fd.summary.contains("Income in "))
      assert(fd.summary.contains("fg-show"))
      assert(fd.summary.contains("/taxYear"))
    }

    it("parses icon=\"chevron\" as useChevron true, otherwise false") {
      val withChevron = <fg-detail icon="chevron">
        <summary>Chevron accordion</summary>
        <p>Body.</p>
      </fg-detail>
      val node1 = Section.processNode(withChevron, pageRoute, factDictionary)
      val fd1 = node1 match { case SectionNode.fgDetail(x) => x; case _ => fail("expected fgDetail") }
      assert(fd1.useChevron == true)

      val withoutIcon = <fg-detail>
        <summary>Default accordion</summary>
        <p>Body.</p>
      </fg-detail>
      val node2 = Section.processNode(withoutIcon, pageRoute, factDictionary)
      val fd2 = node2 match { case SectionNode.fgDetail(x) => x; case _ => fail("expected fgDetail") }
      assert(fd2.useChevron == false)
    }

    it("parses open=\"true\" as open true, otherwise false") {
      val openTrue = Section.processNode(
        <fg-detail open="true"><summary>Open by default</summary></fg-detail>,
        pageRoute,
        factDictionary,
      ) match { case SectionNode.fgDetail(x) => x; case _ => fail() }
      assert(openTrue.open == true)
      val openFalse = Section.processNode(
        <fg-detail><summary>Closed by default</summary></fg-detail>,
        pageRoute,
        factDictionary,
      ) match { case SectionNode.fgDetail(x) => x; case _ => fail() }
      assert(openFalse.open == false)
      val openExplicitFalse = Section.processNode(
        <fg-detail open="false"><summary>Closed</summary></fg-detail>,
        pageRoute,
        factDictionary,
      ) match { case SectionNode.fgDetail(x) => x; case _ => fail() }
      assert(openExplicitFalse.open == false)
    }

    it("parses heading-tag h3 when present, defaults to h4 when missing") {
      val withTag = Section.processNode(
        <fg-detail heading-tag="h3"><summary>X</summary></fg-detail>,
        pageRoute,
        factDictionary,
      ) match {
        case SectionNode.fgDetail(x) => x; case _ => fail()
      }
      assert(withTag.headingTag == "h3")
      val default =
        Section.processNode(<fg-detail><summary>Y</summary></fg-detail>, pageRoute, factDictionary) match {
          case SectionNode.fgDetail(x) => x; case _ => fail()
        }
      assert(default.headingTag == "h4")
    }

    it("parses if-true as Condition; throws if both if-true and if-false") {
      val fd = Section.processNode(
        <fg-detail if-true="/primaryFilerIsBlind"><summary>X</summary></fg-detail>,
        pageRoute,
        factDictionary,
      ) match {
        case SectionNode.fgDetail(x) => x; case _ => fail()
      }
      assert(fd.condition.contains(Condition("/primaryFilerIsBlind", ConditionOperator.isTrue)))
      assertThrows[InvalidFormConfig](
        Section.processNode(
          <fg-detail if-true="/primaryFilerIsBlind" if-false="/primaryFilerAge65OrOlder"><summary>Both</summary></fg-detail>,
          pageRoute,
          factDictionary,
        ),
      )
    }
  }
}

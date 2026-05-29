package gov.irs.twe.generators

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.build.Flags
import gov.irs.twe.parser.Flow
import org.jsoup.nodes.Document
import org.jsoup.Jsoup
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.BeforeAndAfterAll
import os.Path
import scala.jdk.CollectionConverters.ListHasAsScala
import scala.xml.Elem

class WebsiteSpec extends AnyFunSpec with BeforeAndAfterAll {

  private val basicDictionaryConfig = <FactDictionaryModule>
    <Facts>
      <Fact path="/filer/name">
        <Name>Name</Name>
        <Writable>
          <String/>
        </Writable>
      </Fact>

      <Fact path="/isUsCitizenFullYear">
        <Name>Citizenship</Name>
        <Description>Whether the filer was a U.S. Citizen for all of the tax year</Description>

        <Writable>
          <Boolean/>
        </Writable>
      </Fact>
    </Facts>
  </FactDictionaryModule>

  private val dollarDictionaryConfig = <FactDictionaryModule>
    <Facts>
      <Fact path="/income/bonus">
        <Name>Bonus</Name>
        <Writable>
          <Dollar/>
        </Writable>
      </Fact>
    </Facts>
  </FactDictionaryModule>

  private val dateDictionaryConfig = <FactDictionaryModule>
    <Facts>
      <Fact path="/taxYear">
        <Description>The tax year of the return.</Description>
        <TaxYear>2026</TaxYear>
        <Derived>
          <Int>2026</Int>
        </Derived>
      </Fact>

      <Fact path="/lastTaxYear">
        <Description>Previous tax year.</Description>
        <TaxYear>2025</TaxYear>
        <Derived>
          <Int>2025</Int>
        </Derived>
      </Fact>

      <Fact path="/eventDate">
        <Name>Event Date</Name>
        <Writable>
          <Day/>
        </Writable>
      </Fact>
    </Facts>
  </FactDictionaryModule>

  private val basicFormConfig = <FlowConfig>
    <page route="/" title="Basic Test Form">
      <section>
        <fg-set path="/filer/name">
          <question>What is your
            <strong>full</strong>
            name?</question>
          <input type="text"/>
        </fg-set>

        <fg-set path="/isUsCitizenFullYear">
          <question>Were you a
            <strong>U.S. Citizen</strong>
            for all of the tax year?</question>
          <input type="boolean"/>
        </fg-set>
      </section>
    </page>
  </FlowConfig>

  private val factDictionary = FactDictionary.fromXml(basicDictionaryConfig)
  private val flow = Flow.fromXmlConfig(basicFormConfig, factDictionary)

  describe("basic form config") {
    val site = Website.generate(flow, basicDictionaryConfig, Map())
    val document = Jsoup.parse(site.pages.head.content)

    it("contains basic html elements") {
      assert(document.body() != null)
    }

    it("contains the expected <fg-set> elements") {
      val fgSets = document.body().select("fg-set")
      fgSets.eachAttr("path").asScala should contain theSameElementsAs Seq(
        "/filer/name",
        "/isUsCitizenFullYear",
        "/overrideDate", // From audit panel
      )
    }

    it("creates a flow with the expected number of pages") {
      site.pages.map(page => (page.route, page.languageCode)) should contain theSameElementsAs Seq(
        ("/", "en"),
        ("/", "es"),
      )
    }
  }

  describe("WebsitePage") {
    val mockContent = "<html></html>"
    val rootFilePath = Path("/tmp/app/tax-withholding-estimator")

    describe("filepath") {
      it("Uses the root path correctly for route `/`") {
        // given
        val route = "/"
        val languageCode = "en"
        val websitePage = WebsitePage(route, mockContent, languageCode)

        // when
        val filepath = websitePage.filepath(rootFilePath)

        // then
        assert(filepath == rootFilePath / "index.html")
      }

      it("Sets the path correctly for a named route") {
        // given
        val route = "/named"
        val languageCode = "en"
        val websitePage = WebsitePage(route, mockContent, languageCode)

        // when
        val filepath = websitePage.filepath(rootFilePath)

        // then
        assert(filepath == rootFilePath / "named" / "index.html")
      }

      it("Uses the root path correctly for route `/` with translations") {
        // given
        val route = "/"
        val languageCode = "es"
        val websitePage = WebsitePage(route, mockContent, languageCode)

        // when
        val filepath = websitePage.filepath(rootFilePath)

        // then
        assert(filepath == rootFilePath / languageCode / "index.html")
      }

      it("Sets the path correctly for a named route with translations") {
        // given
        val route = "/named"
        val languageCode = "es"
        val websitePage = WebsitePage(route, mockContent, languageCode)

        // when
        val filepath = websitePage.filepath(rootFilePath)

        // then
        assert(filepath == rootFilePath / languageCode / "named" / "index.html")
      }
    }

    describe("with allScreens flag enabled") {
      val flags = Map(Flags.allScreens -> true)
      val site = Website.generate(flow, basicDictionaryConfig, flags)

      it("includes /all-screens as the last page in the generated site") {
        site.pages.map(page => (page.route, page.languageCode)) should contain theSameElementsInOrderAs Seq(
          ("/", "en"),
          ("/", "es"),
          ("/all-screens", "en"),
        )
      }
    }
  }

  describe("hint aria-describedby rendering") {
    def renderDocument(formConfig: Elem, dictionaryConfig: Elem): Document = {
      val factDictionary = FactDictionary.fromXml(dictionaryConfig)
      val renderedFlow = Flow.fromXmlConfig(formConfig, factDictionary)
      val site = Website.generate(renderedFlow, dictionaryConfig, Map())
      Jsoup.parse(site.pages.head.content)
    }

    def assertAriaDescribedBy(
        formConfig: Elem,
        dictionaryConfig: Elem,
        selector: String,
        expectedValue: Option[String],
    ): Unit = {
      val element = renderDocument(formConfig, dictionaryConfig).select(selector)

      expectedValue match {
        case Some(value) => element.attr("aria-describedby") shouldBe value
        case None        => element.hasAttr("aria-describedby") shouldBe false
      }
    }

    it("renders on dollar input with hint") {
      val formConfigWithDollarHint = <FlowConfig>
        <page route="/" title="Dollar Hint Test Form">
          <section>
            <fg-set path="/income/bonus">
              <question>What is your bonus?</question>
              <hint>Enter the annual amount.</hint>
              <input type="dollar"/>
            </fg-set>
          </section>
        </page>
      </FlowConfig>

      assertAriaDescribedBy(
        formConfigWithDollarHint,
        dollarDictionaryConfig,
        "input[name=/income/bonus]",
        Some("/income/bonus-hint"),
      )
    }

    it("does not render on dollar input without hint") {
      val formConfigWithoutHint = <FlowConfig>
        <page route="/" title="Dollar No Hint Test Form">
          <section>
            <fg-set path="/income/bonus">
              <question>What is your bonus?</question>
              <input type="dollar"/>
            </fg-set>
          </section>
        </page>
      </FlowConfig>

      assertAriaDescribedBy(
        formConfigWithoutHint,
        dollarDictionaryConfig,
        "input[name=/income/bonus]",
        None,
      )
    }

    it("renders on boolean fieldset with hint") {
      val formConfigWithBooleanHint = <FlowConfig>
        <page route="/" title="Boolean Hint Test Form">
          <section>
            <fg-set path="/isUsCitizenFullYear">
              <question>Were you a U.S. Citizen for all of the tax year?</question>
              <hint>If you were not a U.S. Citizen for the entire year, select No.</hint>
              <input type="boolean"/>
            </fg-set>
          </section>
        </page>
      </FlowConfig>

      assertAriaDescribedBy(
        formConfigWithBooleanHint,
        basicDictionaryConfig,
        ".usa-fieldset",
        Some("/isUsCitizenFullYear-hint"),
      )
    }

    it("does not render on boolean fieldset without hint") {
      val formConfigWithoutBooleanHint = <FlowConfig>
        <page route="/" title="Boolean No Hint Test Form">
          <section>
            <fg-set path="/isUsCitizenFullYear">
              <question>Were you a U.S. Citizen for all of the tax year?</question>
              <input type="boolean"/>
            </fg-set>
          </section>
        </page>
      </FlowConfig>

      assertAriaDescribedBy(
        formConfigWithoutBooleanHint,
        basicDictionaryConfig,
        ".usa-fieldset",
        None,
      )
    }
  }

  describe("date input year rendering") {
    it("renders only the current tax year by default") {
      val formConfigWithDateInput = <FlowConfig>
        <page route="/" title="Date Input Test Form">
          <section>
            <fg-set path="/eventDate">
              <question>When did this happen?</question>
              <input type="date"/>
            </fg-set>
          </section>
        </page>
      </FlowConfig>

      val factDictionary = FactDictionary.fromXml(dateDictionaryConfig)
      val flow = Flow.fromXmlConfig(formConfigWithDateInput, factDictionary)
      val site = Website.generate(flow, dateDictionaryConfig, Map())
      val document = Jsoup.parse(site.pages.head.content)
      val yearInput = document.select("input[name=/eventDate-year]")

      yearInput.attr("type") shouldBe "text"
      yearInput.hasAttr("readonly") shouldBe true
      yearInput.attr("value") shouldBe "2026"
    }

    it("renders the requested number of previous tax years when previous-years is set") {
      val formConfigWithPreviousYears = <FlowConfig>
        <page route="/" title="Date Input Previous Years Test Form">
          <section>
            <fg-set path="/eventDate">
              <question>When did this happen?</question>
              <input type="date" previous-years="2"/>
            </fg-set>
          </section>
        </page>
      </FlowConfig>

      val factDictionary = FactDictionary.fromXml(dateDictionaryConfig)
      val flow = Flow.fromXmlConfig(formConfigWithPreviousYears, factDictionary)
      val site = Website.generate(flow, dateDictionaryConfig, Map())
      val document = Jsoup.parse(site.pages.head.content)
      val yearOptions = document.select("select[name=/eventDate-year] option")

      yearOptions.eachAttr("value").asScala should contain theSameElementsInOrderAs Seq("2024", "2025", "2026")
      yearOptions.eachText().asScala should contain theSameElementsInOrderAs Seq("2024", "2025", "2026")
    }

    it("renders the requested number of future tax years when future-years is set") {
      val formConfigWithFutureYears = <FlowConfig>
        <page route="/" title="Date Input Future Years Test Form">
          <section>
            <fg-set path="/eventDate">
              <question>When did this happen?</question>
              <input type="date" future-years="2"/>
            </fg-set>
          </section>
        </page>
      </FlowConfig>

      val factDictionary = FactDictionary.fromXml(dateDictionaryConfig)
      val flow = Flow.fromXmlConfig(formConfigWithFutureYears, factDictionary)
      val site = Website.generate(flow, dateDictionaryConfig, Map())
      val document = Jsoup.parse(site.pages.head.content)
      val yearOptions = document.select("select[name=/eventDate-year] option")

      yearOptions.eachAttr("value").asScala should contain theSameElementsInOrderAs Seq("2026", "2027", "2028")
      yearOptions.eachText().asScala should contain theSameElementsInOrderAs Seq("2026", "2027", "2028")
    }

    it("renders previous and future tax years together when both attributes are set") {
      val formConfigWithPreviousAndFutureYears = <FlowConfig>
        <page route="/" title="Date Input Previous And Future Years Test Form">
          <section>
            <fg-set path="/eventDate">
              <question>When did this happen?</question>
              <input type="date" previous-years="1" future-years="1"/>
            </fg-set>
          </section>
        </page>
      </FlowConfig>

      val factDictionary = FactDictionary.fromXml(dateDictionaryConfig)
      val flow = Flow.fromXmlConfig(formConfigWithPreviousAndFutureYears, factDictionary)
      val site = Website.generate(flow, dateDictionaryConfig, Map())
      val document = Jsoup.parse(site.pages.head.content)
      val yearOptions = document.select("select[name=/eventDate-year] option")

      yearOptions.eachAttr("value").asScala should contain theSameElementsInOrderAs Seq("2025", "2026", "2027")
      yearOptions.eachText().asScala should contain theSameElementsInOrderAs Seq("2025", "2026", "2027")
    }
  }
}

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
      site.pages.map(_.route) should contain theSameElementsAs Seq("/")
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
        site.pages.map(_.route) should contain theSameElementsInOrderAs Seq("/", "/all-screens")
      }
    }

    describe("with languageSwitcher flag enabled") {
      val flags = Map(Flags.languageSwitcher -> true)
      val site = Website.generate(flow, basicDictionaryConfig, flags)

      it("includes the language switcher in the header") {
        val document = Jsoup.parse(site.pages.head.content)
        val languageSwitcher = document.select(".language-switcher")
        languageSwitcher should not be empty
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
}

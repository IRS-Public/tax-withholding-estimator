package gov.irs.twe.generators

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.build.Flags
import gov.irs.twe.parser.Flow
import org.jsoup.Jsoup
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.BeforeAndAfterAll
import os.Path
import scala.jdk.CollectionConverters.ListHasAsScala

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
      println(site.pages)
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
  }
}

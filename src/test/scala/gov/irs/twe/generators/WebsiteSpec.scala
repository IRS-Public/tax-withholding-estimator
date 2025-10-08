package gov.irs.twe.generators

import org.jsoup.Jsoup
import org.scalatest.funspec.AnyFunSpec

class WebsiteSpec extends AnyFunSpec {

  describe("basic form config") {
    val basicDictionaryConfig = <FactDictionaryModule>
      <Facts>
        <Fact path="/filer/name">
          <Name>Name</Name>
          <Writable><String/></Writable>
        </Fact>
      </Facts>

      <Fact path="/isUsCitizenFullYear">
        <Name>Citizenship</Name>
        <Description>Whether the filer was a U.S. Citizen for all of the tax year</Description>

        <Writable><Boolean /></Writable>
      </Fact>
    </FactDictionaryModule>

    val basicFormConfig = <FlowConfig>
      <page route="/" title="Basic Test Form">
        <section>
          <fg-set path="/filer/name">
            <question content-key="flow.test./filer/name.question" />
            <input type="text"/>
          </fg-set>

          <fg-set path="/isUsCitizenFullYear">
            <question content-key="flow.test./isUsCitizenFullYear.question" />
            <input type="boolean"/>
          </fg-set>
        </section>
      </page>
    </FlowConfig>

    val site = Website.fromXmlConfig(basicFormConfig, basicDictionaryConfig, Map())
    val document = Jsoup.parse(site.pages.head.content)

    it("contains basic html elements") {
      assert(document.body() != null)
    }

    it("contains 2 <fg-set>s") {
      val fgSets = document.body().select("fg-set")
      assert(fgSets.size() == 2)
    }
  }

}

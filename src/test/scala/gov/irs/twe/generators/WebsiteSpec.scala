package gov.irs.twe.generators

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.generateFlowLocalFile
import gov.irs.twe.parser.Flow
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
            <question>What is your <strong>full</strong> name?</question>
            <input type="text"/>
          </fg-set>

          <fg-set path="/isUsCitizenFullYear">
            <question>Were you a <strong>U.S. Citizen</strong> for all of the tax year?</question>
            <input type="boolean"/>
          </fg-set>
        </section>
      </page>
    </FlowConfig>

    val factDictionary = FactDictionary.fromXml(basicDictionaryConfig)
    val flow = Flow.fromXmlConfig(basicFormConfig, factDictionary)
    // TODO: This is a hack to make the test pass in GHA
    // This needs to be fixed my moving the locale stuff into the form itself (which is very-doable)
    generateFlowLocalFile(basicFormConfig)
    val site = Website.generate(flow, basicDictionaryConfig, Map())
    val document = Jsoup.parse(site.pages.head.content)

    it("contains basic html elements") {
      assert(document.body() != null)
    }

    it("contains 3 <fg-set>s") {
      val fgSets = document.body().select("fg-set")
      assert(fgSets.size() == 3) // The boolean has two
    }
  }

}

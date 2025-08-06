package gov.irs.twe.generators

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

    val basicFormConfig = <FormConfig>
      <page route="/" title="Basic Test Form">
        <section>
          <fg-set path="/filer/name">
            <label>What is your full name?</label>
            <input type="text"/>
          </fg-set>

          <fg-set path="/isUsCitizenFullYear">
            <label>Were you a US Citizen for all of TY2025?</label>
            <input type="boolean"/>
          </fg-set>
        </section>
      </page>
    </FormConfig>

    val site = Website.fromXmlConfig(basicFormConfig, basicDictionaryConfig)

    it("contains basic html elements") {
      val page = site.pages.head
      assert((page.content \\ "html").nonEmpty)
      assert((page.content \\ "body").nonEmpty)
    }

    it("contains 2 <fg-set>s") {
      val page = site.pages.head
      assert((page.content \\ "fg-set").length == 2)
    }
  }

}

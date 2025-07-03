package gov.irs.formflow.generators

import org.scalatest.funspec.AnyFunSpec

class WebsiteSpec extends AnyFunSpec {

  describe("basic form config") {
    val basicFormConfig = <FormConfig>
      <section>
        <question path="/filer/name">
          <label>What is your full name?</label>
          <input type="text"/>
        </question>

        <question path="/filer/citizen/ty25">
          <label>Were you a US Citizen for all of TY2025?</label>
          <input type="boolean"/>
        </question>

        <question path="/filer/citizen/partTy25">
          <label>Did you become a US Citizen during TY2025?</label>
          <input type="boolean"/>
        </question>

        <question path="/filer/primaryResidence">
          <label>Which state was your primary residence?</label>
          <input type="select"/>
        </question>
      </section>
    </FormConfig>

    val site = Website.fromXmlConfig(basicFormConfig)

    it("contains basic html elements") {
      val page = site.pages.head
      assert((page.content \\ "html").nonEmpty)
      assert((page.content \\ "body").nonEmpty)
    }

    it("contains 3 fieldsets") {
      val page = site.pages.head
      assert((page.content \\ "fieldset").length == 4)
    }
  }

}

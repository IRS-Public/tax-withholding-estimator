package gov.irs.twe.generators

import gov.irs.twe.loadTweFactDictionary
import gov.irs.twe.parser.Flow
import org.jsoup.Jsoup
import org.scalatest.funspec.AnyFunSpec
import scala.io.Source
import scala.jdk.CollectionConverters.*
import scala.util.Using

class IncomeHeadingSpec extends AnyFunSpec {
  private val dictionary = loadTweFactDictionary()
  private val incomeConfig = Using.resource(Source.fromResource("twe/flow/income.xml")) { source =>
    xml.XML.loadString(source.mkString)
  }
  private val flow = Flow.fromXmlConfig(incomeConfig, dictionary.factDictionary)
  private val site = Website.generate(flow, dictionary.xml, Map())

  for (languageCode <- List("en", "es"))
    describe(s"income accordion headings in $languageCode") {
      val page = site.pages.find(page => page.route == "/income" && page.languageCode == languageCode).get
      val document = Jsoup.parse(page.content)

      for ((path, expectedCount) <- List("/jobs" -> 6, "/pensions" -> 3))
        it(s"renders all $path accordion summaries below the h5 subsection headings") {
          val summaries = document.select(s"""fg-collection[path="$path"] details.fg-detail > summary""")

          assert(summaries.size() == expectedCount)
          for (summary <- summaries.asScala) {
            assert(summary.children().size() == 1)
            assert(summary.child(0).tagName() == "h6", s"Expected h6 for: ${summary.text()}")
            assert(summary.text().nonEmpty)
          }
        }

      it("keeps top-level investment accordion summaries below the h3 section heading") {
        val summaries = document.select("section.flow > details.fg-detail > summary")

        assert(summaries.size() == 6)
        for (summary <- summaries.asScala) {
          assert(summary.children().size() == 1)
          assert(summary.child(0).tagName() == "h4", s"Expected h4 for: ${summary.text()}")
          assert(summary.text().nonEmpty)
        }
      }
    }
}

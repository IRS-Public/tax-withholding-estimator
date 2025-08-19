package gov.irs.twe.generators

import gov.irs.factgraph.FactDictionary
import gov.irs.twe.parser.{ Flow, Page, PageNode }
import org.jsoup.parser.Tag
import org.jsoup.Jsoup
import os.Path
import scala.io.Source

case class WebsitePage(route: String, content: xml.Elem) {
  def html(): String = {
    val content = "<!DOCTYPE html>" + this.content.toString

    // Many of the changes here are tweaks to make the output easy to read in view-source
    val document = Jsoup.parse(content)

    // Set certain elements to "block" formatting
    // https://github.com/jhy/jsoup/issues/2141#issuecomment-2795853753
    val setElement = document.selectFirst("fg-set")
    if (setElement != null) {
      val tag = setElement.tag()
      tag.set(Tag.Block)
      setElement.children().forEach(child => child.tag().set(Tag.Block))
    }

    // Convert to an HTML string
    var html = document.html()
    // Adding a newline after each <fg-set> block to make them easier to see
    html = html.replace("</fg-set>", "</fg-set>\n")

    html
  }
}

case class Website(pages: List[WebsitePage], factDictionary: xml.Elem) {
  def save(directoryPath: Path): Unit = {
    os.remove.all(directoryPath)

    // Write the pages
    for (page <- this.pages) {
      val target = directoryPath / page.route
      os.write(target, page.html(), null, createFolders = true)
    }

    val resourcesSource = os.pwd / "src" / "main" / "resources" / "twe" / "website-static"
    val resourcesTarget = directoryPath / "resources"
    os.copy(resourcesSource, resourcesTarget)
  }
}

object Website {
  def fromXmlConfig(config: xml.Elem, dictionaryConfig: xml.Elem): Website = {
    val factDictionary = FactDictionary.fromXml(dictionaryConfig)
    val flow = Flow.fromXmlConfig(config, factDictionary)
    generate(flow, dictionaryConfig)
  }

  def generate(flow: Flow, dictionaryConfig: xml.Elem): Website = {
    val navElements = flow.pages.map(page => <li class="steps-nav__item"><a href={page.route}>{page.title}</a></li>)
    val nav = <nav class="steps-nav">
      <ul class="steps-nav__list">
        {navElements}
      </ul>
    </nav>

    val pages = flow.pages.zipWithIndex.map { case (page, index) =>
      val nextPageHref = if (index < flow.pages.length - 1) {
        Some(flow.pages(index + 1).route + ".html")
      } else {
        None
      }
      generatePage(page, dictionaryConfig, nav, nextPageHref)
    }

    Website(pages, dictionaryConfig)
  }

  private def generatePage(
      page: Page,
      dictionaryConfig: xml.Elem,
      nav: xml.Elem,
      nextPageHref: Option[String],
  ): WebsitePage = {
    val pageXml = page.nodes.map {
      case PageNode.section(x) => x.html()
      case PageNode.rawHTML(x) => x
    }

    val title = s"Tax Withholding Estimator - ${page.title} | Internal Revenue Service"

    val continueButton = nextPageHref match {
      case Some(href) =>
        <a class="usa-button margin-top-3 continue-button" href={
          href
        } onclick="return handleSectionContinue(event)">Continue</a>
      case None =>
        <a class="usa-button margin-top-3 continue-button" href="#" onclick="return handleSectionComplete(event)">Submit</a>
    }

    val content = <html lang="en">
      <head>
        <meta charset="utf-8" />
        <meta http-equiv="X-UA-Compatible" content="IE=edge" />
        <meta name="HandheldFriendly" content="True" />
        <meta name="MobileOptimized" content="320" />
        <meta name="viewport" content="width=device-width, initial-scale=1.0" />
        <title>{title}</title>
        <link rel="icon" href="/resources/img/favicon.ico" type="image/x-icon" />
        <link rel="stylesheet" href="/resources/uswds-3.13.0/styles/uswds.min.css"></link>
        <link rel="stylesheet" href="/resources/styles/main.css"></link>
        <link rel="stylesheet" href="/resources/styles/buttons.css"></link>
        <script type="module" src="/resources/js/factgraph-3.1.0.js"></script>
        <script type="module" src="/resources/js/fg-components.js"></script>
        <script type="module" src="/resources/js/debug-components.js"></script>
        <script src="/resources/uswds-3.13.0/js/uswds-init.min.js"></script>
        <link rel="preload" href="/resources/uswds-3.13.0/js/uswds.min.js" as="script" />
      </head>

      <body>
        <a class="usa-skipnav" href='#main-content'>Skip to main content</a>
        <section
          class="usa-banner"
          aria-label="Official website of the United States government"
        >
          <div class="usa-accordion">
            <header class="usa-banner__header">
              <div class="usa-banner__inner">
                <div class="grid-col-auto">
                  <img
                    aria-hidden="true"
                    class="usa-banner__header-flag"
                    src="/resources/uswds-3.13.0/img/us_flag_small.png"
                    alt=""
                  />
                </div>
                <div class="grid-col-fill tablet:grid-col-auto" aria-hidden="true">
                  <p class="usa-banner__header-text">
                    An official website of the United States government
                  </p>
                  <p class="usa-banner__header-action">Here’s how you know</p>
                </div>
                <button
                  type="button"
                  class="usa-accordion__button usa-banner__button"
                  aria-expanded="false"
                  aria-controls="gov-banner-default"
                >
                  <span class="usa-banner__button-text">Here’s how you know</span>
                </button>
              </div>
            </header>
            <div
              class="usa-banner__content usa-accordion__content"
              id="gov-banner-default"
            >
              <div class="grid-row grid-gap-lg">
                <div class="usa-banner__guidance tablet:grid-col-6">
                  <img
                    class="usa-banner__icon usa-media-block__img"
                    src="/resources/uswds-3.13.0/img/icon-dot-gov.svg"
                    role="img"
                    alt=""
                    aria-hidden="true"
                  />
                  <div class="usa-media-block__body">
                    <p>
                      <strong>Official websites use .gov</strong><br />A
                      <strong>.gov</strong> website belongs to an official government
                      organization in the United States.
                    </p>
                  </div>
                </div>
                <div class="usa-banner__guidance tablet:grid-col-6">
                  <img
                    class="usa-banner__icon usa-media-block__img"
                    src="/resources/uswds-3.13.0/img/icon-https.svg"
                    role="img"
                    alt=""
                    aria-hidden="true"
                  />
                  <div class="usa-media-block__body">
                    <p>
                      <strong>Secure .gov websites use HTTPS</strong><br />A
                      <strong>lock</strong> (
                      <span class="icon-lock"
                        ><svg
                          xmlns="http://www.w3.org/2000/svg"
                          width="52"
                          height="64"
                          viewBox="0 0 52 64"
                          class="usa-banner__lock-image"
                          role="img"
                          aria-labelledby="banner-lock-description-default"
                          focusable="false"
                        >
                          <title id="banner-lock-title-default">Lock</title>
                          <desc id="banner-lock-description-default">Locked padlock icon</desc>
                          <path
                            fill="#000000"
                            fill-rule="evenodd"
                            d="M26 0c10.493 0 19 8.507 19 19v9h3a4 4 0 0 1 4 4v28a4 4 0 0 1-4 4H4a4 4 0 0 1-4-4V32a4 4 0 0 1 4-4h3v-9C7 8.507 15.507 0 26 0zm0 8c-5.979 0-10.843 4.77-10.996 10.712L15 19v9h22v-9c0-6.075-4.925-11-11-11z"
                          />
                        </svg> </span
                      >) or <strong>https://</strong> means you’ve safely connected to
                      the .gov website. Share sensitive information only on official,
                      secure websites.
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>
        <header>
           <div class="logo-banner"><img src="/resources/img/irs-logo.svg" alt="" /></div>
        </header>

        <main class="hidden" id="main-content">
          {nav}
          {pageXml}
          {continueButton}
        </main>
        <template id="validate-alert-template">
           <div class="usa-alert usa-alert--error validate-alert" role="alert">
             <div class="usa-alert__body">
               <h4 class="usa-alert__heading">Warning status</h4>
                   <p class="usa-alert__text" id="alert-message">Please complete the required fields</p>
             </div>
           </div>
        </template>
        <script type="text" id="fact-dictionary">{dictionaryConfig}</script>
        <script src="/resources/uswds-3.13.0/js/uswds.min.js"></script>
      </body>
    </html>

    val route = if (page.route == "/") "index.html" else s"${page.route}.html"
    WebsitePage(route, content)
  }

}

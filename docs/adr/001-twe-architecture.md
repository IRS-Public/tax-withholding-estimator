# ADR-001: TWE 2.0 Architecture

- Created: August 27, 2025
- Published:

## Primary author(s)
[primary authors]: #primary-authors

@petrosgov

## Problem Statement

The [Tax Withholding Estimator](https://www.irs.gov/individuals/tax-withholding-estimator) (TWE) is an online web application offered by the IRS.
It compliments the [paper Form W4](https://www.irs.gov/pub/irs-pdf/fw4.pdf?1736746289) by providing accurate withholding estimation for taxpayers who have more complex tax scenarios, such as a change in marital status or multiple jobs.

Updating TWE in response to tax law changes has long been costly and time-consuming for the IRS.
TWE calculates taxes with procedural JavaScript code, an approach has proven difficult to test: intermediate calculations and individual tax rules cannot be dynamically introspected and verified.
Leading implementations from the [public](https://github.com/IRS-Public/direct-file) and [private](https://arxiv.org/abs/2009.06103) sectors have demonstrated that modeling the U.S. Tax Code with a knowledge graph provides a more accurate, maintainable, and testable framework for complex rule logic.

The IRS should replace TWE's dated tax calculation code with a modern knowledge graph, in time to support new tax scope for FS2026.
The resulting application must be one that can be owned and updated by IRS engineers, indefinitely, at low cost to the agency.

## Goals and Non-Goals
[goals and non-goals]: #goals-and-non-goals

### Base Goals

TWE 2.0 must:

* Calculate a taxpayer's withholdings, within the desired tax scope, by January 1, 2026.
* Comply with 508 requirements
* Exhaustively and transparently test for correctness
* Communicate complex information and tax calculations in plain language
* Have a specification that can be easily updated, including by non-engineers
* Be maintainable by the IRS over a long period of time, at the lowest cost possible

### Stretch Goals

TWE 2.0 should:

* Improve the TWE completion rate, especially the significant drop-off in the "Income" section
* Support a development cycle that lives entirely on the on-network IRS GFE (laptop). In theory this is already possible, but we won't know until someone has to do it.

### Non-Goals

TWE 2.0 will not:

* Make Fact Graph-based webforms generic to non-TWE applications (although the code is written with generic applications in mind, and with the hope that it might be adapted for such in the future).

## Decision

TWE 2.0 is a static site generator that integrates with the Fact Graph.
Its Scala codebase turns XML data into HTML templates, and the resulting HTML contain custom elements that connect HTML form inputs to the local Fact Graph JavaScript bundle.

TWE 2.0 ensures that both the tax law and the webform layout are defined as XML data formats.
This makes it easy for engineers (and, with some additional effort, any interested parties), to read, validate, and update tax logic and form layout;
future updates to tax law should only requires changes to our tax logic XML files.
TWE 2.0 can support the entire 1.0 scope, as well as new, high-priority deductions.

TWE 2.0 minimizes its maintenance footprint by building on stable technologies with as few dependencies as possible.
The static site generation uses only one build system (`sbt`), and only a few, stable JVM libraries (6, at the time of this writing, including the Fact Graph).
The compiled client code uses native web browser technologies, like [custom elements](https://developer.mozilla.org/en-US/docs/Web/API/Web_components/Using_custom_elements), that are guaranteed to remain backwards compatible.
The custom JavaScript that powers those custom elements currently consists of a single file.
The only dependencies in the client code are a vendored copy of the [United States Web Design System](https://designsystem.digital.gov/) and a standalone PDF library.

These choices make the application easier to build, deploy, maintain, and update within IRS IT capacity, giving it the greatest chance to be useful and cost-effective for a long period of time.

## Background

### Key terms

<dl>
  <dt>TWE 1.0
  <dd>The current Tax Withholding Estimator, live on irs.gov

  <dt>TWE 2.0
  <dd>The new Tax Withholding Estimator, which leverages the Fact Graph.

  <dt>Direct File Flow
  <dd>The Direct File JSX-based flow implementation. This is the <code>df-client</code> package in the open source release.

  <dt>Flow XML
  <dd>The new, XML-based flow implementation designed for TWE 2.0.

  <dt>Fact Graph
  <dd>The IRS's declarative tax calculation engine, first deployed for Direct File.

  <dt>Fact Dictionary
  <dd>A set of facts about tax logic (i.e. to be eligible for X credit you must be Y years of age) for the Fact Graph

  <dt>TY2025
  <dd>Tax Year 2025

  <dt>FS2026
  <dd>Filing Season 2026 (in which taxes for Tax Year 2025 are paid)
</dl>

### TWE 1.0

TWE 1.0 is a React-based NextJS application.
It has no tax logic engine and it calculates taxes entirely using imperative code.
This makes it difficult to support tax code changes and guarantee the correctness of the math.

The TWE 1.0 and 2.0 teams share the belief that the US Tax Code is too complicated to be modeled by imperative code.
TWE 1.0 has prototyped a rules engine in the past.

### The Fact Graph

The Fact Graph is a bespoke rules engine for modeling the tax code and calculating tax obligation.
It was originally developed to support Direct File.

### The Direct File Flow

The most recent application to be powered by the Fact Graph was Direct File, which was built in React and specified with JSX.
The [`flow.tsx`](https://github.com/IRS-Public/direct-file/blob/e0d5c84451cc52b72d20d04652e306bf4af1a43c/direct-file/df-client/df-client-app/src/flow/flow.tsx) file from Direct File contains the following statement of intent (slightly excerpted):

> The flow, while currently existing as code, has an eventual goal of becoming configuration that tax experts will be able to modify.
> This leads to a few design choices:
>   1. Everything in this file should remain serializable.
>   2. While many of the below react components look like our fact components, they have separate definitions from the components that actually render.
>   3. We have to maintain a mapping between the components we declare here in our config and the components that eventually render.

The Direct File Flow made tremendous progress in this direction.
It contained nice integrations with the Fact Graph, like the `path` attribute (links an input to a specific fact) and the `condition` attribute (only shows a screen if the fact graph matches a logical statement).
It also supported an "All-Screens" view, which became a load-bearing tool for product, design, *and* engineering workflows.

The main limitation of the Direct File Flow, with respect to these goals, was its JSX specification.
JSX is not a data format: it's an [XML-like JavaScript extension]("https://facebook.github.io/jsx/") that transpiles to `React.createElement` calls (or other framework equivalent).
While it's possible to write code [that introspects the resulting React tree](https://github.com/IRS-Public/direct-file/blob/e0d5c84451cc52b72d20d04652e306bf4af1a43c/direct-file/df-client/df-client-app/src/all-screens/AllScreensContent.tsx#L98), at that point the declarative representation has been lost and cannot be recreated.

Nevertheless, these are absolutely the correct goals, fulfilling them simply requires finishing the transition of the Flow from JSX to a cross-platform, serializable data format: XML.
A flow representable as data can be more easily edited by non-technical stakeholders; it also paves the way for a flow-editing GUI.

(N.B. prior to the JSX flow, an even earlier version of Direct File also used XML for the flow.)

## Architecture

When reading the architecture, keep in mind its core goal: to be owned and updated by the IRS, indefinitely, at low cost.

**TWE 2.0 is optimized to make updates to the tax logic, form questions, and website content very easy,** because these requirements change frequently, **at the cost of making the structure, style, and functionality of the webpage more rigid**, because these requirements change infrequently. TWE 2.0 also curtails dependencies wherever reasonable, to reduce ongoing upgrade churn.

### Overview

TWE 2.0 is a static site generator, built in scala.
It builds the static site bundle based on two XML configurations: a Fact Dictionary for tax logic, and Flow XML for the webform.
The generated static HTML largely consists of `<fg-set>` custom elements, which enhance HTML form inputs with the ability to to set values in a fact graph.
Once the taxpayer has input sufficient information, the fact graph is used to calculate their withholdings, and its results are displayed with `<fg-show>` elements.

### Flow XML Config

TWE 2.0 is powered by Flow XML, an XML configuration that specifies a Fact Graph-powered webform.
It allows Tax Logic engineers to think in terms of "questions" that are asked to the user, while abstracting away the implementation details.

TWE 2.0 lightly transforms that XML config into HTML, with some supporting web components that apply the inputs to a locally-saved Fact Graph.
Here is a basic config, which asks for your income and taxes paid to date.

```xml
<page>
  <section>
    <h2>Income</h2>

    <fg-set path="/income">
      <question>Total income for the year</question>
      <input type="dollar"/>
    </fg-set>

    <fg-set path="/taxesPaid">
      <question>Total taxes paid for the year</question>
      <input type="dollar"/>
    </fg-set>
  </section>

  <section>
    <h2>Calculations</h2>

    <p>
      Your <strong>taxable income</strong> is:
      <fg-show path="/roundedTaxableIncome" />
    </p>

    <p>
      Your <strong>suggested adjustment per pay period</strong> is:
      <fg-show path="/adjustmentNeededPerPayPeriod" />
    </p>
  </section>
</page>

```

This defines a single page of the flow.
TWE will transform `<input type="dollar" />` (which is not a valid [input type](https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/input#input_types)), into an `<input type="text" inputtmode="numeric"/>` that sets a Dollar fact in the local fact graph.
It will also use the `<question>` tag to set up the appropriate labeling for the given input (a more dynamic task than you might think, see: radio buttons).

There are also some custom HTML elements (i.e. web components) that handle Fact Graph functions.
`<fg-set>` wraps an input and sets the fact at the associated path.
`<fg-get>` will replace its contents with the current value of the fact at that path.

Everything else, the regular HTML elements (`<p>` and `<strong>`), will get passed through, unmodified, to the static output.
This makes it possible to introduce arbitrary inline content without changing how the flow works.

#### Validation

While parsing the config, Flow XML checks the `fg-*` elements against the facts that the show and specify.

For instance, if you misspell `/income` as `/incom`, TWE will throw a compile-time error saying that that no `/incom` fact exists.
It will also check that you've provided the right type of input for the fact, so you can't accidentally set a fact that expects a `Dollar` value to `type="boolean"`.

#### Tooling

Flow XML configs are valid XML, which makes it easy to take advantage of the mature XML tooling ecosystem, as well as boostrap new tooling on top of the Flow XML flow.

As a trivial example, Flow XML configs support XPath.
Let's say you wanted to know the paths of all the `<fg-set>` elements that have `<input type="Dollar">` children.
You can do that straight from the MacOS terminal:

```bash
xpath < src/main/resources/twe/flow.xml -e '//fg-set[input [@type="dollar"]]/@path'

Found 2 nodes in stdin:
-- NODE --
 path="/income"
-- NODE --
 path="/taxesPaid"
```

Flow XML also contains a RelaxNG specification.
This enables us to run validations in CI that the flow only contains certain elements.
It also enables cross-platform autocomplete for elements and attributes in text editors.

### XML to HTML Generation

Flow XML is a static site generator with an XML specification.
It converts the XML into a folder of static webpage assets.
The functionality of each page is described in HTML, with a handful of custom elements (a.k.a web components) that take HTML input and store the values in a fact graph.
That fact graph, which contains the user's currently-input tax information, is serialized and saved to the browser's session storage.

Adding additional functionality to TWE should adhere to the following guidelines:

* Strictly limit build dependencies.
* Minimize required development setup.
* Use static HTML generation rather than dynamic DOM manipulation (i.e. `fg-components.js` functionality) wherever possible.

#### Stability

The foundational principle of this architecture is to minimize dependencies wherever reasonable.
Generally speaking, fewer dependencies means: shorter onboarding times, easier machine requirements, and less time spent upgrading packages for compliance.

Downloading and building TWE 2.0 from source requires only an `sbt` installation.
TWE 2.0 does not require Docker, TypeScript, or any NodeJS build tools.
To minimize upgrade friction, it has few dependencies, all of which are managed by `sbt` and which largely do not depend on each other;
this also reduces the security scanning surface.

Static-site generation is a stable paradigm.
As long as the generator itself is able to run, the website will operate essentially forever.

#### HTML and Web Components

Websites based in standard HTML are fast, mostly-accessible by default, and supported indefinitely.
Unfortunately, a tax calculation engine cannot be written in just HTML.

Most of the TWE 2.0 interactivity is described via custom elements (a.k.a Web Components).
Custom elements are a mature technology with cross-platform support and backwards compatibility guarantees.
They are implemented with a couple-hundred lines of JavaScript.

The JavaScript functionality is deliberately kept as small as possible, and heavily-scrutinized to maintain accessibility and performance requirements.
To encourage keeping the JavaScript small, it is confined to a single `fg-components.js` file, written without any build tools.
The only time new JavaScript should be introduced is when the desired functionality cannot be reasonably accomplished with static HTML generation.

The deliberate omission of JavaScript *build* tools does not necessarily preclude the use of JavaScript tooling for development.
We lint the custom JavaScript in CI, and we have already integrated [JSDoc](https://jsdoc.app/) for IntelliSense purposes.

## Alternatives Considered

### Single-Page Application (with React)

A tax calculator that updates various parts of the page in response to user input is a fundamentally "reactive" page model that would be a natural fit for a Single-Page Application (SPAs).
The most common SPA library is React, usually used in conjunction with a framework like NextJS or React Router.
Additionally, many web developers are more comfortable working withing SPA frameworks than they are with direct DOM manipulation.

An SPA was rejected because representing the flow as data (XML) is a core goal of this architecture—one from which TWE's longevity and maintainability properties are derived.
SPA UIs are not designed to be specified this way.
One cannot, for instance, instruct NextJS App Router to build all the routes from an XML file.
Therefore, to both use React and specify the flow as a configurable data format, one has to build a new framework for converting XML files into React-powered webpages.

Building a React app this way, "from scratch," is explicitly discouraged by [React's own documentation](https://react.dev/learn/build-a-react-app-from-scratch):

> Starting from scratch is an easy way to get started using React, but a major tradeoff to be aware of is that going this route is often the same as building your own adhoc  framework. As your requirements evolve, you may need to solve more framework-like problems that our recommended frameworks already have well developed and supported solutions for.

Creating a configurable tax form specification and translating it to a reactive webpage is a sufficiently bespoke requirement that it cannot be handled by pre-existing frameworks.
So it will always be necessary to build a TWE-specific framework, whether that framework ultimately uses React or not.

Static site generation was chosen as the "framework" for converting the tax form specification into a webpage, because that paradigm limits the complexity of the implementation: the XML maps to HTML output in an obvious way.
The reactive functionality that remains to be implemented, once the HTML pages with `<fg-set>` have been generated, is limited enough that it can be reasonably written in vanilla JS, eliding the React dependency entirely.

### Small, Client-Side Library

We could employ a lighter-weight client-side library, like Vue, to handle the frontend reactivity that's currently written in vanilla JS.
Such libraries are easy to vendor, have nice syntax, and are reasonably simple to understand.

This might be slightly easier up-front, but would have the long-term cost of introducing a dependency on an external library and documentation. It's also not obvious that the amount of code necessary to write `fg-components.js` with Vue would be less than the amount necessary to write it in regular JS.

At this time, the hand-written Web Components and multi-page model are sufficient for the UX for the first TWE 2.0 launch.
Web Components are part of the web platform and will not only never go out of date, but they will improve in performance over time:
this makes them an attractive choice for an application to should be easy to maintain.

The choice to adopt a frontend interactivity library remains available, if it becomes necessary to update TWE with a more dynamic UX in the future.

## Operations and DevOps

TWE 1.0 is built and deployed as a static site; TWE 2.0 will do the same.

This team already knows how to run `sbt` in GitHub actions thanks to the Fact Graph, and we can simply lift that configuration here.

## Security and Compliance

TWE 1.0 and 2.0 are both static applications that do all their calculations client side.
This is the correct architecture, and it dramatically limits the security profile of TWE.
Static websites are not vulernable to [Cross-Site Scripting (XSS)](https://owasp.org/www-community/attacks/xss/) attacks, because they have no user-generated content. The application is not running on authenticated IRS hardware and has no access to PII.

TWE 2.0 also raises the bar by including as few dependencencies as possible, limiting the possibility for supply chain attacks.
All managed TWE dependencies are declared in the `build.sbt` file and will be scanned the same way the Fact Graph dependencies are.

The only depenencies not in `build.sbt` are vendored builds of:

* The United States Web Design System
* The Fact Graph (note that this does include the Fact Graph dependencies)
* A PDF library (not yet chosen)

Vendoring these libraries allows us to avoid using `npm` entirely, for both the build production asset bundle.
This minimizes churn, simplifies the application, and removes an avenue for a supply chain attack.

## Risks

### Scala

The Fact Graph was written in Scala because Scala can be compiled to both the JVM (to run on the IRS' backend services, which are primarily written in Java) and JavaScript (to run in the browser).
TWE 2.0 has no such requirement: static site generation can be done in any programming language.
It does need to consume the Fact Graph as a library (to validate the facts when compiling the flow) but this can be done easily in any JVM-based language, or, much less easily, in TypeScript.

Writing TWE 2.0 in Scala has one significant benefit:
it demystifies Scala and provides a far friendlier on-ramp to the language than the Fact Graph does.
The Fact Graph (necessarily) uses advanced functional programming techniques to model a complicated calculation engine (the risks associated with this complexity are breifly addressed in the Fact Graph 3.1 ADR);
TWE 2.0, however, mostly just transforms XML into HTML, which is a straightforward programming task.
TWE 2.0 gives engineers a much gentler introduction to the Scala programming environment, and its integration with the Fact Graph is already leading to downstream Fact Graph improvements.

All this having been said: Scala is not considered a standard language choice at the IRS, and hearing that a program is written in Scala sparks immediate—and reasonable—suspicion that the program will be un-maintainable.
Given that maintainability by IRS engineers is a non-negotiable goal (and GFE-based maintainability an important strech goal), it's fair to ask whether TWE 2.0 could accomplish its goals with an implmentation in a language that would raise fewer eyebrows.

It could.
TWE 2.0 could be written in Java, and it is designed so that a Java port would be a reasonable thing to do, even before the launch date of January 1, 2026.
Doing so is not recommended, as using Scala to build TWE 2.0 allows the IRS to build expertise required to maintain the Fact Graph.
(This is what Scala was designed for: enabling high-leverage, functional code to be deployed seamlessly in Java-heavy organizations.)
It is, however, possible.

### Static Site Generation

With maintainability placed at such a premium, it's reasonable to question whether a Scala-based static site generator is more maintainable than a React SPA.
Even if all the problems with integrating an SPA are correctly diagnosed, an SPA would still be a more mainstream choice.

The judgment call being made is that there is no way to implement TWE functionality without writing some form of custom framework (see the "Single-Page Application" section for more details). Therefore, a choice must be made about how to write that framework in a way that maximizes the likelihood that a future engineer—possibly one with little to no social context for the application—will be able to make required changes to it.

Static site generation was chosen as that framework because its implementation is straightforward and simple to understand.
Although this may not being the most common way to build an interactive web application today, once the paradigm is set up, we hope that it is an intuitive and approachable one for the IRS engineers of tomorrow.

# Tax Withholding Estimator (TWE)

### Overview
An online website provided by the Internal Revenue Service (IRS) for a taxpayer to estimate their tax withholdings for a variety of tax situations.
For a deep dive into the technical design of this repository, start [here](./docs/adr/001-twe-architecture.md).
To better understand the math behind tax withholdings, go [here](./docs/taxes/withholdings-basics.md).

### What TWE is (and isn't)
TWE is designed to help taxpayers fine-tune their withholdings with greater precision than the standard Form W-4. The goal is to minimize large refunds or balance due scenarios at year-end when filing a tax return. Unlike tax return filing software that evaluate the prior year as a whole, TWE is used in the middle of a tax year where the remainder of the tax year is unknown. Because it operates *during* the tax year without the benefit of finalized W-2s or information returns(1099s), TWE relies on specific estimation logic and assumptions. By open-sourcing this project, we provide full transparency into those calculations, allowing taxpayers to see exactly how their W-4 recommendations are derived.

### Contributing
We value community feedback and invite public participation. Please see [CONTRIBUTING.md](./CONTRIBUTING.md) for details.

This codebase is dedicated to the public domain under the [Creative Commons Zero v1.0 Universal](LICENSE.md) license (CC0 1.0).

## Legal Disclaimer: Public Repository Access

> This repository contains draft and under-development source code for the IRS Tax Withholding Estimator. It is made available to the public solely for transparency, collaboration, and research purposes. The source code and associated content are not official IRS tools, and must not be used by taxpayers to estimate federal income tax withholding from their paychecks.
>
> **No Endorsement or Warranty**
>
> IRS does not endorse, maintain, or guarantee the accuracy, completeness, or functionality of the code in this repository. The IRS assumes no responsibility or liability for any use of the code by external parties, including individuals, developers, or organizations. This includes—but is not limited to—any tax consequences, computation errors, data loss, or other outcomes resulting from the use or modification of this code.
>
> **Official Tool for Tax Withholding Estimation**
>
> If you are a taxpayer seeking to estimate the federal income tax you want your employer to withhold from your paycheck, please use the official IRS Tax Withholding Estimator available at https://www.irs.gov/individuals/tax-withholding-estimator. If you are a taxpayer seeking to understand tax withholdings and the Internal Revenue Code (IRC), please review official IRS [Publications](https://www.irs.gov/publications), [Forms
](https://www.irs.gov/forms-instructions) or [guidance](https://www.irs.gov/newsroom/irs-guidance). Names and identifiers used in source code or other artifacts (e.g. the names of Facts) in this repository are not intended to reflect official interpretation of the IRC or replacement of IRS Publications, Forms or Guidance.
>
> Use of the code in this repository is at your own risk. This repository is not intended for production use or public consumption as a finalized product.


## Setup

If you are an IRS developer, first follow the instructions in the [IRS Onboarding Docs](./docs/onboarding/onboarding-irs.md).

If you are not a developer, follow the instructions in the [Non-Dev Onboarding Docs](./docs/onboarding/onboarding-nondev.md).

### Instructions

1. Install [Scala 3.7](https://www.scala-lang.org/download) and [sbt](https://www.scala-sbt.org/1.x/docs/Setup.html).
You may choose install these with Coursier, sdkman, or some other method of your choosing;
it shouldn't make a difference.
2. Download the [Fact Graph](https://github.com/IRS-Public/fact-graph) and run `make publish` in that repository
3. Return to this repository and run `make`
4. (Optional) Ensure that you have local installations of `xmllint` (via `libxml2`) and `npx` (via `npm`) command line tools, then run  `make ci-setup` to install the tools required for running the validations; this is useful if you plan to submit a PR.

Additional developer notes and tips for installing LSP integrations and the like can be found in the [Dev Onboarding Docs](./docs/onboarding/onboarding-dev.md).

## Development

Basic development commands are declared via Makefile.

The following commands are particularly useful for most development flows:
* `make` - Build TWE and start a static file server; automatically rebuild on changes
* `make twe` - Build and output TWE to the `/out` directory
* `make clean` - Clean all the build artifacts
* `make format` - Format the Scala and XML code
* `make ci` - Run CI checks locally

To see a list of _all_ available commands, run `make help`.

### Updating the vendored copy of the Fact Graph

The Fact Graph is used in two places: first as a declared Scala dependency in `build.sbt`, and second as a vendored JavaScript file that gets sent to the client.

If you make changes to the Fact Graph, and you want to propagate those changes, you need to do two things:

1. Run `make publish` in the Fact Graph repo

   Note: Scala.js compilation occurs during `make publish`. It can produce slightly different output even with identical source code. As a result, the vendored files (`factgraph-3.1.0.js` and `main.mjs.map`) may change after running this command even if no Fact Graph source was modified. If you haven't made any Fact Graph changes, these don't need to be committed.
2. Run `make copy-fg` in this repo

   Note: `make copy-fg` target assumes that the Fact Graph repo is located in `../fact-graph`.

## Audit Mode

TWE comes bundled with an "Audit Mode" that lets users see how TWE arrives at its calculations.
It can be toggled by running `enableAduitMode()` and `disableAuditMode()` in the browser console.

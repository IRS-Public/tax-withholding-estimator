# Tax Withholding Estimator (TWE)

### Overview
TWE is an online tool provided by the Internal Revenue Service (IRS) designed to help taxpayers estimate their federal tax withholdings while preparing [Form W-4](https://www.irs.gov/pub/irs-pdf/fw4.pdf) or [Form W-4P](https://www.irs.gov/pub/irs-pdf/fw4p.pdf). TWE is built to handle complex scenarios, including multiple jobs, self-employment income, and various credits or deductions. To better understand the math behind tax withholdings, go [here](./docs/taxes/withholdings-basics.md).

This codebase is actively maintained and represents a version of TWE (TWE 2.0) that went live on February 27, 2026. For a deep dive into the architecture and technical design choices for changes between the original version of TWE (TWE 1.0) and this version, start [here](./docs/adr/001-twe-architecture.md).

### What TWE is (and isn't)
TWE helps taxpayers avoid unexpected surprises when they file their taxes by reducing the likelihood of overwithholding (resulting in a large refund) or underwithholding (resulting in a balance due). The primary function of TWE is to generate a Form W-4 (for employees) or Form W-4P (for pension recipients), based on their current tax scenario and financial reality. When these forms are submitted, they instruct payors on exactly how much Federal Income Tax to withhold. Without specific values in Lines 3 through 4c of these forms, employers rely on default assumptions outlined in [Pub. 15](https://www.irs.gov/pub/irs-pdf/p15.pdf) and [Pub. 15-T](https://www.irs.gov/pub/irs-pdf/p15t.pdf), which can lead to inaccurate withholding for taxpayers with more complex tax profiles.

There are several key differences between TWE and preparing an annual tax return through tax filing software:
- **TWE is not a filing tool**: TWE does not send data to the IRS. Instead, it estimates tax liability and uses this estimation to pre-populate Forms W-4 and/or W-4P, which the taxpayer must manually provide to their employer and/or pension or annuity provider, respectively.
- **TWE is predictive, not historical**: A tax return looks backward at finalized data (W-2s and 1099s). In contrast, TWE operates during the tax year using year-to-date data and estimations for the remaining months. Because it relies on estimations and assumptions, the output is an approximation of what will happen for the rest of the year, not a certainty.
- **TWE is federal-only**: TWE does not address state or local income taxes or withholdings.

By open-sourcing this project, we aim to provide deeper insight into how TWE generates withholding recommendations and estimates tax liability. Our goal is to foster trust through transparency, allowing taxpayers to see exactly how the core tax engine processes data and applies year-to-date assumptions to generate the W-4 and W-4P.

### Contributing
Please see [CONTRIBUTING.md](./CONTRIBUTING.md) for details.

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
](https://www.irs.gov/forms-instructions) or [guidance](https://www.irs.gov/newsroom/irs-guidance). Names and identifiers used in source code or other artifacts (e.g. the names of Facts) in this repository are not intended to reflect official interpretation of the IRC or replacement of IRS Publications, Forms, or Guidance.
>
> Use of the code in this repository is at your own risk. This repository is not intended for production use or public consumption as a finalized product.


## Setup
- If you are an IRS employee, follow the instructions in the [IRS Onboarding Docs](./docs/onboarding/onboarding-irs.md).
- If you are a developer, follow the instructions in the [IRS Onboarding Docs](./docs/onboarding/onboarding-irs.md).
- If you are not a developer, follow the instructions in the [Non-Dev Onboarding Docs](./docs/onboarding/onboarding-nondev.md).

### Quickstart

1. Install the version of [Scala](https://www.scala-lang.org/download) specified in [build.sbt](./build.sbt) (currently 3.7.2) and [sbt](https://www.scala-sbt.org/1.x/docs/Setup.html).
   You may choose to install these with [Coursier](https://get-coursier.io/), [sdkman](https://sdkman.io/), [asdf](https://asdf-vm.com/), [mise](https://mise.jdx.dev/), or some other method of your choosing.
2. Download the [Fact Graph](https://github.com/IRS-Public/fact-graph) and run `make publish` in that repository
3. Return to this repository and run `make`
4. (Optional) Ensure that you have local installations of `xmllint` (via `libxml2`) and `npx` (via `npm`) command line tools, then run  `make ci-setup` to install the tools required for running the validations; this is useful if you plan to submit a PR.

Additional developer notes and tips for installing LSP integrations and the like can be found in the [Dev Onboarding Docs](./docs/onboarding/onboarding-dev.md).

### Development

Basic development commands are declared via Makefile.

The following commands are particularly useful for most development flows:
* `make` - Build TWE and start a static file server; automatically rebuild on changes
* `make twe` - Build and output TWE to the `/out` directory
* `make clean` - Clean all the build artifacts
* `make format` - Format the Scala and XML code
* `make ci` - Run CI checks locally

To see a list of _all_ available commands, run `make help`.


## Authorities
Legal foundations for this work include:
* Source Code Harmonization And Reuse in Information Technology Act" of 2024, Public Law 118 - 187
* OMB Memorandum M-16-21, “Federal Source Code Policy: Achieving Efficiency,
  Transparency, and Innovation through Reusable and Open Source Software,” August 8,
  2016
* Federal Acquisition Regulation (FAR) Part 27 – Patents, Data, and Copyrights
* Digital Government Strategy: “Digital Government: Building a 21st Century Platform to
  Better Serve the American People,” May 23, 2012
* Federal Information Technology Acquisition Reform Act (FITARA), December 2014
  (National Defense Authorization Act for Fiscal Year 2015, Title VIII, Subtitle D)
* E-Government Act of 2002, Public Law 107-347
* Clinger-Cohen Act of 1996, Public Law 104-106

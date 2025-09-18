### _TWE Engineering Onboarding Checklist_

**1. General**

- [ ] Read and follow the setup on the [ONBOARDING.md](https://github.com/IRSDigitalService/tax-withholding-estimator/blob/main/ONBOARDING.md) file in tax-withholding-estimator
  - [ ] Update things that are missing!
- [ ] Read the [Architecture ADR](https://github.com/IRSDigitalService/tax-withholding-estimator/blob/main/docs/twe-2.0-adr.md)

**2. Fact graph**

- [ ] Read through the [factgraph tutorial](https://github.com/IRSDigitalService/fact-graph/blob/main/shared/src/main/scala/_tutorial/01_introduction.worksheet.sc)
  - [ ] (optional) Additional [factgraph slidedeck](https://github.com/IRSDigitalService/internal-assets/blob/main/gitlab-wiki/uploads/c4cf92ad5c32ae6fa8a7f2d066e802e7/Tax_Code_to_Factgraph.pdf)
    - [ ] corresponding [video of the walkthrough](https://github.com/IRSDigitalService/internal-assets/blob/main/gitlab-wiki/uploads/6a29a0cc713dd6a8cb0f458854767313/FactGraph101ByShimona.mp4) of that deck (only the first 30 minutes are relevant)
- [ ] [Guide to placeholders](https://github.com/IRSDigitalService/internal-assets/blob/main/twe/fact-graph/guide-to-placeholders.md)
- [ ] [Debugging in the fact graph](https://github.com/IRSDigitalService/internal-assets/blob/main/twe/fact-graph/debugging.md)
- [ ] Review how we do testing
    * Our fact dictionary tests can be found [here](https://github.com/IRSDigitalService/tax-withholding-estimator/tree/main/src/test/scala/gov/irs/twe/factDictionary)
    * We have plans to add end-to-end browser testing, but that's intentionally punted for later
- [ ] Review these PRs to understand how certain things are done
  - [ ] [Understanding how to add new facts and have them display in the UI](https://github.com/IRSDigitalService/tax-withholding-estimator/pull/385)
- [ ] Read through the [FAQ](https://github.com/IRSDigitalService/internal-assets/blob/main/twe/fact-graph/faq.md)

**3. Optional Resources**
- [ ] [General taxes overview](https://github.com/IRSDigitalService/internal-assets/blob/main/twe/general-taxes-overview.md)

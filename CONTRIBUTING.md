# Contributing

* If you are a member of the public, see [members of the public](#members-of-the-public)
* If you are an active maintainer, see [maintainers](#maintainers)

## Members of the public


The primary purpose of this open-source repository is to provide taxpayers with full transparency into the logic and assumptions of TWE.

### Response Expectations
The maintainers will monitor activity and respond to community input as appropriate. Internal review cycles may fluctuate and may influence the timeline for addressing certain issues or PRs.

### How to Engage
A PR should only be opened after an issue has been opened and a PR has been solicited.

* **Issue Tickets:** If you identify potential defects, bugs, or logic errors, please open an issue using our **Bug Report** template. These reports are vital for us to identify improvements that can be escalated for internal review.
* **Pull Requests:** You are welcome to submit Pull Requests (PRs) using the provided **PR Template**.  PRs should only be opened after an issue has been opened and a PR has been solicited by a maintainer. Our maintainers will review and respond to them as capacity allows, though submission does not guarantee a merge.

### Submission Guidelines
To help our team investigate and escalate reports efficiently, please adhere to the following when opening an Issue or Pull Request:

1. **Use the Templates:** When you open a new issue or PR, GitHub will automatically provide a template. Please fill this out as completely as possible.
2. **Protect Privacy:** Do not include any personally identifiable information (PII) such as Social Security Numbers, exact addresses, or real-world employer identification numbers in your descriptions, code, or screenshots.
3. **Be Descriptive:** Clearly explain the "why" behind the change or the "how" behind a bug. For PRs, explain the mathematical or conditional changes made to the logic.


## Maintainers

This repository lives in the public domain in the United States (see [License](LICENSE.md)). Therefore, details about the repository _and its contributors_ are visible to the public.

In this model, individual maintainers are responsible for the quality of the code, commits, and other contributions to the repository.

For further understanding of the rationale behind open source, see [here](https://github.com/irs-public/fact-graph/blob/main/docs/oss/benefits.md).

### Know what is public

As a contributor to this repository, assume that any contribution and associated metadata is visible to the public. This includes:
* Contents of commits (files added, changed, or removed)
* Commit messages
* Pull requests
* Issues
* Comments
* Commit authors (name, email address, GitHub username)

Individual contributors are responsible for sharing only what they are comfortable with making available to the public domain.
As a result, individuals should configure their accounts and git configurations accordingly.

As a matter of policy, the IRS requires that internal contributors use their GitHub `no-reply` email address for authoring commits. See [ONBOARDING - Configure Commit Email Address](/ONBOARDING.md#configure-commit-email-address).

> [!WARNING]
> Sensitive PII and [SBU](https://en.wikipedia.org/wiki/Sensitive_but_unclassified) are not permitted _anywhere_ in open source repositories.
>
> Any accidental exposure of SBU or PII must be immediately reported and remediated as an incident.

### Best Practices

* Be intentional about good git hygiene, if not for the open source community for yourself and your fellow maintainers.
* Prefer the use of other Free and Open Source Software (FOSS) to expand and contribute back to the community.
* Use automated tooling for code formatting and basic linting.
* Enforce code accuracy and test health with automated checks for passing test coverage.
* Use only fake data or leverage "faker"-style libraries for generating realistic data for tests.
* Supplement automated tests/checks with thoughtful code review from peers.
* Perfection should not be the enemy of the good. Instead, iterations should aim for "better".

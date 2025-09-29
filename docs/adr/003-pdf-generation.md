# ADR-003: Client-Side PDF Generation for IRS Tax Forms

- **Status:** Approved
- **Date:** 2025-09-12

## Table of Contents
* [Problem Statement](#problem-statement)
  * [Goals](#goals)
  * [Non-Goals](#non-goals)
* [Decision](#decision-plain-javascript-with-pdf-lib)
* [Alternatives Considered](#alternatives-considered)
* [Risk Mitigation](#risk-mitigation)

## Primary author(s)
[primary authors]: #primary-authors

@rav-gov

## Problem Statement

The live Tax Withholding Estimator (TWE 1.0) currently allows users to download a pre-filled W-4 or W-4P with the recommended values. We need to incorporate the same functionality in TWE 2.0.

### Goals

- Generate pre-filled IRS tax forms ([W-4](https://www.irs.gov/pub/irs-pdf/fw4.pdf), [W-4 (SP)](https://www.irs.gov/pub/irs-pdf/fw4sp.pdf), and [W-4P](https://www.irs.gov/pub/irs-pdf/fw4p.pdf)) with user's calculated values
- Maintain client-side processing for unauthenticated frontend-only application
- Ensure generated PDFs are official IRS forms, not custom recreations
- Graceful error handling for network failures and invalid inputs
- Cross-browser compatibility
- Comprehensive test coverage for reliability

### Non-Goals

- **Server-side processing** - All PDF generation must remain client-side
- **Custom form creation** - Only official IRS PDF forms will be used
- **Form submission** - Only generation and download, not electronic filing
- **Multi-year support** - Initial implementation targets current tax year only
- **Advanced PDF features** - No signatures, annotations, or complex formatting

## Decision

We will implement client-side PDF generation using plain JavaScript and the [`pdf-lib` library](https://pdf-lib.js.org/). This approach will allow us to programmatically fetch official IRS PDF templates, fill them with user data, and enable downloads via the Browser Blob API. 'pdf-lib' is already in use for TWE 1.0, so we know it will work with the existing templates.

## Alternatives Considered

| Approach            | Pros                                      | Cons                                                   | Reason for Rejection                                |
|---------------------|-------------------------------------------|--------------------------------------------------------|-----------------------------------------------------|
| Server-side         | More power, easier testing.               | Violates unauthenticated architecture.                 | Architectural constraints.                          |
| jsPDF (Custom)      | Full control over layout.                 | Not official IRS forms, high compliance risk.          | Official forms are a requirement.                   |
| Scala.js            | Type safety, existing codebase integration. | Build complexity, larger bundle size.                 | Prioritizing maintainability and simplicity.       |
| Third-Party Service | Hosted infrastructure, advanced features. | External dependency, cost, privacy risks.              | Violates client-side requirement.                   |
| HTML-to-PDF         | Familiar HTML/CSS.                        | Not official IRS forms, print formatting issues.       | Official forms are a requirement.                   |

## Risk Mitigation

- **Form failures**: Provide clear error messages and manual completion guidance.
- **Invalid tax data** - Rely on implemented comprehensive input validation before PDF generation
- **IRS form URL changes**: Make template URLs configurable for easy updates.
- **Dependency updates** - Use a minimal package.json to enable Dependabot monitoring and version pinning, without introducing a full Node.js build system.

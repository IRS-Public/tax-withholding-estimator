# ADR-004: Add Internal Debugging Surfaces for TWE Flow Visibility

- **Status:** Accepted
- **Date:** 2026-05-14

## Primary author(s)
[primary authors]: #primary-authors

@jaredcunha

## Summary

### Issue

The Tax Withholding Estimator (TWE) uses Fact Graph-driven conditional logic to determine which fields are shown to the user based on their inputs. While this improves the user experience, it also means that much of the form surface is hidden during ordinary testing and review. As a result, it is harder to verify that fields are rendering correctly, ensure that content is accurate, and diagnose bugs in the flow.

### Context

Diagnosing bugs in TWE is arduous because it requires a lot of technical, manual work. Developers have to:

- Understand the flow logic well enough to construct a user scenario that surfaces the bug
- Use browser dev tools to inspect the fact graph state in `sessionStorage` and the conditions that control field visibility
- Correlate fact graph state with the visible form behavior to confirm a diagnosis

This work can be brittle and time-consuming, especially for complex bugs that require multiple steps to reproduce, and increases the burden on engineers who are debugging and conducting code reviews.

Additionally, external stakeholders, including Treasury and researchers, also need to review this behavior in realistic environments.

### Drivers

- We want a way to review all possible form fields and text content
- We want to inspect the conditions that control field visibility
- We want to track certain facts in the user's fact graph to ensure that they are being set correctly, and what information is used to set them
- We want to import or export the fact graph to a file for easier debugging and testing
- We want stakeholders and researchers to be able to review this behavior
- We want minimal impact on production code and no impact on the taxpayer experience

### Decision

We created two internal debugging surfaces to the Tax Withholding Estimator: an audit panel and an all-screens page.

The **audit panel** is the inspection surface. It will be a feature-rich layer that will expose the conditions that determine whether fields are shown, allow internal users to inspect selected facts and the information used to derive them, and support importing and exporting the fact graph for debugging. The audit panel will be available in production for external stakeholder review, but it will be clearly marked as an internal-only feature.

The **all-screens page** is the review surface. It will render all fields that could potentially appear in the flow regardless of user inputs or fact-graph conditions, making broad content review and debugging easier. The all-screens page will only be available in local development to avoid exposing too much internal functionality in production.

### Alternatives Considered

- **Not create an all-screens page** - This would mean that the audit panel would be the only way to see all fields, which would make it more difficult to test and debug the form. We decided against this because:
  - Having TWE's full form surface visible in a single place is valuable for content review and for getting a holistic view of the flow's behavior. This is especially important as the flow grows in complexity, and it helps people understand the full scope of the form.
- **Create a Direct File-style all-screens page** - This would use a design layout similar to the [all-screens page that Direct File used](https://github.com/IRS-Public/direct-file/blob/main/docs/engineering/working-on-client-app.md#all-screens), with each question in a section presented as a screen, rendered horizontally on the page. We decided against this for several reasons:
  - The TWE flow does not follow the same screen progression as Direct File
  - While it’s possible to achieve that type of layout with TWE, it would require significant rework of the flow structure specifically to accommodate this layout. Doing this would create unnecessary work and risk for a feature that is only used for internal review and debugging.
- **Make the audit panel available in development only** - This would reduce the code sent to production. We decided against it because:
  - It would not meet the need for external stakeholders and researchers to review conditional behavior and other audit mode features in production.
- **Combine the audit panel and all-screens into a single experience** - This would reduce the number of debugging surfaces, as well as more easily expose hidden content to external stakeholders. We decided against this because:
  - This would increase the risk of accidentally exposing audit features to taxpayers that could fundamentally change the experience
  - It would increase the amount of JavaScript sent to production, and increase the risk of bugs in audit features impacting the taxpayer experience

## Consequences

### Positive

- Internal users and external stakeholders have better visibility into conditional flow behavior.
- Testing and debugging are easier because reviewers can inspect field visibility conditions, fact state, and full form coverage more directly.
- Content review is faster because all potentially reachable fields can be reviewed without manually constructing many different user scenarios.

### Negative

- Production will include additional audit-related code paths, which must remain isolated from the normal taxpayer experience.
- Some taxpayers may discover the audit panel in production, so the feature must remain clearly marked as internal-only.
- Engineering will need to maintain separate audit and all-screens surfaces as the flow evolves.

### Follow-Up Obligations

- Keep clear boundaries between audit functionality and the main taxpayer flow.
- Ensure that audit mode continues to not change normal form behavior when disabled.

## Design decisions to capture

### Audit Panel

The audit panel will be enabled by default in local development. In production, it will be enabled explicitly by entering `enableAuditMode()` in the browser console. Conversely, audit mode can be disabled by entering `disableAuditMode()` in the browser console.

Initially, the audit panel was a section underneath the main form, but it was moved to a right-side panel to allow it to be visible alongside the form as users interact with it as they use the estimator. This allows reviewers to correlate the audit information with the visible form behavior more easily, which is especially important for understanding conditional behavior.

Because TWE is not a single-page application, users navigate across multiple URLs as they progress through the form, which would otherwise reset the panel settings on each page load. To address this, the audit panel uses its own `sessionStorage` key to store its state so that it can remain open and continue to function as users navigate through the flow, as one might expect in a single-page application.

When the audit panel was moved to the side, all media queries were updated to use container queries instead, which allows TWE to reflow according to the panel's width. The only exception to this is the step indicator, which is a component from the U.S. Web Design System that uses media queries.

The audit panel includes the following features:

- **Override date:** For testing, allows users to set a custom date different from the current date, which can be used to test time-sensitive behavior.
- **Display conditions toggle:** Visual display that wraps block items in an orange box, or sets inline conditions to the right of the text. This is to show how conditions are affecting the visibility of fields and content, and how some items are nested inside other conditional blocks.
- **Fact tracking:** Individual cards for facts, their descriptions, and the information used to derive them.
- **Fact graph import/export:** Allows developers and testers to import or export fact graphs to attach to bug reports and pull requests. The import feature includes JSON validation

The width of the audit can be resized by the user, and that width is also stored in `sessionStorage`. The width can also be adjusted with arrow keys.

Because the audit panel is available in production, it includes clear notice that it is intended for internal use.

### All Screens Page

The all-screens page will be available only in local development at `/app/tax-withholding-estimator/all-screens/index.html`. When a user navigates to this page, they will see all fields that could potentially be shown in the form, regardless of their inputs. Additionally, the all-screens page will include the same audit panel as described above, so that users can inspect conditions and facts while viewing all screens.

The all-screens page is implemented as a separate HTML file that imports the same JavaScript and CSS as the main form, but it uses a different Thymeleaf layout that renders all fields and includes its own JavaScript features.

This page also includes a feature that automatically opens all accordions on the page. For collections, it automatically creates one item for each set and opens its accordion, as well as any nested accordions within it.

### Shared features - display conditions

The audit panel and all-screens page will share the same implementation to display conditions for fields and text content. Initially, this implementation simply took the condition information stored in `data-` attributes and displayed them as CSS pseudo content.

However, this approach had one significant drawback for the audit panel. For facts inside collections, a collection ID is necessary to track a particular fact. Pseudo-content cannot be selected or copied to the clipboard. Instead, HTML elements are rendered into the page to make copying and pasting those collection IDs easier.

# ADR-005: Taxpayer-Facing Save & Resume (file export/import)

- **Status:** Proposed
- **Date:** 2026-10-14

## Summary

### Issue

The Tax Withholding Estimator (TWE) keeps a taxpayer's progress only in
`sessionStorage`. Closing the browser or tab loses all entered data, and there
is no way for a taxpayer to set the estimate aside and come back to it, or to
move it between devices. The ability to serialize and restore the Fact Graph
already exists, but ADR-004 deliberately scoped it to the **internal audit
panel** (clipboard copy / paste-into-textarea), explicitly *not* for taxpayers.

### Context

ADR-004 built export/import as an internal debugging surface and warned against
exposing audit features to taxpayers. This ADR revisits that boundary for the
**save/resume use case only** — not the rest of the audit panel (fact inspector,
condition overlay, override date, all-screens), which remain internal.

Product direction (Oct 2026) is to let taxpayers save and resume an estimate,
and to use a stable, documented file format so the same file can be imported
into other IRS tools we build.

### Decision

Add a taxpayer-facing **Save & resume** surface to the main TWE flow:

- **Save** downloads `twe-estimate.json`, a versioned wrapper around the Fact
  Graph JSON: `{ format: "irs-twe-estimate", version: 1, savedAt, factGraph }`.
- **Resume** reads a chosen file, validates the wrapper, confirms with the user
  (loading replaces current progress), then calls the existing
  `window.loadFactGraph()`.
- Implemented in a dedicated `save-resume.js` that **reuses** the existing
  `factGraph.toJson()` / `loadFactGraph()` plumbing — no new serialization.
- Presented as a header button beside the language switcher, opening a named
  native dialog with save/resume controls and a privacy note. On mobile, the
  button occupies its own row below the logo and language switcher.

This **amends ADR-004**: export/import of the Fact Graph is no longer
internal-only. The remainder of the audit panel stays internal-only and
flag-gated.

### Why a versioned file wrapper

A thin, named, versioned envelope (vs. a bare Fact Graph dump) lets any future
tool detect the format, reject unrelated files, and migrate across schema
versions. `save-resume.js` tolerates a bare Fact Graph on import for backward
compatibility with audit-panel copies.

### Alternatives Considered

- **Keep it audit-only (status quo / ADR-004):** rejected — does not meet the
  taxpayer save/resume need.
- **Server-side save (account-based):** rejected for now — TWE is anonymous and
  stores no PII server-side; a device-local file keeps that property and needs
  no backend or auth.
- **Clipboard copy/paste (as in audit panel):** rejected for taxpayers — poor
  UX and no durable artifact to move between devices.

## Consequences

### Positive
- Taxpayers can save and resume; work survives closing the browser.
- A documented, versioned interchange format usable by other IRS tools.
- Reuses shipped serialization; minimal new surface area.

### Negative / Risks
- The saved file contains the taxpayer's financial inputs. It stays on their
  device, but they are now responsible for a file that holds sensitive data —
  the UI must say so.
- Loading a file replaces current progress; mitigated with a confirm step.
- Importing arbitrary JSON must fail safe; `loadFactGraph` validates against the
  fact dictionary and errors are surfaced, not thrown into the form.

### Follow-Up Obligations
- **UX, Section 508, content, and privacy review** before enabling in
  production (this ADR is *Proposed* pending that review).
- Keep the file `format`/`version` documented wherever other tools consume it.
- Keep save/resume isolated from the internal audit panel surfaces.

## Primary author(s)

[primary authors]: #primary-authors

Added via TSOA IRS App team, October 2026.

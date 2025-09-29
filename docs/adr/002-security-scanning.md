# ADR-002: Adopt Static Security Scanning for Scala 3 and Thymeleaf-Based Static Sites

- **Status:** Approved
- **Date:** 2025-09-06

## Primary author(s)
[primary authors]: #primary-authors

@rav-gov

## Problem Statement

We need a consistent, low-noise, security-only scanning baseline across:

- Scala 3 codebases (JVM and Scala.js) — Tax Withholding Estimator 2.0 (Scala 3.7.2) and Fact Graph (Scala 3.3.6).
- The statically generated site from TWE 2.0 (HTML/JS/CSS), using Thymeleaf templates.

The solution must be static-only (no runtime hooks), integrate cleanly with CI, and provide actionable signals with minimal developer friction.

## Background

- Two active Scala 3 codebases span JVM and Scala.js, requiring a cross-platform approach and CI integration.
- SonarQube has poor support for Scala 3.
- The static site uses [Thymeleaf](https://www.thymeleaf.org/doc/tutorials/3.1/usingthymeleaf.html); validators must be template-aware while enforcing security constraints.

## Key Terms

- **SAST** — Static Application Security Testing.
- **ESLint (security rules)** — JavaScript linter with `security` plugin.
- **html-validate** — HTML linter with security rules.
- **Thymeleaf** — Server-side template engine using `th:*` attributes.
- **Semgrep** — Cross-language SAST tool with SARIF support.

## Goals and Non-Goals

### Goals

- Establish a static security scanning baseline for:
  - Scala 3 applications (JVM/JS) via Semgrep.
  - The static site via JS/HTML security linting.
- Fail CI based on clearly defined thresholds.
- Minimize developer friction while keeping signal quality high.

### Non-Goals

- General linting/formatting (e.g., Scalafmt, Prettier, Stylelint).
- Non-security Scala rules or code coverage.
- Dynamic/runtime scanning (DAST).
- Accessibility linting outside security context.

## Decision

Adopt a repo-wide security scanning baseline combining:

- Scala SAST via Semgrep
- Static site security validation via ESLint, html-validate, and Semgrep.

## Implementation Details

### Repository and Pull Request Protections

- Enable [Dependabot alerts](https://docs.github.com/code-security/dependabot/dependabot-alerts/about-dependabot-alerts).

## CI Enforcement Policy

- **Dependabot alerts**: Enabled at repo level.
- **html-validate**: Fail on severity `error`
- **ESLint**: Fail on severity `error`
- **Semgrep**: Fail on severity ≥ `WARNING` (`--severity WARNING --error`).

## CI Workflow for Security Checks

1. Run html-validate
2. Run eslint
3. Run Semgrep against all supported languages

## Suppressions

- Prefer config-based suppressions (downgraded rules).
- Inline suppressions allowed *only* with justification.
- Track ownership and review suppressions regularly.

## Custom Rule Maintenance

- html-validate rules live in `src/main/resources/twe/security` (`.htmlvalidate.json`).

## Version Pinning Strategy

- Pin GitHub Actions to specific SHAs tied to major releases.
  - *For example:* `Actions/setup-python@e797f83bcb11b83ae66e0230d6156d7c80228e7c #v6.0.0`
- Pin CLI tools because reproducibility is critical.

## Alternatives Considered

- **Scapegoat and/or Wartremover** - these tools have limited support for Scala 3.
  - *We should move to these tools as they catch up with Scala 3 support so we can remove the dependency on Python*
- **SonarQube for Scala** — Scala 3 support is incomplete.
- **Generic quality tools only (Scalafix/Scalafmt)** — Not security-focused.
- **HTMLHint instead of html-validate** — Less flexible.
- **Skip Semgrep and rely only on ESLint/html-validate** — Would miss Scala language security patterns
- **Use Retire.js to scan vendored / CDN JavaScript** - We don't rely on 3rd party scripts and do not plan to in the near future.
- **Use Gitleaks for secret scanning** — The application is unauthenticated and does not handle sensitive credentials or secrets, making secret scanning unnecessary at this stage.

## Risks

- Developer churn during initial enforcement.
- Potential false positives requiring tuning.
- Maintenance overhead for rule sets and tools.
- Relying on third-party Scala SAST tools which may lag language features.

## CI Performance Impact

### Before caching
- **Install Node (for ESLint and HTML-validate)**: 5s
- **Install HTML-validate**: 6s
- **Generate static site for scanning**: 12s
- **HTML security validation**: 1s
- **Install ESLint and ESLint Security Plugin**: 5s
- **JavaScript security linting**: 32s
- **Set up Python 3.12**: 0s
- **Install pipx 1.7.1**: 4s
- **Install Semgrep**: 26s
- **Semgrep security-focused static analysis**: 11s

- **Total**: *102 seconds (1 minute 42 seconds)*

### After caching
- **Install Node (for ESLint and HTML-validate)**: 4s
- **Install HTML-validate**: 5s
- **Generate static site for scanning**: 45s
- **HTML security validation**: 2s
- **Install ESLint and ESLint Security Plugin**: 4s
- **JavaScript security linting**: 1s
- **Set up Python 3.12**: 0s
- **Install pipx 1.7.1**: 7s
- **Restore or save pip packages from cache**: 1s
- **Install Semgrep**: 9s
- **Semgrep security-focused static analysis**: 8s

- **Total**: *86 seconds (1 minute 26 seconds)*

We'll monitor CI duration and prune rule sets or add path filters if necessary.

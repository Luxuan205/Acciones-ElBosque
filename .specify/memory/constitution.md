<!--
SYNC IMPACT REPORT
==================
Version change: 1.0.0 → 2.0.0 (MAJOR)
Reason for MAJOR bump: Core architectural principle redefined from
  "Microservice Autonomy" to "Module Cohesion". The architecture is a
  modular monolith, not a microservices system. Multiple principles and
  the Technology Standards section changed to remove distributed-systems
  assumptions (Feign clients, service registry, independent deployability
  per module). This is a backward-incompatible governance redefinition.

Modified principles:
  - I. Microservice Autonomy (NON-NEGOTIABLE)
    → I. Module Cohesion (NON-NEGOTIABLE)
    [Architectural framing fully replaced]
  - II. API Contract-First
    [Updated: now focuses on external HTTP contracts and module-boundary
     APIs; removed inter-service Feign/REST framing]
  - IV. Security & Compliance by Design
    [Updated: auth/security framed as a module, not a separate service;
     removed Feign client references]

Added sections: None
Removed sections: None (Technology Standards significantly rewritten)

Templates requiring updates:
  - .specify/templates/plan-template.md ✅ — Constitution Check gates are
    generic; compatible with updated principles
  - .specify/templates/spec-template.md ✅ — No constitution-specific
    mandatory sections changed
  - .specify/templates/tasks-template.md ✅ — Task categories still align;
    no microservice-specific task types were mandatory
  - .specify/templates/commands/*.md ✅ — No command files present

Follow-up TODOs:
  - Clarify whether eureka-server and gateway are infrastructure sidecars
    deployed alongside the monolith or are truly separate applications.
    Current constitution treats them as infrastructure components.
-->

# Acciones-ElBosque Constitution

## Core Principles

### I. Module Cohesion (NON-NEGOTIABLE)

The backend is a **modular monolith**: all business modules (auth, market-data,
orders, portfolio, audit-compliance, mailing, subscriptions) are part of a
single deployable application, organized by bounded context rather than by
deployment unit.

- Each module MUST have a clearly defined boundary: its own package namespace
  (`com.accioneselbosque.<module>`) and a public API interface (a Spring
  `@Service` or facade class) through which other modules access it.
- Modules MUST communicate with each other via in-process Java method calls
  through their public API, NOT via HTTP or message queues internal to the
  monolith.
- A module MUST NOT reach into another module's internal packages directly.
  Only the module's public facade is accessible from the outside.
- Each module MUST compile and its unit tests MUST pass independently
  (i.e., can be tested in isolation with mocked dependencies).
- The `gateway` and `eureka-server` components are treated as infrastructure
  sidecars, not as core application modules.

**Rationale**: Modular cohesion gives the team the organizational benefits of
bounded contexts without the operational overhead of distributed systems.
Clear module boundaries prevent the codebase from becoming an unstructured
"big ball of mud" as features are added.

### II. API Contract-First

All externally-facing HTTP contracts (exposed through the gateway) and all
cross-module Java interfaces MUST be defined before implementation begins on
the consuming side.

- New external endpoints MUST be documented in the feature's `contracts/`
  folder under `specs/[###-feature-name]/contracts/` before coding starts.
- Cross-module interfaces (Java interfaces/facades) MUST be agreed upon and
  written before the consuming module begins implementing against them.
- Breaking changes to an existing external contract or cross-module interface
  MUST be flagged in the PR description and require explicit team
  acknowledgment before merging.
- The gateway MUST remain the single entry point for all external traffic.

**Rationale**: Contract-first prevents integration failures discovered only at
demo time and makes parallel team development viable even within a monolith.

### III. Test-Before-Ship (NON-NEGOTIABLE)

No code MUST be merged to `develop` unless:

1. The PR author has confirmed local compilation succeeds
   (`mvn package -DskipTests=false`).
2. At least one automated test (unit or integration) covers the changed
   business logic.
3. All existing tests in the affected module still pass.

TDD is strongly encouraged (Red → Green → Refactor). Where TDD is applied it
MUST follow the sequence: write failing test → get team approval on test →
implement until green.

**Rationale**: The PR checklist already requires local testing. This principle
elevates that checklist item to a non-bypassable governance gate.

### IV. Security & Compliance by Design

Authentication and authorization are owned by the `auth` module and MUST NOT
be duplicated in other modules.

- All protected endpoints MUST validate JWT tokens through the gateway filter
  or the shared `auth` module's Spring Security configuration — never with
  ad-hoc string parsing inside a business module.
- The `audit-compliance` module MUST capture every state-changing operation
  (order placement, account changes, subscription changes) as an immutable
  audit record, invoked via its public facade from the relevant business module.
- No secrets, API keys, or passwords MUST be committed to the repository.
  Use environment variables or Spring Cloud Config (`configuration-service`).
- SonarCloud quality gates (configured in CI) MUST NOT be bypassed;
  a failing gate blocks the PR.

**Rationale**: This is a financial-domain application. Authentication holes and
missing audit trails are not cosmetic issues — they are critical failures.

### V. Simplicity & Conventional Workflow

Team workflow MUST follow the branching and commit conventions documented in
`README.md`. These conventions are binding, not advisory.

- Branch naming: `feature/HU-XX-<desc>`, `fix/<desc>`, `hotfix/<desc>`,
  `docs/<desc>` — no exceptions.
- Commit format: Conventional Commits (`<type>(<scope>): <description>`,
  max 72 chars first line, imperative infinitive in Spanish or English).
- One branch = one task. Mixed-concern commits MUST be split before merging.
- Direct pushes to `main` or `develop` are **prohibited**.
- All merges to `develop` MUST go through a Pull Request with the mandatory
  checklist completed.
- Complexity MUST be justified. If a design decision adds significant
  complexity, document the reason in the PR description.

**Rationale**: Uniform conventions make peer review tractable for a four-person
team and produce a clean, auditable git history for academic evaluation.

## Technology Standards

- **Language**: Java 17 (LTS) — no newer language features unless Spring Boot
  dependency support is confirmed.
- **Framework**: Spring Boot 3.x + Spring Cloud 2023.x. Spring Cloud components
  (`gateway`, `configuration-service`) are used as infrastructure; they do NOT
  imply a microservices deployment model.
- **Build**: Maven multi-module project. Each bounded-context module is a Maven
  module within the parent POM. Gradle is not permitted without team consensus.
- **Containerization**: Docker. A `docker-compose.yml` at the repository root
  MUST allow local end-to-end startup of the full application stack.
- **CI/CD**: GitHub Actions. Pipelines MUST at minimum build and test the full
  application. SonarCloud integration is required.
- **Module communication**: In-process Java calls only between business modules.
  HTTP is reserved for external clients (via the gateway) and third-party APIs.
- **Email**: The `mailing` module is the single point for all outbound email;
  other modules MUST invoke it via its public facade, never send email directly.

## Development Workflow

1. Pull latest `develop` before creating a branch.
2. Create a feature/fix branch from `develop` following naming conventions.
3. Implement with tests (see Principle III).
4. Open a Pull Request to `develop`; complete the PR checklist in `README.md`.
5. At least one teammate MUST review and approve before merge.
6. Delete the branch after a successful merge.
7. `main` is updated only at sprint close, via a PR from `develop`.
8. Hotfixes branch from `main` and MUST be merged back to both `main` and
   `develop`.

All work items SHOULD reference the corresponding Historia de Usuario (HU)
number in the branch name and commit message (e.g., `Refs #HU-03`).

## Governance

- This constitution supersedes all informal team agreements. Where conflict
  exists between a verbal agreement and this document, this document governs.
- **Amendment procedure**: Any principle may be amended by opening a PR that
  modifies this file, with a written rationale. All four team members MUST
  approve the PR before merging.
- **Versioning policy**: Follow semantic versioning.
  - MAJOR — principle removed or redefined in a backward-incompatible way.
  - MINOR — new principle or section added.
  - PATCH — clarifications, wording, or formatting fixes.
- **Compliance review**: At the start of each sprint, the team lead reviews
  open PRs for constitution compliance. Non-compliant PRs are blocked until
  the violation is resolved or a formal amendment is approved.
- **Guidance file**: For day-to-day development context and commands, refer
  to `README.md` and the `.specify/` directory.

**Version**: 2.0.0 | **Ratified**: 2026-05-10 | **Last Amended**: 2026-05-10

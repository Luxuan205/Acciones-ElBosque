# Implementation Plan: AB-41 — Gestión de Usuarios por el Administrador

**Branch**: `026-gestion-usuarios` | **Date**: 2026-05-13 | **Spec**: [spec.md](./spec.md)
**Input**: `specs/026-gestion-usuarios/spec.md`

## Summary

Extiende el módulo `auth` con endpoints administrativos para buscar, filtrar, ver, suspender,
desbloquear, cambiar roles y restablecer contraseñas de usuarios. Reutiliza la entidad `Investor`
existente y los nuevos estados SUSPENDED y BLOCKED del spec AB-16 (MFA). Al suspender, se
invalida la sesión activa del usuario eliminando sus `MfaSession` activas.

## Technical Context

**Language/Version**: Java 17
**Primary Dependencies**: Spring Boot 4.0.6, Spring Data JPA (Specification para filtros dinámicos), Lombok
**Storage**: PostgreSQL — ninguna tabla nueva; usa `investor`, `mfa_session`; añade campo `role` a `investor` (V30)
**Testing**: JUnit 5, @WebMvcTest, @DataJpaTest, Mockito
**Target Platform**: REST API backend
**Project Type**: Módulo `auth`
**Performance Goals**: Cambios de estado < 5 segundos (SC-001); búsquedas < 3 segundos para 10k usuarios (SC-004)
**Constraints**: No se puede suspender al último ADMIN; cambios de rol ADMIN requieren confirmación extra
**Scale/Scope**: Proyecto académico

## Constitution Check

| Principio | Requisito | Estado |
|-----------|-----------|--------|
| I. Module Cohesion | Gestión de usuarios en `auth`; auditoría enviada a `audit-compliance` in-process | ✅ PASS |
| II. API Contract-First | `contracts/admin-users-api.md` define CRUD administrativo de usuarios | ✅ PASS |
| III. Test-Before-Ship | Tests: suspender ACTIVE → no puede login; desbloquear BLOCKED → puede login; no puede eliminar último ADMIN | ✅ PASS |
| IV. Security & Compliance | Solo ADMIN; toda acción registrada en `audit-compliance` | ✅ PASS |
| V. Conventional Workflow | Rama `026-gestion-usuarios` | ✅ PASS |

**GATE PASSED — sin violaciones.**

## Project Structure

### Documentation (this feature)

```text
specs/026-gestion-usuarios/
├── plan.md
├── research.md
├── data-model.md
├── contracts/
│   └── admin-users-api.md
└── tasks.md
```

### Source Code (auth)

```text
backend/auth/src/main/java/com/accioneselbosque/auth/
├── controller/
│   └── AdminUserController.java          ← GET /admin/users, GET /admin/users/{id}, PATCH /admin/users/{id}/status, PATCH /admin/users/{id}/role, POST /admin/users/{id}/reset-password
├── service/
│   └── AdminUserService.java             ← buscar, suspender, desbloquear, cambiar rol, reset password
├── model/
│   └── InvestorRole.java                 ← INVESTOR | BROKER | ADMIN
└── repository/
    └── InvestorSpecification.java        ← Spring Data Specification para filtros dinámicos

app/src/main/resources/db/migration/
└── V30__add_role_to_investor.sql         ← columna role VARCHAR(20) DEFAULT 'INVESTOR'
```

**Structure Decision**: El campo `role` actualmente se incluye en el JWT pero no se persiste en
`investor`. V30 lo añade como columna persistida. La suspensión invalida la sesión del usuario
eliminando sus `mfa_session` activas. Para restablecimiento de contraseña se reutiliza
`VerificationToken` con un nuevo tipo `PASSWORD_RESET`.
**Input**: Feature specification from `/specs/[###-feature-name]/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

[Extract from feature spec: primary requirement + technical approach from research]

## Technical Context

<!--
  ACTION REQUIRED: Replace the content in this section with the technical details
  for the project. The structure here is presented in advisory capacity to guide
  the iteration process.
-->

**Language/Version**: [e.g., Python 3.11, Swift 5.9, Rust 1.75 or NEEDS CLARIFICATION]  
**Primary Dependencies**: [e.g., FastAPI, UIKit, LLVM or NEEDS CLARIFICATION]  
**Storage**: [if applicable, e.g., PostgreSQL, CoreData, files or N/A]  
**Testing**: [e.g., pytest, XCTest, cargo test or NEEDS CLARIFICATION]  
**Target Platform**: [e.g., Linux server, iOS 15+, WASM or NEEDS CLARIFICATION]
**Project Type**: [e.g., library/cli/web-service/mobile-app/compiler/desktop-app or NEEDS CLARIFICATION]  
**Performance Goals**: [domain-specific, e.g., 1000 req/s, 10k lines/sec, 60 fps or NEEDS CLARIFICATION]  
**Constraints**: [domain-specific, e.g., <200ms p95, <100MB memory, offline-capable or NEEDS CLARIFICATION]  
**Scale/Scope**: [domain-specific, e.g., 10k users, 1M LOC, 50 screens or NEEDS CLARIFICATION]

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

[Gates determined based on constitution file]

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)
<!--
  ACTION REQUIRED: Replace the placeholder tree below with the concrete layout
  for this feature. Delete unused options and expand the chosen structure with
  real paths (e.g., apps/admin, packages/something). The delivered plan must
  not include Option labels.
-->

```text
# [REMOVE IF UNUSED] Option 1: Single project (DEFAULT)
src/
├── models/
├── services/
├── cli/
└── lib/

tests/
├── contract/
├── integration/
└── unit/

# [REMOVE IF UNUSED] Option 2: Web application (when "frontend" + "backend" detected)
backend/
├── src/
│   ├── models/
│   ├── services/
│   └── api/
└── tests/

frontend/
├── src/
│   ├── components/
│   ├── pages/
│   └── services/
└── tests/

# [REMOVE IF UNUSED] Option 3: Mobile + API (when "iOS/Android" detected)
api/
└── [same as backend above]

ios/ or android/
└── [platform-specific structure: feature modules, UI flows, platform tests]
```

**Structure Decision**: [Document the selected structure and reference the real
directories captured above]

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., 4th project] | [current need] | [why 3 projects insufficient] |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient] |

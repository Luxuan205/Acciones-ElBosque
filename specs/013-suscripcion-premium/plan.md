# Implementation Plan: AB-18 — Activación de Suscripción Premium

**Branch**: `013-suscripcion-premium` | **Date**: 2026-05-13 | **Spec**: [spec.md](./spec.md)
**Input**: `specs/013-suscripcion-premium/spec.md`

## Summary

Gestión del ciclo de vida de la suscripción PREMIUM de un inversionista. Los campos
`subscription_type` (STANDARD/PREMIUM) y `subscription_expires_at` ya existen en la tabla
`investor` (V8). Este spec añade la lógica de activación, consulta de estado y degradación
automática mediante un job periódico. Todo en el módulo `auth`.

## Technical Context

**Language/Version**: Java 17
**Primary Dependencies**: Spring Boot 4.0.6, Spring Data JPA, Spring Scheduling (`@Scheduled`)
**Storage**: PostgreSQL — campos existentes en `investor`; nueva tabla `subscription_event` (V15)
**Testing**: JUnit 5, @WebMvcTest, @DataJpaTest, Mockito
**Target Platform**: REST API backend
**Project Type**: Módulo `auth`
**Performance Goals**: Activación efectiva < 5 segundos (SC-001); job de degradación 1 vez/24h (SC-002)
**Constraints**: Solo INVESTOR tiene restricción de suscripción; BROKER y ADMIN siempre tienen acceso completo
**Scale/Scope**: Proyecto académico

## Constitution Check

| Principio | Requisito | Estado |
|-----------|-----------|--------|
| I. Module Cohesion | Suscripción íntegramente en `auth`; otros módulos consultan vía facade `SubscriptionGate` | ✅ PASS |
| II. API Contract-First | `contracts/subscription-api.md` define POST /subscriptions/activate y GET /subscriptions/status | ✅ PASS |
| III. Test-Before-Ship | Tests: activar STANDARD→PREMIUM; ya-PREMIUM; job degrada vencidas; acceso bloqueado a no-premium | ✅ PASS |
| IV. Security & Compliance | Pago es externo; este módulo solo cambia estado tras confirmación; auditoría de cada activación | ✅ PASS |
| V. Conventional Workflow | Rama `013-suscripcion-premium` | ✅ PASS |

**GATE PASSED — sin violaciones.**

## Project Structure

### Documentation (this feature)

```text
specs/013-suscripcion-premium/
├── plan.md
├── research.md
├── data-model.md
├── contracts/
│   └── subscription-api.md
└── tasks.md
```

### Source Code (auth)

```text
backend/auth/src/main/java/com/accioneselbosque/auth/
├── controller/
│   └── SubscriptionController.java   ← POST /subscriptions/activate, GET /subscriptions/status
├── service/
│   ├── SubscriptionService.java      ← activar, consultar estado, renovar
│   └── SubscriptionExpiryJob.java    ← @Scheduled — degradar cuentas vencidas
├── model/
│   └── SubscriptionEvent.java        ← historial de activaciones/degradaciones
├── facade/
│   └── SubscriptionGate.java         ← isPremiumActive(investorId) — usado por otros módulos
├── dto/
│   ├── SubscriptionStatusResponse.java
│   └── ActivateSubscriptionResponse.java
└── repository/
    └── SubscriptionEventRepository.java

app/src/main/resources/db/migration/
└── V15__create_subscription_event_table.sql
```

**Structure Decision**: Los campos de suscripción ya están en `investor` (V8). Se añade
`subscription_event` para historial/auditoría. `SubscriptionGate` es la facade pública que
otros módulos (`market-data`, `notifications`) usan para verificar si el usuario es premium.
El job corre a medianoche UTC diariamente vía `@Scheduled`.

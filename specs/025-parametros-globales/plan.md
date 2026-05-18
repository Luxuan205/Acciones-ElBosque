# Implementation Plan: AB-40 — Gestión de Parámetros Globales del Sistema

**Branch**: `025-parametros-globales` | **Date**: 2026-05-13 | **Spec**: [spec.md](./spec.md)
**Input**: `specs/025-parametros-globales/spec.md`

## Summary

Extiende el módulo `configuration` con una tabla de parámetros globales del sistema (commission
rate, duración de suscripción, intentos de login, TTL de OTP, etc.). Los demás módulos leen los
parámetros in-process vía la facade `GlobalParameterService`. El historial de cambios es inmutable.
Solo el administrador puede modificar parámetros.

## Technical Context

**Language/Version**: Java 17
**Primary Dependencies**: Spring Boot 4.0.6, Spring Data JPA, Spring Cache (`@Cacheable`)
**Storage**: PostgreSQL — tablas `global_parameter` y `parameter_change_history` (V28–V29)
**Testing**: JUnit 5, @WebMvcTest, @DataJpaTest, Mockito
**Target Platform**: REST API backend
**Project Type**: Módulo `configuration`
**Performance Goals**: Cambios activos < 5 segundos (SC-001)
**Constraints**: Solo ADMIN; valores fuera de rango rechazados; historial inmutable
**Scale/Scope**: Proyecto académico

## Constitution Check

| Principio | Requisito | Estado |
|-----------|-----------|--------|
| I. Module Cohesion | Parámetros en `configuration`; otros módulos leen vía `GlobalParameterService` facade in-process | ✅ PASS |
| II. API Contract-First | `contracts/parameters-api.md` define GET/PUT /config/parameters | ✅ PASS |
| III. Test-Before-Ship | Tests: valor válido actualiza; valor fuera de rango rechazado; cambio aplica inmediatamente | ✅ PASS |
| IV. Security & Compliance | Solo ADMIN; cada cambio registrado en `parameter_change_history` | ✅ PASS |
| V. Conventional Workflow | Rama `025-parametros-globales` | ✅ PASS |

**GATE PASSED — sin violaciones.**

## Project Structure

### Source Code (configuration)

```text
backend/configuration/src/main/java/com/accioneselbosque/configuration/
├── controller/
│   └── GlobalParameterController.java    ← GET /config/parameters, PUT /config/parameters/{key}, GET /config/parameters/{key}/history
├── service/
│   └── GlobalParameterService.java       ← facade: getParameter(key), updateParameter(key, value, adminId)
├── model/
│   ├── GlobalParameter.java
│   └── ParameterChangeHistory.java
└── repository/
    ├── GlobalParameterRepository.java
    └── ParameterChangeHistoryRepository.java

app/src/main/resources/db/migration/
├── V28__create_global_parameter_table.sql
└── V29__create_parameter_change_history_table.sql
```

**Structure Decision**: Los parámetros se cargan en caché `@Cacheable` con TTL de 60 segundos.
Al modificar un parámetro, `GlobalParameterService` invalida la caché vía `@CacheEvict`. El
historial de cambios usa la misma estrategia de inmutabilidad que `audit_event`: no hay DELETE
en el repositorio. Los valores por defecto se insertan en V28 como seed data.

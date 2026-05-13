# Implementation Plan: AB-17 — Gestión de Perfil de Usuario

**Branch**: `002-gestion-perfil` | **Date**: 2026-05-10 | **Spec**: [spec.md](./spec.md)
**Input**: `specs/002-gestion-perfil/spec.md`

## Summary

El inversionista autenticado puede ver y editar datos personales editables (nombre,
teléfono), cambiar contraseña verificando la actual, y gestionar preferencias
(canal de notificaciones, idioma). Todo en `auth-security-service`; los cambios se
registran en auditoría interna.

## Technical Context

**Language/Version**: Java 17
**Primary Dependencies**: Spring Boot 4.0.6, Spring Security 6.x, Spring Data JPA, Lombok, Bean Validation
**Storage**: PostgreSQL — schema `auth_db` (Flyway)
**Testing**: JUnit 5, @WebMvcTest, @DataJpaTest, Mockito
**Target Platform**: REST API backend
**Project Type**: Módulo `auth-security-service`
**Performance Goals**: Perfil cargado y guardado en < 2 min (SC-001 spec)
**Constraints**: Correo y documento son read-only; historial de cambios para auditoría interna
**Scale/Scope**: Proyecto académico

## Constitution Check

| Principio | Requisito | Estado |
|-----------|-----------|--------|
| I. Module Cohesion | Perfil y preferencias en `auth-security-service`; sin llamadas cross-módulo | ✅ PASS |
| II. API Contract-First | `contracts/profile-api.md` antes de implementar el frontend | ✅ PASS |
| III. Test-Before-Ship | Tests de actualización, cambio de contraseña y preferencias | ✅ PASS |
| IV. Security & Compliance | JWT requerido; cambio de contraseña verifica la actual; historial de auditoría (FR-011) | ✅ PASS |
| V. Conventional Workflow | Rama `002-gestion-perfil` | ✅ PASS |

**GATE PASSED — sin violaciones.**

## Project Structure

### Documentation

```text
specs/002-gestion-perfil/
├── plan.md, research.md, data-model.md, quickstart.md
└── contracts/profile-api.md
```

### Source Code (auth-security-service)

```text
backend/auth/src/main/java/com/accioneselbosque/auth/
├── controller/
│   └── ProfileController.java          ← GET /auth/profile, PUT /auth/profile/personal,
│                                          PUT /auth/change-password, GET|PUT /auth/preferences
├── service/
│   ├── ProfileService.java
│   └── PasswordChangeService.java
├── model/
│   ├── Investor.java                   ← + campo phone (nullable)
│   ├── InvestorPreferences.java        ← notifChannel, language (1-to-1 con Investor)
│   └── ProfileChangeLog.java           ← auditoría: field, changedAt, investorId
├── repository/
│   ├── InvestorPreferencesRepository.java
│   └── ProfileChangeLogRepository.java
└── dto/
    ├── ProfileResponse.java
    ├── UpdatePersonalDataRequest.java  ← fullName, phone (opcionales individualmente)
    ├── ChangePasswordRequest.java      ← currentPassword, newPassword, confirmNewPassword
    └── UpdatePreferencesRequest.java   ← notifChannel, language
```

```text
db/migration/
├── V3__add_phone_to_investor.sql
├── V4__create_investor_preferences_table.sql
└── V5__create_profile_change_log_table.sql
```

**Structure Decision**: Módulo `auth-security-service`. Preferencias en tabla separada
(relación 1-to-1 con Investor) para extensibilidad. `ProfileChangeLog` registra cada
campo modificado con timestamp para auditoría interna.

# Implementation Plan: AB-38 — Auditoría y Cumplimiento Legal

**Branch**: `023-auditoria-legal` | **Date**: 2026-05-13 | **Spec**: [spec.md](./spec.md)
**Input**: `specs/023-auditoria-legal/spec.md`

## Summary

Primer spec del módulo `audit-compliance` (nuevo Maven module). Provee un log de auditoría
inmutable donde todos los módulos registran eventos significativos vía la facade
`AuditService.record(event)`. Los registros son de solo lectura; el administrador puede
consultarlos y exportarlos. Un job mensual archiva registros con más de 5 años.

## Technical Context

**Language/Version**: Java 17
**Primary Dependencies**: Spring Boot 4.0.6, Spring Data JPA, Spring Data Pageable
**Storage**: PostgreSQL — tabla `audit_event` (V27); `archived` flag para archivado
**Testing**: JUnit 5, @WebMvcTest, @DataJpaTest, Mockito
**Target Platform**: REST API backend
**Project Type**: Módulo `audit-compliance` (nuevo Maven module)
**Performance Goals**: Escritura < 1 segundo (SC-002); consultas < 10 segundos período 1 año (SC-005)
**Constraints**: Log completamente inmutable; solo ADMIN puede consultarlo (BROKER solo sus propios eventos)
**Scale/Scope**: Proyecto académico

## Constitution Check

| Principio | Requisito | Estado |
|-----------|-----------|--------|
| I. Module Cohesion | Auditoría en `audit-compliance`; todos los módulos usan `AuditFacade` in-process | ✅ PASS |
| II. API Contract-First | `contracts/audit-api.md` define GET /audit/events para el administrador | ✅ PASS |
| III. Test-Before-Ship | Tests: evento registrado correctamente; intento de modificar → error; consulta con filtros | ✅ PASS |
| IV. Security & Compliance | Solo ADMIN accede al log completo; registros no modificables | ✅ PASS |
| V. Conventional Workflow | Rama `023-auditoria-legal` | ✅ PASS |

**GATE PASSED — sin violaciones.**

## Project Structure

### Source Code (audit-compliance — nuevo módulo)

```text
backend/audit-compliance/src/main/java/com/accioneselbosque/audit/
├── controller/
│   └── AuditController.java              ← GET /audit/events (solo ADMIN/BROKER)
├── service/
│   ├── AuditService.java                 ← facade pública — record(AuditEventDto)
│   └── AuditArchiveJob.java              ← @Scheduled mensual — archivar >5 años
├── model/
│   ├── AuditEvent.java
│   └── AuditEventType.java               ← AUTH_SUCCESS, AUTH_FAILURE, ORDER_CREATED, etc.
└── repository/
    └── AuditEventRepository.java         ← findAll con filtros; NO métodos de modificación

backend/audit-compliance/pom.xml          ← nuevo módulo Maven

app/src/main/resources/db/migration/
└── V27__create_audit_event_table.sql
```

**Structure Decision**: `AuditService` acepta un `AuditEventRecord` (record inmutable) y persiste
el `AuditEvent`. No hay métodos de update/delete en `AuditEventRepository`. El endpoint solo
expone GET — no hay PUT, POST ni DELETE en `AuditController` para los registros.

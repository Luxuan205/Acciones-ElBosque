# Implementation Plan: AB-39 — Dashboard Directivo

**Branch**: `024-dashboard-directivo` | **Date**: 2026-05-13 | **Spec**: [spec.md](./spec.md)
**Input**: `specs/024-dashboard-directivo/spec.md`

## Summary

Vista de solo lectura para el administrador que agrega métricas operativas en tiempo real
(estado del mercado, órdenes activas, usuarios conectados) y resumen financiero por período
(volumen, comisiones, nuevos registros, suscripciones premium). Implementado en el módulo `app`
que ya agrega todos los módulos. No requiere nuevas tablas — consulta in-process a `orders`,
`portfolio`, `auth` y `configuration`.

## Technical Context

**Language/Version**: Java 17
**Primary Dependencies**: Spring Boot 4.0.6, módulos existentes in-process
**Storage**: PostgreSQL — sin nuevas tablas; consultas sobre tablas existentes
**Testing**: JUnit 5, @WebMvcTest, Mockito (facades mockeadas)
**Target Platform**: REST API backend
**Project Type**: Módulo `app`
**Performance Goals**: Dashboard carga < 5 segundos (SC-001); métricas actualizadas < 60 segundos (SC-002)
**Constraints**: Solo ADMIN; vista de solo lectura
**Scale/Scope**: Proyecto académico

## Constitution Check

| Principio | Requisito | Estado |
|-----------|-----------|--------|
| I. Module Cohesion | Dashboard en `app` que agrega in-process desde módulos existentes | ✅ PASS |
| II. API Contract-First | `contracts/dashboard-api.md` define GET /admin/dashboard | ✅ PASS |
| III. Test-Before-Ship | Tests: métricas correctas; acceso no-ADMIN bloqueado | ✅ PASS |
| IV. Security & Compliance | Exclusivo para ADMIN; solo lectura | ✅ PASS |
| V. Conventional Workflow | Rama `024-dashboard-directivo` | ✅ PASS |

**GATE PASSED — sin violaciones.**

## Project Structure

### Source Code (app)

```text
backend/app/src/main/java/com/accioneselbosque/app/
├── controller/
│   └── AdminDashboardController.java     ← GET /admin/dashboard, GET /admin/dashboard/summary
└── service/
    └── DashboardService.java             ← agrega métricas in-process desde otros módulos
```

**Structure Decision**: Sin nuevas tablas. `DashboardService` llama in-process a:
- `orders` → conteo de órdenes activas (PENDING + QUEUED)
- `portfolio` → saldo total en plataforma
- `auth` → usuarios activos (sesiones recientes, suscripciones PREMIUM activas, nuevos registros)
- `configuration` → estado del mercado (abierto/cerrado)
Las métricas se calculan en cada petición al dashboard; no hay caché en el MVP.

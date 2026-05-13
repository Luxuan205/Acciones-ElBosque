# Implementation Plan: AB-29 — Gestión de Horarios y Configuración de Mercados

**Branch**: `008-configuracion-mercados` | **Date**: 2026-05-10 | **Spec**: [spec.md](./spec.md)
**Input**: `specs/008-configuracion-mercados/spec.md`

## Summary

El administrador configura el horario bursátil (hora apertura/cierre, días hábiles)
y gestiona el calendario de festivos. El sistema cambia el estado del mercado
(OPEN/CLOSED) automáticamente. El módulo `configuration-service` expone el estado
actual para que `order` y otros módulos lo consulten in-process.

## Technical Context

**Language/Version**: Java 17
**Primary Dependencies**: Spring Boot 4.0.6, Spring Data JPA, Lombok, Bean Validation, Spring Scheduling
**Storage**: PostgreSQL — schema `config_db` (Flyway)
**Testing**: JUnit 5, @WebMvcTest, @DataJpaTest, Mockito
**Target Platform**: REST API backend
**Project Type**: Módulo `configuration-service`
**Performance Goals**: Cambios propagados a otros módulos en < 60s (SC-003)
**Constraints**: Solo admins modifican configuración; cambios en sesión activa aplican en próxima sesión; zona horaria UTC-5
**Scale/Scope**: Proyecto académico; un único mercado

## Constitution Check

| Principio | Requisito | Estado |
|-----------|-----------|--------|
| I. Module Cohesion | Configuración de mercado en `configuration-service`; expone `MarketStatusService` como interfaz pública para otros módulos | ✅ PASS |
| II. API Contract-First | `contracts/market-config-api.md` define endpoints de configuración | ✅ PASS |
| III. Test-Before-Ship | Tests de festivos, transición OPEN/CLOSED, control de acceso solo-admin | ✅ PASS |
| IV. Security & Compliance | Solo rol ADMIN puede modificar (SC-004 0% no-admins); cambios auditados | ✅ PASS |
| V. Conventional Workflow | Rama `008-configuracion-mercados` | ✅ PASS |

**GATE PASSED — sin violaciones.**

## Project Structure

### Source Code (configuration-service)

```text
backend/configuration/src/main/java/com/accioneselbosque/configuration/
├── controller/
│   └── MarketConfigController.java     ← GET|PUT /config/market/schedule
│                                          GET|POST /config/market/holidays
│                                          DELETE /config/market/holidays/{id}
│                                          GET /config/market/status           ← público para otros módulos
├── service/
│   ├── MarketScheduleService.java      ← CRUD horario + cálculo siguiente sesión
│   └── MarketStatusService.java        ← isMarketOpen() — consultado por otros módulos
├── model/
│   ├── MarketSchedule.java             ← openTime, closeTime, workingDays (bitmask o Set<DayOfWeek>)
│   └── MarketHoliday.java              ← date, description, type
├── repository/
│   ├── MarketScheduleRepository.java
│   └── MarketHolidayRepository.java
└── dto/
    ├── MarketScheduleDto.java
    ├── MarketHolidayDto.java
    └── MarketStatusDto.java            ← status (OPEN|CLOSED), nextOpen, nextClose
```

```text
db/migration/
├── V1__create_market_schedule_table.sql    ← seed: L-V 09:00–15:30 UTC-5
└── V2__create_market_holiday_table.sql
```

**Structure Decision**: `MarketStatusService.isMarketOpen()` es la interfaz pública
que consumen `order` y `market-data` in-process. El scheduler
(`@Scheduled`) evalúa el horario + festivos cada minuto para mantener el estado
consistente.

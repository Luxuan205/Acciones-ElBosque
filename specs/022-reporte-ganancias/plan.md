# Implementation Plan: AB-37 — Reporte de Ganancias y Pérdidas

**Branch**: `022-reporte-ganancias` | **Date**: 2026-05-13 | **Spec**: [spec.md](./spec.md)
**Input**: `specs/022-reporte-ganancias/spec.md`

## Summary

Primer spec del módulo `portfolio` (nuevo Maven module). Gestiona las posiciones abiertas del
inversionista y calcula ganancias/pérdidas realizadas y no realizadas. Las `Position` se crean
y actualizan cuando las órdenes del módulo `orders` son ejecutadas. El reporte se genera bajo
demanda consultando el precio actual en `market-data` in-process.

## Technical Context

**Language/Version**: Java 17
**Primary Dependencies**: Spring Boot 4.0.6, Spring Data JPA, Lombok
**Storage**: PostgreSQL — tablas `position` y `transaction` (V25–V26)
**Testing**: JUnit 5, @WebMvcTest, @DataJpaTest, Mockito
**Target Platform**: REST API backend
**Project Type**: Módulo `portfolio` (nuevo Maven module)
**Performance Goals**: Reporte generado < 5 segundos para 50 posiciones (SC-001); exportación < 30s (SC-004)
**Constraints**: Solo transacciones registradas en el sistema; moneda base COP
**Scale/Scope**: Proyecto académico

## Constitution Check

| Principio | Requisito | Estado |
|-----------|-----------|--------|
| I. Module Cohesion | P&L en `portfolio`; precio actual de `market-data` in-process; transacciones desde `orders` in-process | ✅ PASS |
| II. API Contract-First | `contracts/portfolio-api.md` define GET /portfolio/report y GET /portfolio/positions | ✅ PASS |
| III. Test-Before-Ship | Tests: precio promedio ponderado correcto; ganancia no realizada correcta; filtro por período | ✅ PASS |
| IV. Security & Compliance | JWT requerido; cada usuario solo ve su propio portafolio | ✅ PASS |
| V. Conventional Workflow | Rama `022-reporte-ganancias` | ✅ PASS |

**GATE PASSED — sin violaciones.**

## Project Structure

### Source Code (portfolio — nuevo módulo)

```text
backend/portfolio/src/main/java/com/accioneselbosque/portfolio/
├── controller/
│   └── PortfolioController.java          ← GET /portfolio/positions, GET /portfolio/report, GET /portfolio/report/export
├── service/
│   ├── PortfolioService.java             ← calcular P&L, precio promedio ponderado
│   └── PositionUpdateService.java        ← facade pública — actualizar posición al ejecutar orden
├── model/
│   ├── Position.java                     ← posición abierta en un símbolo
│   └── Transaction.java                  ← historial de transacciones (compras y ventas ejecutadas)
├── facade/
│   └── PortfolioFacade.java             ← getAvailableTitles(investorId, symbol) — usada por orders module
└── repository/
    ├── PositionRepository.java
    └── TransactionRepository.java

backend/portfolio/pom.xml                 ← nuevo módulo Maven

app/src/main/resources/db/migration/
├── V25__create_position_table.sql
└── V26__create_transaction_table.sql
```

**Structure Decision**: `PortfolioFacade.getAvailableTitles()` resuelve la dependencia circular con
`orders` — el módulo `orders` llama a `portfolio` in-process para saber cuántos títulos puede vender.
`PositionUpdateService` es llamado por `orders` cuando una orden es EXECUTED.

# Implementation Plan: AB-27 — Visualización de Portafolio de Inversiones

**Branch**: `006-portafolio-inversiones` | **Date**: 2026-05-10 | **Spec**: [spec.md](./spec.md)
**Input**: `specs/006-portafolio-inversiones/spec.md`

## Summary

El inversionista ve sus posiciones abiertas: símbolo, cantidad, precio promedio de
compra, precio actual de mercado, valor de posición y ganancia/pérdida. El módulo
`portfolio` persiste las posiciones; los precios actuales se enriquecen
consultando `market-data` in-process al construir el response.

## Technical Context

**Language/Version**: Java 17
**Primary Dependencies**: Spring Boot 4.0.6, Spring Data JPA, Lombok, Bean Validation
**Storage**: PostgreSQL — schema `portfolio_db` (Flyway)
**Testing**: JUnit 5, @WebMvcTest, @DataJpaTest, Mockito
**Target Platform**: REST API backend
**Project Type**: Módulo `portfolio`
**Performance Goals**: Portafolio en < 3s para 50 posiciones (SC-001); precios < 60s desfase (SC-002)
**Constraints**: Precio promedio ponderado para múltiples compras; solo portafolio propio
**Scale/Scope**: Proyecto académico

## Constitution Check

| Principio | Requisito | Estado |
|-----------|-----------|--------|
| I. Module Cohesion | Posiciones en `portfolio`; precio actual via interfaz pública de `market-data` | ✅ PASS |
| II. API Contract-First | `contracts/portfolio-api.md` define GET /portfolio/holdings | ✅ PASS |
| III. Test-Before-Ship | Tests de precio promedio ponderado, cálculo G/P, posición vacía | ✅ PASS |
| IV. Security & Compliance | JWT; 0% acceso a portafolio ajeno (SC-004) | ✅ PASS |
| V. Conventional Workflow | Rama `006-portafolio-inversiones` | ✅ PASS |

**GATE PASSED — sin violaciones.**

## Project Structure

### Source Code (portfolio)

```text
backend/portfolio/src/main/java/com/accioneselbosque/portfolio_query_service/
├── controller/
│   └── PortfolioController.java        ← GET /portfolio/holdings, GET /portfolio/summary
├── service/
│   ├── PortfolioService.java           ← agrega posiciones + enriquece con precios actuales
│   └── PositionCalculator.java         ← precio promedio ponderado, P&L absoluto y %
├── model/
│   └── Position.java                   ← investorId, symbol, quantity, avgBuyPrice, currency
├── repository/
│   └── PositionRepository.java         ← findByInvestorId(id)
└── dto/
    ├── PositionDto.java                ← + currentPrice, positionValue, pnlAmount, pnlPercent, dayChange
    └── PortfolioSummaryDto.java        ← totalValue, totalPnl, dayChange
```

```text
db/migration/
└── V3__create_position_table.sql
```

**Structure Decision**: `Position` almacena precio promedio ponderado (`avgBuyPrice`).
Precio actual se inyecta al construir `PositionDto` consultando `market-data`
in-process. El valor total y P&L del portafolio se calculan en memoria en `PortfolioService`.

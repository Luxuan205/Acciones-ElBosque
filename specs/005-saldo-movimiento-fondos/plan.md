# Implementation Plan: AB-26 — Consulta de Saldo y Movimiento de Fondos

**Branch**: `005-saldo-movimiento-fondos` | **Date**: 2026-05-10 | **Spec**: [spec.md](./spec.md)
**Input**: `specs/005-saldo-movimiento-fondos/spec.md`

## Summary

El inversionista consulta su saldo (total, reservado en órdenes activas/encoladas,
disponible) y el historial paginado de movimientos (depósitos, retiros, compras, ventas,
comisiones) filtrable por rango de fechas. Todo en `portfolio`.

## Technical Context

**Language/Version**: Java 17
**Primary Dependencies**: Spring Boot 4.0.6, Spring Data JPA, Lombok, Bean Validation, Spring Data Pageable
**Storage**: PostgreSQL — schema `portfolio_db` (Flyway)
**Testing**: JUnit 5, @WebMvcTest, @DataJpaTest, Mockito
**Target Platform**: REST API backend
**Project Type**: Módulo `portfolio`
**Performance Goals**: Historial en < 3s (SC-002); saldo actualizado en < 30s tras operación (SC-001)
**Constraints**: Paginación 20 registros/página; aislamiento total entre inversionistas; moneda COP
**Scale/Scope**: Proyecto académico

## Constitution Check

| Principio | Requisito | Estado |
|-----------|-----------|--------|
| I. Module Cohesion | Saldo y movimientos en `portfolio`; sin cross-module HTTP | ✅ PASS |
| II. API Contract-First | `contracts/balance-api.md` define GET /portfolio/balance y /movements | ✅ PASS |
| III. Test-Before-Ship | Tests de paginación, filtrado por fechas, aislamiento entre inversores | ✅ PASS |
| IV. Security & Compliance | JWT; 0% acceso a cuentas ajenas (SC-004) | ✅ PASS |
| V. Conventional Workflow | Rama `005-saldo-movimiento-fondos` | ✅ PASS |

**GATE PASSED — sin violaciones.**

## Project Structure

### Source Code (portfolio)

```text
backend/portfolio/src/main/java/com/accioneselbosque/portfolio_query_service/
├── controller/
│   └── BalanceController.java          ← GET /portfolio/balance
│                                          GET /portfolio/movements?from=&to=&page=&size=
├── service/
│   ├── BalanceService.java             ← saldo total, reservado y disponible
│   └── FundMovementService.java        ← historial paginado con filtros
├── model/
│   ├── AccountBalance.java             ← investorId, totalBalance, currency
│   └── FundMovement.java               ← investorId, type, amount, balanceAfter, createdAt, orderId (nullable)
├── repository/
│   ├── AccountBalanceRepository.java
│   └── FundMovementRepository.java     ← findByInvestorIdAndCreatedAtBetween(…, Pageable)
└── dto/
    ├── BalanceSummaryResponse.java     ← total, reserved, available
    └── FundMovementPageResponse.java   ← content: List<FundMovementDto>, totalPages, totalElements
```

```text
db/migration/
├── V1__create_account_balance_table.sql
└── V2__create_fund_movement_table.sql
```

**Structure Decision**: `AccountBalance` mantiene saldo actual. Cada `FundMovement`
registra el saldo resultante (`balanceAfter`) para hacer el historial self-contained.
El saldo reservado se calcula dinámicamente sumando órdenes ACTIVE/QUEUED del investor.

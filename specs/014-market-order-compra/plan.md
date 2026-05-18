# Implementation Plan: AB-19 — Generación de Market Order de Compra

**Branch**: `014-market-order-compra` | **Date**: 2026-05-13 | **Spec**: [spec.md](./spec.md)
**Input**: `specs/014-market-order-compra/spec.md`

## Summary

Primer spec del módulo `orders` (nuevo Maven module). Permite al inversionista colocar una
market order de compra: el sistema valida saldo disponible, reserva el monto estimado, registra
la orden con estado PENDING (en horario bursátil) o QUEUED (fuera de horario), y notifica al
inversionista. El precio de mercado lo provee `market-data` in-process.

## Technical Context

**Language/Version**: Java 17
**Primary Dependencies**: Spring Boot 4.0.6, Spring Data JPA, Lombok, Bean Validation
**Storage**: PostgreSQL — nuevo módulo `orders` con tablas `market_order` y `balance_reservation` (V16–V17)
**Testing**: JUnit 5, @WebMvcTest, @DataJpaTest, Mockito
**Target Platform**: REST API backend
**Project Type**: Módulo `orders` (nuevo Maven module)
**Performance Goals**: Flujo completo (selección → confirmación) < 30 segundos (SC-001)
**Constraints**: Saldo insuficiente → rechazo inmediato; QUEUED si mercado cerrado
**Scale/Scope**: Proyecto académico

## Constitution Check

| Principio | Requisito | Estado |
|-----------|-----------|--------|
| I. Module Cohesion | Órdenes en `orders`; precio consultado a `market-data` in-process; saldo reservado in-process | ✅ PASS |
| II. API Contract-First | `contracts/market-order-buy-api.md` define POST /orders/market/buy | ✅ PASS |
| III. Test-Before-Ship | Tests: compra con saldo suficiente; rechazo por saldo insuficiente; QUEUED fuera de horario | ✅ PASS |
| IV. Security & Compliance | JWT requerido (INVESTOR); auditoría de cada orden colocada vía `audit-compliance` facade | ✅ PASS |
| V. Conventional Workflow | Rama `014-market-order-compra` | ✅ PASS |

**GATE PASSED — sin violaciones.**

## Project Structure

### Documentation (this feature)

```text
specs/014-market-order-compra/
├── plan.md
├── research.md
├── data-model.md
├── contracts/
│   └── market-order-buy-api.md
└── tasks.md
```

### Source Code (orders — nuevo módulo)

```text
backend/orders/src/main/java/com/accioneselbosque/orders/
├── controller/
│   └── MarketOrderController.java      ← POST /orders/market/buy
├── service/
│   ├── MarketOrderService.java         ← validar saldo, reservar, crear orden, notificar
│   └── MarketHoursGate.java           ← isMarketOpen() → configuration facade
├── model/
│   ├── Order.java                      ← entidad base de orden
│   ├── OrderType.java                  ← MARKET_BUY, MARKET_SELL, LIMIT_BUY, LIMIT_SELL
│   ├── OrderStatus.java               ← QUEUED, PENDING, EXECUTED, CANCELLED, REJECTED
│   └── BalanceReservation.java        ← reserva de saldo ligada a la orden
├── dto/
│   ├── PlaceMarketBuyRequest.java
│   └── OrderResponse.java
└── repository/
    ├── OrderRepository.java
    └── BalanceReservationRepository.java

backend/orders/pom.xml                  ← nuevo módulo Maven

app/src/main/resources/db/migration/
├── V16__create_order_table.sql
└── V17__create_balance_reservation_table.sql
```

**Structure Decision**: El modelo `Order` es compartido por todos los specs de órdenes (014-018).
`BalanceReservation` bloquea el saldo estimado hasta ejecución o cancelación. `MarketHoursGate`
llama in-process al `configuration` module para saber si el mercado está abierto.

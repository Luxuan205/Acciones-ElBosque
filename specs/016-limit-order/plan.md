# Implementation Plan: AB-21 — Colocación de Limit Order

**Branch**: `016-limit-order` | **Date**: 2026-05-13 | **Spec**: [spec.md](./spec.md)
**Input**: `specs/016-limit-order/spec.md`

## Summary

Extiende el módulo `orders` con limit orders (compra y venta). Reutiliza `market_order` con
`order_type = LIMIT_BUY | LIMIT_SELL` y los campos `limit_price` y `expires_at` ya definidos
en V16. Un job periódico (`LimitOrderEvaluationJob`) comprueba si el precio actual alcanzó el
límite y genera la ejecución. Soporta GTC (Good Till Cancelled) y GTD (Good Till Date).

## Technical Context

**Language/Version**: Java 17
**Primary Dependencies**: Spring Boot 4.0.6, Spring Data JPA, Spring Scheduling
**Storage**: PostgreSQL — reutiliza `market_order` (V16), `balance_reservation` (V17), `title_reservation` (V18)
**Testing**: JUnit 5, @WebMvcTest, @DataJpaTest, Mockito
**Target Platform**: REST API backend
**Project Type**: Módulo `orders`
**Performance Goals**: 100% de limit orders con condición cumplida ejecutadas automáticamente (SC-002)
**Constraints**: GTC sin fecha de expiración; GTD expira en fecha indicada
**Scale/Scope**: Proyecto académico

## Constitution Check

| Principio | Requisito | Estado |
|-----------|-----------|--------|
| I. Module Cohesion | Limit orders en `orders`; precio consultado a `market-data` in-process | ✅ PASS |
| II. API Contract-First | `contracts/limit-order-api.md` define POST /orders/limit/buy y POST /orders/limit/sell | ✅ PASS |
| III. Test-Before-Ship | Tests: limit buy creada; condición cumplida → EXECUTED; expiración → CANCELLED | ✅ PASS |
| IV. Security & Compliance | JWT requerido; auditoría de cada orden y ejecución | ✅ PASS |
| V. Conventional Workflow | Rama `016-limit-order` | ✅ PASS |

**GATE PASSED — sin violaciones.**

## Project Structure

### Source Code (orders)

```text
backend/orders/src/main/java/com/accioneselbosque/orders/
├── controller/
│   └── LimitOrderController.java         ← POST /orders/limit/buy, POST /orders/limit/sell
├── service/
│   ├── LimitOrderService.java            ← validar, crear, cancelar limit orders
│   └── LimitOrderEvaluationJob.java      ← @Scheduled — evaluar condiciones de precio + expiración
└── dto/
    ├── PlaceLimitBuyRequest.java         ← symbol, quantity, limitPrice, expiresAt (optional)
    └── PlaceLimitSellRequest.java
```

**Structure Decision**: No requiere nuevas tablas — `market_order.limit_price` y `expires_at` ya
existen (V16). El job de evaluación corre cada 30 segundos durante horario bursátil.
GTC = `expires_at NULL`; GTD = `expires_at` con fecha específica.

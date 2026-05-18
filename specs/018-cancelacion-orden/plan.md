# Implementation Plan: AB-23 — Cancelación de Orden

**Branch**: `018-cancelacion-orden` | **Date**: 2026-05-13 | **Spec**: [spec.md](./spec.md)
**Input**: `specs/018-cancelacion-orden/spec.md`

## Summary

Añade cancelación de órdenes (market, limit, condicionales) al módulo `orders`. La cancelación
libera los recursos reservados (saldo o títulos), garantiza que si una orden se ejecuta y cancela
simultáneamente solo uno de los dos estados prevalece (idempotencia), y registra el evento en
auditoría. Incluye cancelación individual y masiva.

## Technical Context

**Language/Version**: Java 17
**Primary Dependencies**: Spring Boot 4.0.6, Spring Data JPA, `@Transactional` (optimistic locking)
**Storage**: PostgreSQL — añade columna `cancellation_reason` a `market_order` (V20)
**Testing**: JUnit 5, @WebMvcTest, @DataJpaTest, Mockito
**Target Platform**: REST API backend
**Project Type**: Módulo `orders`
**Performance Goals**: Cancelación + liberación de recursos < 5 segundos (SC-001)
**Constraints**: No se puede cancelar EXECUTED; race condition manejada con optimistic locking
**Scale/Scope**: Proyecto académico

## Constitution Check

| Principio | Requisito | Estado |
|-----------|-----------|--------|
| I. Module Cohesion | Cancelación íntegramente en `orders`; liberación de recursos in-process | ✅ PASS |
| II. API Contract-First | `contracts/cancel-order-api.md` define DELETE /orders/{id} y DELETE /orders | ✅ PASS |
| III. Test-Before-Ship | Tests: cancelar PENDING libera saldo; cancelar EXECUTED → error; cancelación masiva | ✅ PASS |
| IV. Security & Compliance | Solo el propietario puede cancelar su orden (valida investor_id); auditoría | ✅ PASS |
| V. Conventional Workflow | Rama `018-cancelacion-orden` | ✅ PASS |

**GATE PASSED — sin violaciones.**

## Project Structure

### Source Code (orders)

```text
backend/orders/src/main/java/com/accioneselbosque/orders/
├── controller/
│   └── OrderCancellationController.java   ← DELETE /orders/{id}, DELETE /orders (bulk)
├── service/
│   └── OrderCancellationService.java      ← cancelar, liberar reservas, notificar
└── dto/
    ├── CancellationResponse.java
    └── BulkCancellationResponse.java

app/src/main/resources/db/migration/
└── V20__add_cancellation_reason_to_order.sql
```

**Structure Decision**: `@Transactional` con optimistic locking (`@Version` en `Order.java`) para
manejar la race condition ejecución/cancelación simultánea. Al detectar `OptimisticLockException`,
se rechaza la cancelación con el estado actualizado de la orden.

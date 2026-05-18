# Implementation Plan: AB-20 — Generación de Market Order de Venta

**Branch**: `015-market-order-venta` | **Date**: 2026-05-13 | **Spec**: [spec.md](./spec.md)
**Input**: `specs/015-market-order-venta/spec.md`

## Summary

Extiende el módulo `orders` con market orders de venta. Comparte el modelo `Order` y la tabla
`market_order` definidos en AB-19. Valida que el inversionista posea suficientes títulos libres
(no reservados por otras órdenes activas), reserva los títulos con `TitleReservation`, y crea
la orden con estado PENDING o QUEUED según el horario bursátil.

## Technical Context

**Language/Version**: Java 17
**Primary Dependencies**: Spring Boot 4.0.6, Spring Data JPA, Lombok
**Storage**: PostgreSQL — nueva tabla `title_reservation` (V18); reutiliza `market_order` (V16)
**Testing**: JUnit 5, @WebMvcTest, @DataJpaTest, Mockito
**Target Platform**: REST API backend
**Project Type**: Módulo `orders`
**Performance Goals**: Flujo completo < 30 segundos (SC-001)
**Constraints**: Solo títulos libres (no reservados); monto neto = precio × cantidad − comisión
**Scale/Scope**: Proyecto académico

## Constitution Check

| Principio | Requisito | Estado |
|-----------|-----------|--------|
| I. Module Cohesion | Venta en `orders`; portafolio de títulos consultado a `portfolio` in-process | ✅ PASS |
| II. API Contract-First | `contracts/market-order-sell-api.md` define POST /orders/market/sell | ✅ PASS |
| III. Test-Before-Ship | Tests: venta con títulos suficientes; rechazo por títulos insuficientes; QUEUED fuera de horario | ✅ PASS |
| IV. Security & Compliance | JWT requerido; auditoría de cada orden | ✅ PASS |
| V. Conventional Workflow | Rama `015-market-order-venta` | ✅ PASS |

**GATE PASSED — sin violaciones.**

## Project Structure

### Source Code (orders)

```text
backend/orders/src/main/java/com/accioneselbosque/orders/
├── controller/
│   └── MarketOrderController.java   ← añadir POST /orders/market/sell (mismo controller que AB-19)
├── service/
│   └── MarketSellService.java       ← validar títulos, reservar, crear orden MARKET_SELL
├── model/
│   └── TitleReservation.java        ← reserva de títulos ligada a la orden de venta
└── repository/
    └── TitleReservationRepository.java

app/src/main/resources/db/migration/
└── V18__create_title_reservation_table.sql
```

**Structure Decision**: `TitleReservation` es el equivalente de `BalanceReservation` para las
órdenes de venta: bloquea los títulos hasta ejecución o cancelación. El portafolio de títulos
disponibles se consulta in-process al `portfolio` module.

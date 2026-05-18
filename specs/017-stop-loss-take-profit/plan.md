# Implementation Plan: AB-22 — Configuración de Stop-Loss y Take-Profit

**Branch**: `017-stop-loss-take-profit` | **Date**: 2026-05-13 | **Spec**: [spec.md](./spec.md)
**Input**: `specs/017-stop-loss-take-profit/spec.md`

## Summary

Añade órdenes condicionales de protección al módulo `orders`. Un `ConditionalOrder` define un
nivel de precio que, al ser alcanzado, genera automáticamente una market order de venta.
Stop-loss y take-profit para la misma posición se vinculan entre sí: al activarse uno, el otro
se cancela automáticamente (OCO — One Cancels the Other).

## Technical Context

**Language/Version**: Java 17
**Primary Dependencies**: Spring Boot 4.0.6, Spring Data JPA, Spring Scheduling
**Storage**: PostgreSQL — nueva tabla `conditional_order` (V19)
**Testing**: JUnit 5, @WebMvcTest, @DataJpaTest, Mockito
**Target Platform**: REST API backend
**Project Type**: Módulo `orders`
**Performance Goals**: Activación < 5 segundos tras alcanzar el precio (SC-002)
**Constraints**: Al activarse uno de un par OCO, el otro se cancela; solo posiciones activas
**Scale/Scope**: Proyecto académico

## Constitution Check

| Principio | Requisito | Estado |
|-----------|-----------|--------|
| I. Module Cohesion | Órdenes condicionales en `orders`; precio actual de `market-data` in-process | ✅ PASS |
| II. API Contract-First | `contracts/conditional-order-api.md` define CRUD de stop-loss y take-profit | ✅ PASS |
| III. Test-Before-Ship | Tests: SL activo → genera venta; TP activo cancela SL; posición cerrada cancela ambos | ✅ PASS |
| IV. Security & Compliance | JWT requerido; auditoría de creación y activación | ✅ PASS |
| V. Conventional Workflow | Rama `017-stop-loss-take-profit` | ✅ PASS |

**GATE PASSED — sin violaciones.**

## Project Structure

### Source Code (orders)

```text
backend/orders/src/main/java/com/accioneselbosque/orders/
├── controller/
│   └── ConditionalOrderController.java   ← CRUD stop-loss y take-profit
├── service/
│   ├── ConditionalOrderService.java      ← crear, modificar, cancelar órdenes condicionales
│   └── ConditionalOrderEvaluationJob.java ← @Scheduled — verificar precios cada 30s
├── model/
│   ├── ConditionalOrder.java
│   └── ConditionalOrderType.java         ← STOP_LOSS | TAKE_PROFIT
└── repository/
    └── ConditionalOrderRepository.java

app/src/main/resources/db/migration/
└── V19__create_conditional_order_table.sql
```

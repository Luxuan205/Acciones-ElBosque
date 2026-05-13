# Implementation Plan: AB-24 — Encolamiento de Orden Fuera de Horario Bursátil

**Branch**: `003-encolamiento-orden` | **Date**: 2026-05-10 | **Spec**: [spec.md](./spec.md)
**Input**: `specs/003-encolamiento-orden/spec.md`

## Summary

Al colocar una orden fuera del horario bursátil, `order` la persiste con
estado `QUEUED`. Un `@Scheduled` cron la procesa en FIFO al abrir el mercado. El
inversionista puede cancelar órdenes en estado QUEUED. El estado del mercado se
consulta al módulo `configuration-service` in-process.

## Technical Context

**Language/Version**: Java 17
**Primary Dependencies**: Spring Boot 4.0.6, Spring Data JPA, Lombok, Bean Validation, Spring Scheduling
**Storage**: PostgreSQL — schema `order_db` (Flyway)
**Testing**: JUnit 5, @WebMvcTest, @DataJpaTest, Mockito
**Target Platform**: REST API backend
**Project Type**: Módulo `order`
**Performance Goals**: 100% órdenes procesadas en < 5 min tras apertura (SC-002)
**Constraints**: Máx 10 órdenes en cola por inversionista; FIFO; cancelación solo de QUEUED
**Scale/Scope**: Proyecto académico

## Constitution Check

| Principio | Requisito | Estado |
|-----------|-----------|--------|
| I. Module Cohesion | Toda la lógica de orden en `order`; estado mercado leído via interfaz pública in-process | ✅ PASS |
| II. API Contract-First | `contracts/order-queue-api.md` define endpoints antes de implementar | ✅ PASS |
| III. Test-Before-Ship | Tests del scheduler, del límite de 10 órdenes y de cancelación | ✅ PASS |
| IV. Security & Compliance | JWT requerido; cada transición de estado queda auditada | ✅ PASS |
| V. Conventional Workflow | Rama `003-encolamiento-orden` | ✅ PASS |

**GATE PASSED — sin violaciones.**

## Project Structure

### Source Code (order)

```text
backend/order/src/main/java/com/accioneselbosque/order_service/
├── controller/
│   └── OrderController.java            ← POST /orders, DELETE /orders/{id}/cancel,
│                                          GET /orders?status=QUEUED
├── service/
│   ├── OrderService.java               ← crea orden; decide QUEUED vs ACTIVE según mercado
│   ├── OrderQueueProcessor.java        ← @Scheduled("cron") — ejecuta cola en apertura
│   └── MarketStatusService.java        ← isMarketOpen() consultando configuration-service
├── model/
│   ├── Order.java                      ← status: QUEUED|ACTIVE|EXECUTED|FAILED|CANCELLED
│   └── OrderStatus.java (enum)
├── repository/
│   └── OrderRepository.java           ← findByInvestorIdAndStatusOrderByCreatedAtAsc
└── dto/
    ├── CreateOrderRequest.java
    └── OrderResponse.java
```

```text
db/migration/
└── V1__create_order_table.sql
```

**Structure Decision**: `Order` tiene campo `status` (enum). El scheduler procesa
`WHERE status = 'QUEUED' ORDER BY created_at ASC` (FIFO) al detectar apertura del
mercado consultando `configuration-service`. Límite de 10 órdenes validado en
`OrderService` antes de persistir.

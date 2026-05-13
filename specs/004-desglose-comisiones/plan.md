# Implementation Plan: AB-25 — Visualización y Desglose de Comisiones

**Branch**: `004-desglose-comisiones` | **Date**: 2026-05-10 | **Spec**: [spec.md](./spec.md)
**Input**: `specs/004-desglose-comisiones/spec.md`

## Summary

Antes de confirmar una orden, `order` calcula y devuelve un desglose de costos:
precio unitario, cantidad, valor bruto, tasa de comisión (según suscripción del
inversionista), monto de comisión y total. El desglose es un DTO de cálculo (no se
persiste); la confirmación aplica exactamente los mismos montos.

## Technical Context

**Language/Version**: Java 17
**Primary Dependencies**: Spring Boot 4.0.6, Spring Data JPA, Lombok, Bean Validation
**Storage**: PostgreSQL — schema `order_db`; tabla `commission_rate` con tasas por tipo suscripción
**Testing**: JUnit 5, @WebMvcTest, Mockito (cálculo de comisión estándar vs premium)
**Target Platform**: REST API backend
**Project Type**: Módulo `order`
**Performance Goals**: Cálculo de desglose < 500ms (experiencia fluida en UI)
**Constraints**: Monto cobrado == monto mostrado en desglose (SC-002, 100% exactitud)
**Scale/Scope**: Proyecto académico

## Constitution Check

| Principio | Requisito | Estado |
|-----------|-----------|--------|
| I. Module Cohesion | Cálculo de comisión en `order`; tasa leída de DB local | ✅ PASS |
| II. API Contract-First | `contracts/commission-api.md` define POST /orders/preview | ✅ PASS |
| III. Test-Before-Ship | Tests para suscripción estándar (1.5%) y premium (0.8%), fondos insuficientes | ✅ PASS |
| IV. Security & Compliance | JWT requerido; monto del desglose vinculante al confirmar | ✅ PASS |
| V. Conventional Workflow | Rama `004-desglose-comisiones` | ✅ PASS |

**GATE PASSED — sin violaciones.**

## Project Structure

### Source Code (order)

```text
backend/order/src/main/java/com/accioneselbosque/order_service/
├── controller/
│   └── OrderPreviewController.java     ← POST /orders/preview
├── service/
│   └── CommissionCalculatorService.java ← calcular(symbol, qty, price, subscriptionType)
├── model/
│   └── CommissionRate.java             ← subscriptionType (STANDARD|PREMIUM), ratePercent
├── repository/
│   └── CommissionRateRepository.java   ← findBySubscriptionType(type)
└── dto/
    ├── OrderPreviewRequest.java        ← symbol, quantity, orderType, investorId
    └── OrderPreviewResponse.java       ← unitPrice, quantity, grossValue, ratePercent,
                                           commissionAmount, netTotal, subscriptionType
```

```text
db/migration/
└── V2__create_commission_rate_table.sql
    -- INSERT: STANDARD 1.5%, PREMIUM 0.8%
```

**Structure Decision**: `OrderPreviewResponse` es puramente un DTO de cálculo. Al
confirmar la orden, el servicio recalcula con los mismos parámetros para garantizar
exactitud — no se reutiliza el preview del cliente (anti-tampering).

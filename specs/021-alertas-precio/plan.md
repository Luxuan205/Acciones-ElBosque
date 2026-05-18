# Implementation Plan: AB-35 — Alertas de Precio Personalizadas

**Branch**: `021-alertas-precio` | **Date**: 2026-05-13 | **Spec**: [spec.md](./spec.md)
**Input**: `specs/021-alertas-precio/spec.md`

## Summary

Extiende `notifications` con alertas de precio personalizadas para inversionistas PREMIUM.
Dos tipos: umbral absoluto (precio alcanza X) y variación porcentual (precio varía ±X% desde
el precio de referencia). Un job periódico evalúa las alertas activas contra los precios del
`market-data` module y despacha notificaciones. Una vez disparada, la alerta pasa a TRIGGERED
hasta que el usuario la reactive.

## Technical Context

**Language/Version**: Java 17
**Primary Dependencies**: Spring Boot 4.0.6, Spring Data JPA, Spring Scheduling
**Storage**: PostgreSQL — tabla `price_alert` (V24)
**Testing**: JUnit 5, @WebMvcTest, @DataJpaTest, Mockito
**Target Platform**: REST API backend
**Project Type**: Módulo `notifications`
**Performance Goals**: Activación < 15 segundos del evento de precio (SC-002); 100% PREMIUM monitoreadas (SC-001)
**Constraints**: Solo usuarios PREMIUM; alerta vuelve a TRIGGERED tras dispararse (no se repite automáticamente)
**Scale/Scope**: Proyecto académico

## Constitution Check

| Principio | Requisito | Estado |
|-----------|-----------|--------|
| I. Module Cohesion | Alertas de precio en `notifications`; precio consultado a `market-data` in-process; gate PREMIUM consultado a `auth` in-process | ✅ PASS |
| II. API Contract-First | `contracts/price-alerts-api.md` define CRUD de alertas de precio | ✅ PASS |
| III. Test-Before-Ship | Tests: BASIC bloqueado; PREMIUM crea alerta; condición cumplida → TRIGGERED; suscripción vencida → SUSPENDED | ✅ PASS |
| IV. Security & Compliance | JWT + gate PREMIUM; historial completo | ✅ PASS |
| V. Conventional Workflow | Rama `021-alertas-precio` | ✅ PASS |

**GATE PASSED — sin violaciones.**

## Project Structure

### Source Code (notifications)

```text
backend/notifications/src/main/java/com/accioneselbosque/notifications/
├── controller/
│   └── PriceAlertController.java          ← CRUD alertas de precio
├── service/
│   ├── PriceAlertService.java             ← CRUD + gate PREMIUM
│   └── PriceAlertEvaluationJob.java       ← @Scheduled(30s) — evaluar condiciones
├── model/
│   ├── PriceAlert.java
│   ├── PriceAlertType.java                ← ABSOLUTE | PERCENTAGE
│   └── PriceAlertStatus.java             ← ACTIVE | TRIGGERED | INACTIVE | SUSPENDED
└── repository/
    └── PriceAlertRepository.java

app/src/main/resources/db/migration/
└── V24__create_price_alert_table.sql
```

**Structure Decision**: El job de evaluación corre cada 30 segundos durante horario bursátil.
`SubscriptionGate.isPremiumActive()` del módulo `auth` se llama al crear/reactivar una alerta.
Al vencer la suscripción premium, el job de degradación de `auth` publica un evento que
`PriceAlertService` escucha para suspender las alertas activas del usuario.

# Implementation Plan: AB-34 — Alertas de Mercado

**Branch**: `020-alertas-mercado` | **Date**: 2026-05-13 | **Spec**: [spec.md](./spec.md)
**Input**: `specs/020-alertas-mercado/spec.md`

## Summary

Extiende el módulo `notifications` con suscripciones a alertas de eventos de mercado: apertura,
cierre, suspensión de negociación y volumen inusual. El módulo `market-data` publica eventos
in-process al ocurrir estos eventos; el `notifications` module los recibe y despacha al canal
preferido del inversionista según sus suscripciones activas.

## Technical Context

**Language/Version**: Java 17
**Primary Dependencies**: Spring Boot 4.0.6, Spring Data JPA, Spring Events (`ApplicationEventPublisher`)
**Storage**: PostgreSQL — tabla `market_alert_subscription` (V23)
**Testing**: JUnit 5, @WebMvcTest, @DataJpaTest, Mockito
**Target Platform**: REST API backend
**Project Type**: Módulo `notifications`
**Performance Goals**: 95% de alertas entregadas < 30 segundos del evento (SC-002)
**Constraints**: Alertas de precio personalizadas en AB-35; este spec cubre eventos de mercado generales
**Scale/Scope**: Proyecto académico

## Constitution Check

| Principio | Requisito | Estado |
|-----------|-----------|--------|
| I. Module Cohesion | Suscripciones en `notifications`; eventos publicados por `market-data` in-process via Spring Events | ✅ PASS |
| II. API Contract-First | `contracts/market-alerts-api.md` define CRUD de suscripciones | ✅ PASS |
| III. Test-Before-Ship | Tests: suscriptor recibe alerta de apertura; modificar umbral; desactivar → no alerta | ✅ PASS |
| IV. Security & Compliance | JWT requerido; historial de alertas accesible | ✅ PASS |
| V. Conventional Workflow | Rama `020-alertas-mercado` | ✅ PASS |

**GATE PASSED — sin violaciones.**

## Project Structure

### Source Code (notifications)

```text
backend/notifications/src/main/java/com/accioneselbosque/notifications/
├── controller/
│   └── MarketAlertController.java         ← CRUD suscripciones de alertas de mercado
├── service/
│   ├── MarketAlertService.java            ← crear/modificar/cancelar suscripciones
│   └── MarketAlertDispatcher.java         ← @EventListener — recibe eventos de market-data y notifica
├── model/
│   ├── MarketAlertSubscription.java
│   └── MarketAlertType.java               ← MARKET_OPEN | MARKET_CLOSE | TRADING_SUSPENDED | UNUSUAL_VOLUME
└── repository/
    └── MarketAlertSubscriptionRepository.java

app/src/main/resources/db/migration/
└── V23__create_market_alert_subscription_table.sql
```

**Structure Decision**: `market-data` publica un `ApplicationEvent` (Spring) cuando ocurre un
evento de mercado. `MarketAlertDispatcher` en `notifications` escucha ese evento in-process
y consulta `MarketAlertSubscriptionRepository` para encontrar los suscriptores activos.

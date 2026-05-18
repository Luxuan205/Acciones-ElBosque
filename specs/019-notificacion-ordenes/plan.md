# Implementation Plan: AB-33 — Notificación de Estado de Órdenes

**Branch**: `019-notificacion-ordenes` | **Date**: 2026-05-13 | **Spec**: [spec.md](./spec.md)
**Input**: `specs/019-notificacion-ordenes/spec.md`

## Summary

Primer spec del módulo `notifications` (nuevo Maven module). Establece la infraestructura de
notificaciones: envío de emails por cambios de estado de órdenes, historial de notificaciones,
reintentos automáticos. Los módulos `orders`, `audit-compliance`, etc. invocan la facade
`NotificationService` in-process. El canal push se deja como stub para implementación futura.

## Technical Context

**Language/Version**: Java 17
**Primary Dependencies**: Spring Boot 4.0.6, Spring Mail, Spring Data JPA, Spring Retry
**Storage**: PostgreSQL — tablas `notification` y `notification_attempt` (V21–V22)
**Testing**: JUnit 5, @WebMvcTest, @DataJpaTest, Mockito, GreenMail (SMTP embebido)
**Target Platform**: REST API backend
**Project Type**: Módulo `notifications` (nuevo Maven module)
**Performance Goals**: 95% entregadas < 10 segundos del evento (SC-002); retención 12 meses (SC-004)
**Constraints**: Canal preferido del inversionista (EMAIL | PUSH | BOTH); default = EMAIL
**Scale/Scope**: Proyecto académico

## Constitution Check

| Principio | Requisito | Estado |
|-----------|-----------|--------|
| I. Module Cohesion | Notificaciones en `notifications`; otros módulos usan `NotificationFacade` in-process | ✅ PASS |
| II. API Contract-First | `contracts/notifications-api.md` define GET /notifications (historial) | ✅ PASS |
| III. Test-Before-Ship | Tests: orden EXECUTED → notificación enviada; reintentos en fallo; historial correcto | ✅ PASS |
| IV. Security & Compliance | Historial solo accesible por el propio inversionista; auditoría de envíos | ✅ PASS |
| V. Conventional Workflow | Rama `019-notificacion-ordenes` | ✅ PASS |

**GATE PASSED — sin violaciones.**

## Project Structure

### Source Code (notifications — nuevo módulo)

```text
backend/notifications/src/main/java/com/accioneselbosque/notifications/
├── controller/
│   └── NotificationController.java       ← GET /notifications (historial del inversionista)
├── service/
│   ├── NotificationService.java          ← facade pública — sendOrderNotification(event)
│   ├── EmailNotificationSender.java      ← envío vía Spring Mail
│   └── PushNotificationSender.java       ← stub (push no implementado en MVP)
├── model/
│   ├── Notification.java
│   ├── NotificationAttempt.java
│   └── NotificationEventType.java        ← ORDER_EXECUTED, ORDER_CANCELLED, ORDER_REJECTED, etc.
├── dto/
│   └── NotificationDto.java
└── repository/
    ├── NotificationRepository.java
    └── NotificationAttemptRepository.java

backend/notifications/pom.xml             ← nuevo módulo Maven

app/src/main/resources/db/migration/
├── V21__create_notification_table.sql
└── V22__create_notification_attempt_table.sql
```

**Structure Decision**: `NotificationService` es la facade que todos los módulos usan para enviar
notificaciones. `NotificationAttempt` registra cada intento de entrega (éxito/fallo/reintento).
`InvestorPreferences.notifChannel` (ya en `auth`) determina el canal.

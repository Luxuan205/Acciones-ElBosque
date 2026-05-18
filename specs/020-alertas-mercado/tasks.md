# Tasks: AB-34 — Alertas de Mercado

**Input**: `specs/020-alertas-mercado/` (plan.md, spec.md, data-model.md, research.md, contracts/market-alerts-api.md)
**Module**: `notifications` — `com.accioneselbosque.notifications`
**Branch**: `020-alertas-mercado`
**Prerequisito**: AB-33 (módulo `notifications` base y canal de entrega disponibles); AB-28 (`market-data` publica `ApplicationEvent` de mercado)

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Puede correr en paralelo
- **[US1]**: Suscripción a alertas de eventos de mercado (P1)
- **[US2]**: Gestión de suscripciones a alertas (P2)

---

## Phase 1: Setup — Migración DB y estructura del módulo

- [x] T001 Crear migración `backend/app/src/main/resources/db/migration/V23__create_market_alert_subscription_table.sql`
- [x] T002 Crear estructura de directorios del módulo `backend/notifications/src/main/java/com/accioneselbosque/notifications/` con subdirectorios `controller/`, `service/`, `model/`, `repository/`, `dto/`, `exception/`

**Checkpoint Setup**: Migración V23 aplicada; estructura de paquetes lista.

---

## Phase 2: Foundational — Entidades, repositorios, DTOs, excepciones

**⚠️ CRÍTICO**: Nada de las historias de usuario puede comenzar hasta completar esta fase.

- [x] T003 [P] Crear enum `MarketAlertType` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/model/MarketAlertType.java`
- [x] T004 [P] Crear entidad `MarketAlertSubscription` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/model/MarketAlertSubscription.java`
- [x] T005 Crear repositorio `MarketAlertSubscriptionRepository` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/repository/MarketAlertSubscriptionRepository.java`
- [x] T006 [P] Crear DTOs `CreateMarketAlertRequest`, `UpdateMarketAlertRequest`, `MarketAlertSubscriptionDto` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/dto/`
- [x] T007 [P] Crear excepción `MarketAlertNotFoundException` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/exception/MarketAlertNotFoundException.java`
- [x] T008 [P] Crear excepción `SymbolRequiredException` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/exception/SymbolRequiredException.java`
- [x] T009 [P] Crear excepción `ThresholdRequiredException` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/exception/ThresholdRequiredException.java`
- [x] T010 Crear `GlobalExceptionHandler` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/exception/GlobalExceptionHandler.java`

**Checkpoint Foundational**: Entidades, repositorio, DTOs y excepciones disponibles.

---

## Phase 3: User Story 1 — Suscripción a alertas de eventos de mercado (Priority: P1) 🎯 MVP

**Goal**: El inversionista puede suscribirse a alertas de apertura/cierre de mercados y recibirlas por su canal preferido cuando el sistema detecta el evento.

**Independent Test**: `POST /notifications/market-alerts` con tipo MARKET_OPEN → 201 con suscripción creada; evento de apertura publicado → notificación enviada.

- [x] T011 [P] [US1] Crear POJO `MarketEvent` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/model/MarketEvent.java`
- [x] T012 [P] [US1] Implementar `MarketAlertService.subscribe()` con validaciones de negocio en `backend/notifications/src/main/java/com/accioneselbosque/notifications/service/MarketAlertService.java`
- [x] T013 [US1] Implementar `MarketAlertDispatcher` con `@EventListener` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/service/MarketAlertDispatcher.java` (depende de T012)
- [x] T014 [US1] Implementar endpoint `POST /notifications/market-alerts` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/controller/MarketAlertController.java` (depende de T012)

**Checkpoint US1**: Suscripción creada exitosamente; evento de mercado dispara notificación al canal preferido del inversionista.

---

## Phase 4: User Story 2 — Gestión de suscripciones a alertas (Priority: P2)

**Goal**: El inversionista puede consultar, actualizar y eliminar sus suscripciones activas de alertas de mercado.

**Independent Test**: `GET /notifications/market-alerts` → lista de suscripciones del inversionista autenticado; `DELETE /notifications/market-alerts/{id}` → 204 y suscripción eliminada.

- [x] T015 [P] [US2] Añadir `getSubscriptions(investorId)` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/service/MarketAlertService.java`
- [x] T016 [P] [US2] Añadir `updateSubscription(investorId, subscriptionId, request)` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/service/MarketAlertService.java`
- [x] T017 [US2] Añadir `deleteSubscription(investorId, subscriptionId)` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/service/MarketAlertService.java`
- [x] T018 [US2] Añadir endpoints `GET /notifications/market-alerts`, `PUT /notifications/market-alerts/{id}`, `DELETE /notifications/market-alerts/{id}` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/controller/MarketAlertController.java` (depende de T015-T017)

**Checkpoint US2**: CRUD completo de suscripciones funcional; un inversionista no puede ver ni modificar suscripciones de otro.

---

## Phase 5: Polish

- [x] T019 [P] Verificar validaciones de negocio en `MarketAlertService` (tipo inválido → 400, símbolo requerido para alertas específicas, threshold requerido donde aplica) en `backend/notifications/src/main/java/com/accioneselbosque/notifications/service/MarketAlertService.java`
- [x] T020 [P] Test `@DataJpaTest` para `MarketAlertSubscriptionRepository` en `backend/notifications/src/test/java/com/accioneselbosque/notifications/repository/MarketAlertSubscriptionRepositoryTest.java`
- [x] T021 [P] Test `@WebMvcTest` para `MarketAlertController` en `backend/notifications/src/test/java/com/accioneselbosque/notifications/controller/MarketAlertControllerTest.java`
- [x] T022 [P] Test Mockito para `MarketAlertService` en `backend/notifications/src/test/java/com/accioneselbosque/notifications/service/MarketAlertServiceTest.java`
- [x] T023 [P] Test Mockito para `MarketAlertDispatcher` en `backend/notifications/src/test/java/com/accioneselbosque/notifications/service/MarketAlertDispatcherTest.java`
- [x] T024 Ejecutar suite completa: `mvn test -pl backend/notifications`

---

## Dependencias clave

- T005 (repositorio) → depende de T004 (entidad)
- T010 (GlobalExceptionHandler) → depende de T007-T009 (excepciones)
- T012 (MarketAlertService) → depende de T003-T010
- T013 (MarketAlertDispatcher) → depende de T012
- T014 (MarketAlertController POST) → depende de T012
- T015-T017 (métodos adicionales del servicio) → dependen de T012
- T018 (endpoints GET/PUT/DELETE) → depende de T015-T017
- Phase 3 completa → Phase 4 puede comenzar en paralelo con resources disponibles

## Parallel Execution Example: User Story 1

```
# Lanzar en paralelo (sin dependencias entre sí):
Task: "Crear MarketEvent POJO en .../model/MarketEvent.java"
Task: "Implementar MarketAlertService.subscribe() en .../service/MarketAlertService.java"

# Secuencial (dependen de subscribe()):
Task: "Implementar MarketAlertDispatcher @EventListener"
Task: "Implementar POST /notifications/market-alerts"
```

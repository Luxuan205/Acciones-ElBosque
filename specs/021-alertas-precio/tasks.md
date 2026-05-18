# Tasks: AB-35 — Alertas de Precio Personalizadas

**Input**: `specs/021-alertas-precio/` (plan.md, spec.md, data-model.md, contracts/price-alerts-api.md)
**Module**: `notifications` — `com.accioneselbosque.notifications`
**Branch**: `021-alertas-precio`
**Prerequisito**: AB-33 (módulo `notifications` base disponible); AB-36 (gate PREMIUM en `auth`); AB-28 (`StockSnapshotService` en `market-data`)

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Puede correr en paralelo
- **[US1]**: Alerta de precio por umbral absoluto (P1)
- **[US2]**: Alerta de precio por variación porcentual (P2)
- **[US3]**: Gestión de alertas de precio configuradas (P2)

---

## Phase 1: Setup — Migración DB

- [x] T001 Crear migración `backend/app/src/main/resources/db/migration/V24__create_price_alert_table.sql` — tabla `price_alert`: columnas `id BIGSERIAL PK`, `investor_id BIGINT NOT NULL REFERENCES investor(id)`, `symbol VARCHAR(20) NOT NULL`, `alert_type VARCHAR(20) NOT NULL` (ABSOLUTE|PERCENTAGE), `threshold NUMERIC(18,4) NOT NULL`, `reference_price NUMERIC(18,4) NULL`, `status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'`, `created_at TIMESTAMP NOT NULL DEFAULT NOW()`, `triggered_at TIMESTAMP NULL`; índice `price_alert_investor_idx ON price_alert(investor_id, status)`

**Checkpoint Setup**: Migración V24 aplicada; tabla `price_alert` verificada en BD.

---

## Phase 2: Foundational — Entidades, repositorio, DTOs, excepciones

- [x] T002 [P] Crear enum `PriceAlertType` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/model/PriceAlertType.java` — valores `ABSOLUTE` y `PERCENTAGE`
- [x] T003 [P] Crear enum `PriceAlertStatus` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/model/PriceAlertStatus.java` — valores `ACTIVE`, `TRIGGERED`, `INACTIVE`, `SUSPENDED`
- [x] T004 Crear entidad JPA `PriceAlert` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/model/PriceAlert.java` — `@Entity @Table(name="price_alert")`; campos: id, investorId, symbol, alertType (`@Enumerated EnumType.STRING`), threshold, referencePrice, status (`@Enumerated`), createdAt, triggeredAt; Lombok `@Data @Builder @NoArgsConstructor @AllArgsConstructor` (depende de T002, T003)
- [x] T005 Crear `PriceAlertRepository` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/repository/PriceAlertRepository.java` — `JpaRepository<PriceAlert, Long>`; métodos: `findByInvestorIdOrderByCreatedAtDesc(Long investorId)`, `findByStatusAndAlertType(PriceAlertStatus status, PriceAlertType type)`, `findByStatus(PriceAlertStatus status)` (depende de T004)
- [x] T006 [P] Crear DTOs en `backend/notifications/src/main/java/com/accioneselbosque/notifications/dto/`: `CreatePriceAlertRequest` (symbol, alertType, threshold); `PriceAlertDto` (id, symbol, alertType, threshold, referencePrice, status, createdAt, triggeredAt)
- [x] T007 [P] Crear excepción `PriceAlertNotFoundException` (→ 404) en `backend/notifications/src/main/java/com/accioneselbosque/notifications/exception/PriceAlertNotFoundException.java`
- [x] T008 [P] Crear excepción `AlertNotModifiableException` (→ 422) en `backend/notifications/src/main/java/com/accioneselbosque/notifications/exception/AlertNotModifiableException.java` — lanzada al intentar modificar una alerta TRIGGERED o SUSPENDED

**Checkpoint Foundational**: Entidad `PriceAlert` mapeada; repositorio disponible; DTOs compilables.

---

## Phase 3: User Story 1 — Alerta de precio por umbral absoluto (P1) 🎯 MVP

**Goal**: Un inversionista PREMIUM puede crear una alerta que se dispara cuando el precio de una acción alcanza un valor absoluto definido. Al crearse, el job de evaluación comienza a monitorearla. Al dispararse, pasa a TRIGGERED y envía notificación.

**Independent Test**: Usuario PREMIUM crea alerta ABSOLUTE para ECOPETROL a 3500; usuario BASIC intenta crear → 403 PREMIUM_REQUIRED; simular precio >= 3500 → alerta pasa a TRIGGERED y `NotificationService.send()` invocado.

- [x] T009 [P] [US1] Implementar `PriceAlertService.createAlert()` y `deleteAlert()` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/service/PriceAlertService.java` — `createAlert(Long investorId, CreatePriceAlertRequest req)`: (1) verificar suscripción PREMIUM via gate en `auth` in-process; si BASIC → `PremiumRequiredException`; (2) para ABSOLUTE: persistir con `referencePrice = null`; (3) retornar `PriceAlertDto`; `deleteAlert(Long investorId, Long alertId)`: cargar, verificar ownership, eliminar (depende de T004–T008)
- [x] T010 [US1] Implementar `PriceAlertEvaluationJob` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/service/PriceAlertEvaluationJob.java` — `@Scheduled(fixedDelay = 30000)`; carga todas las alertas ACTIVE con tipo ABSOLUTE; para cada una: obtener precio actual via `StockSnapshotService` in-process; si `currentPrice >= alert.threshold`: actualizar `status=TRIGGERED`, `triggeredAt=NOW()`; publicar `ApplicationEvent` de tipo `PriceAlertTriggeredEvent` para que `NotificationService` lo procese (depende de T005, T009)
- [x] T011 [US1] Implementar endpoint `POST /notifications/price-alerts` y `DELETE /notifications/price-alerts/{id}` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/controller/PriceAlertController.java` — `@PreAuthorize("hasRole('INVESTOR'")`; extrae `investorId` del JWT; delega a `PriceAlertService`; 201 con `PriceAlertDto` para creación; 204 para eliminación (depende de T009)

**Checkpoint US1**: POST crea alerta; BASIC recibe 403; job evalúa y marca TRIGGERED; notificación enviada.

---

## Phase 4: User Story 2 — Alerta de precio por variación porcentual (P2)

**Goal**: Un inversionista PREMIUM puede crear una alerta que se dispara cuando el precio varía más del X% respecto al precio de referencia al momento de crear la alerta.

**Independent Test**: Crear alerta PERCENTAGE 5% para BANCOLOMBIA con precio actual 42000; simular precio a 44200 (5.2% de variación) → alerta pasa a TRIGGERED.

- [x] T012 [P] [US2] Ampliar `PriceAlertService.createAlert()` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/service/PriceAlertService.java` para tipo PERCENTAGE: consultar precio actual de `StockSnapshotService` y almacenarlo como `referencePrice` al momento de crear la alerta (depende de T009)
- [x] T013 [US2] Añadir evaluación PERCENTAGE en `PriceAlertEvaluationJob` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/service/PriceAlertEvaluationJob.java` — para alertas ACTIVE tipo PERCENTAGE: calcular `variation = |currentPrice - referencePrice| / referencePrice * 100`; si `variation >= alert.threshold` → marcar TRIGGERED y publicar evento (depende de T010)

**Checkpoint US2**: Alerta PERCENTAGE creada con `referencePrice` capturado; job evalúa variación porcentual y dispara cuando se supera el umbral.

---

## Phase 5: User Story 3 — Gestión de alertas de precio (P2)

**Goal**: El inversionista puede listar sus alertas, actualizar el umbral de una alerta ACTIVE, reactivar una alerta TRIGGERED (vuelve a ACTIVE), y desactivarla/activarla manualmente.

**Independent Test**: Crear 3 alertas; listar → 3 resultados; modificar umbral de una ACTIVE → nuevo threshold; reactivar TRIGGERED → vuelve a ACTIVE.

- [x] T014 [P] [US3] Añadir método `getAlerts(Long investorId)` en `PriceAlertService` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/service/PriceAlertService.java` — retorna lista de `PriceAlertDto` del inversionista (depende de T009)
- [x] T015 [P] [US3] Añadir método `updateAlert(Long investorId, Long alertId, BigDecimal newThreshold)` en `PriceAlertService` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/service/PriceAlertService.java` — verificar ownership; si status != ACTIVE → `AlertNotModifiableException`; actualizar threshold (depende de T009)
- [x] T016 [P] [US3] Añadir método `reactivateAlert(Long investorId, Long alertId)` en `PriceAlertService` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/service/PriceAlertService.java` — si status != TRIGGERED → ignorar o error; cambiar a ACTIVE, limpiar `triggeredAt` (depende de T009)
- [x] T017 [US3] Añadir endpoints `GET /notifications/price-alerts`, `PUT /notifications/price-alerts/{id}`, `PATCH /notifications/price-alerts/{id}/reactivate` en `PriceAlertController` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/controller/PriceAlertController.java` (depende de T014–T016)

**Checkpoint US3**: CRUD de alertas funcional; intentar modificar TRIGGERED → 422; reactivar correctamente.

---

## Phase 6: Polish

- [x] T018 [P] Añadir lógica de suspensión por vencimiento de suscripción en `PriceAlertService` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/service/PriceAlertService.java` — listener de evento `SubscriptionExpiredEvent`: marcar SUSPENDED todas las alertas ACTIVE del inversionista; listener de `SubscriptionRenewedEvent`: reactivar las SUSPENDED (FR-008)
- [x] T019 [P] Añadir validación en `PriceAlertEvaluationJob` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/service/PriceAlertEvaluationJob.java` — verificar horario bursátil via `MarketStatusService` antes de evaluar; si mercado cerrado → skip sin error
- [x] T020 Ejecutar suite: `mvn test -pl backend/notifications` — todos los tests de alertas de precio pasan

---

## Dependencias clave

- T004 (PriceAlert) → depende de T002, T003 (enums)
- T005 (repositorio) → depende de T004
- T009 (PriceAlertService) → depende de T004–T008
- T010 (EvaluationJob) → depende de T005, T009
- T011 (PriceAlertController) → depende de T009
- T012 (extensión PERCENTAGE) → depende de T009
- T013 (evaluación PERCENTAGE) → depende de T010
- T014–T016 (gestión) → dependen de T009
- T017 (controller gestión) → depende de T014–T016

## Parallel Execution Example — US1

```
Phase 2 completa (T002–T008)
        │
        ├──[Agente A]── T009 (PriceAlertService.createAlert + deleteAlert)
        │                       │
        │               T010 (PriceAlertEvaluationJob ABSOLUTE)
        │               T011 (PriceAlertController POST+DELETE)
        │
        └── T002, T003, T006, T007, T008 corren en paralelo entre sí
```

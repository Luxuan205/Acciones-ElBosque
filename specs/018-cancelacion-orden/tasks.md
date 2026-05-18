# Tasks: AB-23 — Cancelación de Orden

**Input**: `specs/018-cancelacion-orden/` (plan.md, spec.md, data-model.md, contracts/cancel-order-api.md)
**Module**: `orders` — `com.accioneselbosque.orders`
**Branch**: `018-cancelacion-orden`
**Prerequisito**: AB-19-AB-22 (Order, BalanceReservation, TitleReservation, ConditionalOrder disponibles)

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Puede correr en paralelo
- **[US1]**: Cancelación individual de orden (P1)
- **[US2]**: Cancelación masiva (P2)
- **[US3]**: Manejo de race condition ejecución/cancelación simultánea (P3)

---

## Phase 1: Setup — Migración DB

- [x] T001 Crear migración `backend/app/src/main/resources/db/migration/V20__add_cancellation_fields_to_order.sql` — `ALTER TABLE market_order ADD COLUMN version BIGINT NOT NULL DEFAULT 0, ADD COLUMN cancellation_reason VARCHAR(200) NULL`

**Checkpoint Setup**: V20 aplica sin errores; columnas `version` y `cancellation_reason` verificadas en `market_order`.

---

## Phase 2: Foundational — `@Version` en Order, DTOs, excepción

- [x] T002 [P] Añadir `@Version private Long version` a la entidad `Order` en `backend/orders/src/main/java/com/accioneselbosque/orders/model/Order.java` — habilita optimistic locking en JPA
- [x] T003 [P] Crear DTOs en `backend/orders/src/main/java/com/accioneselbosque/orders/dto/`: `CancellationResponse` (orderId, previousStatus, newStatus, resourcesReleased, amountReleased, titlesReleased, cancelledAt), `BulkCancellationResponse` (totalRequested, totalCancelled, totalFailed, cancelled list, failed list), `BulkCancellationFailure` (orderId, currentStatus, reason)
- [x] T004 Crear excepción `OrderNotCancellableException` (→ 422) en `backend/orders/src/main/java/com/accioneselbosque/orders/exception/OrderNotCancellableException.java` con campo `currentStatus`; registrar en `GlobalExceptionHandler`

**Checkpoint Foundational**: Order.version con @Version; DTOs de cancelación disponibles.

---

## Phase 3: User Story 1 — Cancelación individual (P1) 🎯 MVP

**Goal**: `DELETE /orders/{id}` cancela una orden PENDING o QUEUED. Libera la reserva correspondiente (BalanceReservation para compras, TitleReservation para ventas). Retorna `CancellationResponse` con recursos liberados. EXECUTED → 422.

**Independent Test**: Cancelar PENDING MARKET_BUY → 200 con amountReleased>0, BalanceReservation.released=true; cancelar EXECUTED → 422 CANNOT_CANCEL.

- [x] T005 [P] [US1] Escribir `OrderCancellationControllerTest` en `backend/orders/src/test/java/com/accioneselbosque/orders/controller/OrderCancellationControllerTest.java` con `@WebMvcTest`: DELETE /orders/{id} PENDING → 200; EXECUTED → 422; orden de otro investor → 403; sin JWT → 401
- [x] T006 [P] [US1] Escribir `OrderCancellationServiceTest` en `backend/orders/src/test/java/com/accioneselbosque/orders/service/OrderCancellationServiceTest.java` con Mockito: cancelar MARKET_BUY PENDING → BalanceReservation.released=true, amountReleased>0; cancelar MARKET_SELL PENDING → TitleReservation.released=true; cancelar EXECUTED → `OrderNotCancellableException`
- [x] T007 [US1] Implementar `OrderCancellationService` en `backend/orders/src/main/java/com/accioneselbosque/orders/service/OrderCancellationService.java` — `cancel(investorId, orderId, reason)`: (1) cargar Order; (2) verificar investorId == order.investorId; (3) si status ∉ {PENDING, QUEUED} → `OrderNotCancellableException(currentStatus)`; (4) `order.status = CANCELLED`, `order.cancellationReason = reason`; (5) determinar tipo de reserva por order_type y liberar: MARKET_BUY/LIMIT_BUY → `BalanceReservation.released=true, releasedAt=NOW()`; MARKET_SELL/LIMIT_SELL → `TitleReservation.released=true`; CONDITIONAL → sin reserva; (6) persistir en `@Transactional`; retornar `CancellationResponse` (depende de T002-T004)
- [x] T008 [US1] Implementar `OrderCancellationController` en `backend/orders/src/main/java/com/accioneselbosque/orders/controller/OrderCancellationController.java` — `DELETE /orders/{id}` (JWT rol INVESTOR, query param `reason` opcional); delega a `OrderCancellationService.cancel()`; retorna 200 (depende de T007)

**Checkpoint US1**: Cancelación individual funcional; saldo/títulos liberados; no-cancelable EXECUTED confirmado.

---

## Phase 4: User Story 2 — Cancelación masiva (P2)

**Goal**: `DELETE /orders` acepta lista de orderIds y cancela todas las posibles. Órdenes ya ejecutadas o canceladas se reportan como fracasos sin rollback del lote completo.

**Independent Test**: Lista con 3 PENDING y 1 EXECUTED → totalCancelled=3, totalFailed=1 con motivo.

- [x] T009 [P] [US2] Agregar tests a `OrderCancellationControllerTest`: DELETE /orders con lista mixta → 200 BulkCancellationResponse con conteos correctos; lista vacía → 400; agregar tests a `OrderCancellationServiceTest`: procesamiento individual, fracasos no detienen el lote
- [x] T010 [US2] Añadir método `cancelBulk(investorId, orderIds, reason)` en `OrderCancellationService` — iterar sobre cada orderId: llamar `cancel()` en try/catch; capturar `OrderNotCancellableException` y agregar a lista de failed; continuar con los demás; retornar `BulkCancellationResponse` (depende de T007)
- [x] T011 [US2] Añadir `DELETE /orders` en `OrderCancellationController` — acepta `@RequestBody List<Long> orderIds` y `reason` opcional; delega a `cancelBulk()`; retorna 200 (depende de T010)

**Checkpoint US2**: Cancelación masiva sin rollback global; conteo correcto de éxitos y fracasos.

---

## Phase 5: User Story 3 — Race condition ejecución/cancelación (P3)

**Goal**: Si la orden se ejecuta simultáneamente al intentar cancelarla, `OptimisticLockException` se captura y se devuelve el estado actual de la orden al cliente con 409 CONFLICT.

**Independent Test**: Simular `OptimisticLockException` en `cancel()` → respuesta 409 con currentStatus=EXECUTED.

- [x] T012 [P] [US3] Agregar tests a `OrderCancellationServiceTest`: mock que lanza `OptimisticLockException` → `OrderConcurrentModificationException` (409) con estado actual; agregar a `OrderCancellationControllerTest`: 409 response cuando race condition
- [x] T013 [US3] Envolver persistencia en `OrderCancellationService.cancel()` con `try/catch(OptimisticLockException)` → cargar orden actualizada y lanzar `OrderConcurrentModificationException(currentStatus)` (→ 409); crear la excepción en `backend/orders/src/main/java/com/accioneselbosque/orders/exception/OrderConcurrentModificationException.java`; registrar en `GlobalExceptionHandler` (depende de T007)

**Checkpoint US3**: Race condition manejada; 409 con estado real de la orden al cliente.

---

## Phase 6: Polish

- [x] T014 [P] Verificar que la cancelación de ConditionalOrder también cancela el OCO partner si existe — añadir rama en `OrderCancellationService.cancel()` para `order_type = CONDITIONAL`; cargar `ConditionalOrder` y cancelar partner
- [x] T015 [P] Verificar que el campo `version` en `Order` tiene `@Version` y que JPA lanza `OptimisticLockException` correctamente al detectar conflicto — crear test @DataJpaTest que simule el escenario
- [x] T016 Ejecutar suite: `mvn test -pl backend/orders` — todos los tests de cancelación pasan

---

## Dependencias clave

- T002 (Order.@Version) → depende de que la entidad Order exista (AB-19)
- T007 (OrderCancellationService) → depende de T002-T004 y de BalanceReservation/TitleReservation repos (AB-19/AB-20)
- T010 (cancelBulk) → depende de T007
- T013 (race condition) → modifica T007; implementar tras US1
- US2 → extiende US1; puede implementarse en paralelo si T007 está completo
- US3 → modifica la misma clase de US1; implementar en secuencia

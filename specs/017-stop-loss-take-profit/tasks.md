# Tasks: AB-22 — Configuración de Stop-Loss y Take-Profit

**Input**: `specs/017-stop-loss-take-profit/` (plan.md, spec.md, data-model.md, contracts/conditional-order-api.md)
**Module**: `orders` — `com.accioneselbosque.orders`
**Branch**: `017-stop-loss-take-profit`
**Prerequisito**: AB-19 (módulo `orders`, market_order disponible para vincular triggered_order_id)

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Puede correr en paralelo
- **[US1]**: Creación de stop-loss (P1)
- **[US2]**: Creación de take-profit y vinculación OCO (P2)
- **[US3]**: Evaluación automática y activación por job (P3)

---

## Phase 1: Setup — Migración DB

- [x] T001 Crear migración `backend/app/src/main/resources/db/migration/V19__create_conditional_order_table.sql` — tabla `conditional_order` (id BIGSERIAL PK, investor_id BIGINT NOT NULL FK → investor.id, type VARCHAR(20) NOT NULL, symbol VARCHAR(20) NOT NULL, quantity INT NOT NULL CHECK (quantity > 0), trigger_price DECIMAL(18,2) NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', oco_partner_id BIGINT NULL REFERENCES conditional_order(id), triggered_order_id BIGINT NULL REFERENCES market_order(id), created_at TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP NOT NULL DEFAULT NOW()); índices `cond_order_investor_idx ON conditional_order(investor_id, status)` y `cond_order_symbol_status_idx ON conditional_order(symbol, status)`

**Checkpoint Setup**: V19 aplica sin errores; tabla `conditional_order` con auto-referencia verificada.

---

## Phase 2: Foundational — Entidad, enum, repositorio, DTOs

- [x] T002 [P] Crear enum `ConditionalOrderType` en `backend/orders/src/main/java/com/accioneselbosque/orders/model/ConditionalOrderType.java` — valores: STOP_LOSS, TAKE_PROFIT
- [x] T003 [P] Crear enum `ConditionalOrderStatus` en `backend/orders/src/main/java/com/accioneselbosque/orders/model/ConditionalOrderStatus.java` — valores: ACTIVE, TRIGGERED, CANCELLED
- [x] T004 [P] Crear entidad JPA `ConditionalOrder` en `backend/orders/src/main/java/com/accioneselbosque/orders/model/ConditionalOrder.java` — id (Long), investorId (Long), type (ConditionalOrderType), symbol (String), quantity (int), triggerPrice (BigDecimal), status (ConditionalOrderStatus), ocoPartnerId (Long, nullable, FK auto-referencia), triggeredOrderId (Long, nullable, FK → market_order), createdAt, updatedAt
- [x] T005 [P] Crear `ConditionalOrderRepository` en `backend/orders/src/main/java/com/accioneselbosque/orders/repository/ConditionalOrderRepository.java` — `findByInvestorIdAndStatus(Long investorId, ConditionalOrderStatus status)`, `findBySymbolAndStatus(String symbol, ConditionalOrderStatus status)`
- [x] T006 [P] Crear DTOs en `backend/orders/src/main/java/com/accioneselbosque/orders/dto/`: `CreateStopLossRequest` (symbol, quantity, triggerPrice, takeProfitId opcional), `CreateTakeProfitRequest` (symbol, quantity, triggerPrice, stopLossId opcional), `ConditionalOrderResponse` (id, type, symbol, quantity, triggerPrice, status, ocoPartnerId, createdAt)

**Checkpoint Foundational**: ConditionalOrder mapeada; auto-referencia oco_partner_id configurable.

---

## Phase 3: User Story 1 — Creación de stop-loss (P1) 🎯 MVP

**Goal**: `POST /orders/conditional/stop-loss` crea un ConditionalOrder tipo STOP_LOSS con status=ACTIVE. El inversionista especifica triggerPrice (precio que activa la venta automática). Si se provee takeProfitId, establece el vínculo OCO.

**Independent Test**: POST /orders/conditional/stop-loss con symbol=ECOPETROL, qty=5, triggerPrice=1800 → 201 con status=ACTIVE; con takeProfitId → ocoPartnerId≠null.

- [x] T007 [P] [US1] Escribir `ConditionalOrderControllerTest` en `backend/orders/src/test/java/com/accioneselbosque/orders/controller/ConditionalOrderControllerTest.java` con `@WebMvcTest`: POST stop-loss → 201 ACTIVE; con takeProfitId → ocoPartnerId seteado; sin JWT → 401; triggerPrice ≤ 0 → 400
- [x] T008 [P] [US1] Escribir `ConditionalOrderServiceTest` en `backend/orders/src/test/java/com/accioneselbosque/orders/service/ConditionalOrderServiceTest.java` con Mockito: crear SL sin OCO → status=ACTIVE, ocoPartnerId=null; crear SL con TP existente → ocoPartnerId=takeProfitId, tp.ocoPartnerId=stopLossId (vínculo bidireccional)
- [x] T009 [US1] Implementar `ConditionalOrderService` en `backend/orders/src/main/java/com/accioneselbosque/orders/service/ConditionalOrderService.java` — (1) `createStopLoss(investorId, request)`: persistir ConditionalOrder(STOP_LOSS, ACTIVE); si takeProfitId presente: cargar el TP existente, setear oco_partner_id bidireccional, persistir ambos; retornar `ConditionalOrderResponse`; (2) `createTakeProfit(investorId, request)`: lógica simétrica; (3) `cancel(investorId, id)`: verificar propiedad, marcar CANCELLED, cancelar OCO partner si ACTIVE (depende de T002-T006)
- [x] T010 [US1] Implementar `ConditionalOrderController` en `backend/orders/src/main/java/com/accioneselbosque/orders/controller/ConditionalOrderController.java` — `POST /orders/conditional/stop-loss`, `POST /orders/conditional/take-profit`, `DELETE /orders/conditional/{id}`, `GET /orders/conditional` (JWT rol INVESTOR) (depende de T009)

**Checkpoint US1**: Stop-loss creado; vínculo OCO bidireccional funcional; cancelación manual funcional.

---

## Phase 4: User Story 2 — Creación de take-profit y vínculo OCO (P2)

**Goal**: `POST /orders/conditional/take-profit` crea un ConditionalOrder tipo TAKE_PROFIT. Si se provee stopLossId, establece el par OCO.

**Independent Test**: POST take-profit → 201; con stopLossId → ambos tienen ocoPartnerId; cancelar uno → el otro pasa a CANCELLED.

- [x] T011 [P] [US2] Agregar tests a `ConditionalOrderControllerTest`: POST take-profit → 201; con stopLossId → OCO bidireccional; DELETE take-profit → cancela también el stop-loss vinculado
- [x] T012 [US2] Verificar que `ConditionalOrderService.createTakeProfit()` y `cancel()` están implementados en T009 — si falta alguno, completar (depende de T009)

**Checkpoint US2**: Take-profit funcional; cancelación en cascada del par OCO verificada.

---

## Phase 5: User Story 3 — Evaluación automática (P3)

**Goal**: Job cada 30s. STOP_LOSS ACTIVE: si `currentPrice <= triggerPrice` → marcar TRIGGERED, generar MARKET_SELL, cancelar OCO partner. TAKE_PROFIT ACTIVE: si `currentPrice >= triggerPrice` → idem.

**Independent Test**: Mock precio ≤ triggerPrice de SL → tras job: SL=TRIGGERED, TP=CANCELLED, nueva MARKET_SELL creada.

- [x] T013 [P] [US3] Escribir `ConditionalOrderEvaluationJobTest` en `backend/orders/src/test/java/com/accioneselbosque/orders/service/ConditionalOrderEvaluationJobTest.java` con Mockito: precio alcanza SL → TRIGGERED + partner CANCELLED + MARKET_SELL generada; precio alcanza TP → idem simétrico; precio no alcanza → sin cambio
- [x] T014 [US3] Implementar `ConditionalOrderEvaluationJob` en `backend/orders/src/main/java/com/accioneselbosque/orders/service/ConditionalOrderEvaluationJob.java` — `@Scheduled(fixedRate=30000)`: (1) si `!MarketHoursGate.isMarketOpen()` → no evaluar; (2) cargar todos los ConditionalOrder con status=ACTIVE; (3) para cada uno: obtener precio de `MarketDataFacade.getCurrentPrice(symbol)`; (4) evaluar condición por tipo; (5) si se activa: `status=TRIGGERED`, generar `MarketSellService.placeSell()` con los parámetros de la orden condicional, guardar `triggeredOrderId`; cancelar partner si existe: `partner.status=CANCELLED`; persistir ambos en `@Transactional` (depende de T009)

**Checkpoint US3**: Activación automática funcional; OCO partner cancelado; MARKET_SELL generada.

---

## Phase 6: Polish

- [x] T015 [P] Verificar que el vínculo OCO es bidireccional al crearlo: si A tiene ocoPartnerId=B, entonces B tiene ocoPartnerId=A — revisar `ConditionalOrderService.createStopLoss()` y `createTakeProfit()`
- [x] T016 [P] Verificar que el job usa `@Transactional` por orden evaluada (no en el lote completo) — rollback individual no afecta otras evaluaciones
- [x] T017 Ejecutar suite: `mvn test -pl backend/orders` — todos los tests de stop-loss/take-profit pasan

---

## Dependencias clave

- T009 (ConditionalOrderService) → depende de T002-T006 (entidades, repos, DTOs)
- T010 (ConditionalOrderController) → depende de T009
- T014 (EvaluationJob) → depende de T009 y de `MarketSellService` (AB-20)
- US2 → extiende el mismo servicio de US1; implementar en conjunto
- US3 → implementar tras US1 y US2 (necesita crear SL/TP antes de evaluarlos)

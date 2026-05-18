# Tasks: AB-21 — Colocación de Limit Order

**Input**: `specs/016-limit-order/` (plan.md, spec.md, contracts/limit-order-api.md)
**Module**: `orders` — `com.accioneselbosque.orders`
**Branch**: `016-limit-order`
**Prerequisito**: AB-19 y AB-20 (market_order con limit_price y expires_at, BalanceReservation, TitleReservation disponibles)

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Puede correr en paralelo
- **[US1]**: Colocación de limit order de compra GTC/GTD (P1)
- **[US2]**: Colocación de limit order de venta (P2)
- **[US3]**: Evaluación automática y ejecución/expiración por job periódico (P3)

---

## Phase 1: Setup

> No se requieren nuevas migraciones DB. `market_order.limit_price` y `expires_at` ya existen (V16).

- [x] T001 Verificar que `market_order.limit_price` y `market_order.expires_at` existen en `backend/app/src/main/resources/db/migration/V16__create_order_table.sql` — confirmar presencia de ambas columnas antes de continuar

**Checkpoint Setup**: Columnas limit_price y expires_at confirmadas en V16.

---

## Phase 2: Foundational — DTOs y excepciones

- [x] T002 [P] Crear DTO `PlaceLimitBuyRequest` en `backend/orders/src/main/java/com/accioneselbosque/orders/dto/PlaceLimitBuyRequest.java` — symbol (@NotBlank), quantity (@Min(1)), limitPrice (@DecimalMin("0.01") BigDecimal), expiresAt (LocalDateTime, optional — null = GTC)
- [x] T003 [P] Crear DTO `PlaceLimitSellRequest` en `backend/orders/src/main/java/com/accioneselbosque/orders/dto/PlaceLimitSellRequest.java` — symbol, quantity, limitPrice, expiresAt (optional)
- [x] T004 [P] Crear DTO `LimitOrderResponse` en `backend/orders/src/main/java/com/accioneselbosque/orders/dto/LimitOrderResponse.java` — orderId, type (LIMIT_BUY|LIMIT_SELL), symbol, quantity, limitPrice, expiresAt, status, createdAt

**Checkpoint Foundational**: DTOs de limit order disponibles; módulo compila.

---

## Phase 3: User Story 1 — Limit order de compra GTC/GTD (P1) 🎯 MVP

**Goal**: `POST /orders/limit/buy` crea una LIMIT_BUY con limitPrice y expiresAt (null=GTC). Valida saldo, reserva balance, persiste con status=PENDING. El job evaluará si el precio baja al límite.

**Independent Test**: POST /orders/limit/buy con limitPrice < precio actual → 201 status=PENDING; saldo insuficiente → 422.

- [x] T005 [P] [US1] Escribir `LimitOrderControllerTest` en `backend/orders/src/test/java/com/accioneselbosque/orders/controller/LimitOrderControllerTest.java` con `@WebMvcTest`: POST limit/buy → 201; saldo insuficiente → 422; sin JWT → 401; limitPrice ≤ 0 → 400
- [x] T006 [P] [US1] Escribir `LimitOrderServiceTest` en `backend/orders/src/test/java/com/accioneselbosque/orders/service/LimitOrderServiceTest.java` con Mockito: saldo suficiente → Order LIMIT_BUY PENDING, BalanceReservation persiste; expiresAt=null → GTC (expires_at=NULL en DB); expiresAt presente → GTD
- [x] T007 [US1] Implementar `LimitOrderService` en `backend/orders/src/main/java/com/accioneselbosque/orders/service/LimitOrderService.java` — (1) `placeLimitBuy(investorId, request)`: calcular totalEstimated con limitPrice; verificar saldo disponible; persistir Order(LIMIT_BUY, PENDING) y BalanceReservation en `@Transactional`; retornar `LimitOrderResponse`; (2) `placeLimitSell(investorId, request)`: similar pero verifica títulos y crea TitleReservation (depende de T002-T004)
- [x] T008 [US1] Implementar `LimitOrderController` en `backend/orders/src/main/java/com/accioneselbosque/orders/controller/LimitOrderController.java` — `POST /orders/limit/buy` y `POST /orders/limit/sell` (JWT rol INVESTOR); delega a `LimitOrderService`; retorna 201 (depende de T007)

**Checkpoint US1**: Limit order de compra creada; saldo reservado; GTC y GTD diferenciados.

---

## Phase 4: User Story 2 — Limit order de venta (P2)

**Goal**: `POST /orders/limit/sell` crea LIMIT_SELL con limitPrice. Valida títulos disponibles, reserva TitleReservation, persiste PENDING.

**Independent Test**: POST /orders/limit/sell → 201; títulos insuficientes → 422 INSUFFICIENT_TITLES.

- [x] T009 [P] [US2] Agregar tests a `LimitOrderControllerTest`: POST limit/sell → 201; títulos insuficientes → 422; agregar tests a `LimitOrderServiceTest`: Order LIMIT_SELL creada con TitleReservation
- [x] T010 [US2] Verificar que `LimitOrderService.placeLimitSell()` está implementado en T007 — si no, completarlo en `LimitOrderService` (depende de T007)

**Checkpoint US2**: Limit sell creada con TitleReservation; rechazo correcto por títulos insuficientes.

---

## Phase 5: User Story 3 — Evaluación automática y expiración (P3)

**Goal**: `LimitOrderEvaluationJob` corre cada 30s durante horario bursátil. Evalúa LIMIT_BUY activas: si `currentPrice <= limitPrice` → genera MARKET_BUY de ejecución. LIMIT_SELL: si `currentPrice >= limitPrice` → genera MARKET_SELL. GTD vencidas → CANCELLED con recursos liberados.

**Independent Test**: Mock currentPrice=limitPrice para una LIMIT_BUY → tras ejecutar job → Order EXECUTED; mock GTD expirada → Order CANCELLED, BalanceReservation released.

- [x] T011 [P] [US3] Escribir `LimitOrderEvaluationJobTest` en `backend/orders/src/test/java/com/accioneselbosque/orders/service/LimitOrderEvaluationJobTest.java` con Mockito: precio alcanza límite de compra → estado EXECUTED; precio no alcanza → sin cambio; GTD expirada → CANCELLED; balance liberado al cancelar
- [x] T012 [US3] Implementar `LimitOrderEvaluationJob` en `backend/orders/src/main/java/com/accioneselbosque/orders/service/LimitOrderEvaluationJob.java` — `@Scheduled(fixedRate=30000)`: (1) si `!MarketHoursGate.isMarketOpen()` → no evaluar; (2) `SELECT * FROM market_order WHERE status='PENDING' AND order_type IN ('LIMIT_BUY','LIMIT_SELL')`; (3) para cada orden: obtener precio actual de `MarketDataFacade`; LIMIT_BUY: si precio<=límite → marcar EXECUTED, liberar BalanceReservation; LIMIT_SELL: si precio>=límite → marcar EXECUTED, liberar TitleReservation; (4) `SELECT * FROM market_order WHERE expires_at < NOW() AND status='PENDING'` → marcar CANCELLED, liberar reservas (depende de T007)

**Checkpoint US3**: Job ejecuta y expira limit orders correctamente; recursos liberados en cada caso.

---

## Phase 6: Polish

- [x] T013 [P] Verificar que `LimitOrderEvaluationJob` no corre fuera de horario bursátil — condición `isMarketOpen()` al inicio del método
- [x] T014 [P] Verificar que la ejecución de limit orders por el job persiste en la misma transacción que la liberación de reservas — `@Transactional` en el método de evaluación individual
- [x] T015 Ejecutar suite: `mvn test -pl backend/orders` — todos los tests de limit order pasan

---

## Dependencias clave

- T007 (LimitOrderService) → depende de T002-T004 (DTOs) y AB-19 (Order, BalanceReservation existentes)
- T008 (LimitOrderController) → depende de T007
- T012 (EvaluationJob) → depende de T007 (LimitOrderService para lógica de estado)
- US2 → extiende LimitOrderService de US1; implementar en la misma iteración
- US3 → implementar tras US1 y US2 (necesita orders ya creadas)

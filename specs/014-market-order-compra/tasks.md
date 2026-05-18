# Tasks: AB-19 — Generación de Market Order de Compra

**Input**: `specs/014-market-order-compra/` (plan.md, spec.md, data-model.md, contracts/market-order-buy-api.md)
**Module**: `orders` (nuevo Maven module) — `com.accioneselbosque.orders`
**Branch**: `014-market-order-compra`
**Prerequisito**: AB-19 requiere módulo `market-data` disponible in-process (precio), `auth` (saldo), `configuration` (horario bursátil)

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Puede correr en paralelo
- **[US1]**: Preview y colocación de market order de compra en mercado abierto (P1)
- **[US2]**: Colocación de market order cuando mercado está cerrado — QUEUED (P2)
- **[US3]**: Rechazo por saldo insuficiente (P3)

---

## Phase 1: Setup — Nuevo módulo Maven y migraciones DB

- [x] T001 Crear `backend/orders/pom.xml` — nuevo módulo Maven `orders` con groupId `com.accioneselbosque`, packaging jar, parent `backend/pom.xml`; dependencias: spring-boot-starter-web, spring-boot-starter-data-jpa, spring-boot-starter-validation, lombok, spring-boot-starter-test
- [x] T002 Añadir `<module>orders</module>` en `backend/pom.xml` dentro del bloque `<modules>`
- [x] T003 Crear `backend/orders/src/main/java/com/accioneselbosque/orders/OrdersApplication.java` — clase `@SpringBootApplication` con anotación `@ComponentScan`
- [x] T004 Crear migración `backend/app/src/main/resources/db/migration/V16__create_order_table.sql` — tabla `market_order` con todos los campos del data-model.md (id, investor_id, order_type, status, symbol, quantity, estimated_price, execution_price, commission, total_estimated, limit_price, expires_at, rejection_reason, created_at, updated_at); índices `order_investor_idx` y `order_status_idx`
- [x] T005 Crear migración `backend/app/src/main/resources/db/migration/V17__create_balance_reservation_table.sql` — tabla `balance_reservation` (id, order_id UNIQUE FK → market_order.id ON DELETE CASCADE, investor_id FK, amount, released BOOLEAN DEFAULT FALSE, created_at, released_at NULL); índice `balance_res_investor_idx ON balance_reservation(investor_id, released)`

**Checkpoint Setup**: `mvn compile -pl backend/orders` exitoso; V16-V17 aplican sin errores.

---

## Phase 2: Foundational — Entidades, enums, repositorios, excepciones, DTOs

- [x] T006 [P] Crear enum `OrderType` en `backend/orders/src/main/java/com/accioneselbosque/orders/model/OrderType.java` — valores: MARKET_BUY, MARKET_SELL, LIMIT_BUY, LIMIT_SELL
- [x] T007 [P] Crear enum `OrderStatus` en `backend/orders/src/main/java/com/accioneselbosque/orders/model/OrderStatus.java` — valores: QUEUED, PENDING, EXECUTED, CANCELLED, REJECTED
- [x] T008 [P] Crear entidad JPA `Order` en `backend/orders/src/main/java/com/accioneselbosque/orders/model/Order.java` — todos los campos del data-model.md mapeados a la tabla `market_order`; anotaciones @Entity, @Table("market_order"), @Id, @GeneratedValue, @Enumerated(STRING)
- [x] T009 [P] Crear entidad JPA `BalanceReservation` en `backend/orders/src/main/java/com/accioneselbosque/orders/model/BalanceReservation.java` — orderId (Long, UNIQUE), investorId (Long), amount (BigDecimal), released (boolean), createdAt, releasedAt (nullable)
- [x] T010 [P] Crear `OrderRepository` en `backend/orders/src/main/java/com/accioneselbosque/orders/repository/OrderRepository.java` — `findByInvestorIdOrderByCreatedAtDesc(Long investorId)`, `findByStatusAndSymbol(OrderStatus status, String symbol)`
- [x] T011 [P] Crear `BalanceReservationRepository` en `backend/orders/src/main/java/com/accioneselbosque/orders/repository/BalanceReservationRepository.java` — `findByInvestorIdAndReleasedFalse(Long investorId)`, `findByOrderId(Long orderId)`
- [x] T012 [P] Crear `MarketHoursGate` en `backend/orders/src/main/java/com/accioneselbosque/orders/service/MarketHoursGate.java` — `isMarketOpen()`: consulta `GlobalParameterService.getString("trading.market_open_time")` y `"trading.market_close_time"` del módulo `configuration`; retorna true si hora UTC actual está dentro del rango (fallback: 09:00-17:00 UTC)
- [x] T013 [P] Crear DTOs en `backend/orders/src/main/java/com/accioneselbosque/orders/dto/`: `PlaceMarketBuyRequest` (symbol, quantity), `CommissionBreakdown` (estimatedPrice, quantity, commission, totalEstimated), `OrderResponse` (orderId, status, symbol, quantity, breakdown, createdAt, message opcional)
- [x] T014 Crear excepción `InsufficientBalanceException` (→ 422) en `backend/orders/src/main/java/com/accioneselbosque/orders/exception/InsufficientBalanceException.java`; registrar en `GlobalExceptionHandler` en `backend/orders/src/main/java/com/accioneselbosque/orders/exception/GlobalExceptionHandler.java`

**Checkpoint Foundational**: Módulo `orders` compila; entidades Order y BalanceReservation mapeadas; MarketHoursGate inyectable.

---

## Phase 3: User Story 1 — Compra en mercado abierto (P1) 🎯 MVP

**Goal**: `GET /orders/market/buy/preview` calcula desglose de comisión. `POST /orders/market/buy` crea la orden con status=PENDING cuando el mercado está abierto, reserva saldo y retorna OrderResponse.

**Independent Test**: Con mock de mercado abierto y saldo suficiente → POST /orders/market/buy → 201 con status=PENDING; GET preview → 200 con breakdown correcto.

- [x] T015 [P] [US1] Escribir `MarketOrderControllerTest` en `backend/orders/src/test/java/com/accioneselbosque/orders/controller/MarketOrderControllerTest.java` con `@WebMvcTest`: GET preview → 200 con commission=0.3% del bruto; POST buy mercado abierto → 201 status=PENDING; símbolo no encontrado → 404; sin JWT → 401
- [x] T016 [P] [US1] Escribir `MarketOrderServiceTest` en `backend/orders/src/test/java/com/accioneselbosque/orders/service/MarketOrderServiceTest.java` con Mockito: saldo suficiente + mercado abierto → order PENDING, BalanceReservation persiste; commission = quantity * precio * 0.003; totalEstimated = bruto + commission
- [x] T017 [US1] Implementar `MarketOrderService` en `backend/orders/src/main/java/com/accioneselbosque/orders/service/MarketOrderService.java` — (1) `preview(symbol, quantity)`: obtener precio de `MarketDataFacade.getCurrentPrice(symbol)`; calcular commission (tasa de `GlobalParameterService`), totalEstimated; (2) `placeBuy(investorId, request)`: llamar preview; calcular saldo disponible = saldo total − sum(reservas activas); si insuficiente → `InsufficientBalanceException`; determinar status via `MarketHoursGate.isMarketOpen()`; persistir Order y BalanceReservation; publicar `OrderPlacedEvent` vía `ApplicationEventPublisher` (depende de T006-T014)
- [x] T018 [US1] Implementar `MarketOrderController` en `backend/orders/src/main/java/com/accioneselbosque/orders/controller/MarketOrderController.java` — `GET /orders/market/buy/preview` (sin auth) y `POST /orders/market/buy` (JWT rol INVESTOR); delega a `MarketOrderService`; retorna 200/201 (depende de T017)

**Checkpoint US1**: Preview y compra en mercado abierto funcionales; commission 0.3% correcto.

---

## Phase 4: User Story 2 — Compra con mercado cerrado — QUEUED (P2)

**Goal**: `POST /orders/market/buy` cuando `MarketHoursGate.isMarketOpen()` retorna false crea la orden con status=QUEUED y reserva saldo igualmente.

**Independent Test**: Mock `isMarketOpen()=false` → POST buy → 201 con status=QUEUED y message de mercado cerrado.

- [x] T019 [P] [US2] Agregar tests a `MarketOrderControllerTest`: mercado cerrado → 201 status=QUEUED con message; agregar tests a `MarketOrderServiceTest`: mock isMarketOpen=false → Order.status=QUEUED
- [x] T020 [US2] Verificar que `MarketOrderService.placeBuy()` asigna QUEUED cuando isMarketOpen()=false — el if/else ya debe cubrirse en T017; si no, añadir la rama en `MarketOrderService` (depende de T017)

**Checkpoint US2**: Orden QUEUED creada con saldo reservado cuando mercado cerrado.

---

## Phase 5: User Story 3 — Rechazo por saldo insuficiente (P3)

**Goal**: Si saldo disponible < totalEstimated, la orden es rechazada con 422 INSUFFICIENT_BALANCE.

**Independent Test**: Mock saldo disponible=0 → POST buy → 422 con error INSUFFICIENT_BALANCE.

- [x] T021 [P] [US3] Agregar tests a `MarketOrderServiceTest`: saldo disponible insuficiente → `InsufficientBalanceException`; saldo exacto igual al total → aprobado; agregar test a `MarketOrderControllerTest`: response 422
- [x] T022 [US3] Verificar que `MarketOrderService.placeBuy()` calcula saldo disponible correctamente sumando reservas activas (released=false) y lanza `InsufficientBalanceException` — lógica ya en T017; si incompleta, completar el cálculo (depende de T017)

**Checkpoint US3**: Rechazo 422 cuando saldo insuficiente; sin rechazo cuando saldo exacto.

---

## Phase 6: Polish

- [x] T023 [P] Verificar que `Order.commission` usa tasa de `GlobalParameterService.getDecimal("trading.commission_rate_pct")` con fallback 0.3 cuando el parámetro no existe — revisar `MarketOrderService`
- [x] T024 [P] Verificar que `BalanceReservation` se persiste en la misma transacción `@Transactional` que `Order` — rollback si cualquier persistencia falla
- [x] T025 Ejecutar suite: `mvn test -pl backend/orders` — todos los tests de market order compra pasan

---

## Dependencias clave

- T017 (MarketOrderService) → depende de T006-T014 (entidades, enums, repos, excepciones, DTOs)
- T018 (MarketOrderController) → depende de T017
- T020 (QUEUED path) → depende de T017 (misma clase, extensión de rama)
- T022 (saldo insuficiente) → depende de T017 (misma clase, lógica ya contenida)
- US2 y US3 → extienden el mismo servicio de US1; implementar en secuencia

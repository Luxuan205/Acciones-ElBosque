# Tasks: AB-20 — Generación de Market Order de Venta

**Input**: `specs/015-market-order-venta/` (plan.md, spec.md, data-model.md, contracts/market-order-sell-api.md)
**Module**: `orders` — `com.accioneselbosque.orders`
**Branch**: `015-market-order-venta`
**Prerequisito**: AB-19 (módulo `orders` creado; Order, BalanceReservation, MarketHoursGate disponibles)

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Puede correr en paralelo
- **[US1]**: Preview y colocación de market order de venta en mercado abierto (P1)
- **[US2]**: Colocación de market order de venta con mercado cerrado — QUEUED (P2)
- **[US3]**: Rechazo por títulos insuficientes (P3)

---

## Phase 1: Setup — Migración DB

- [x] T001 Crear migración `backend/app/src/main/resources/db/migration/V18__create_title_reservation_table.sql` — tabla `title_reservation` (id BIGSERIAL PK, order_id BIGINT NOT NULL UNIQUE FK → market_order.id ON DELETE CASCADE, investor_id BIGINT NOT NULL FK → investor.id, symbol VARCHAR(20) NOT NULL, quantity INT NOT NULL CHECK (quantity > 0), released BOOLEAN NOT NULL DEFAULT FALSE, created_at TIMESTAMP NOT NULL DEFAULT NOW(), released_at TIMESTAMP NULL); índice `title_res_investor_symbol_idx ON title_reservation(investor_id, symbol, released)`

**Checkpoint Setup**: V18 aplica sin errores; tabla `title_reservation` verificada.

---

## Phase 2: Foundational — Entidad, repositorio, excepción, DTOs

- [x] T002 [P] Crear entidad JPA `TitleReservation` en `backend/orders/src/main/java/com/accioneselbosque/orders/model/TitleReservation.java` — orderId (Long, UNIQUE), investorId (Long), symbol (String), quantity (int), released (boolean), createdAt, releasedAt (nullable)
- [x] T003 [P] Crear `TitleReservationRepository` en `backend/orders/src/main/java/com/accioneselbosque/orders/repository/TitleReservationRepository.java` — `findByInvestorIdAndSymbolAndReleasedFalse(Long investorId, String symbol)`, `findByOrderId(Long orderId)`
- [x] T004 [P] Crear DTO `PlaceMarketSellRequest` en `backend/orders/src/main/java/com/accioneselbosque/orders/dto/PlaceMarketSellRequest.java` — symbol (@NotBlank @Size(max=20)), quantity (@Min(1))
- [x] T005 Crear excepción `InsufficientTitlesException` (→ 422) en `backend/orders/src/main/java/com/accioneselbosque/orders/exception/InsufficientTitlesException.java`; registrar en `GlobalExceptionHandler` con error code `INSUFFICIENT_TITLES`

**Checkpoint Foundational**: Módulo `orders` compila con TitleReservation; InsufficientTitlesException mapeada.

---

## Phase 3: User Story 1 — Venta en mercado abierto (P1) 🎯 MVP

**Goal**: `GET /orders/market/sell/preview` calcula desglose de comisión para venta. `POST /orders/market/sell` crea orden MARKET_SELL con status=PENDING, reserva títulos con TitleReservation, retorna OrderResponse.

**Independent Test**: Mock portafolio con 10 títulos ECOPETROL → POST /orders/market/sell qty=5 → 201 status=PENDING; GET preview → breakdown correcto con total_estimated=netAmount.

- [x] T006 [P] [US1] Escribir `MarketSellControllerTest` en `backend/orders/src/test/java/com/accioneselbosque/orders/controller/MarketSellControllerTest.java` con `@WebMvcTest`: GET preview → 200 con breakdown; POST sell mercado abierto → 201 status=PENDING; símbolo no encontrado → 404; sin JWT → 401
- [x] T007 [P] [US1] Escribir `MarketSellServiceTest` en `backend/orders/src/test/java/com/accioneselbosque/orders/service/MarketSellServiceTest.java` con Mockito: títulos suficientes + mercado abierto → Order MARKET_SELL PENDING, TitleReservation persiste; commission = quantity * precio * 0.003; total_estimated = netAmount (precio*qty − commission)
- [x] T008 [US1] Implementar `MarketSellService` en `backend/orders/src/main/java/com/accioneselbosque/orders/service/MarketSellService.java` — (1) `preview(symbol, quantity)`: obtener precio de `MarketDataFacade.getCurrentPrice(symbol)`; calcular commission, netAmount; (2) `placeSell(investorId, request)`: calcular títulos disponibles = portafolio − reservas activas del mismo símbolo vía `TitleReservationRepository`; si insuficientes → `InsufficientTitlesException`; determinar status via `MarketHoursGate.isMarketOpen()`; persistir Order (MARKET_SELL) y TitleReservation en `@Transactional`; publicar `OrderPlacedEvent` (depende de T002-T005)
- [x] T009 [US1] Añadir `GET /orders/market/sell/preview` y `POST /orders/market/sell` en `MarketOrderController` en `backend/orders/src/main/java/com/accioneselbosque/orders/controller/MarketOrderController.java` — delega a `MarketSellService`; retorna 200/201 (depende de T008)

**Checkpoint US1**: Preview y venta en mercado abierto funcionales; TitleReservation creada en misma transacción.

---

## Phase 4: User Story 2 — Venta con mercado cerrado — QUEUED (P2)

**Goal**: POST /orders/market/sell cuando mercado cerrado → status=QUEUED con message de mercado cerrado; títulos reservados igualmente.

**Independent Test**: Mock `isMarketOpen()=false` → POST sell → 201 status=QUEUED.

- [x] T010 [P] [US2] Agregar tests a `MarketSellControllerTest`: mercado cerrado → 201 status=QUEUED con message; agregar tests a `MarketSellServiceTest`: mock isMarketOpen=false → Order.status=QUEUED
- [x] T011 [US2] Verificar que la rama QUEUED en `MarketSellService.placeSell()` está cubierta — ya debe estar en T008; si no, añadir la condición (depende de T008)

**Checkpoint US2**: Orden MARKET_SELL QUEUED creada con títulos reservados cuando mercado cerrado.

---

## Phase 5: User Story 3 — Rechazo por títulos insuficientes (P3)

**Goal**: Si títulos disponibles < quantity, la orden es rechazada con 422 INSUFFICIENT_TITLES.

**Independent Test**: Mock portafolio vacío → POST sell qty=1 → 422 INSUFFICIENT_TITLES.

- [x] T012 [P] [US3] Agregar tests: portafolio=0 → `InsufficientTitlesException`; portafolio=5, reservas=5 → excepción; portafolio=5, reservas=3, qty=2 → aprobado
- [x] T013 [US3] Verificar cálculo de títulos disponibles en `MarketSellService` — incluye títulos en portafolio (`PortfolioFacade.getAvailableTitles(investorId, symbol)`) menos reservas activas (`TitleReservationRepository.findByInvestorIdAndSymbolAndReleasedFalse`) (depende de T008)

**Checkpoint US3**: Rechazo 422 con títulos insuficientes; suma correcta de reservas activas.

---

## Phase 6: Polish

- [x] T014 [P] Verificar que `total_estimated` para SELL representa el monto neto que recibirá el inversionista (precio×qty − commission), no el bruto — revisar `MarketSellService.preview()`
- [x] T015 Ejecutar suite: `mvn test -pl backend/orders` — todos los tests de market order venta pasan

---

## Dependencias clave

- T008 (MarketSellService) → depende de T002-T005 y del módulo `portfolio` (PortfolioFacade para títulos disponibles)
- T009 (MarketOrderController) → depende de T008
- US2 y US3 → extienden la misma lógica de US1; implementar en secuencia

# Tasks: AB-24 — Encolamiento de Orden Fuera de Horario Bursátil

**Input**: `specs/003-encolamiento-orden/` (plan.md, spec.md, data-model.md, contracts/order-queue-api.md, research.md)
**Module**: `order` — `com.accioneselbosque.order_service`
**Branch**: `003-encolamiento-orden`
**Prerequisito**: AB-29 (configuration-service con MarketStatusService.isMarketOpen() disponible in-process)

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Puede correr en paralelo
- **[US1]**: Crear orden → QUEUED si mercado cerrado; scheduler FIFO en apertura (P1)
- **[US2]**: Cancelar orden QUEUED; límite 10 órdenes encoladas por investor (P2)

---

## Phase 1: Setup — Migración DB

- [ ] T001 Crear migración `backend/order/src/main/resources/db/migration/V1__create_order_table.sql` con tabla `"order"` (comillas porque ORDER es keyword SQL reservado): id UUID PK, investor_id UUID NOT NULL, broker_id UUID NULL, symbol, quantity, order_type CHECK('BUY','SELL'), status CHECK('QUEUED','ACTIVE','EXECUTED','FAILED','CANCELLED') DEFAULT 'QUEUED', unit_price, gross_value, commission_rate, commission_amt, net_total DECIMAL(18,2), created_at, processed_at NULL; índices `idx_order_investor_status(investor_id, status)` e `idx_order_status_created(status, created_at ASC)`

**Checkpoint Setup**: `./mvnw flyway:migrate` aplica V1 sin errores.

---

## Phase 2: Foundational — Modelo, enum y repositorio

- [ ] T002 Crear enum `OrderStatus` en `backend/order/src/main/java/com/accioneselbosque/order_service/model/OrderStatus.java` con valores: `QUEUED`, `ACTIVE`, `EXECUTED`, `FAILED`, `CANCELLED`
- [ ] T003 Crear entidad JPA `Order` en `backend/order/src/main/java/com/accioneselbosque/order_service/model/Order.java` — todos los campos del data-model.md; `@Enumerated(EnumType.STRING) OrderStatus status`; tabla `"order"` (quoted con `@Table(name = "\"order\"")`); `brokerId` nullable
- [ ] T004 Crear `OrderRepository` en `backend/order/src/main/java/com/accioneselbosque/order_service/repository/OrderRepository.java` — métodos: `findByInvestorIdAndStatusOrderByCreatedAtAsc(UUID, OrderStatus)`, `countByInvestorIdAndStatus(UUID, OrderStatus)`, `findByIdAndInvestorId(UUID, UUID)`, `findByInvestorId(UUID, Pageable)`
- [ ] T005 Crear interfaz `MarketStatusService` en `backend/order/src/main/java/com/accioneselbosque/order_service/service/MarketStatusService.java` con método `boolean isMarketOpen()` — implementación inyecta el bean del módulo `configuration-service` in-process
- [ ] T006 Crear excepciones: `OrderQueueLimitException` (→ 422), `OrderNotCancellableException` (→ 409), `OrderNotFoundException` (→ 404) en `exception/`; crear `GlobalExceptionHandler` con `@ControllerAdvice`

**Checkpoint Foundational**: Compilación limpia; `Order` mapeado a la tabla correctamente.

---

## Phase 3: User Story 1 — Crear orden con encolamiento automático (P1) 🎯 MVP

**Goal**: `POST /orders` crea una orden. Si el mercado está cerrado → estado QUEUED. Si abierto → ACTIVE. `@Scheduled` procesa la cola FIFO al detectar apertura.

**Independent Test**: Con mercado cerrado: `POST /orders` → 201 QUEUED en DB. Con mercado abierto: → 201 ACTIVE. Tras abrir el mercado, esperar ≤ 60s: órdenes QUEUED cambian a ACTIVE (verificar `processed_at` en DB).

### Tests — US1

- [ ] T007 [P] [US1] Escribir `OrderControllerTest` en `src/test/java/.../controller/OrderControllerTest.java` con `@WebMvcTest`: test `POST /orders` mercado cerrado → 201 con status QUEUED; test `POST /orders` mercado abierto → 201 con status ACTIVE
- [ ] T008 [P] [US1] Escribir `OrderServiceTest` en `src/test/java/.../service/OrderServiceTest.java` con Mockito: test `createOrder()` llama `MarketStatusService.isMarketOpen()`; si false → status=QUEUED; si true → status=ACTIVE; test `OrderQueueProcessor.processQueue()` cambia QUEUED → ACTIVE en orden FIFO

### Implementación — US1

- [ ] T009 [P] [US1] Crear `CreateOrderRequest` DTO en `dto/CreateOrderRequest.java` — symbol (@NotBlank), quantity (@Positive int), orderType (@NotBlank), unitPrice (@Positive BigDecimal)
- [ ] T010 [P] [US1] Crear `OrderResponse` DTO en `dto/OrderResponse.java` — orderId, symbol, quantity, orderType, status, unitPrice, grossValue, commissionRate, commissionAmt, netTotal, createdAt, processedAt
- [ ] T011 [US1] Implementar `OrderService.createOrder(UUID investorId, CreateOrderRequest)` en `service/OrderService.java` — (1) consultar `MarketStatusService.isMarketOpen()`; (2) calcular grossValue, commissionAmt (consultar CommissionRateRepository del módulo 004 in-process), netTotal; (3) persistir Order con status QUEUED o ACTIVE según mercado; retornar OrderResponse
- [ ] T012 [US1] Implementar `OrderQueueProcessor` en `service/OrderQueueProcessor.java` con `@Scheduled(fixedRate = 60000)` — si `MarketStatusService.isMarketOpen()`: buscar todas las órdenes QUEUED ORDER BY created_at ASC; para cada una setear status=ACTIVE y processedAt=now(); persistir por lotes (depende de T011)
- [ ] T013 [US1] Implementar `OrderController` en `controller/OrderController.java` con `POST /orders` → `@Valid @RequestBody`, extrae investorId del JWT, delega a `orderService.createOrder()`, retorna 201; `GET /orders` → paginado filtrable por status (depende de T011)

**Checkpoint US1**: Crear orden QUEUED/ACTIVE funciona; scheduler procesa cola.

---

## Phase 4: User Story 2 — Cancelar orden y validar límite de cola (P2)

**Goal**: `DELETE /orders/{id}/cancel` cancela una orden QUEUED. El sistema rechaza la 11ª orden QUEUED con 422.

**Independent Test**: Crear 10 órdenes QUEUED → 11ª devuelve 422. Cancelar una QUEUED → 200. Intentar cancelar una ACTIVE → 409.

### Tests — US2

- [ ] T014 [P] [US2] Agregar tests a `OrderControllerTest`: `DELETE /orders/{id}/cancel` QUEUED → 200; orden ACTIVE → 409; orden no existe → 404; 11ª orden QUEUED → 422
- [ ] T015 [P] [US2] Agregar tests a `OrderServiceTest`: `cancelOrder()` lanza `OrderNotCancellableException` si status ≠ QUEUED; `createOrder()` lanza `OrderQueueLimitException` si hay 10 QUEUED

### Implementación — US2

- [ ] T016 [US2] Agregar validación de límite en `OrderService.createOrder()` — antes de persistir: `if (orderRepository.countByInvestorIdAndStatus(investorId, QUEUED) >= 10) throw new OrderQueueLimitException()` (modifica T011)
- [ ] T017 [US2] Implementar `OrderService.cancelOrder(UUID orderId, UUID investorId)` — buscar por `findByIdAndInvestorId()` → 404 si no existe; verificar `status == QUEUED` → lanzar `OrderNotCancellableException` con status actual si no; setear status=CANCELLED, persistir
- [ ] T018 [US2] Agregar `DELETE /orders/{orderId}/cancel` en `OrderController` — delega a `orderService.cancelOrder()`, retorna 200

**Checkpoint US2**: Tests T014–T015 pasan; límite de 10 enforced; cancelación solo sobre QUEUED.

---

## Phase 5: Polish

- [ ] T019 [P] Verificar que `@Table(name = "\"order\"")` genera SQL correcto en PostgreSQL (keyword reservado)
- [ ] T020 [P] Ejecutar suite completa: `./mvnw test -pl backend/order`

---

## Dependencias clave

- T003 (Order entity) → depende de T001 (V1 migration) y T002 (OrderStatus enum)
- T011 (OrderService.createOrder) → inyecta CommissionRateRepository de módulo 004 in-process (requiere que 004 esté en classpath)
- T012 (scheduler) → depende de T011 (usa el mismo repositorio)
- T016 (límite 10) → modifica T011, debe hacerse en la misma transacción
- US2 puede empezarse en paralelo con US1 a nivel de tests (T014, T015) pero la impl depende de T011

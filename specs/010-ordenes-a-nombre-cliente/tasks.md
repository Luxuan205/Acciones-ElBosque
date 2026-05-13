# Tasks: AB-32 — Generación y Firma de Órdenes a Nombre del Cliente

**Input**: `specs/010-ordenes-a-nombre-cliente/` (plan.md, spec.md, data-model.md, contracts/broker-order-api.md, research.md)
**Module**: `order` — `com.accioneselbosque.order_service`
**Branch**: `010-ordenes-a-nombre-cliente`
**Prerequisito**: AB-24 (OrderService y tabla `"order"` existen); AB-31 (BrokerClientAssignmentRepository disponible in-process)

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Puede correr en paralelo
- **[US1]**: Broker crea orden a nombre de cliente asignado (P1)
- **[US2]**: Broker consulta historial de órdenes generadas (P2)

---

## Phase 1: Setup — Migración DB

- [ ] T001 Crear migración `backend/order/src/main/resources/db/migration/V2__add_broker_id_to_order.sql` — `ALTER TABLE "order" ADD COLUMN broker_id UUID`; `CREATE INDEX idx_order_broker ON "order"(broker_id) WHERE broker_id IS NOT NULL`

**Checkpoint Setup**: Columna `broker_id` nullable presente en la tabla; índice parcial creado.

---

## Phase 2: Foundational — Validator de asignación

- [ ] T002 Actualizar entidad `Order.java` en `backend/order/src/main/java/com/accioneselbosque/order_service/model/Order.java` — agregar campo `brokerId` (UUID, `@Column(nullable = true)`)
- [ ] T003 Crear `BrokerAssignmentValidator` en `backend/order/src/main/java/com/accioneselbosque/order_service/service/BrokerAssignmentValidator.java` — inyecta `BrokerClientAssignmentRepository` de auth-security-service in-process; método `assertBrokerAssignedToClient(UUID brokerId, UUID clientId)`: llama `existsByBrokerIdAndInvestorIdAndActive(brokerId, clientId, true)` → lanza `ClientNotAssignedException` (→ 403) si false
- [ ] T004 Crear excepción `ClientNotAssignedException` en `exception/` si no existe en este módulo; registrar en `GlobalExceptionHandler` → 403

**Checkpoint Foundational**: `BrokerAssignmentValidator` compilado; campo `brokerId` mapeado en entidad.

---

## Phase 3: User Story 1 — Broker crea orden para cliente (P1) 🎯 MVP

**Goal**: `POST /orders/broker` crea una orden con `investorId = clientId` y `brokerId = broker del JWT`. La comisión usa la suscripción del *cliente* (no del broker). El response incluye clientName y brokerName resueltos in-process.

**Independent Test**: Broker asignado al cliente: `POST /orders/broker` → 201 con brokerId y investorId correctos en DB. Broker NO asignado al cliente → 403. Cliente con balance insuficiente → 400.

### Tests — US1

- [ ] T005 [P] [US1] Escribir `BrokerOrderControllerTest` en `src/test/java/.../controller/BrokerOrderControllerTest.java` con `@WebMvcTest`: test POST broker asignado → 201; broker no asignado → 403; balance insuficiente → 400; JWT con rol INVESTOR (no BROKER) → 403
- [ ] T006 [P] [US1] Escribir `BrokerOrderServiceTest` con Mockito: test `createBrokerOrder()` llama `BrokerAssignmentValidator.assertBrokerAssignedToClient()`; test que `brokerId` se persiste en la orden; test que tasa de comisión usa suscripción del cliente (no del broker)

### Implementación — US1

- [ ] T007 [P] [US1] Crear `BrokerOrderRequest` DTO en `dto/BrokerOrderRequest.java` — clientId (@NotNull UUID), symbol (@NotBlank), quantity (@Positive int), orderType (@NotBlank), unitPrice (@Positive @DecimalMin("0.01") BigDecimal)
- [ ] T008 [P] [US1] Crear `BrokerOrderResponse` DTO en `dto/BrokerOrderResponse.java` — orderId, clientId, clientName, brokerId, brokerName, symbol, quantity, orderType, status, unitPrice, grossValue, commissionRate, commissionAmt, netTotal, createdAt
- [ ] T009 [US1] Implementar `BrokerOrderService.createBrokerOrder(UUID brokerId, BrokerOrderRequest)` en `service/BrokerOrderService.java` — (1) `BrokerAssignmentValidator.assertBrokerAssignedToClient(brokerId, clientId)`; (2) cargar `subscriptionType` del cliente in-process via `InvestorRepository`; (3) llamar `OrderService.createOrder()` con `investorId=clientId` y `brokerId=brokerId`; (4) resolver `clientName` y `brokerName` via `InvestorRepository`; retornar `BrokerOrderResponse` (depende de T003)
- [ ] T010 [US1] Implementar `BrokerOrderController` en `controller/BrokerOrderController.java` con `POST /orders/broker` — `@PreAuthorize("hasRole('BROKER')")`, extrae brokerId del JWT, `@Valid @RequestBody BrokerOrderRequest`, delega a `BrokerOrderService.createBrokerOrder()`, retorna 201 (depende de T009)

**Checkpoint US1**: Orden creada con brokerId no nulo; validación de asignación enforced; 403 para broker no asignado.

---

## Phase 4: User Story 2 — Historial de órdenes del broker (P2)

**Goal**: `GET /orders/broker/history` retorna paginado de todas las órdenes donde `broker_id = brokerId del JWT`, filtrable por clientId y rango de fechas.

**Independent Test**: Broker con 3 órdenes para 2 clientes distintos: sin filtros → 3. `?clientId=X` → solo órdenes de X. `?from=2026-05-01&to=2026-05-10` → solo las de ese período.

### Tests — US2

- [ ] T011 [P] [US2] Agregar tests a `BrokerOrderControllerTest`: `GET /orders/broker/history` → 200 paginado; `?clientId=X` filtra; `?from` inválido → 400; `from > to` → 400
- [ ] T012 [P] [US2] Agregar tests a `BrokerOrderServiceTest`: `getBrokerHistory()` solo retorna órdenes del broker autenticado (brokerId del JWT); resolución de clientName y brokerName

### Implementación — US2

- [ ] T013 [P] [US2] Crear `BrokerOrderHistoryResponse` DTO en `dto/BrokerOrderHistoryResponse.java` — brokerId, content (List de {orderId, clientId, clientName, brokerName, symbol, quantity, orderType, status, netTotal, createdAt, processedAt}), totalElements, totalPages, page, size
- [ ] T014 Agregar método a `OrderRepository` en `repository/OrderRepository.java` — `findByBrokerIdAndOptionalFilters(UUID brokerId, UUID clientId nullable, LocalDateTime from, LocalDateTime to, Pageable)` via `@Query` JPQL con condiciones opcionales
- [ ] T015 [US2] Implementar `BrokerOrderService.getBrokerHistory(UUID brokerId, UUID clientId, LocalDate from, LocalDate to, int page)` — construir PageRequest(page, 20); llamar `OrderRepository.findByBrokerIdAndOptionalFilters()`; para cada orden resolver clientName y brokerName via InvestorRepository in-process; retornar BrokerOrderHistoryResponse
- [ ] T016 [US2] Agregar `GET /orders/broker/history` en `BrokerOrderController` — `@RequestParam` opcionales clientId, from, to, page; validar from ≤ to; delegar a `BrokerOrderService.getBrokerHistory()`; retornar 200 (depende de T015)

**Checkpoint US2**: Historial paginado con filtros funciona; solo aparecen órdenes del broker autenticado.

---

## Phase 5: Polish

- [ ] T017 Verificar trazabilidad en DB — ejecutar `SELECT broker_id IS NOT NULL as is_broker_order, COUNT(*) FROM "order" GROUP BY 1` y confirmar que las órdenes del broker tienen broker_id
- [ ] T018 [P] Ejecutar suite: `./mvnw test -pl backend/order`

---

## Dependencias clave

- T002 (Order.brokerId field) → depende de T001 (V2 migration)
- T003 (BrokerAssignmentValidator) → inyecta `BrokerClientAssignmentRepository` de auth-security-service in-process (requiere AB-31 en classpath)
- T009 (BrokerOrderService) → delega a `OrderService.createOrder()` (AB-24) con el brokerId extra — asegurarse que `createOrder()` acepta brokerId como parámetro opcional sin romper las llamadas directas
- T014 (@Query JPQL) → la query debe manejar `clientId IS NULL OR o.investorId = :clientId` para parámetros opcionales

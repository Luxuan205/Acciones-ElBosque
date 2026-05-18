# Tasks: AB-31 — Gestión de Clientes Asignados por el Comisionista

**Input**: `specs/009-clientes-comisionista/` (plan.md, spec.md, data-model.md, contracts/broker-clients-api.md, research.md)
**Module**: `auth-security-service` — `com.accioneselbosque.auth`
**Branch**: `009-clientes-comisionista`
**Prerequisito**: AB-15 (Investor entity existe en auth_db); AB-26 (BalanceService in-process); AB-24 (OrderRepository in-process)

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Puede correr en paralelo
- **[US1]**: Broker ve lista de clientes asignados con resumen de cuenta; buscar y filtrar (P1)
- **[US2]**: Broker ve detalle de cliente con portafolio y últimas 5 órdenes (P2)

---

## Phase 1: Setup — Migración DB

- [x] T001 Crear migración `backend/auth/src/main/resources/db/migration/V6__create_broker_client_assignment_table.sql` — tabla `broker_client_assignment` (id UUID PK, broker_id UUID NOT NULL, investor_id UUID NOT NULL, assigned_at TIMESTAMP DEFAULT NOW(), active BOOLEAN NOT NULL DEFAULT TRUE, deactivated_at TIMESTAMP NULL); constraint UNIQUE(broker_id, investor_id); índices `idx_bca_broker_active(broker_id, active)`, `idx_bca_investor(investor_id)`

**Checkpoint Setup**: Tabla creada con constraint UNIQUE y ambos índices.

---

## Phase 2: Foundational — Entidad, repositorio y gate de seguridad

- [x] T002 Crear entidad JPA `BrokerClientAssignment` en `backend/auth/src/main/java/com/accioneselbosque/auth/model/BrokerClientAssignment.java` — id (UUID), brokerId (UUID), investorId (UUID), assignedAt (LocalDateTime), active (boolean), deactivatedAt (LocalDateTime nullable)
- [x] T003 Crear `BrokerClientAssignmentRepository` en `repository/BrokerClientAssignmentRepository.java` — `findByBrokerIdAndActive(UUID brokerId, boolean active, Pageable)` retorna `Page<BrokerClientAssignment>`; `existsByBrokerIdAndInvestorIdAndActive(UUID, UUID, boolean)`; query JPQL con búsqueda opcional por nombre y filtro por status
- [x] T004 Crear excepción `ClientNotAssignedException` (? 403) en `exception/`; registrar en `GlobalExceptionHandler` existente
- [x] T005 Crear `BrokerIsolationGuard` en `service/BrokerIsolationGuard.java` — método `assertBrokerOwnsClient(UUID brokerId, UUID investorId)`: llama `existsByBrokerIdAndInvestorIdAndActive(brokerId, investorId, true)` ? lanza `ClientNotAssignedException` si false

**Checkpoint Foundational**: Guard de aislamiento probado unitariamente; compilación limpia.

---

## Phase 3: User Story 1 — Lista de clientes con búsqueda y filtro (P1) ?? MVP

**Goal**: `GET /brokers/me/clients` retorna lista paginada (20/página) de clientes asignados al broker autenticado. Soporta `search` (fullName) y `status` (ACTIVE/INACTIVE/PENDING). Cada entrada incluye availableBalance y activeOrdersCount enriquecidos in-process.

**Independent Test**: Broker A con 2 clientes: sin filtros ? 2 resultados. `?search=carlos` ? 1 resultado. Broker B sin clientes ? lista vacía. Broker A no puede ver clientes de Broker B.

### Tests — US1

- [x] T006 [P] [US1] Escribir `BrokerClientControllerTest` en `src/test/java/.../controller/BrokerClientControllerTest.java` con `@WebMvcTest`: test `GET /brokers/me/clients` JWT BROKER ? 200 con lista; test JWT INVESTOR (no BROKER) ? 403; test `?search=X` filtra por nombre; test `?status=INVALID` ? 400
- [x] T007 [P] [US1] Escribir `BrokerClientServiceTest` con Mockito: test `getClients()` solo retorna clientes activos del broker autenticado; test enriquecimiento con availableBalance y activeOrdersCount; test que datos de otro broker no aparecen

### Implementación — US1

- [x] T008 [P] [US1] Crear `ClientSummaryDto` en `dto/ClientSummaryDto.java` — investorId, fullName, email, accountStatus, availableBalance (BigDecimal), activeOrdersCount (int), assignedAt
- [x] T009 [US1] Implementar `BrokerClientService.getClients(UUID brokerId, String search, String status, int page)` en `service/BrokerClientService.java` — (1) consultar `BrokerClientAssignmentRepository` con parámetros opcionales vía @Query JPQL con JOIN a Investor; (2) para cada cliente: enriquecer `availableBalance` inyectando `BalanceService` in-process y `activeOrdersCount` via `OrderRepository.countByInvestorIdAndStatusIn()` in-process; (3) retornar paginado
- [x] T010 [US1] Implementar `BrokerClientController` en `controller/BrokerClientController.java` con `GET /brokers/me/clients` — `@PreAuthorize("hasRole('BROKER')")`, extrae brokerId del JWT, delega a `BrokerClientService.getClients()` (depende de T009)

**Checkpoint US1**: Lista de clientes con enriquecimiento funciona; aislamiento verificado (Broker A ? Broker B).

---

## Phase 4: User Story 2 — Detalle de cliente con portafolio (P2)

**Goal**: `GET /brokers/me/clients/{investorId}` retorna detalle completo: saldo, portafolioSummary (in-process de portfolio) y últimas 5 órdenes (in-process de order). Broker que intenta ver cliente de otro broker ? 403.

**Independent Test**: Broker A consulta cliente asignado ? 200 con portfolioSummary y recentOrders. Broker A consulta cliente de Broker B ? 403. ID de investor inexistente ? 404.

### Tests — US2

- [x] T011 [P] [US2] Agregar tests a `BrokerClientControllerTest`: `GET /brokers/me/clients/{id}` propio ? 200 con detalle; cliente ajeno ? 403; ID inexistente ? 404
- [x] T012 [P] [US2] Agregar tests a `BrokerClientServiceTest`: `getClientDetail()` llama `BrokerIsolationGuard.assertBrokerOwnsClient()`; portfolioSummary proviene de PortfolioService in-process; recentOrders = últimas 5 del OrderRepository

### Implementación — US2

- [x] T013 [P] [US2] Crear `ClientDetailDto` en `dto/ClientDetailDto.java` — investorId, fullName, email, phone, accountStatus, totalBalance, availableBalance, assignedAt, portfolioSummary (PortfolioSummaryDto), recentOrders (List<OrderResponse> max 5)
- [x] T014 [US2] Implementar `BrokerClientService.getClientDetail(UUID brokerId, UUID investorId)` — (1) llamar `BrokerIsolationGuard.assertBrokerOwnsClient()` ? 403 si falla; (2) cargar Investor ? 404 si no existe; (3) enriquecer: `BalanceService.getBalance()` in-process, `PortfolioService.getSummary()` in-process, `OrderRepository.findByInvestorIdOrderByCreatedAtDesc(investorId, PageRequest.of(0,5))` in-process; (4) mapear a ClientDetailDto
- [x] T015 [US2] Agregar `GET /brokers/me/clients/{investorId}` en `BrokerClientController` — delega a `getClientDetail()`; retorna 200 (depende de T014)

**Checkpoint US2**: Detalle completo con portafolio y órdenes; aislamiento enforced.

---

## Phase 5: Polish

- [x] T016 Verificar que `?status=` acepta exactamente ACTIVE, INACTIVE, PENDING (case-sensitive) — valores inválidos ? 400 con mensaje descriptivo
- [x] T017 [P] Ejecutar suite: `./mvnw test -pl backend/auth`

---

## Dependencias clave

- T002 (BrokerClientAssignment) ? depende de T001 (V6 migration)
- T005 (BrokerIsolationGuard) ? depende de T003 (repositorio)
- T009 (BrokerClientService.getClients) ? inyecta BalanceService (AB-26) y OrderRepository (AB-24) in-process
- T014 (getClientDetail) ? inyecta adicionalmente PortfolioService (AB-27) in-process
- T003 requiere @Query JPQL con JOIN a Investor — escribir la query cuidando el alias correcto de la tabla `auth_db.investor`

# Tasks: AB-26 — Consulta de Saldo y Movimiento de Fondos

**Input**: `specs/005-saldo-movimiento-fondos/` (plan.md, spec.md, data-model.md, contracts/balance-api.md, research.md)
**Module**: `portfolio` — `com.accioneselbosque.portfolio_query_service`
**Branch**: `005-saldo-movimiento-fondos`
**Prerequisito**: AB-24 (order existe; `OrderRepository` disponible in-process para calcular saldo reservado)

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Puede correr en paralelo
- **[US1]**: Consultar saldo (total, reservado, disponible) (P1)
- **[US2]**: Historial paginado de movimientos con filtro por fechas (P2)

---

## Phase 1: Setup — Migraciones DB

- [x] T001 Crear migración `backend/portfolio/src/main/resources/db/migration/V1__create_account_balance_table.sql` — tabla `account_balance` (id UUID PK, investor_id UUID UNIQUE NOT NULL, total_balance DECIMAL(18,2) DEFAULT 0.00, currency VARCHAR(3) DEFAULT 'COP', updated_at, created_at)
- [x] T002 Crear migración `backend/portfolio/src/main/resources/db/migration/V2__create_fund_movement_table.sql` — tabla `fund_movement` (id UUID PK, investor_id UUID NOT NULL, type VARCHAR(15) CHECK('DEPOSIT','WITHDRAWAL','PURCHASE','SALE','COMMISSION'), amount DECIMAL(18,2) NOT NULL CHECK != 0, balance_after DECIMAL(18,2), currency VARCHAR(3) DEFAULT 'COP', description VARCHAR(200), order_id UUID NULL, created_at); índice `idx_fund_movement_investor_created(investor_id, created_at DESC)`

**Checkpoint Setup**: Migraciones V1 y V2 aplican sin errores.

---

## Phase 2: Foundational — Entidades, enum y repositorios

- [x] T003 Crear enum `MovementType` en `backend/portfolio/src/main/java/com/accioneselbosque/portfolio_query_service/model/MovementType.java` — DEPOSIT, WITHDRAWAL, PURCHASE, SALE, COMMISSION
- [x] T004 [P] Crear entidad JPA `AccountBalance` en `backend/portfolio/src/main/java/com/accioneselbosque/portfolio_query_service/model/AccountBalance.java` — id (UUID), investorId (UUID, UNIQUE), totalBalance (BigDecimal), currency (String), updatedAt (@PreUpdate), createdAt
- [x] T005 Crear entidad JPA `FundMovement` en `backend/portfolio/src/main/java/com/accioneselbosque/portfolio_query_service/model/FundMovement.java` — id (UUID), investorId (UUID), type (@Enumerated STRING MovementType), amount (BigDecimal), balanceAfter (BigDecimal), currency, description, orderId (UUID nullable), createdAt (depende de T003)
- [x] T006 [P] Crear `AccountBalanceRepository` en `repository/AccountBalanceRepository.java` — `findByInvestorId(UUID)` retorna `Optional<AccountBalance>`
- [x] T007 [P] Crear `FundMovementRepository` en `repository/FundMovementRepository.java` — `findByInvestorIdAndCreatedAtBetween(UUID, LocalDateTime, LocalDateTime, Pageable)` retorna `Page<FundMovement>`; `findByInvestorId(UUID, Pageable)`
- [x] T008 Crear `GlobalExceptionHandler` en `exception/GlobalExceptionHandler.java` — maneja `MethodArgumentTypeMismatchException` (fechas inválidas ? 400), `AccessDeniedException` (? 403)

**Checkpoint Foundational**: Compilación limpia; repositorios verificados con `@DataJpaTest`.

---

## Phase 3: User Story 1 — Consultar saldo (P1) ?? MVP

**Goal**: `GET /portfolio/balance` retorna totalBalance, reservedBalance (suma de órdenes ACTIVE/QUEUED in-process) y availableBalance.

**Independent Test**: Investor con $5.000.000 depositados y 1 orden QUEUED de $400.925 ? totalBalance=5000000, reservedBalance=400925, availableBalance=4599075.

### Tests — US1

- [x] T009 [P] [US1] Escribir `BalanceControllerTest` en `src/test/java/.../controller/BalanceControllerTest.java` con `@WebMvcTest`: test `GET /portfolio/balance` ? 200 con totalBalance, reservedBalance, availableBalance; test que otro investor no puede ver el saldo del primero (mismo endpoint, JWT distinto ? sus propios datos)
- [x] T010 [P] [US1] Escribir `BalanceServiceTest` con Mockito: test `getBalance()` suma netTotal de órdenes ACTIVE/QUEUED para calcular reservedBalance; test availableBalance = totalBalance - reservedBalance; test con reservedBalance > totalBalance ? availableBalance = 0 (no negativo)

### Implementación — US1

- [x] T011 [P] [US1] Crear `BalanceSummaryResponse` DTO en `dto/BalanceSummaryResponse.java` — investorId, totalBalance, reservedBalance, availableBalance, currency, updatedAt
- [x] T012 [US1] Implementar `BalanceService.getBalance(UUID investorId)` en `service/BalanceService.java` — (1) cargar `AccountBalance` por investorId, crear con 0 si no existe (primer acceso); (2) calcular `reservedBalance` consultando `OrderRepository` in-process: `SUM(net_total) WHERE investor_id=? AND status IN ('ACTIVE','QUEUED')`; (3) `availableBalance = max(0, totalBalance - reservedBalance)`; retornar BalanceSummaryResponse
- [x] T013 [US1] Implementar `BalanceController` en `controller/BalanceController.java` con `GET /portfolio/balance` — extrae investorId del JWT, delega a `balanceService.getBalance()`, retorna 200

**Checkpoint US1**: `GET /portfolio/balance` retorna saldo correcto con reserva calculada.

---

## Phase 4: User Story 2 — Historial paginado de movimientos (P2)

**Goal**: `GET /portfolio/movements` retorna lista paginada (20/página) de movimientos, filtrable por rango de fechas.

**Independent Test**: Investor con 3 movimientos en distintas fechas: `?from=2026-01-01&to=2026-05-10` ? 2 resultados. `?page=0&size=20` ? paginación correcta con totalElements y totalPages.

### Tests — US2

- [x] T014 [P] [US2] Agregar tests a `BalanceControllerTest`: `GET /portfolio/movements` sin filtros ? 200 paginado; `?from=X&to=Y` filtra correctamente; `?from=` fecha inválida ? 400; aislamiento entre investors
- [x] T015 [P] [US2] Escribir `FundMovementServiceTest` con `@DataJpaTest`: test `getMovements()` retorna Page con contenido y metadata correctos; test filtro por fechas es inclusivo en ambos extremos

### Implementación — US2

- [x] T016 [P] [US2] Crear `FundMovementDto` en `dto/FundMovementDto.java` — movementId, type, amount, balanceAfter, currency, description, orderId, createdAt
- [x] T017 [P] [US2] Crear `FundMovementPageResponse` en `dto/FundMovementPageResponse.java` — content (List<FundMovementDto>), totalElements, totalPages, page, size
- [x] T018 [US2] Implementar `FundMovementService.getMovements(UUID investorId, LocalDate from, LocalDate to, int page)` en `service/FundMovementService.java` — construir `Pageable` con `PageRequest.of(page, 20)`; si `from` null usar `LocalDate.MIN`; si `to` null usar `LocalDate.now()`; convertir fechas a `LocalDateTime` (to ? endOfDay); llamar `fundMovementRepository.findByInvestorIdAndCreatedAtBetween()`; mapear a `FundMovementPageResponse`
- [x] T019 [US2] Agregar `GET /portfolio/movements` en `BalanceController` — `@RequestParam` opcionales from, to, page (default 0); validar `from <= to` ? 400 si viola; delegar a `FundMovementService.getMovements()`

**Checkpoint US2**: Tests T014–T015 pasan; paginación y filtros funcionan.

---

## Phase 5: Polish

- [x] T020 [P] Verificar aislamiento: endpoint siempre usa `investorId` del JWT, nunca de un parámetro de URL
- [x] T021 [P] Ejecutar suite: `./mvnw test -pl backend/portfolio`

---

## Dependencias clave

- T012 (BalanceService) ? inyecta `OrderRepository` de order in-process; requiere AB-24 en classpath
- T005 (FundMovement) ? depende de T003 (MovementType enum)
- T018 (FundMovementService) ? depende de T007 (FundMovementRepository) y T005 (FundMovement entity)
- `FundMovementService.recordMovement()` (usado por order al ejecutar órdenes) debe ser implementado como método auxiliar en T018

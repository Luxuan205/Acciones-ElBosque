# Tasks: AB-37 — Reporte de Ganancias, Pérdidas y Comisiones

**Input**: `specs/022-reporte-ganancias/` (plan.md, spec.md, data-model.md, contracts/portfolio-api.md)
**Module**: `portfolio` — `com.accioneselbosque.portfolio`
**Branch**: `022-reporte-ganancias`
**Prerequisito**: AB-14–AB-23 (`market_order` y módulo `orders` disponibles; `StockSnapshotService` en `market-data` disponible in-process)

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Puede correr en paralelo
- **[US1]**: Consulta de rentabilidad por posición (P1)
- **[US2]**: Reporte de rentabilidad por período (P2)
- **[US3]**: Exportación del reporte (P3)

---

## Phase 1: Setup — Módulo Maven y migraciones DB

- [x] T001 Crear `backend/portfolio/pom.xml` — módulo Maven `portfolio`, dependencias: spring-boot-starter-data-jpa, spring-boot-starter-web, lombok, spring-boot-starter-security; declarar como dependencia en `backend/pom.xml` y en `backend/app/pom.xml`
- [x] T002 Crear migración `backend/app/src/main/resources/db/migration/V25__create_position_table.sql` — tabla `position` con columnas id, investor_id (FK → investor), symbol, current_quantity, avg_purchase_price, cash_balance, created_at, updated_at; constraint UNIQUE (investor_id, symbol); índice `position_investor_idx`
- [x] T003 Crear migración `backend/app/src/main/resources/db/migration/V26__create_transaction_table.sql` — tabla `transaction` con columnas id, investor_id (FK → investor), order_id (FK → market_order), transaction_type, symbol, quantity, execution_price, commission, gross_amount, net_amount, realized_gain (NULL), avg_price_at_time (NULL), executed_at; índices `transaction_investor_idx` y `transaction_symbol_idx`

**Checkpoint Setup**: V25 y V26 aplican sin errores; tablas `position` y `transaction` verificadas; módulo `portfolio` compila (`mvn compile -pl backend/portfolio`).

---

## Phase 2: Foundational — Entidades, repositorios, DTOs, excepciones

- [x] T004 [P] Crear entidad `Position` en `backend/portfolio/src/main/java/com/accioneselbosque/portfolio/model/Position.java` — `@Entity @Table(name="position")`; campos: id, investorId, symbol, currentQuantity, avgPurchasePrice, cashBalance, createdAt, updatedAt; `@UniqueConstraint(investor_id, symbol)`
- [x] T005 [P] Crear entidad `Transaction` en `backend/portfolio/src/main/java/com/accioneselbosque/portfolio/model/Transaction.java` — `@Entity @Table(name="transaction")`; campos: id, investorId, orderId, transactionType (enum BUY|SELL), symbol, quantity, executionPrice, commission, grossAmount, netAmount, realizedGain, avgPriceAtTime, executedAt
- [x] T006 [P] Crear enum `TransactionType` en `backend/portfolio/src/main/java/com/accioneselbosque/portfolio/model/TransactionType.java` — valores BUY y SELL
- [x] T007 [P] Crear enum `ReportPeriod` en `backend/portfolio/src/main/java/com/accioneselbosque/portfolio/model/ReportPeriod.java` — valores TODAY, WEEK, MONTH, YEAR, CUSTOM
- [x] T008 [P] Crear `PositionRepository` en `backend/portfolio/src/main/java/com/accioneselbosque/portfolio/repository/PositionRepository.java` — `JpaRepository<Position, Long>`; métodos: `findByInvestorId(Long investorId)`, `findByInvestorIdAndSymbol(Long investorId, String symbol)` (retorna Optional)
- [x] T009 [P] Crear `TransactionRepository` en `backend/portfolio/src/main/java/com/accioneselbosque/portfolio/repository/TransactionRepository.java` — `JpaRepository<Transaction, Long>`; métodos: `findByInvestorIdAndExecutedAtBetween(Long investorId, LocalDateTime from, LocalDateTime to)`, `findByInvestorIdAndSymbolAndExecutedAtBetween(Long, String, LocalDateTime, LocalDateTime)`
- [x] T010 [P] Crear DTOs en `backend/portfolio/src/main/java/com/accioneselbosque/portfolio/dto/`: `PositionDto` (symbol, currentQuantity, avgPurchasePrice, currentPrice, unrealizedGain, unrealizedGainPct); `TransactionDto` (transactionType, symbol, quantity, executionPrice, commission, grossAmount, netAmount, realizedGain, executedAt); `PortfolioPositionsResponse` (positions list, totalInvested, totalMarketValue, totalUnrealizedGain); `PortfolioReportDto` (period, from, to, totalRealizedGain, positions list, transactions list)
- [x] T011 [P] Crear excepción `InvalidPeriodException` (→ 400) en `backend/portfolio/src/main/java/com/accioneselbosque/portfolio/exception/InvalidPeriodException.java`
- [x] T012 Crear `GlobalExceptionHandler` en `backend/portfolio/src/main/java/com/accioneselbosque/portfolio/exception/GlobalExceptionHandler.java` — maneja `InvalidPeriodException` → 400 `INVALID_PERIOD`; maneja `AccessDeniedException` → 403 (depende de T011)

**Checkpoint Foundational**: Entidades con mappings correctos; repositorios disponibles; DTOs compilados; `GlobalExceptionHandler` registrado.

---

## Phase 3: User Story 1 — Consulta de rentabilidad por posición (P1) 🎯 MVP

**Goal**: `GET /portfolio/positions` retorna todas las posiciones abiertas del inversionista autenticado con precio promedio ponderado, precio actual de mercado, ganancia/pérdida no realizada en valor absoluto y porcentaje, y totales del portafolio.

**Independent Test**: Con portafolio con 2 posiciones abiertas (ECOPETROL: 100 acciones a COP 1920; PFBCOLOM: 20 acciones a COP 38000) y precio de mercado actual de 1972 y 39500 respectivamente → `unrealizedGain` = 5200 y 30000, `unrealizedGainPct` = 2.71 y 3.95, `totalUnrealizedGain` = 35200.

- [x] T013 [P] [US1] Crear `PositionUpdateService` en `backend/portfolio/src/main/java/com/accioneselbosque/portfolio/service/PositionUpdateService.java` — método público `onOrderExecuted(Long investorId, String symbol, TransactionType type, int quantity, BigDecimal executionPrice, BigDecimal commission, Long orderId)`: si BUY: upsert `Position` recalculando PPP; si SELL: restar `currentQuantity`; crear registro `Transaction` con campos calculados; persistir en `@Transactional` (depende de T004-T009)
- [x] T014 [P] [US1] Crear `PortfolioFacade` en `backend/portfolio/src/main/java/com/accioneselbosque/portfolio/facade/PortfolioFacade.java` — métodos: `getAvailableTitles(Long investorId, String symbol)` → retorna `currentQuantity` de `Position` o 0; `getAvailableBalance(Long investorId)` → retorna `cashBalance` de Position con `symbol = '_CASH'` o 0 (depende de T008)
- [x] T015 [US1] Implementar `PortfolioService` en `backend/portfolio/src/main/java/com/accioneselbosque/portfolio/service/PortfolioService.java` — método `getPositions(Long investorId)`: (1) cargar posiciones via `PositionRepository`; (2) para cada posición: consultar precio actual in-process a `StockSnapshotService`; (3) calcular `unrealizedGain = (currentPrice - avgPurchasePrice) × currentQuantity`; (4) calcular `unrealizedGainPct`; (5) fallback a `avgPurchasePrice` si precio no disponible; (6) calcular totales; retornar `PortfolioPositionsResponse` (depende de T004, T008, T010)
- [x] T016 [US1] Implementar `PortfolioController` en `backend/portfolio/src/main/java/com/accioneselbosque/portfolio/controller/PortfolioController.java` — `GET /portfolio/positions` (JWT rol INVESTOR); extrae `investorId` del JWT; delega a `PortfolioService.getPositions()`; retorna 200 (depende de T015)

**Checkpoint US1**: `GET /portfolio/positions` retorna posiciones con ganancia no realizada calculada; portafolio vacío retorna lista vacía con totales en 0.

---

## Phase 4: User Story 2 — Reporte de rentabilidad por período (P2)

**Goal**: `GET /portfolio/report?period=MONTH` retorna el reporte filtrado por período (TODAY, WEEK, MONTH, YEAR, CUSTOM). Para CUSTOM valida que `from` <= `to` y que ambas fechas estén presentes.

**Independent Test**: `GET /portfolio/report?period=MONTH` con 3 transacciones SELL en el último mes y 2 fuera del período → `transactions` contiene solo las 3 del mes; `GET /portfolio/report?period=CUSTOM&from=2026-01-01&to=2026-03-31` filtra correctamente; `from > to` → 400 `INVALID_PERIOD`.

- [x] T017 [P] [US2] Crear `PeriodResolver` en `backend/portfolio/src/main/java/com/accioneselbosque/portfolio/service/PeriodResolver.java` — método `resolve(ReportPeriod period, LocalDate from, LocalDate to)`: TODAY/WEEK/MONTH/YEAR → rangos estándar; CUSTOM → validar `from != null && to != null && !from.isAfter(to)`, lanzar `InvalidPeriodException` si falla (depende de T007, T011)
- [x] T018 [US2] Añadir método `getReport(Long investorId, ReportPeriod period, LocalDate from, LocalDate to)` en `PortfolioService` en `backend/portfolio/src/main/java/com/accioneselbosque/portfolio/service/PortfolioService.java` — resolver rango; cargar posiciones actuales; cargar `Transaction` del período; calcular `totalRealizedGain`; retornar `PortfolioReportDto` (depende de T015, T017)
- [x] T019 [US2] Añadir `GET /portfolio/report` en `PortfolioController` en `backend/portfolio/src/main/java/com/accioneselbosque/portfolio/controller/PortfolioController.java` — query params: `period` (default MONTH), `from`, `to`; delega a `PortfolioService.getReport()`; 400 `INVALID_PERIOD` si fechas inválidas (depende de T018)

**Checkpoint US2**: Filtrado por período correcto; CUSTOM con rango inválido retorna 400.

---

## Phase 5: User Story 3 — Exportación del reporte (P3)

**Goal**: `GET /portfolio/report/export?period=MONTH` genera y retorna un CSV en memoria con detalle completo del reporte. `Content-Type: text/csv`, `Content-Disposition: attachment; filename="portfolio-report.csv"`.

**Independent Test**: Respuesta con `Content-Type: text/csv`; CSV contiene fila de cabecera, fila por posición abierta y fila por transacción; `Content-Disposition` incluye filename correcto.

- [x] T020 [P] [US3] Crear `CsvReportExporter` en `backend/portfolio/src/main/java/com/accioneselbosque/portfolio/service/CsvReportExporter.java` — método `export(PortfolioReportDto report)`: generar CSV en memoria; sección "POSICIONES ABIERTAS" y sección "TRANSACCIONES"; retornar `byte[]` (depende de T010)
- [x] T021 [US3] Añadir `GET /portfolio/report/export` en `PortfolioController` en `backend/portfolio/src/main/java/com/accioneselbosque/portfolio/controller/PortfolioController.java` — llama a `PortfolioService.getReport()` luego a `CsvReportExporter.export()`; retorna `ResponseEntity<byte[]>` con headers correctos (depende de T019, T020)

**Checkpoint US3**: CSV descargable con todas las posiciones y transacciones del período.

---

## Phase 6: Polish

- [x] T022 [P] Validar edge case portafolio vacío en `PortfolioService` — retornar `PortfolioPositionsResponse` con `positions=[]` y totales en 0 en `backend/portfolio/src/main/java/com/accioneselbosque/portfolio/service/PortfolioService.java`
- [x] T023 [P] Validar fallback de precio no disponible en `PortfolioService` — usar `avgPurchasePrice` con `unrealizedGain=0` y log WARN en `backend/portfolio/src/main/java/com/accioneselbosque/portfolio/service/PortfolioService.java`
- [x] T024 [P] Registrar `PositionUpdateService` y `PortfolioFacade` como beans públicos en `backend/portfolio/src/main/java/com/accioneselbosque/portfolio/PortfolioModuleConfig.java` (`@Configuration`)
- [x] T025 Ejecutar suite: `mvn test -pl backend/portfolio` — todos los tests pasan; `mvn verify -pl backend/app` sin errores de integración DB

---

## Dependencias clave

- T002, T003 (migraciones) → deben aplicarse antes de levantar la app con el módulo `portfolio`
- T004, T005 (entidades) → bloquean T008, T009, T013, T015
- T008, T009 (repositorios) → bloquean T013, T014, T015, T018
- T010 (DTOs) → bloquean T015, T018, T020
- T015 (`getPositions`) → bloquea T016, T018
- T017 (`PeriodResolver`) → bloquea T018
- T018 (`getReport`) → bloquea T019
- T019 (endpoint `/report`) → bloquea T021
- T020 (`CsvReportExporter`) → bloquea T021
- T013 (`PositionUpdateService`) → necesario para que `orders` notifique ejecuciones

## Parallel Execution Example — US1

```
Prereqs completados (T001–T012)
        │
        ├──[Agente A]── T013 PositionUpdateService
        │
        └──[Agente B]── T014 PortfolioFacade
                        T015 PortfolioService.getPositions  ← espera T008, T009
                        T016 PortfolioController GET /positions  ← espera T015
```

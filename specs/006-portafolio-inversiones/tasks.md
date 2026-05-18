# Tasks: AB-27 — Visualización de Portafolio de Inversiones

**Input**: `specs/006-portafolio-inversiones/` (plan.md, spec.md, data-model.md, contracts/portfolio-api.md, research.md)
**Module**: `portfolio` — `com.accioneselbosque.portfolio_query_service`
**Branch**: `006-portafolio-inversiones`
**Prerequisito**: AB-26 (portfolio_db schema existe); AB-28 (StockSnapshotService disponible in-process para precios actuales)

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Puede correr en paralelo
- **[US1]**: Ver posiciones abiertas con precio actual y P&L por posición (P1)
- **[US2]**: Ver resumen del portafolio con totales agregados (P2)

---

## Phase 1: Setup — Migración DB

- [x] T001 Crear migración `backend/portfolio/src/main/resources/db/migration/V3__create_position_table.sql` — tabla `position` (id UUID PK, investor_id UUID NOT NULL, symbol VARCHAR(20) NOT NULL, name VARCHAR(100) NOT NULL, quantity INT NOT NULL CHECK > 0, avg_buy_price DECIMAL(18,2) NOT NULL CHECK > 0, currency VARCHAR(3) DEFAULT 'COP', updated_at, created_at); constraint UNIQUE(investor_id, symbol); índice `idx_position_investor(investor_id)`

**Checkpoint Setup**: V3 aplica sin errores; constraint UNIQUE verificable.

---

## Phase 2: Foundational — Entidad, repositorio y calculador

- [x] T002 Crear entidad JPA `Position` en `backend/portfolio/src/main/java/com/accioneselbosque/portfolio_query_service/model/Position.java` — id (UUID), investorId (UUID), symbol, name, quantity (int), avgBuyPrice (BigDecimal), currency, updatedAt (@PreUpdate), createdAt; `@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"investor_id","symbol"}))`
- [x] T003 Crear `PositionRepository` en `repository/PositionRepository.java` — `findByInvestorId(UUID)`, `findByInvestorIdAndSymbol(UUID, String)` retorna `Optional<Position>`
- [x] T004 Crear `PositionCalculator` en `service/PositionCalculator.java` — métodos estáticos o @Component: `recalculateAvgPrice(int oldQty, BigDecimal oldAvg, int addedQty, BigDecimal addedPrice)` ? `(oldQty×oldAvg + addedQty×addedPrice) / (oldQty+addedQty)` con escala 2 HALF_UP; `pnlAmount(BigDecimal currentPrice, BigDecimal avgBuyPrice, int qty)`; `pnlPercent(BigDecimal pnlAmount, BigDecimal avgBuyPrice, int qty)`

**Checkpoint Foundational**: `PositionCalculator` verifica fórmulas con datos conocidos; compilación limpia.

---

## Phase 3: User Story 1 — Ver posiciones con P&L (P1) ?? MVP

**Goal**: `GET /portfolio/holdings` retorna todas las posiciones abiertas enriquecidas con `currentPrice` (in-process de StockSnapshotService) y P&L calculado en memoria.

**Independent Test**: Investor con posición PFBCOLOM 10 unidades a avgBuyPrice=38900, currentPrice=39500 ? positionValue=395000, pnlAmount=6000, pnlPercent=1.54. Sin posiciones ? lista vacía.

### Tests — US1

- [x] T005 [P] [US1] Escribir `PortfolioControllerTest` en `src/test/java/.../controller/PortfolioControllerTest.java` con `@WebMvcTest`: test `GET /portfolio/holdings` ? 200 con lista; test portafolio vacío ? lista vacía; test investor no puede ver portafolio de otro
- [x] T006 [P] [US1] Escribir `PositionCalculatorTest` en `src/test/java/.../service/PositionCalculatorTest.java` — test precio promedio ponderado con 2 compras; test P&L positivo/negativo/cero; test pnlPercent cuando avgBuyPrice == currentPrice ? 0.00
- [x] T007 [P] [US1] Escribir `PortfolioServiceTest` con Mockito: test que `StockSnapshotService` es llamado por cada posición; test fallback cuando snapshot no existe ? currentPrice = avgBuyPrice, pnlAmount = 0

### Implementación — US1

- [x] T008 [P] [US1] Crear `PositionDto` en `dto/PositionDto.java` — symbol, name, quantity, avgBuyPrice, currentPrice, positionValue, pnlAmount, pnlPercent, dayChange, dayChangePct, currency (todos BigDecimal nullable para currentPrice/dayChange cuando no hay snapshot)
- [x] T009 [US1] Implementar `PortfolioService.getHoldings(UUID investorId)` en `service/PortfolioService.java` — (1) cargar posiciones con `positionRepository.findByInvestorId()`; (2) para cada posición: obtener snapshot via `StockSnapshotService.findBySymbol()` in-process; si null ? currentPrice=avgBuyPrice, dayChange=null; (3) calcular positionValue, pnlAmount, pnlPercent con `PositionCalculator`; retornar List<PositionDto> (depende de T004)
- [x] T010 [US1] Implementar `PortfolioController` en `controller/PortfolioController.java` con `GET /portfolio/holdings` — extrae investorId del JWT, delega a `portfolioService.getHoldings()`, retorna 200

**Checkpoint US1**: Holdings retornados con P&L correcto; fallback a avgBuyPrice cuando no hay snapshot.

---

## Phase 4: User Story 2 — Resumen del portafolio (P2)

**Goal**: `GET /portfolio/summary` agrega totalValue, totalPnl, totalPnlPct, totalDayChange y positionCount.

**Independent Test**: Con 2 posiciones ? totalValue = sum(positionValue); totalPnl = sum(pnlAmount); portafolio vacío ? todos los valores = 0.00.

### Tests — US2

- [x] T011 [P] [US2] Agregar test a `PortfolioControllerTest`: `GET /portfolio/summary` ? 200 con campos correctos; portafolio vacío ? todos cero
- [x] T012 [P] [US2] Agregar test a `PortfolioServiceTest`: `getSummary()` suma correctamente con 2 posiciones; `totalPnlPct` = 0 cuando totalCost = 0

### Implementación — US2

- [x] T013 [P] [US2] Crear `PortfolioSummaryDto` en `dto/PortfolioSummaryDto.java` — investorId, totalValue, totalCost, totalPnl, totalPnlPct, totalDayChange, positionCount, currency
- [x] T014 [US2] Implementar `PortfolioService.getSummary(UUID investorId)` — reutilizar `getHoldings()` (T009); agregar en memoria: totalValue=SUM(positionValue), totalCost=SUM(avgBuyPrice×qty), totalPnl=totalValue-totalCost, totalPnlPct=totalPnl/totalCost×100 (0 si cost=0), totalDayChange=SUM(dayChange×qty ignorando nulls); retornar PortfolioSummaryDto
- [x] T015 [US2] Agregar `GET /portfolio/summary` en `PortfolioController` (depende de T014)

**Checkpoint US2**: Summary correcto con múltiples posiciones; portafolio vacío retorna ceros.

---

## Phase 5: Polish

- [x] T016 Verificar constraint UNIQUE(investor_id, symbol) — intentar insertar posición duplicada en test lanza excepción de DB; la capa de servicio debe usar `findByInvestorIdAndSymbol()` + update en vez de doble insert
- [x] T017 [P] Ejecutar suite: `./mvnw test -pl backend/portfolio`

---

## Dependencias clave

- T002 (Position entity) ? depende de T001 (V3 migration)
- T009 (PortfolioService) ? inyecta `StockSnapshotService` de market-data in-process (requiere AB-28 en classpath)
- `PortfolioService.updatePosition()` (usado por order al ejecutar compras/ventas) debe implementarse como método auxiliar en T009 usando `PositionCalculator.recalculateAvgPrice()`
- US2 puede desarrollarse en paralelo con US1 una vez T009 esté completo (getSummary reutiliza getHoldings)

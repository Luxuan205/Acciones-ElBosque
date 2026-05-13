# Tasks: AB-28 — Dashboard de Comportamiento de Acciones

**Input**: `specs/007-dashboard-acciones/` (plan.md, spec.md, data-model.md, contracts/market-api.md, research.md)
**Module**: `market-data` — `com.accioneselbosque.market_data_service`
**Branch**: `007-dashboard-acciones`
**Prerequisito**: AB-29 (MarketStatusService.isMarketOpen() disponible in-process para el indicador `stale`)

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Puede correr en paralelo
- **[US1]**: Ver lista de acciones con precio/variación; buscar y ordenar (P1)
- **[US2]**: Ver detalle de acción + puntos intradiarios (P2)

---

## Phase 1: Setup — Migraciones DB con seed data

- [ ] T001 Crear migración `backend/market-data/src/main/resources/db/migration/V1__create_stock_snapshot_table.sql` — tabla `stock_snapshot` (id UUID PK, symbol VARCHAR(20) UNIQUE NOT NULL, name VARCHAR(100), current_price, previous_close, day_change DECIMAL(18,2) DEFAULT 0, day_change_pct DECIMAL(7,4) DEFAULT 0, volume BIGINT DEFAULT 0, updated_at TIMESTAMP DEFAULT NOW()); INSERT seed de 10 acciones colombianas: PFBCOLOM(39500), NUTRESA(68200), ISA(22100), ECOPETROL(1950), CEMARGOS(7800), GRUPOSURA(18300), EXITO(13700), ETB(410), PFDAVVNDA(52600), CLH(2350)
- [ ] T002 Crear migración `backend/market-data/src/main/resources/db/migration/V2__create_intraday_price_point_table.sql` — tabla `intraday_price_point` (id UUID PK, symbol VARCHAR(20) NOT NULL, timestamp TIMESTAMP NOT NULL, price DECIMAL(18,2), volume BIGINT DEFAULT 0); constraint UNIQUE(symbol, timestamp); índice `idx_intraday_symbol_ts(symbol, timestamp DESC)`

**Checkpoint Setup**: V1 aplica con 10 símbolos seed; V2 crea tabla intraday.

---

## Phase 2: Foundational — Entidades, repositorios y configuración

- [ ] T003 [P] Crear entidad JPA `StockSnapshot` en `backend/market-data/src/main/java/com/accioneselbosque/market_data_service/model/StockSnapshot.java` — id (UUID), symbol (UNIQUE), name, currentPrice, previousClose, dayChange, dayChangePct, volume (long), updatedAt (@PreUpdate)
- [ ] T004 [P] Crear entidad JPA `IntradayPricePoint` en `backend/market-data/src/main/java/com/accioneselbosque/market_data_service/model/IntradayPricePoint.java` — id (UUID), symbol, timestamp (LocalDateTime), price, volume; `@Table(uniqueConstraints = @UniqueConstraint(columnNames={"symbol","timestamp"}))`
- [ ] T005 [P] Crear `StockSnapshotRepository` en `repository/StockSnapshotRepository.java` — `findAll(Sort)`, `findBySymbolContainingIgnoreCaseOrNameContainingIgnoreCase(String, String, Sort)`, `findBySymbol(String)` retorna `Optional<StockSnapshot>`, `saveAll(List<StockSnapshot>)`
- [ ] T006 [P] Crear `IntradayPricePointRepository` en `repository/IntradayPricePointRepository.java` — `findBySymbolAndTimestampBetween(String, LocalDateTime, LocalDateTime)`, `deleteByTimestampBefore(LocalDateTime)`
- [ ] T007 Crear `GlobalExceptionHandler` en `exception/GlobalExceptionHandler.java` — `SymbolNotFoundException` → 404; `IllegalArgumentException` (sort inválido) → 400

**Checkpoint Foundational**: 10 snapshots seed cargables; compilación limpia.

---

## Phase 3: User Story 1 — Listar, buscar y ordenar acciones (P1) 🎯 MVP

**Goal**: `GET /market/stocks` retorna todas las acciones con precio actual. Soporta `search` (símbolo/nombre) y `sort` (name_asc, name_desc, dayChangePct_asc, dayChangePct_desc). Incluye `marketOpen` y campo `stale` cuando el mercado está cerrado.

**Independent Test**: Sin parámetros → 10 acciones ordenadas por nombre. `?search=eco` → solo ECOPETROL. `?sort=dayChangePct_desc` → ordenado descendente. Cuando mercado cerrado → `marketOpen: false`, `stale: true` en todas.

### Tests — US1

- [ ] T008 [P] [US1] Escribir `MarketControllerTest` en `src/test/java/.../controller/MarketControllerTest.java` con `@WebMvcTest`: test sin parámetros → 200 con lista; test `?search=pfb` → filtra correctamente; test `?sort=INVALID` → 400; test `marketOpen` refleja estado del `MarketStatusService`
- [ ] T009 [P] [US1] Escribir `StockSnapshotServiceTest` con Mockito: test `listStocks()` llama repositorio con Sort correcto; test `searchStocks()` pasa mismo string a ambos parámetros del método derivado; test `stale` = true cuando mercado cerrado

### Implementación — US1

- [ ] T010 [P] [US1] Crear `StockSummaryDto` en `dto/StockSummaryDto.java` — symbol, name, currentPrice, previousClose, dayChange, dayChangePct, volume, updatedAt, stale (boolean)
- [ ] T011 [US1] Implementar `StockSnapshotService` en `service/StockSnapshotService.java` — método `listStocks(String search, String sort)`: (1) validar `sort` contra valores permitidos → `IllegalArgumentException` si inválido; (2) construir `Sort` apropiado; (3) si search no null/blank → `findBySymbolContainingIgnoreCaseOrNameContainingIgnoreCase()`; si no → `findAll(sort)`; (4) mapear a `StockSummaryDto` con `stale = !MarketStatusService.isMarketOpen()`
- [ ] T012 [US1] Implementar `MarketController` en `controller/MarketController.java` con `GET /market/stocks` — `@RequestParam` opcionales search y sort (default "name_asc"); delegar a `StockSnapshotService.listStocks()`; incluir `marketOpen` en response (depende de T011)

**Checkpoint US1**: Lista, búsqueda y ordenamiento funcionan; `stale` y `marketOpen` correctos.

---

## Phase 4: User Story 2 — Detalle de acción e intraday (P2)

**Goal**: `GET /market/stocks/{symbol}` retorna detalle completo. `GET /market/stocks/{symbol}/intraday` retorna puntos de 5 min de la sesión actual. El ingestor `@Scheduled` actualiza snapshots cada 60s y registra puntos intradiarios. Purga intraday al cierre del mercado.

**Independent Test**: `GET /market/stocks/PFBCOLOM` → 200 con todos los campos. `GET .../FAKE` → 404. Después de 65s, `updatedAt` del snapshot debe haber cambiado.

### Tests — US2

- [ ] T013 [P] [US2] Agregar tests a `MarketControllerTest`: `GET /market/stocks/PFBCOLOM` → 200 con detalle; `GET /market/stocks/FAKE` → 404; `GET /market/stocks/PFBCOLOM/intraday` → 200 con lista de puntos
- [ ] T014 [P] [US2] Escribir `MarketDataIngestorTest` con `@SpringBootTest(webEnvironment=NONE)`: test que `ingestPrices()` genera variación ±2% sobre precio anterior; test que `purgeIntraday()` elimina puntos anteriores al inicio de la sesión actual

### Implementación — US2

- [ ] T015 [P] [US2] Crear `StockDetailDto` en `dto/StockDetailDto.java` — todos los campos de StockSummaryDto + `marketOpen` (boolean)
- [ ] T016 [P] [US2] Crear `IntradayDataDto` en `dto/IntradayDataDto.java` — symbol, date (LocalDate), interval (String "5min"), points (List<{timestamp, price, volume}>)
- [ ] T017 [US2] Implementar `StockSnapshotService.findBySymbol(String symbol)` — `findBySymbol()` del repo, lanzar `SymbolNotFoundException` si no existe; mapear a `StockDetailDto` con `marketOpen` del `MarketStatusService`
- [ ] T018 [US2] Implementar `StockSnapshotService.getIntraday(String symbol)` — verificar símbolo existe; cargar `IntradayPricePoint` de hoy entre startOfDay y now(); mapear a `IntradayDataDto`
- [ ] T019 [US2] Implementar `MarketDataIngestor` en `service/MarketDataIngestor.java` con `@Scheduled(fixedRate=60000)` — para cada `StockSnapshot`: nueva variación = `currentPrice × (1 + random(-0.02..0.02))`; actualizar `dayChange = newPrice - previousClose`, `dayChangePct`; guardar punto intraday si mercado abierto; `saveAll()`
- [ ] T020 [US2] Agregar `@Scheduled` de purga en `MarketDataIngestor` — al detectar cierre del mercado (`isMarketOpen()` pasa de true a false): `deleteByTimestampBefore(startOfToday)`
- [ ] T021 [US2] Agregar `GET /market/stocks/{symbol}` y `GET /market/stocks/{symbol}/intraday` en `MarketController` (depende de T017, T018)

**Checkpoint US2**: Detalle e intraday funcionan; ingestor actualiza precios cada 60s; purga al cierre.

---

## Phase 5: Polish

- [ ] T022 Exponer `StockSnapshotService` como bean público consumible por portfolio (AB-27) y market-data (AB-36) in-process — verificar visibilidad del bean en el contexto de Spring
- [ ] T023 [P] Ejecutar suite: `./mvnw test -pl backend/market-data`

---

## Dependencias clave

- T003, T004 → dependen de T001, T002 (migrations)
- T011 (StockSnapshotService) → inyecta `MarketStatusService` de configuration-service in-process (requiere AB-29)
- T019 (MarketDataIngestor) → depende de T005, T006 (repositorios) y T011 (StockSnapshotService para leer snapshots)
- `StockSnapshotService.findBySymbol()` (T017) es el método que consumen portfolio (AB-27) y watchlist (AB-36) in-process

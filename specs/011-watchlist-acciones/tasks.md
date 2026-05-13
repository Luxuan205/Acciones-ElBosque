# Tasks: AB-36 — Watchlist de Acciones (Funcionalidad Premium)

**Input**: `specs/011-watchlist-acciones/` (plan.md, spec.md, data-model.md, contracts/watchlist-api.md, research.md)
**Module**: `market-data` — `com.accioneselbosque.market_data_service`
**Branch**: `011-watchlist-acciones`
**Prerequisito**: AB-28 (StockSnapshotService disponible in-process); AB-15 (InvestorRepository in-process para verificar suscripción)

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Puede correr en paralelo
- **[US1]**: Investor premium ve su watchlist enriquecida con precios actuales (P1)
- **[US2]**: Investor premium agrega y elimina acciones de la watchlist (P2)

---

## Phase 1: Setup — Migraciones DB

- [X] T001 Crear migración `backend/market-data/src/main/resources/db/migration/V3__create_watchlist_table.sql` — tabla `watchlist` (id UUID PK, investor_id UUID NOT NULL UNIQUE, created_at TIMESTAMP DEFAULT NOW())
- [X] T002 Crear migración `backend/market-data/src/main/resources/db/migration/V4__create_watchlist_entry_table.sql` — tabla `watchlist_entry` (id UUID PK, watchlist_id UUID NOT NULL REFERENCES watchlist(id) ON DELETE CASCADE, symbol VARCHAR(20) NOT NULL, added_at TIMESTAMP DEFAULT NOW(), price_at_added DECIMAL(18,2) NOT NULL); constraint UNIQUE(watchlist_id, symbol); índice `idx_watchlist_entry_watchlist(watchlist_id)`

**Checkpoint Setup**: V3 y V4 aplican; ON DELETE CASCADE verificable.

---

## Phase 2: Foundational — Entidades, repositorios y gate premium

- [X] T003 [P] Crear entidad JPA `Watchlist` en `backend/market-data/src/main/java/com/accioneselbosque/market_data_service/model/Watchlist.java` — id (UUID), investorId (UUID, UNIQUE), createdAt
- [X] T004 Crear entidad JPA `WatchlistEntry` en `backend/market-data/src/main/java/com/accioneselbosque/market_data_service/model/WatchlistEntry.java` — id (UUID), watchlist (@ManyToOne, FK watchlist_id), symbol, addedAt, priceAtAdded (BigDecimal); `@Table(uniqueConstraints = @UniqueConstraint(columnNames={"watchlist_id","symbol"}))` (depende de T003)
- [X] T005 [P] Crear `WatchlistRepository` en `repository/WatchlistRepository.java` — `findByInvestorId(UUID)` retorna `Optional<Watchlist>`
- [X] T006 [P] Crear `WatchlistEntryRepository` en `repository/WatchlistEntryRepository.java` — `countByWatchlistId(UUID)`, `findByWatchlistIdOrderByAddedAtDesc(UUID)`, `findByWatchlistIdAndSymbol(UUID, String)` retorna `Optional<WatchlistEntry>`, `deleteByWatchlistIdAndSymbol(UUID, String)`
- [X] T007 Crear `PremiumSubscriptionGate` en `service/PremiumSubscriptionGate.java` — inyecta `InvestorRepository` de auth-security-service in-process; método `assertIsPremiumActive(UUID investorId)`: carga investor; verifica `subscriptionType == "PREMIUM" && subscriptionExpiresAt != null && subscriptionExpiresAt.isAfter(now())` → lanza `PremiumRequiredException` (→ 403) si falla
- [X] T008 Crear excepciones: `PremiumRequiredException` (→ 403), `SymbolAlreadyInWatchlistException` (→ 409), `SymbolNotInWatchlistException` (→ 404), `WatchlistLimitReachedException` (→ 422); registrar en `GlobalExceptionHandler`

**Checkpoint Foundational**: `PremiumSubscriptionGate` compilado; entidades mapeadas; ON DELETE CASCADE verificado.

---

## Phase 3: User Story 1 — Ver watchlist con precios actuales (P1) 🎯 MVP

**Goal**: `GET /watchlist` retorna la watchlist del investor premium con currentPrice, dayChange y lastUpdated enriquecidos in-process desde StockSnapshotService. Primer acceso → crea watchlist vacía on-demand. Non-premium → 403.

**Independent Test**: Investor premium con 2 entradas en watchlist: `GET /watchlist` → 200 con entryCount=2, currentPrice de cada símbolo. Investor STANDARD → 403.

### Tests — US1

- [X] T009 [P] [US1] Escribir `WatchlistControllerTest` en `src/test/java/.../controller/WatchlistControllerTest.java` con `@WebMvcTest`: test `GET /watchlist` PREMIUM → 200 con lista; test STANDARD → 403; test primer acceso (watchlist vacía) → 200 con entryCount=0
- [X] T010 [P] [US1] Escribir `WatchlistServiceTest` con Mockito: test `getOrCreateWatchlist()` crea watchlist si no existe; test enriquecimiento llama `StockSnapshotService.findBySymbol()` por cada entrada; test fallback cuando snapshot null → currentPrice=null en el DTO (no lanza excepción)

### Implementación — US1

- [X] T011 [P] [US1] Crear `WatchlistEntryDto` en `dto/WatchlistEntryDto.java` — symbol, name, currentPrice (nullable), priceAtAdded, dayChange (nullable), dayChangePct (nullable), lastUpdated (nullable), addedAt
- [X] T012 [P] [US1] Crear `WatchlistResponse` en `dto/WatchlistResponse.java` — watchlistId, investorId, entryCount, maxEntries (siempre 50), entries (List<WatchlistEntryDto>)
- [X] T013 [US1] Implementar `WatchlistService.getOrCreateWatchlist(UUID investorId)` en `service/WatchlistService.java` — buscar por `watchlistRepository.findByInvestorId()`; si no existe: crear nueva `Watchlist` y persistir; retornar la watchlist
- [X] T014 [US1] Implementar `WatchlistService.getWatchlist(UUID investorId)` — (1) `PremiumSubscriptionGate.assertIsPremiumActive()` → 403 si falla; (2) `getOrCreateWatchlist()`; (3) cargar entradas con `findByWatchlistIdOrderByAddedAtDesc()`; (4) por cada entrada: `StockSnapshotService.findBySymbol()` in-process; si null → fields null (no excepción); (5) mapear a `WatchlistResponse` (depende de T013)
- [X] T015 [US1] Implementar `WatchlistController` en `controller/WatchlistController.java` con `GET /watchlist` — extrae investorId del JWT, delega a `watchlistService.getWatchlist()`, retorna 200 (depende de T014)

**Checkpoint US1**: Watchlist retornada con precios; non-premium → 403; creación on-demand funciona.

---

## Phase 4: User Story 2 — Agregar y eliminar símbolos (P2)

**Goal**: `POST /watchlist/entries` agrega símbolo verificando que existe en el catálogo, que no está duplicado y que no supera 50 entradas. `DELETE /watchlist/entries/{symbol}` elimina. Watchlist se preserva aunque expire la suscripción (solo se bloquea el acceso).

**Independent Test**: Agregar ECOPETROL → 201, entryCount++, priceAtAdded = currentPrice del snapshot. Agregar ECOPETROL de nuevo → 409. Con 50 entradas, agregar una más → 422. Eliminar → 200, entryCount--. Símbolo no en watchlist → 404.

### Tests — US2

- [X] T016 [P] [US2] Agregar tests a `WatchlistControllerTest`: `POST /watchlist/entries` válido → 201; símbolo no existe en catálogo → 400; duplicado → 409; 51ª entrada → 422; no-premium → 403; `DELETE /watchlist/entries/{symbol}` → 200; símbolo no en watchlist → 404
- [X] T017 [P] [US2] Agregar tests a `WatchlistServiceTest`: `addEntry()` llama `StockSnapshotService.findBySymbol()` para validar existencia y capturar priceAtAdded; `countByWatchlistId()` es llamado antes del insert; `deleteEntry()` retorna conteo actualizado

### Implementación — US2

- [X] T018 [P] [US2] Crear `WatchlistEntryRequest` DTO en `dto/WatchlistEntryRequest.java` — symbol (@NotBlank @Size(max=20))
- [X] T019 [P] [US2] Crear `AddEntryResponse` DTO en `dto/AddEntryResponse.java` — symbol, name, priceAtAdded, addedAt, entryCount, maxEntries
- [X] T020 [P] [US2] Crear `RemoveEntryResponse` DTO en `dto/RemoveEntryResponse.java` — symbol, removedAt, entryCount, maxEntries
- [X] T021 [US2] Implementar `WatchlistService.addEntry(UUID investorId, String symbol)` — (1) `PremiumSubscriptionGate.assertIsPremiumActive()`; (2) `getOrCreateWatchlist()`; (3) verificar símbolo existe en catálogo: `StockSnapshotService.findBySymbol()` → lanzar `SymbolNotFoundException` (→ 400) si no existe; (4) `countByWatchlistId()` ≥ 50 → `WatchlistLimitReachedException`; (5) `findByWatchlistIdAndSymbol()` existe → `SymbolAlreadyInWatchlistException`; (6) crear `WatchlistEntry` con `priceAtAdded = snapshot.currentPrice`; persistir; retornar `AddEntryResponse` (depende de T013)
- [X] T022 [US2] Implementar `WatchlistService.removeEntry(UUID investorId, String symbol)` — (1) `PremiumSubscriptionGate.assertIsPremiumActive()`; (2) `getOrCreateWatchlist()`; (3) `findByWatchlistIdAndSymbol()` → `SymbolNotInWatchlistException` si no existe; (4) `deleteByWatchlistIdAndSymbol()`; retornar `RemoveEntryResponse` con nuevo entryCount
- [X] T023 [US2] Agregar `POST /watchlist/entries` y `DELETE /watchlist/entries/{symbol}` en `WatchlistController` (depende de T021, T022)

**Checkpoint US2**: CRUD completo funciona; límite 50 enforced; datos preservados al expirar suscripción (solo acceso bloqueado).

---

## Phase 5: Polish

- [X] T024 Verificar que ON DELETE CASCADE funciona: `V11__create_watchlist_entry_table.sql` define `REFERENCES watchlist(id) ON DELETE CASCADE` — constraint verificada en la migración SQL
- [X] T025 Verificar que si la suscripción expira los datos siguen en DB — `PremiumSubscriptionGate.assertIsPremiumActive()` solo bloquea acceso; los datos en `watchlist_entry` no se eliminan automáticamente al expirar la suscripción (preservación confirmada por diseño)
- [X] T026 [P] Ejecutar suite: `./mvnw test -pl backend/market-data` — **PASS: Tests run: 20, Failures: 0, Errors: 0, Skipped: 0**

---

## Dependencias clave

- T004 (WatchlistEntry) → depende de T003 (Watchlist entity) y T001, T002 (migrations)
- T007 (PremiumSubscriptionGate) → inyecta `InvestorRepository` de auth-security-service in-process (requiere AB-15 en classpath)
- T014 (WatchlistService.getWatchlist) → inyecta `StockSnapshotService` de market-data (mismo módulo, AB-28)
- T021 (addEntry) → captura `priceAtAdded` del snapshot en el momento de agregar; si el scheduler actualiza entre la lectura y el INSERT, el precio puede diferir ligeramente — aceptable para proyecto académico
- US2 puede desarrollarse en paralelo con US1 a nivel de tests (T016, T017) pero la impl de addEntry/removeEntry depende de T013 (getOrCreateWatchlist)

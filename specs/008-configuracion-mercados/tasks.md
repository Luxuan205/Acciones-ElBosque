# Tasks: AB-29 — Gestión de Horarios y Configuración de Mercados

**Input**: `specs/008-configuracion-mercados/` (plan.md, spec.md, data-model.md, contracts/market-config-api.md, research.md)
**Module**: `configuration` — `com.accioneselbosque.configuration`
**Branch**: `008-configuracion-mercados`
**Sin prerequisitos** — módulo base del que dependen order y market-data.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Puede correr en paralelo
- **[US1]**: Admin configura horario; estado OPEN/CLOSED se actualiza automáticamente (P1)
- **[US2]**: Admin gestiona festivos; el estado considera festivos en el cálculo (P2)

---

## Phase 1: Setup — Migraciones DB con seed

- [x] T001 Crear migración `backend/configuration/src/main/resources/db/migration/V1__create_market_schedule_table.sql` — tabla `market_schedule` (id UUID PK, open_time TIME NOT NULL, close_time TIME NOT NULL, working_days INTEGER NOT NULL CHECK BETWEEN 1 AND 127, updated_at TIMESTAMP DEFAULT NOW(), updated_by UUID NULL); INSERT seed: open_time='09:00:00', close_time='15:30:00', working_days=31 (Mon–Fri)
- [x] T002 Crear migración `backend/configuration/src/main/resources/db/migration/V2__create_market_holiday_table.sql` — tabla `market_holiday` (id UUID PK, date DATE NOT NULL UNIQUE, description VARCHAR(200) NOT NULL, type VARCHAR(20) NOT NULL DEFAULT 'NATIONAL' CHECK('NATIONAL','REGIONAL','SPECIAL'), created_at TIMESTAMP DEFAULT NOW()); índice `idx_market_holiday_date(date)`

**Checkpoint Setup**: Seed de horario Mon–Fri 09:00–15:30 cargado; tablas creadas.

---

## Phase 2: Foundational — Modelo, converter y repositorios

- [x] T003 Crear `WorkingDaysConverter` en `backend/configuration/src/main/java/com/accioneselbosque/configuration/config/WorkingDaysConverter.java` implementando `@Converter AttributeConverter<Set<DayOfWeek>, Integer>` — bit0=MONDAY(1), bit1=TUESDAY(2), bit2=WEDNESDAY(4), bit3=THURSDAY(8), bit4=FRIDAY(16); `convertToDatabaseColumn` suma los bits; `convertToEntityAttribute` reconstruye el Set
- [x] T004 Crear entidad JPA `MarketSchedule` en `backend/configuration/src/main/java/com/accioneselbosque/configuration/model/MarketSchedule.java` — id (UUID), openTime (LocalTime), closeTime (LocalTime), workingDays (Set<DayOfWeek> con @Convert(converter=WorkingDaysConverter.class)), updatedAt, updatedBy (UUID nullable) (depende de T003)
- [x] T005 Crear entidad JPA `MarketHoliday` en `backend/configuration/src/main/java/com/accioneselbosque/configuration/model/MarketHoliday.java` — id (UUID), date (LocalDate), description, type (String), createdAt
- [x] T006 [P] Crear `MarketScheduleRepository` en `repository/MarketScheduleRepository.java` — `findFirst()` retorna `Optional<MarketSchedule>` (single-row table)
- [x] T007 [P] Crear `MarketHolidayRepository` en `repository/MarketHolidayRepository.java` — `findByDate(LocalDate)`, `existsByDate(LocalDate)`, `findByDateBetween(LocalDate, LocalDate)` (para listar por año)
- [x] T008 Crear excepciones: `HolidayAlreadyExistsException` (→ 409), `HolidayNotFoundException` (→ 404), `InvalidScheduleException` (→ 400); crear `GlobalExceptionHandler`

**Checkpoint Foundational**: `WorkingDaysConverter` convierte 31 ↔ {MON,TUE,WED,THU,FRI}; compilación limpia.

---

## Phase 3: User Story 1 — Horario bursátil y estado automático (P1) 🎯 MVP

**Goal**: `GET /config/market/status` retorna OPEN/CLOSED. `GET|PUT /config/market/schedule` gestiona el horario. Un `AtomicBoolean` en `MarketStatusService` se refresca cada 60s con un `@Scheduled`.

**Independent Test**: Con horario Mon–Fri 09:00–15:30 UTC-5 y prueba ejecutada en horario hábil → `status: "OPEN"`. Con horario modificado a 00:00–00:01 → `status: "CLOSED"`. PUT horario con openTime ≥ closeTime → 400.

### Tests — US1

- [x] T009 [P] [US1] Escribir `MarketConfigControllerTest` en `src/test/java/.../controller/MarketConfigControllerTest.java` con `@WebMvcTest`: test `GET /config/market/status` → 200 con status, nextClose, nextOpen; test `PUT /config/market/schedule` admin → 200; test `PUT` no-admin → 403; test openTime ≥ closeTime → 400
- [x] T010 [P] [US1] Escribir `MarketStatusServiceTest` con Mockito: test `isMarketOpen()` retorna true dentro del horario en día hábil; retorna false fuera de horario; retorna false en fin de semana; retorna false en festivo; test `AtomicBoolean` se actualiza correctamente con `refreshStatus()`

### Implementación — US1

- [x] T011 [P] [US1] Crear `MarketScheduleDto` en `dto/MarketScheduleDto.java` — openTime (String "HH:mm"), closeTime (String "HH:mm"), workingDays (List<String>), timezone (siempre "America/Bogota")
- [x] T012 [P] [US1] Crear `MarketStatusDto` en `dto/MarketStatusDto.java` — status ("OPEN"/"CLOSED"), today, currentTime, timezone, nextClose (nullable), nextOpen (nullable), isHoliday (boolean), holidayName (nullable)
- [x] T013 [US1] Implementar `MarketStatusService` en `service/MarketStatusService.java` — `AtomicBoolean marketOpen` actualizado por `@Scheduled(fixedRate=60000)`; método público `isMarketOpen()` retorna el valor del AtomicBoolean; método privado `refreshStatus()`: (1) `ZoneId.of("America/Bogota")`; (2) si hoy es festivo → false; (3) si DayOfWeek no en workingDays → false; (4) si LocalTime fuera de [openTime, closeTime) → false; else true; construir `MarketStatusDto` completo
- [x] T014 [US1] Implementar `MarketScheduleService` en `service/MarketScheduleService.java` — `getSchedule()`: `findFirst()` del repo; `updateSchedule(MarketScheduleDto adminId)`: validar openTime < closeTime → `InvalidScheduleException`; persistir; refrescar status inmediatamente (depende de T013)
- [x] T015 [US1] Implementar `MarketConfigController` en `controller/MarketConfigController.java` con: `GET /config/market/status` (sin auth check, cualquier rol); `GET /config/market/schedule` (`@PreAuthorize("hasRole('ADMIN')")`); `PUT /config/market/schedule` (ADMIN) (depende de T013, T014)

**Checkpoint US1**: Estado OPEN/CLOSED correcto con zona horaria America/Bogota; PUT protegido por rol ADMIN; `isMarketOpen()` disponible para otros módulos in-process.

---

## Phase 4: User Story 2 — Gestión de festivos (P2)

**Goal**: Admin puede listar, agregar y eliminar festivos. El cálculo de `isMarketOpen()` considera los festivos.

**Independent Test**: Agregar festivo para hoy → `GET /config/market/status` → `isHoliday: true`, status: CLOSED. Agregar fecha duplicada → 409. DELETE festivo → 204. GET lista → incluye el festivo agregado.

### Tests — US2

- [x] T016 [P] [US2] Agregar tests a `MarketConfigControllerTest`: `POST /config/market/holidays` válido → 201; fecha duplicada → 409; `DELETE /config/market/holidays/{id}` → 204; ID inexistente → 404; `GET /config/market/holidays` → lista; no-admin en POST/DELETE → 403
- [x] T017 [P] [US2] Agregar tests a `MarketStatusServiceTest`: `isMarketOpen()` retorna false cuando hoy es festivo (mock de MarketHolidayRepository retorna festivo)

### Implementación — US2

- [x] T018 [P] [US2] Crear `MarketHolidayDto` en `dto/MarketHolidayDto.java` — id (UUID), date (LocalDate), description, type
- [x] T019 [US2] Implementar `MarketScheduleService.listHolidays(int year)` — `findByDateBetween(firstDayOfYear, lastDayOfYear)`; `addHoliday(MarketHolidayDto)` — verificar `existsByDate()` → `HolidayAlreadyExistsException`; persistir; `deleteHoliday(UUID id)` — buscar → `HolidayNotFoundException`; eliminar
- [x] T020 [US2] Modificar `MarketStatusService.refreshStatus()` para consultar `MarketHolidayRepository.findByDate(today)` y marcar `isHoliday = true` si existe (depende de T013, T007)
- [x] T021 [US2] Agregar en `MarketConfigController`: `GET /config/market/holidays?year=` (ADMIN), `POST /config/market/holidays` (ADMIN), `DELETE /config/market/holidays/{id}` (ADMIN) (depende de T019)

**Checkpoint US2**: Festivos considerados en el cálculo de estado; CRUD completo funciona.

---

## Phase 5: Polish

- [x] T022 Verificar que `MarketStatusService` es el bean que los demás módulos (order, market-data) inyectan in-process — añadir anotación `@Primary` si hay ambigüedad de beans
- [X] T023 [P] Ejecutar suite: `./mvnw test -pl backend/configuration` — **PASS: Tests run: 17, Failures: 0, Errors: 0, Skipped: 0**

---

## Dependencias clave

- T004 (MarketSchedule) → depende de T001 (V1 seed), T003 (WorkingDaysConverter)
- T005 (MarketHoliday) → depende de T002 (V2 migration)
- T013 (MarketStatusService) → depende de T006 (schedule repo), T007 (holiday repo) — es el bean crítico que otros módulos consumen
- T020 → modifica T013; deben coordinarse para no romper el AtomicBoolean
- Este módulo (AB-29) debe compilar ANTES que AB-24 (order) y AB-28 (market-data)

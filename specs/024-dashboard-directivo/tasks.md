# Tasks: AB-39 — Dashboard Directivo

**Input**: `specs/024-dashboard-directivo/` (plan.md, spec.md, contracts/dashboard-api.md)
**Module**: `app` — `com.accioneselbosque` (sin módulo nuevo; el dashboard vive en `backend/app/`)
**Branch**: `024-dashboard-directivo`
**Prerequisito**: Módulos `auth`, `orders`, `portfolio`, `configuration` disponibles in-process; sin nuevas tablas DB

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Puede correr en paralelo
- **[US1]**: Vista de métricas operativas en tiempo real (P1)
- **[US2]**: Resumen financiero del período (P2)
- **[US3]**: Accesos directos a acciones administrativas (P2)

---

## Phase 1: Setup — Estructura de paquetes

- [x] T001 Crear directorio `backend/app/src/main/java/com/accioneselbosque/app/controller/` si no existe
- [x] T002 Crear directorio `backend/app/src/main/java/com/accioneselbosque/app/service/` si no existe
- [x] T003 Crear directorio `backend/app/src/main/java/com/accioneselbosque/app/dto/` si no existe

**Checkpoint Setup**: Estructura de paquetes lista; `backend/app` compila (`mvn compile -pl backend/app`).

---

## Phase 2: Foundational — DTOs y excepciones base

- [x] T004 [P] Crear enum `DashboardPeriod` en `backend/app/src/main/java/com/accioneselbosque/app/dto/DashboardPeriod.java` — valores `TODAY`, `WEEK`, `MONTH`
- [x] T005 [P] Crear DTO `OperationalMetricsDto` en `backend/app/src/main/java/com/accioneselbosque/app/dto/OperationalMetricsDto.java` — record con campos: `String marketStatus`, `long activeOrders`, `long connectedUsers`, `long todayTransactions`, `long activeSystemAlerts`
- [x] T006 [P] Crear DTO `FinancialSummaryDto` en `backend/app/src/main/java/com/accioneselbosque/app/dto/FinancialSummaryDto.java` — record con campos: `DashboardPeriod period`, `LocalDate from`, `LocalDate to`, `BigDecimal totalTransactionVolume`, `BigDecimal estimatedCommissionRevenue`, `long newRegistrations`, `long activePremiumSubscriptions`
- [x] T007 [P] Crear DTO `AdminLinkDto` en `backend/app/src/main/java/com/accioneselbosque/app/dto/AdminLinkDto.java` — record con campos: `String label`, `String endpoint`, `String description`
- [x] T008 [P] Crear excepción `InvalidDashboardPeriodException` (→ 400) en `backend/app/src/main/java/com/accioneselbosque/app/exception/InvalidDashboardPeriodException.java`

**Checkpoint Foundational**: DTOs compilables; enum DashboardPeriod disponible.

---

## Phase 3: User Story 1 — Vista de métricas operativas en tiempo real (P1) 🎯 MVP

**Goal**: `GET /admin/dashboard` retorna métricas operativas en tiempo real: estado del mercado (abierto/cerrado), número de órdenes activas (PENDING + QUEUED), usuarios conectados (sesiones recientes), volumen de transacciones del día. Solo accesible para rol ADMIN.

**Independent Test**: `GET /admin/dashboard` con JWT ADMIN → 200 con `OperationalMetricsDto`; `marketStatus` coincide con `MarketStatusService`; JWT de INVESTOR → 403.

- [x] T009 [P] [US1] Crear `DashboardService` en `backend/app/src/main/java/com/accioneselbosque/app/service/DashboardService.java` — método `getOperationalMetrics()`: (1) `marketStatus` via `MarketStatusService` in-process; (2) `activeOrders` via `OrderRepository.countByStatusIn(List.of(PENDING, QUEUED))` in-process; (3) `connectedUsers` via `InvestorRepository.countByLastLoginAfter(now().minus(30, MINUTES))` in-process; (4) `todayTransactions` via `TransactionRepository.countByExecutedAtAfter(today())` in-process; retornar `OperationalMetricsDto` (depende de T005)
- [x] T010 [US1] Añadir `@PreAuthorize("hasRole('ADMIN')")` en `SecurityConfig` en `backend/auth/src/main/java/com/accioneselbosque/auth/config/SecurityConfig.java` para ruta `/admin/**` — `requestMatchers("/admin/**").hasRole("ADMIN")`
- [x] T011 [US1] Crear `AdminDashboardController` en `backend/app/src/main/java/com/accioneselbosque/app/controller/AdminDashboardController.java` — `@RestController @RequestMapping("/admin/dashboard") @PreAuthorize("hasRole('ADMIN'")`; endpoint `GET /admin/dashboard` delega a `DashboardService.getOperationalMetrics()`; retorna 200 con `OperationalMetricsDto` (depende de T009)

**Checkpoint US1**: `GET /admin/dashboard` retorna métricas correctas; no-ADMIN recibe 403.

---

## Phase 4: User Story 2 — Resumen financiero del período (P2)

**Goal**: `GET /admin/dashboard/summary?period=MONTH` retorna el resumen financiero del período: volumen total de transacciones, ingresos estimados por comisiones, nuevos registros y suscripciones premium activas.

**Independent Test**: `GET /admin/dashboard/summary?period=TODAY` → totales coinciden con la suma de transacciones del día; `period=WEEK` refleja los últimos 7 días; parámetro inválido → 400.

- [x] T012 [P] [US2] Añadir método `getFinancialSummary(DashboardPeriod period)` en `DashboardService` en `backend/app/src/main/java/com/accioneselbosque/app/service/DashboardService.java` — resolver rango de fechas según period; (1) `totalTransactionVolume` via `TransactionRepository.sumGrossAmountByExecutedAtBetween()` in-process; (2) `estimatedCommissionRevenue` via suma de comisiones del período; (3) `newRegistrations` via `InvestorRepository.countByCreatedAtBetween()`; (4) `activePremiumSubscriptions` via `InvestorRepository.countBySubscriptionTypeAndExpiresAtAfter(PREMIUM, now())` in-process; retornar `FinancialSummaryDto` (depende de T006)
- [x] T013 [US2] Añadir endpoint `GET /admin/dashboard/summary` en `AdminDashboardController` en `backend/app/src/main/java/com/accioneselbosque/app/controller/AdminDashboardController.java` — query param `period` (default MONTH); si valor inválido → `InvalidDashboardPeriodException` → 400; delega a `DashboardService.getFinancialSummary()`; retorna 200 con `FinancialSummaryDto` (depende de T012)

**Checkpoint US2**: Totales del resumen financiero coinciden con los datos fuente; período inválido → 400.

---

## Phase 5: User Story 3 — Accesos directos a acciones administrativas (P2)

**Goal**: `GET /admin/dashboard/links` retorna la lista de accesos directos a módulos de gestión frecuente (gestión de usuarios, configuración de mercados, log de auditoría) con sus endpoints y descripciones.

**Independent Test**: `GET /admin/dashboard/links` retorna lista con al menos 3 accesos directos; cada uno con `label`, `endpoint` y `description` no nulos.

- [x] T014 [P] [US3] Añadir método `getAdminLinks()` en `DashboardService` en `backend/app/src/main/java/com/accioneselbosque/app/service/DashboardService.java` — retorna lista fija de `AdminLinkDto`: gestión de usuarios (`/admin/users`), configuración de mercados (`/config/markets`), log de auditoría (`/audit/events`), parámetros globales (`/config/parameters`) (depende de T007)
- [x] T015 [US3] Añadir endpoint `GET /admin/dashboard/links` en `AdminDashboardController` en `backend/app/src/main/java/com/accioneselbosque/app/controller/AdminDashboardController.java` — delega a `DashboardService.getAdminLinks()`; retorna 200 con `List<AdminLinkDto>` (depende de T014)

**Checkpoint US3**: Lista de accesos directos retorna 4 entradas; cada acceso tiene endpoint válido y descripción.

---

## Phase 6: Polish

- [x] T016 [P] Añadir manejo de `InvalidDashboardPeriodException` en el `GlobalExceptionHandler` existente de `app` o crear uno en `backend/app/src/main/java/com/accioneselbosque/app/exception/` — `@RestControllerAdvice`; maneja `InvalidDashboardPeriodException` → 400; maneja `AccessDeniedException` → 403 (depende de T008)
- [x] T017 [P] Añadir validación de null-safety en `DashboardService.getFinancialSummary()` en `backend/app/src/main/java/com/accioneselbosque/app/service/DashboardService.java` — si algún repositorio retorna `null` (e.g., sin transacciones), sustituir con `BigDecimal.ZERO` o 0L en lugar de propagar NPE
- [x] T018 [P] Añadir test `@WebMvcTest` para `AdminDashboardController` en `backend/app/src/test/java/com/accioneselbosque/app/controller/AdminDashboardControllerTest.java` — verificar: ADMIN recibe 200; INVESTOR recibe 403; sin JWT recibe 401; período inválido recibe 400
- [x] T019 Ejecutar suite: `mvn test -pl backend/app` — todos los tests del dashboard pasan

---

## Dependencias clave

- T009 (DashboardService.getOperationalMetrics) → depende de T005 y de módulos `orders`, `auth`, `configuration` disponibles in-process
- T010 (SecurityConfig) → debe agregarse antes de que los tests de seguridad pasen
- T011 (AdminDashboardController GET /metrics) → depende de T009, T010
- T012 (DashboardService.getFinancialSummary) → depende de T006 y de módulos `portfolio`, `auth` in-process
- T013 (GET /summary) → depende de T012
- T014 (getAdminLinks) → depende de T007
- T015 (GET /links) → depende de T014
- T016 (GlobalExceptionHandler) → depende de T008
- T017 (null-safety) → modifica T012
- T018 (tests) → depende de T011, T013, T015

## Parallel Execution Example — US1

```
Phase 2 completa (T004–T008)
        │
        ├──[Agente A]── T009 (DashboardService.getOperationalMetrics)
        │
        └──[Agente B]── T010 (SecurityConfig /admin/** → ADMIN)
                                │
                        [merge] T011 (AdminDashboardController GET /admin/dashboard)
```

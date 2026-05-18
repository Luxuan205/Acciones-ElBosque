# Tasks: AB-38 — Módulo de Auditoría Legal

**Input**: `specs/023-auditoria-legal/` (plan.md, spec.md, data-model.md, research.md, contracts/audit-api.md)
**Module**: `audit-compliance` — `com.accioneselbosque.audit`
**Branch**: `023-auditoria-legal`
**Prerequisito**: Tabla `investor` existente (referenciada por FK en `audit_event`)

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Puede correr en paralelo
- **[US1]**: Registro automático de eventos de auditoría (P1)
- **[US2]**: Consulta y filtrado del log por el administrador (P1)
- **[US3]**: Retención y archivo de registros (P2)

---

## Phase 1: Setup

- [x] T001 Crear módulo Maven `backend/audit-compliance/pom.xml` con dependencias Spring Boot Starter, Spring Data JPA, Lombok; declarar como módulo hijo en `backend/pom.xml`
- [x] T002 Crear migración Flyway `backend/app/src/main/resources/db/migration/V27__create_audit_event_table.sql` con tabla `audit_event`, columnas `id`, `event_type`, `investor_id`, `performed_by`, `reference_type`, `reference_id`, `detail`, `result`, `ip_address`, `archived`, `occurred_at`; índices `audit_investor_idx`, `audit_type_idx`, `audit_performed_by_idx`
- [x] T003 Crear clase de configuración del módulo `backend/audit-compliance/src/main/java/com/accioneselbosque/audit/AuditComplianceModule.java` con anotación `@Configuration`

**Checkpoint Setup**: Módulo Maven compilable; migración V27 aplicada; tabla `audit_event` verificada en BD.

---

## Phase 2: Foundational — Entidades, repositorio inmutable, DTOs, excepciones

**⚠️ CRÍTICO**: Sin setters en la entidad. El repositorio NO extiende `JpaRepository` — solo `Repository`.

- [x] T004 [P] Crear enum `AuditEventType` en `backend/audit-compliance/src/main/java/com/accioneselbosque/audit/model/AuditEventType.java` — categorías AUTH (`AUTH_SUCCESS`, `AUTH_FAILURE`, `AUTH_MFA_FAILED`, `ACCOUNT_LOCKED`, `ACCOUNT_UNLOCKED`), ORDERS (`ORDER_CREATED`, `ORDER_CANCELLED`, `ORDER_EXECUTED`, `ORDER_REJECTED`), PROFILE (`PROFILE_UPDATED`, `PASSWORD_CHANGED`), SUBSCRIPTION (`SUBSCRIPTION_ACTIVATED`, `SUBSCRIPTION_EXPIRED`), ADMIN (`USER_SUSPENDED`, `USER_UNSUSPENDED`, `ROLE_CHANGED`, `PARAMETER_CHANGED`), ALERTS (`PRICE_ALERT_CREATED`, `PRICE_ALERT_TRIGGERED`)
- [x] T005 [P] Crear enum `AuditResult` en `backend/audit-compliance/src/main/java/com/accioneselbosque/audit/model/AuditResult.java` — valores `SUCCESS` y `FAILURE`
- [x] T006 Crear entidad JPA `AuditEvent` en `backend/audit-compliance/src/main/java/com/accioneselbosque/audit/model/AuditEvent.java` — `@Entity @Table(name="audit_event")`; todos los campos con `@Column(updatable = false)`; sin setters (solo `@Getter`); constructor completo via Lombok `@AllArgsConstructor @NoArgsConstructor(access = PROTECTED)`
- [x] T007 [P] Crear record `AuditEventRecord` en `backend/audit-compliance/src/main/java/com/accioneselbosque/audit/model/AuditEventRecord.java` — campos: `AuditEventType eventType`, `Long investorId`, `Long performedBy`, `String referenceType`, `Long referenceId`, `Map<String, Object> detail`, `AuditResult result`, `String ipAddress`, `Instant occurredAt`
- [x] T008 [P] Crear repositorio `AuditEventRepository` en `backend/audit-compliance/src/main/java/com/accioneselbosque/audit/repository/AuditEventRepository.java` — extiende `Repository<AuditEvent, Long>` (NO `JpaRepository`); métodos de consulta paginada con `@Query` JPQL con filtros opcionales por `investorId`, `eventType`, `result`, `from`, `to`, `archived`; sin ningún método `save`, `delete` ni `@Modifying` (por ahora)
- [x] T009 [P] Crear DTO `AuditEventResponseDto` en `backend/audit-compliance/src/main/java/com/accioneselbosque/audit/dto/AuditEventResponseDto.java` — todos los campos de `AuditEvent` + método estático `from(AuditEvent)`
- [x] T010 [P] Crear DTO `AuditEventFilterDto` en `backend/audit-compliance/src/main/java/com/accioneselbosque/audit/dto/AuditEventFilterDto.java` — campos opcionales: `Long investorId`, `String eventType`, `String result`, `LocalDate from`, `LocalDate to`, `boolean includeArchived`, `int page`, `int size`
- [x] T011 [P] Crear excepción `AuditAccessDeniedException` en `backend/audit-compliance/src/main/java/com/accioneselbosque/audit/exception/AuditAccessDeniedException.java` — lanzada cuando un rol no autorizado intenta consultar el log
- [x] T012 Crear `GlobalExceptionHandler` en `backend/audit-compliance/src/main/java/com/accioneselbosque/audit/exception/GlobalExceptionHandler.java` — `@RestControllerAdvice`; maneja `AuditAccessDeniedException` (→ 403) y errores de validación de parámetros (→ 400) (depende de T011)

**Checkpoint Foundational**: Entidad con `@Column(updatable=false)` en todos los campos; repositorio sin métodos de escritura; DTOs compilables.

---

## Phase 3: User Story 1 — Registro automático de eventos de auditoría (P1) 🎯 MVP

**Goal**: Proveer la facade pública `AuditService.record(AuditEventRecord)` que cualquier módulo puede llamar para registrar un evento de auditoría de forma asíncrona e inmutable. Fire-and-forget: nunca lanza excepción al llamador.

**Independent Test**: Inyectar `AuditService`, llamar `record()` con un `AuditEventRecord` válido, consultar con `EntityManager` y verificar que el evento existe con todos los campos correctos.

- [x] T013 [P] [US1] Crear servicio facade `AuditService` en `backend/audit-compliance/src/main/java/com/accioneselbosque/audit/service/AuditService.java` — método `@Async void record(AuditEventRecord event)`: mapea record a entidad `AuditEvent` y persiste via `EntityManager.persist()`; maneja excepciones internamente con log, sin propagarlas al llamador
- [x] T014 [P] [US1] Añadir `@EnableAsync` en `backend/audit-compliance/src/main/java/com/accioneselbosque/audit/AuditComplianceModule.java` para habilitar ejecución asíncrona de `AuditService.record()`
- [x] T015 [US1] Verificar en `backend/audit-compliance/src/main/java/com/accioneselbosque/audit/model/AuditEvent.java` que todos los campos tienen `@Column(updatable = false, insertable = true)` y que no existe ningún setter generado
- [x] T016 [US1] Documentar en Javadoc de `AuditService.record()` el contrato de la facade: fire-and-forget, timestamp debe ser el del evento original, nunca lanza excepción al llamador en `backend/audit-compliance/src/main/java/com/accioneselbosque/audit/service/AuditService.java`

**Checkpoint US1**: `AuditService.record()` inyectable en cualquier módulo; evento persistido con campos correctos; llamador no percibe bloqueo.

---

## Phase 4: User Story 2 — Consulta y filtrado del log por el administrador (P1)

**Goal**: Exponer `GET /audit/events` y `GET /audit/events/export` solo para ADMIN, con filtros combinados y paginación. Log de solo lectura: sin endpoints PUT/POST/DELETE.

**Independent Test**: `GET /audit/events?investorId=X&eventType=AUTH_FAILURE&from=...&to=...` → solo eventos que coinciden en orden cronológico descendente; sin JWT → 401; JWT de INVESTOR → 403.

- [x] T017 [P] [US2] Crear `AuditQueryService` en `backend/audit-compliance/src/main/java/com/accioneselbosque/audit/service/AuditQueryService.java` — método `Page<AuditEventResponseDto> findEvents(AuditEventFilterDto filter)`: delega en `AuditEventRepository` con filtros opcionales; traduce `LocalDate` a `Instant` para rango temporal
- [x] T018 [P] [US2] Crear `AuditExportService` en `backend/audit-compliance/src/main/java/com/accioneselbosque/audit/service/AuditExportService.java` — método `void exportToCsv(AuditEventFilterDto filter, HttpServletResponse response)`: escribe CSV al `OutputStream` con `Content-Disposition: attachment; filename="audit-log.csv"` aplicando todos los filtros activos
- [x] T019 [US2] Crear `AuditController` en `backend/audit-compliance/src/main/java/com/accioneselbosque/audit/controller/AuditController.java` — `@RestController`, `@RequestMapping("/audit")`, `@PreAuthorize("hasRole('ADMIN')")`; `GET /audit/events` → retorna `Page<AuditEventResponseDto>`; `GET /audit/events/export` → delega a `AuditExportService`; sin métodos POST, PUT ni DELETE (depende de T017, T018)
- [x] T020 [US2] Añadir query JPQL con filtros dinámicos en `AuditEventRepository` en `backend/audit-compliance/src/main/java/com/accioneselbosque/audit/repository/AuditEventRepository.java` — condiciones `AND (:investorId IS NULL OR e.investorId = :investorId)` por cada filtro; ordenada por `occurredAt DESC`

**Checkpoint US2**: `GET /audit/events` con JWT ADMIN retorna 200 paginado; exportación retorna CSV; INVESTOR → 403; no existe endpoint de modificación.

---

## Phase 5: User Story 3 — Retención y archivo de registros (P2)

**Goal**: Job mensual que marca `archived = TRUE` en registros con más de 5 años. El endpoint acepta `includeArchived=true`. El archivado no elimina ni modifica el contenido.

**Independent Test**: Insertar registro con `occurred_at` hace 6 años y uno reciente → ejecutar job → antiguo: `archived=TRUE`; reciente: `archived=FALSE`; `GET /audit/events?includeArchived=false` → archivado no aparece; `?includeArchived=true` → ambos aparecen.

- [x] T021 [P] [US3] Crear `AuditArchiveJob` en `backend/audit-compliance/src/main/java/com/accioneselbosque/audit/service/AuditArchiveJob.java` — `@Scheduled(cron = "0 0 2 1 * *")`; ejecuta query de archivado para marcar `archived = TRUE` en registros con `occurred_at < NOW() - INTERVAL '5 years'`; log de filas archivadas
- [x] T022 [P] [US3] Añadir único método `@Modifying` en `AuditEventRepository` en `backend/audit-compliance/src/main/java/com/accioneselbosque/audit/repository/AuditEventRepository.java` — query que actualiza `archived = TRUE` por fecha límite, exclusivo para `AuditArchiveJob`
- [x] T023 [US3] Verificar en `AuditQueryService` en `backend/audit-compliance/src/main/java/com/accioneselbosque/audit/service/AuditQueryService.java` que `findEvents()` aplica `archived = FALSE` por defecto y `archived IN (TRUE, FALSE)` cuando `includeArchived = true`
- [x] T024 [US3] Añadir `@EnableScheduling` en `backend/audit-compliance/src/main/java/com/accioneselbosque/audit/AuditComplianceModule.java` para habilitar el job mensual

**Checkpoint US3**: Job archiva solo registros con más de 5 años sin eliminarlos; contenido de registros archivados idéntico al original.

---

## Phase 6: Polish

- [x] T025 Registrar `audit-compliance` como dependencia en `backend/app/pom.xml` para que Spring Boot lo escanee
- [x] T026 Añadir regla en `SecurityConfig` para que `/audit/**` requiera rol ADMIN y rechace cualquier otro rol con 403 en `backend/app/src/main/java/com/accioneselbosque/Application.java` o clase de configuración de seguridad
- [x] T027 Añadir validación de parámetros en `AuditController` en `backend/audit-compliance/src/main/java/com/accioneselbosque/audit/controller/AuditController.java`: `from` debe ser anterior a `to`; `size` máximo 200; `investorId` positivo; 400 con mensaje descriptivo en caso de violación
- [x] T028 Revisar en `AuditEvent` y todos los servicios del módulo que no existe ningún `@Setter`, ningún método `merge()` o `update()` en `backend/audit-compliance/src/main/java/com/accioneselbosque/audit/`
- [x] T029 Añadir propiedad `audit.retention.years=5` en `backend/app/src/main/resources/application.properties`; inyectarla en `AuditArchiveJob` via `@Value("${audit.retention.years:5}")` en `backend/audit-compliance/src/main/java/com/accioneselbosque/audit/service/AuditArchiveJob.java`

---

## Dependencias clave

| Tarea | Depende de |
|-------|-----------|
| T006 | T004, T005 — entidad referencia los enums |
| T007 | T004, T005 — record referencia los mismos enums |
| T008 | T006 — repositorio tipado sobre AuditEvent |
| T009 | T006 — DTO construido desde la entidad |
| T012 | T011 — GlobalExceptionHandler requiere excepciones |
| T013 | T006, T007, T003 — AuditService persiste AuditEvent desde AuditEventRecord |
| T014 | T013 — @EnableAsync activa el async de T013 |
| T017 | T008, T009, T010 — QueryService delega en repositorio y retorna DTOs |
| T018 | T009, T010 — ExportService usa mismos filtros y DTOs |
| T019 | T017, T018 — controller orquesta ambos servicios |
| T020 | T008 — query dinámica añadida al repositorio existente |
| T021 | T022 — job usa la query de archivado del repositorio |
| T022 | T008 — único método @Modifying añadido al repositorio |
| T023 | T017 — verificación sobre QueryService ya implementado |
| T024 | T021 — @EnableScheduling necesario para el job |
| T025 | T001–T024 — integración en app cuando módulo completo |

## Parallel Execution Example — US1

```
[Phase 2 completa]
       │
       ├─── [Agente A] T013 — AuditService.java (@Async record())
       │
       └─── [Agente B] T014 — @EnableAsync en AuditComplianceModule.java
                                    │
                                    ▼
                     T015 — verificar @Column(updatable=false) en AuditEvent
                                    │
                                    ▼
                     T016 — Javadoc contrato facade AuditService
```

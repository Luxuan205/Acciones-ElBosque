# Tasks: AB-40 — Gestión de Parámetros Globales del Sistema

**Input**: `specs/025-parametros-globales/` (plan.md, spec.md, data-model.md, contracts/parameters-api.md)
**Module**: `configuration` — `com.accioneselbosque.configuration`
**Branch**: `025-parametros-globales`
**Prerequisito**: Módulo `configuration` existente (V3-V4 ya aplicadas); módulo `audit-compliance` disponible in-process para registrar cambios

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Puede correr en paralelo
- **[US1]**: Consulta de parámetros del sistema (P1)
- **[US2]**: Modificación de parámetros del sistema (P1)
- **[US3]**: Historial de cambios de parámetros (P2)

---

## Phase 1: Setup — Migraciones DB

- [x] T001 Crear migración `backend/app/src/main/resources/db/migration/V28__create_global_parameter_table.sql` — tabla `global_parameter`: columnas `key VARCHAR(100) PK`, `value VARCHAR(500) NOT NULL`, `data_type VARCHAR(20) NOT NULL` (STRING|INTEGER|DECIMAL|BOOLEAN), `category VARCHAR(50) NOT NULL`, `description VARCHAR(300) NOT NULL`, `min_value VARCHAR(100) NULL`, `max_value VARCHAR(100) NULL`, `created_at TIMESTAMP NOT NULL DEFAULT NOW()`, `updated_at TIMESTAMP NOT NULL DEFAULT NOW()`; seed inicial con parámetros de Seguridad (max_login_attempts=5, token_ttl_minutes=60, otp_ttl_minutes=10), Suscripciones (premium_duration_days=30), Auditoría (audit_retention_years=5), Trading (limit_order_default_ttl_days=90, max_price_alerts_per_user=10)
- [x] T002 Crear migración `backend/app/src/main/resources/db/migration/V29__create_parameter_change_history_table.sql` — tabla `parameter_change_history`: columnas `id BIGSERIAL PK`, `parameter_key VARCHAR(100) NOT NULL REFERENCES global_parameter(key)`, `previous_value VARCHAR(500) NOT NULL`, `new_value VARCHAR(500) NOT NULL`, `changed_by BIGINT NOT NULL`, `changed_at TIMESTAMP NOT NULL DEFAULT NOW()`, `reason VARCHAR(300) NULL`; índice `param_history_key_idx ON parameter_change_history(parameter_key, changed_at DESC)`

**Checkpoint Setup**: V28 y V29 aplicadas; tabla `global_parameter` con datos semilla verificados; tabla `parameter_change_history` vacía lista.

---

## Phase 2: Foundational — Entidades, repositorios, DTOs, excepciones

- [x] T003 [P] Crear enum `ParameterDataType` en `backend/configuration/src/main/java/com/accioneselbosque/configuration/model/ParameterDataType.java` — valores `STRING`, `INTEGER`, `DECIMAL`, `BOOLEAN`
- [x] T004 Crear entidad JPA `GlobalParameter` en `backend/configuration/src/main/java/com/accioneselbosque/configuration/model/GlobalParameter.java` — `@Entity @Table(name="global_parameter")`; `@Id` es `key` (String, no autogenerado); campos: key, value, dataType (`@Enumerated EnumType.STRING`), category, description, minValue, maxValue, createdAt, updatedAt; `@PreUpdate` para `updatedAt`; Lombok `@Data @NoArgsConstructor @AllArgsConstructor`
- [x] T005 Crear entidad JPA `ParameterChangeHistory` en `backend/configuration/src/main/java/com/accioneselbosque/configuration/model/ParameterChangeHistory.java` — `@Entity @Table(name="parameter_change_history")`; campos: id (`@GeneratedValue IDENTITY`), parameterKey, previousValue, newValue, changedBy (Long), changedAt, reason; todos los campos con `@Column(updatable = false)` (registro inmutable); Lombok `@Getter @NoArgsConstructor @AllArgsConstructor @Builder`
- [x] T006 [P] Crear `GlobalParameterRepository` en `backend/configuration/src/main/java/com/accioneselbosque/configuration/repository/GlobalParameterRepository.java` — `JpaRepository<GlobalParameter, String>`; método: `findAllByOrderByCategoryAscKeyAsc()`
- [x] T007 [P] Crear `ParameterChangeHistoryRepository` en `backend/configuration/src/main/java/com/accioneselbosque/configuration/repository/ParameterChangeHistoryRepository.java` — `JpaRepository<ParameterChangeHistory, Long>`; métodos: `findByParameterKeyOrderByChangedAtDesc(String key)`, `findTopByParameterKeyOrderByChangedAtDesc(String key)` (retorna Optional — para revert)
- [x] T008 [P] Crear DTOs en `backend/configuration/src/main/java/com/accioneselbosque/configuration/dto/`: `GlobalParameterDto` (key, value, dataType, category, description, minValue, maxValue); `UpdateParameterRequest` (value, reason); `ParameterChangeHistoryDto` (id, parameterKey, previousValue, newValue, changedBy, changedAt, reason); `GroupedParametersResponse` (Map<String, List<GlobalParameterDto>> groupedByCategory)
- [x] T009 [P] Crear excepción `ParameterNotFoundException` (→ 404) en `backend/configuration/src/main/java/com/accioneselbosque/configuration/exception/ParameterNotFoundException.java`
- [x] T010 [P] Crear excepción `InvalidParameterValueException` (→ 400) en `backend/configuration/src/main/java/com/accioneselbosque/configuration/exception/InvalidParameterValueException.java` — con campo `parameterKey` y `reason`
- [x] T011 Extender `GlobalExceptionHandler` en `backend/configuration/src/main/java/com/accioneselbosque/configuration/exception/GlobalExceptionHandler.java` — añadir manejo de `ParameterNotFoundException` → 404 y `InvalidParameterValueException` → 400 con mensaje descriptivo (depende de T009, T010)

**Checkpoint Foundational**: Entidades disponibles; repositorios inyectables; DTOs compilables; excepciones manejadas.

---

## Phase 3: User Story 1 — Consulta de parámetros del sistema (P1) 🎯 MVP

**Goal**: `GET /config/parameters` retorna todos los parámetros organizados por categoría. Solo accesible para ADMIN.

**Independent Test**: `GET /config/parameters` con JWT ADMIN → 200 con `GroupedParametersResponse` con al menos 4 categorías (Seguridad, Suscripciones, Auditoría, Trading); JWT INVESTOR → 403.

- [x] T012 [P] [US1] Implementar `GlobalParameterService.getAllGroupedByCategory()` en `backend/configuration/src/main/java/com/accioneselbosque/configuration/service/GlobalParameterService.java` — cargar todos los parámetros via `GlobalParameterRepository.findAllByOrderByCategoryAscKeyAsc()`; agrupar por `category` en `Map<String, List<GlobalParameterDto>>`; retornar `GroupedParametersResponse`; anotar con `@Cacheable("parameters")` (depende de T004–T008)
- [x] T013 [US1] Crear `GlobalParameterController` en `backend/configuration/src/main/java/com/accioneselbosque/configuration/controller/GlobalParameterController.java` — `@RestController @RequestMapping("/config/parameters") @PreAuthorize("hasRole('ADMIN'")`; `GET /config/parameters` delega a `GlobalParameterService.getAllGroupedByCategory()`; retorna 200 con `GroupedParametersResponse` (depende de T012)
- [x] T014 [US1] Añadir `@EnableCaching` en la clase de configuración principal del módulo `configuration` en `backend/configuration/src/main/java/com/accioneselbosque/configuration/` o en la clase existente de configuración del módulo

**Checkpoint US1**: `GET /config/parameters` retorna todos los parámetros agrupados; INVESTOR recibe 403.

---

## Phase 4: User Story 2 — Modificación de parámetros del sistema (P1)

**Goal**: `PUT /config/parameters/{key}` actualiza el valor de un parámetro si es válido, invalida la caché, registra el cambio en `parameter_change_history` y publica evento a `audit-compliance`. El nuevo valor entra en vigor de inmediato (caché TTL: 60 segundos).

**Independent Test**: `PUT /config/parameters/max_login_attempts` con value="3" → 200; verificar que `GlobalParameterRepository.findById("max_login_attempts").get().getValue()` es "3"; intentar value="-1" → 400 `INVALID_VALUE`; intentar con key inexistente → 404.

- [x] T015 [P] [US2] Implementar método privado `validateValue(GlobalParameter param, String newValue)` en `GlobalParameterService` en `backend/configuration/src/main/java/com/accioneselbosque/configuration/service/GlobalParameterService.java` — verificar tipo: si INTEGER/DECIMAL, parsear y verificar rango `[minValue, maxValue]`; si falla → `InvalidParameterValueException`
- [x] T016 [US2] Implementar `GlobalParameterService.updateParameter(String key, String newValue, String reason, Long adminId)` en `backend/configuration/src/main/java/com/accioneselbosque/configuration/service/GlobalParameterService.java` — (1) cargar parámetro, si no existe → `ParameterNotFoundException`; (2) validar valor via `validateValue()`; (3) guardar valor anterior; (4) actualizar `GlobalParameter.value`; (5) crear `ParameterChangeHistory` con previousValue, newValue, changedBy, reason; (6) `@CacheEvict(value="parameters", allEntries=true)`; retornar `GlobalParameterDto` actualizado; todo en `@Transactional` (depende de T015, T006, T007)
- [x] T017 [US2] Añadir endpoint `PUT /config/parameters/{key}` en `GlobalParameterController` en `backend/configuration/src/main/java/com/accioneselbosque/configuration/controller/GlobalParameterController.java` — extrae `adminId` del JWT; delega a `GlobalParameterService.updateParameter()`; retorna 200 con `GlobalParameterDto` actualizado (depende de T016)

**Checkpoint US2**: Valor válido actualiza inmediatamente; valor fuera de rango → 400; key inexistente → 404; cambio registrado en `parameter_change_history`.

---

## Phase 5: User Story 3 — Historial de cambios de parámetros (P2)

**Goal**: `GET /config/parameters/{key}/history` retorna el historial completo de cambios del parámetro. `POST /config/parameters/{key}/revert` aplica el valor anterior como un nuevo cambio (con su propio registro de auditoría).

**Independent Test**: Modificar un parámetro 3 veces → `GET /.../history` retorna 3 entradas en orden descendente; `POST /.../revert` aplica el valor previo más reciente y crea una 4ª entrada en el historial.

- [x] T018 [P] [US3] Añadir método `getHistory(String key)` en `GlobalParameterService` en `backend/configuration/src/main/java/com/accioneselbosque/configuration/service/GlobalParameterService.java` — verificar que el parámetro existe; retornar `ParameterChangeHistoryRepository.findByParameterKeyOrderByChangedAtDesc(key)` mapeado a `List<ParameterChangeHistoryDto>` (depende de T007)
- [x] T019 [US3] Añadir método `revertParameter(String key, Long adminId)` en `GlobalParameterService` en `backend/configuration/src/main/java/com/accioneselbosque/configuration/service/GlobalParameterService.java` — cargar el cambio más reciente via `findTopByParameterKeyOrderByChangedAtDesc()`; si no hay historial → 409; invocar `updateParameter(key, previousValue, "Revert", adminId)` para reutilizar la lógica de validación + registro (depende de T016, T018)
- [x] T020 [US3] Añadir endpoints `GET /config/parameters/{key}/history` y `POST /config/parameters/{key}/revert` en `GlobalParameterController` en `backend/configuration/src/main/java/com/accioneselbosque/configuration/controller/GlobalParameterController.java` (depende de T018, T019)

**Checkpoint US3**: Historial retorna todos los cambios en orden descendente; revert aplica valor anterior como nuevo cambio con su propio registro.

---

## Phase 6: Polish

- [x] T021 [P] Añadir `@PreAuthorize("hasRole('ADMIN')")` a nivel de clase en `GlobalParameterController` verificando que cubre todos los endpoints en `backend/configuration/src/main/java/com/accioneselbosque/configuration/controller/GlobalParameterController.java`
- [x] T022 [P] Añadir validación `@NotBlank` en `UpdateParameterRequest.value` en `backend/configuration/src/main/java/com/accioneselbosque/configuration/dto/UpdateParameterRequest.java` para rechazar strings vacíos antes de llegar al servicio
- [x] T023 [P] Verificar que `ParameterChangeHistory` no expone métodos de modificación — confirmar ausencia de setters y de métodos `@Modifying` sobre el historial en `backend/configuration/src/main/java/com/accioneselbosque/configuration/repository/ParameterChangeHistoryRepository.java`
- [x] T024 [P] Añadir `@Transactional(readOnly = true)` en `getAllGroupedByCategory()` y `getHistory()` en `GlobalParameterService` en `backend/configuration/src/main/java/com/accioneselbosque/configuration/service/GlobalParameterService.java`
- [x] T025 Ejecutar suite: `mvn test -pl backend/configuration` — todos los tests de parámetros pasan

---

## Dependencias clave

- T001 → T002 (V29 referencia `global_parameter.key` — V28 debe aplicarse primero)
- T004 (GlobalParameter) → depende de T003 (ParameterDataType)
- T006 (GlobalParameterRepository) → depende de T004
- T007 (ParameterChangeHistoryRepository) → depende de T005
- T012 (getAllGroupedByCategory) → depende de T004–T008
- T013 (controller GET) → depende de T012
- T015 (validateValue) → depende de T004 (accede a minValue, maxValue, dataType)
- T016 (updateParameter) → depende de T015, T006, T007
- T017 (PUT endpoint) → depende de T016
- T018 (getHistory) → depende de T007
- T019 (revertParameter) → depende de T016, T018
- T020 (history+revert endpoints) → depende de T018, T019

## Parallel Execution Example — US1

```
Phase 2 completa (T003–T011)
        │
        ├──[Agente A]── T012 (GlobalParameterService.getAllGroupedByCategory + @Cacheable)
        │                       │
        │               T013 (GlobalParameterController GET /config/parameters)
        │
        └──[Agente B]── T014 (@EnableCaching en módulo configuration)
```

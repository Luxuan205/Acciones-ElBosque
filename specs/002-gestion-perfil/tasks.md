# Tasks: AB-17 — Gestión de Perfil de Usuario

**Input**: `specs/002-gestion-perfil/` (plan.md, spec.md, data-model.md, contracts/profile-api.md, research.md)
**Module**: `auth-security-service` — `com.accioneselbosque.auth`
**Branch**: `002-gestion-perfil`
**Prerequisito**: AB-15 (Investor entity y auth_db schema ya existen)

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Puede correr en paralelo
- **[US1]**: Ver y actualizar datos personales (P1)
- **[US2]**: Cambiar contraseña verificando la actual (P2)
- **[US3]**: Gestionar preferencias de notificación e idioma (P3)

---

## Phase 1: Setup — Migraciones DB

- [X] T001 Crear migración `backend/auth/src/main/resources/db/migration/V3__add_phone_to_investor.sql` — `ALTER TABLE auth_db.investor ADD COLUMN phone VARCHAR(25)` — migración creada en `backend/app/src/main/resources/db/migration/V5__add_phone_to_investor.sql`
- [X] T002 Crear migración `backend/auth/src/main/resources/db/migration/V4__create_investor_preferences_table.sql` — creada en `backend/app/src/main/resources/db/migration/V6__create_investor_preferences_table.sql`
- [X] T003 Crear migración `backend/auth/src/main/resources/db/migration/V5__create_profile_change_log_table.sql` — creada en `backend/app/src/main/resources/db/migration/V7__create_profile_change_log_table.sql`

**Checkpoint Setup**: `./mvnw flyway:migrate` aplica V3–V5 sin errores.

---

## Phase 2: Foundational — Entidades y repositorios

- [X] T004 Agregar campo `phone VARCHAR(25)` a la entidad `Investor.java` existente en `backend/auth/src/main/java/com/accioneselbosque/auth/model/Investor.java` — campo nullable con `@Column(nullable = true)`
- [X] T005 [P] Crear entidad JPA `InvestorPreferences` en `backend/auth/src/main/java/com/accioneselbosque/auth/model/InvestorPreferences.java` — id, investor (@OneToOne), notifChannel (enum NotifChannel), language (enum Language), updatedAt
- [X] T006 [P] Crear entidad JPA `ProfileChangeLog` en `backend/auth/src/main/java/com/accioneselbosque/auth/model/ProfileChangeLog.java` — id, investorId (Long), fieldName, oldValue, newValue, changedAt
- [X] T007 [P] Crear enums `NotifChannel` (EMAIL, SMS, NONE) y `Language` (es, en) en `backend/auth/src/main/java/com/accioneselbosque/auth/model/`
- [X] T008 [P] Crear `InvestorPreferencesRepository` en `backend/auth/src/main/java/com/accioneselbosque/auth/repository/InvestorPreferencesRepository.java` — `findByInvestor(Investor)`
- [X] T009 [P] Crear `ProfileChangeLogRepository` en `backend/auth/src/main/java/com/accioneselbosque/auth/repository/ProfileChangeLogRepository.java` — `findByInvestorIdOrderByChangedAtDesc(Long)`
- [X] T010 Crear excepciones: `InvalidCurrentPasswordException.java`, `AccountAlreadyActiveException.java` en `exception/` para mapear a 401 y 400 respectivamente

**Checkpoint Foundational**: Compilación limpia; Flyway aplica migraciones V3–V5 correctamente.

---

## Phase 3: User Story 1 — Ver y actualizar datos personales (P1) 🎯 MVP

**Goal**: El investor autenticado puede ver su perfil y actualizar fullName y/o phone. Los cambios quedan en audit log.

**Independent Test**: `GET /auth/profile` → 200 con datos del JWT. `PUT /auth/profile/personal` con `{"fullName":"Nuevo"}` → 200; verificar en DB que `full_name` cambió y `profile_change_log` tiene el registro.

### Tests — US1

- [X] T011 [P] [US1] Escribir `ProfileControllerTest` en `src/test/java/.../controller/ProfileControllerTest.java` con `standaloneSetup`: test `GET /auth/profile` → 200; test `PUT /auth/profile/personal` datos válidos → 200
- [X] T012 [P] [US1] Escribir `ProfileServiceTest` en `src/test/java/.../service/ProfileServiceTest.java`: test `updatePersonalData()` persiste cambios y crea entradas en ProfileChangeLog; test que email y documentNumber NO cambian aunque vengan en el request

### Implementación — US1

- [X] T013 [P] [US1] Crear `ProfileResponse` DTO en `dto/ProfileResponse.java` — investorId, fullName, email, documentNumber, phone, accountStatus, createdAt
- [X] T014 [P] [US1] Crear `UpdatePersonalDataRequest` DTO en `dto/UpdatePersonalDataRequest.java` — fullName (@Size(2,100) nullable), phone (@Pattern nullable); validación custom que al menos uno no sea null
- [X] T015 [US1] Implementar `ProfileService.getProfile(Long investorId)` en `service/ProfileService.java` — carga Investor por id, mapea a ProfileResponse
- [X] T016 [US1] Implementar `ProfileService.updatePersonalData(Long investorId, UpdatePersonalDataRequest)` — actualiza solo fullName y/o phone; por cada campo modificado crea `ProfileChangeLog` con oldValue y newValue; persiste (depende de T015)
- [X] T017 [US1] Implementar `ProfileController` en `controller/ProfileController.java` con `GET /auth/profile` y `PUT /auth/profile/personal` — extrae investorId del JWT (`Authentication`), delega a ProfileService (depende de T015, T016)

**Checkpoint US1**: `GET /auth/profile` y `PUT /auth/profile/personal` funcionan; audit log se crea correctamente.

---

## Phase 4: User Story 2 — Cambiar contraseña (P2)

**Goal**: El investor puede cambiar su contraseña verificando primero la actual. El cambio queda en audit log con valores `[REDACTED]`.

**Independent Test**: `PUT /auth/change-password` con `currentPassword` correcto y `newPassword` válido → 200. Con `currentPassword` incorrecto → 401.

### Tests — US2

- [X] T018 [P] [US2] Agregar tests a `ProfileControllerTest`: `PUT /auth/change-password` correcto → 200; currentPassword incorrecto → 401; newPassword ≠ confirmNewPassword → 400
- [X] T019 [P] [US2] Agregar tests a `ProfileServiceTest`: `changePassword()` llama a BCryptPasswordEncoder.matches(); lanza `InvalidCurrentPasswordException` si no coincide; audit log contiene `[REDACTED]` para oldValue y newValue

### Implementación — US2

- [X] T020 [US2] Crear `ChangePasswordRequest` DTO en `dto/ChangePasswordRequest.java` — currentPassword (@NotBlank), newPassword (@NotBlank @Size(8,72)), confirmNewPassword (@NotBlank)
- [X] T021 [US2] Implementar `PasswordChangeService.changePassword(Long investorId, ChangePasswordRequest)` en `service/PasswordChangeService.java` — (1) cargar investor; (2) `BCryptPasswordEncoder.matches(currentPassword, hash)` → lanzar `InvalidCurrentPasswordException` si falla; (3) validar `newPassword == confirmNewPassword`; (4) hashear con BCrypt(12); (5) persistir; (6) crear `ProfileChangeLog` con fieldName="password", oldValue="[REDACTED]", newValue="[REDACTED]"
- [X] T022 [US2] Agregar `PUT /auth/change-password` en `ProfileController` — `@Valid @RequestBody ChangePasswordRequest`, delega a `PasswordChangeService`, retorna 200

**Checkpoint US2**: Cambio de contraseña funciona; contraseña errónea retorna 401; audit log tiene `[REDACTED]`.

---

## Phase 5: User Story 3 — Gestionar preferencias (P3)

**Goal**: El investor puede ver y actualizar su canal de notificaciones e idioma. Las preferencias se crean on-demand.

**Independent Test**: `GET /auth/preferences` primer acceso → 200 con defaults (EMAIL, es). `PUT /auth/preferences` con notifChannel=SMS → 200; verificar en DB.

### Tests — US3

- [X] T023 [P] [US3] Agregar tests a `ProfileControllerTest`: `GET /auth/preferences` → 200 con defaults si no existen; `PUT /auth/preferences` con notifChannel inválido → 400
- [X] T024 [P] [US3] Agregar tests a `ProfileServiceTest`: `getOrCreatePreferences()` crea fila si no existe; `updatePreferences()` persiste cambios

### Implementación — US3

- [X] T025 [P] [US3] Crear `UpdatePreferencesRequest` DTO en `dto/UpdatePreferencesRequest.java` — notifChannel (NotifChannel enum), language (Language enum)
- [X] T026 [P] [US3] Crear `PreferencesResponse` DTO en `dto/PreferencesResponse.java` — notifChannel, language, updatedAt
- [X] T027 [US3] Implementar `ProfileService.getOrCreatePreferences(Long investorId)` — busca por investor; si no existe, crea con defaults (EMAIL, es) y persiste; retorna PreferencesResponse
- [X] T028 [US3] Implementar `ProfileService.updatePreferences(Long investorId, UpdatePreferencesRequest)` — carga o crea preferencias, actualiza campos, persiste (depende de T027)
- [X] T029 [US3] Agregar `GET /auth/preferences` y `PUT /auth/preferences` en `ProfileController` (depende de T027, T028)

**Checkpoint US3**: Las 4 rutas del contrato funcionan correctamente.

---

## Phase 6: Polish

- [X] T030 Verificar que email y documentNumber son inmutables — `PUT /auth/profile/personal` con campo email es ignorado silenciosamente (no 400 ni 422) — confirmado en ProfileService.updatePersonalData: solo actualiza fullName y phone
- [X] T031 [P] Ejecutar suite completa: `./mvnw test -pl backend/auth` — **PASS: Tests run: 42, Failures: 0, Errors: 0, Skipped: 0**

---

## Dependencias clave

- T004 (Investor.phone) → depende de T001 (V3 migration)
- T005, T006 → dependen de T002, T003 (V4, V5 migrations)
- T016 (updatePersonalData) → depende de T006 (ProfileChangeLog entity)
- T021 (changePassword) → inyecta `BCryptPasswordEncoder` bean de SecurityConfig (AB-15)
- US3 (preferencias) puede desarrollarse en paralelo con US2 — archivos distintos

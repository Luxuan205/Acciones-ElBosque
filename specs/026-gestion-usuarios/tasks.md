# Tasks: AB-41 — Gestión de Usuarios por el Administrador

**Input**: `specs/026-gestion-usuarios/` (plan.md, spec.md, data-model.md, contracts/admin-users-api.md)
**Module**: `auth` — `com.accioneselbosque.auth`
**Branch**: `026-gestion-usuarios`
**Prerequisito**: AB-15 (entidad `Investor` existente); AB-16 (tabla `mfa_session` disponible para invalidar sesiones)

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Puede correr en paralelo
- **[US1]**: Consulta y búsqueda de usuarios (P1)
- **[US2]**: Modificación de estado de usuario (P1)
- **[US3]**: Gestión de roles de usuario (P2)
- **[US4]**: Restablecimiento de contraseña por el administrador (P2)

---

## Phase 1: Setup — Migración DB

- [x] T001 Crear migración `backend/app/src/main/resources/db/migration/V30__add_role_to_investor.sql` — `ALTER TABLE investor ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'INVESTOR'`; `UPDATE investor SET role = 'ADMIN' WHERE email = 'admin@accioneselbosque.com'` (seed del primer ADMIN); índice `investor_role_status_idx ON investor(role, account_status)`

**Checkpoint Setup**: V30 aplicada; campo `role` añadido a `investor`; al menos un registro con `role='ADMIN'`.

---

## Phase 2: Foundational — Enum, campo en entidad, DTOs, Specification, excepciones

- [x] T002 [P] Crear enum `InvestorRole` en `backend/auth/src/main/java/com/accioneselbosque/auth/model/InvestorRole.java` — valores `INVESTOR`, `BROKER`, `ADMIN`
- [x] T003 Añadir campo `@Enumerated(EnumType.STRING) private InvestorRole role` a la entidad `Investor` en `backend/auth/src/main/java/com/accioneselbosque/auth/model/Investor.java` (depende de T002)
- [x] T004 [P] Crear DTO `AdminUserDto` en `backend/auth/src/main/java/com/accioneselbosque/auth/dto/AdminUserDto.java` — record con campos: `Long id`, `String firstName`, `String lastName`, `String email`, `String documentNumber`, `String accountStatus`, `String subscriptionType`, `LocalDate subscriptionExpiry`, `String role`, `LocalDateTime createdAt`, `LocalDateTime lastLogin`
- [x] T005 [P] Crear DTO `AdminUserDetailDto` en `backend/auth/src/main/java/com/accioneselbosque/auth/dto/AdminUserDetailDto.java` — extiende `AdminUserDto` con `List<RecentActivityDto> recentActivity` (últimos 10 eventos de auditoría)
- [x] T006 [P] Crear DTO `RecentActivityDto` en `backend/auth/src/main/java/com/accioneselbosque/auth/dto/RecentActivityDto.java` — record con campos: `String eventType`, `String description`, `LocalDateTime occurredAt`
- [x] T007 [P] Crear DTO `UpdateUserStatusRequest` en `backend/auth/src/main/java/com/accioneselbosque/auth/dto/UpdateUserStatusRequest.java` — record con campos: `String newStatus` (ACTIVE|SUSPENDED), `@NotBlank String reason`
- [x] T008 [P] Crear DTO `UpdateUserRoleRequest` en `backend/auth/src/main/java/com/accioneselbosque/auth/dto/UpdateUserRoleRequest.java` — record con campos: `InvestorRole newRole`, `@NotBlank String reason`, `boolean confirmed` (requerido true para asignar rol ADMIN)
- [x] T009 [P] Crear DTO `PagedUsersResponse` en `backend/auth/src/main/java/com/accioneselbosque/auth/dto/PagedUsersResponse.java` — record con campos: `List<AdminUserDto> content`, `long totalElements`, `int page`, `int size`
- [x] T010 [P] Crear `InvestorSpecification` en `backend/auth/src/main/java/com/accioneselbosque/auth/repository/InvestorSpecification.java` — clase con métodos estáticos que retornan `Specification<Investor>`: `hasEmailContaining(String email)`, `hasNameContaining(String name)`, `hasStatus(String status)`, `hasSubscriptionType(String type)` — usando `CriteriaBuilder`; permiten composición con `and()`
- [x] T011 Añadir `extends JpaSpecificationExecutor<Investor>` a `InvestorRepository` en `backend/auth/src/main/java/com/accioneselbosque/auth/repository/InvestorRepository.java` — para habilitar consultas dinámicas con Specification (depende de T010)
- [x] T012 [P] Crear excepción `UserNotFoundException` (→ 404) en `backend/auth/src/main/java/com/accioneselbosque/auth/exception/UserNotFoundException.java`
- [x] T013 [P] Crear excepción `LastAdminException` (→ 409 CONFLICT) en `backend/auth/src/main/java/com/accioneselbosque/auth/exception/LastAdminException.java` — lanzada al intentar suspender o cambiar el rol del único administrador activo
- [x] T014 [P] Crear excepción `AdminConfirmationRequiredException` (→ 422) en `backend/auth/src/main/java/com/accioneselbosque/auth/exception/AdminConfirmationRequiredException.java` — lanzada cuando se intenta asignar rol ADMIN sin `confirmed=true`
- [x] T015 Extender `GlobalExceptionHandler` en `backend/auth/src/main/java/com/accioneselbosque/auth/exception/GlobalExceptionHandler.java` — añadir manejo de `UserNotFoundException` → 404, `LastAdminException` → 409, `AdminConfirmationRequiredException` → 422 (depende de T012–T014)

**Checkpoint Foundational**: Campo `role` en `Investor`; `InvestorSpecification` disponible; DTOs compilables; excepciones manejadas.

---

## Phase 3: User Story 1 — Consulta y búsqueda de usuarios (P1) 🎯 MVP

**Goal**: `GET /admin/users` retorna lista paginada filtrable por nombre, correo, estado y suscripción. `GET /admin/users/{id}` retorna el perfil completo con historial de actividad reciente.

**Independent Test**: Crear 5 usuarios con distintos estados; `GET /admin/users?status=SUSPENDED` → solo suspendidos; `GET /admin/users/{id}` → perfil con `recentActivity` de máximo 10 entradas; JWT INVESTOR → 403.

- [x] T016 [P] [US1] Implementar `AdminUserService.searchUsers(String email, String name, String status, String subscriptionType, int page, int size)` en `backend/auth/src/main/java/com/accioneselbosque/auth/service/AdminUserService.java` — componer `Specification<Investor>` con los filtros no nulos via `InvestorSpecification`; ejecutar `InvestorRepository.findAll(spec, PageRequest.of(page, size))`; mapear a `PagedUsersResponse` (depende de T004, T010, T011)
- [x] T017 [P] [US1] Implementar `AdminUserService.getUserDetail(Long userId)` en `backend/auth/src/main/java/com/accioneselbosque/auth/service/AdminUserService.java` — cargar `Investor` por id; si no existe → `UserNotFoundException`; cargar últimos 10 eventos de auditoría via `AuditService` in-process; mapear a `AdminUserDetailDto` (depende de T005, T006, T012)
- [x] T018 [US1] Crear `AdminUserController` en `backend/auth/src/main/java/com/accioneselbosque/auth/controller/AdminUserController.java` — `@RestController @RequestMapping("/admin/users") @PreAuthorize("hasRole('ADMIN'")`; `GET /admin/users` con query params opcionales `email`, `name`, `status`, `subscriptionType`, `page=0`, `size=20`; `GET /admin/users/{id}` delega a `getUserDetail()`; ambos retornan 200 (depende de T016, T017)

**Checkpoint US1**: Búsqueda con filtros retorna solo usuarios que coinciden; perfil detallado incluye actividad reciente; INVESTOR recibe 403.

---

## Phase 4: User Story 2 — Modificación de estado de usuario (P1)

**Goal**: `PATCH /admin/users/{id}/status` suspende o desbloquea a un usuario. La suspensión invalida sesiones activas eliminando `MfaSession` del usuario. No se puede suspender al último ADMIN activo.

**Independent Test**: Suspender usuario ACTIVE → 200, `accountStatus=SUSPENDED`; intentar login con ese usuario → 401/403; desbloquear BLOCKED → 200, `accountStatus=ACTIVE`; suspender el único ADMIN → 409 LAST_ADMIN.

- [x] T019 [P] [US2] Implementar `AdminUserService.updateUserStatus(Long adminId, Long userId, UpdateUserStatusRequest req)` en `backend/auth/src/main/java/com/accioneselbosque/auth/service/AdminUserService.java` — (1) cargar Investor; (2) si `newStatus=SUSPENDED` y usuario es el último ADMIN con role=ADMIN y status=ACTIVE → `LastAdminException`; (3) actualizar `accountStatus`; (4) si `SUSPENDED`: `MfaSessionRepository.deleteByInvestorId(userId)` para invalidar sesiones; (5) registrar en `AuditService` in-process; `@Transactional` (depende de T003, T007, T013)
- [x] T020 [US2] Añadir endpoint `PATCH /admin/users/{id}/status` en `AdminUserController` en `backend/auth/src/main/java/com/accioneselbosque/auth/controller/AdminUserController.java` — `@RequestBody UpdateUserStatusRequest`; delega a `AdminUserService.updateUserStatus()`; retorna 200 con `AdminUserDto` actualizado (depende de T019)

**Checkpoint US2**: Suspensión invalida sesión activa; usuario suspendido no puede autenticarse; último ADMIN → 409.

---

## Phase 5: User Story 3 — Gestión de roles de usuario (P2)

**Goal**: `PATCH /admin/users/{id}/role` cambia el rol de un usuario. Asignar rol ADMIN requiere `confirmed=true`. No se puede bajar el rol del último ADMIN activo.

**Independent Test**: Cambiar INVESTOR → BROKER → 200; cambiar a ADMIN sin `confirmed=true` → 422; cambiar rol del único ADMIN → 409; registrado en auditoría.

- [x] T021 [P] [US3] Implementar `AdminUserService.updateUserRole(Long adminId, Long userId, UpdateUserRoleRequest req)` en `backend/auth/src/main/java/com/accioneselbosque/auth/service/AdminUserService.java` — (1) cargar Investor; (2) si `newRole=ADMIN` y `!req.confirmed()` → `AdminConfirmationRequiredException`; (3) si rol actual es ADMIN y conteo de admins activos == 1 → `LastAdminException`; (4) si cambio implica reducción de permisos (ADMIN → otro): `MfaSessionRepository.deleteByInvestorId(userId)` para forzar re-login; (5) actualizar `investor.role`; (6) registrar en auditoría; `@Transactional` (depende de T003, T013, T014)
- [x] T022 [US3] Añadir endpoint `PATCH /admin/users/{id}/role` en `AdminUserController` en `backend/auth/src/main/java/com/accioneselbosque/auth/controller/AdminUserController.java` — `@RequestBody UpdateUserRoleRequest`; delega a `AdminUserService.updateUserRole()`; retorna 200 con `AdminUserDto` actualizado (depende de T021)

**Checkpoint US3**: Cambio de rol ADMIN requiere confirmación; reducción de permisos invalida sesión; último ADMIN protegido.

---

## Phase 6: User Story 4 — Restablecimiento de contraseña por el administrador (P2)

**Goal**: `POST /admin/users/{id}/reset-password` inicia el proceso de restablecimiento: genera un token de verificación y envía el enlace al correo del usuario. El administrador no puede establecer la contraseña directamente.

**Independent Test**: `POST /admin/users/{id}/reset-password` → 200; verificar que se crea un `VerificationToken` de tipo PASSWORD_RESET para el usuario y que `MailService.sendPasswordResetEmail()` fue invocado; el administrador no recibe la contraseña ni el token.

- [x] T023 [P] [US4] Implementar `AdminUserService.initiatePasswordReset(Long adminId, Long userId)` en `backend/auth/src/main/java/com/accioneselbosque/auth/service/AdminUserService.java` — (1) cargar Investor; si no existe → `UserNotFoundException`; (2) reutilizar `VerificationTokenService` para generar token de tipo PASSWORD_RESET con TTL 24h; (3) llamar a `MailService.sendPasswordResetEmail(investor.getEmail(), token)` — el admin no ve el token; (4) registrar en auditoría: `ADMIN_INITIATED_PASSWORD_RESET`; retornar 200 sin datos del token (depende de T012)
- [x] T024 [US4] Añadir endpoint `POST /admin/users/{id}/reset-password` en `AdminUserController` en `backend/auth/src/main/java/com/accioneselbosque/auth/controller/AdminUserController.java` — delega a `AdminUserService.initiatePasswordReset()`; retorna 200 con mensaje `{"message": "Password reset email sent to user's registered address"}` (depende de T023)

**Checkpoint US4**: Email de restablecimiento enviado al usuario; admin no recibe contraseña ni token; evento registrado en auditoría.

---

## Phase 7: Polish

- [x] T025 [P] Verificar regla `@PreAuthorize("hasRole('ADMIN')")` cubre todos los endpoints de `AdminUserController` en `backend/auth/src/main/java/com/accioneselbosque/auth/controller/AdminUserController.java`; añadir también regla en `SecurityConfig` para `/admin/**`
- [x] T026 [P] Verificar que `InvestorRepository.countByRoleAndAccountStatus(ADMIN, ACTIVE)` existe o añadir el método en `backend/auth/src/main/java/com/accioneselbosque/auth/repository/InvestorRepository.java` — necesario para el guard del último ADMIN
- [x] T027 [P] Añadir `@Transactional` en `AdminUserService.updateUserStatus()` y `updateUserRole()` para garantizar atomicidad entre la actualización de estado y la invalidación de sesión en `backend/auth/src/main/java/com/accioneselbosque/auth/service/AdminUserService.java`
- [x] T028 [P] Añadir índice `investor_role_status_idx ON investor(role, account_status)` si no fue incluido en V30 — verificar en `backend/app/src/main/resources/db/migration/V30__add_role_to_investor.sql`
- [x] T029 Ejecutar suite: `mvn test -pl backend/auth` — todos los tests de gestión de usuarios pasan

---

## Dependencias clave

- T003 (Investor.role) → depende de T002 (InvestorRole enum)
- T011 (JpaSpecificationExecutor) → depende de T010 (InvestorSpecification)
- T015 (GlobalExceptionHandler) → depende de T012–T014 (excepciones)
- T016, T017 (AdminUserService métodos) → dependen de T003–T015
- T018 (AdminUserController) → depende de T016, T017
- T019 (updateUserStatus) → depende de T003, T007, T013
- T020 (PATCH /status) → depende de T019
- T021 (updateUserRole) → depende de T003, T013, T014
- T022 (PATCH /role) → depende de T021
- T023 (initiatePasswordReset) → depende de T012
- T024 (POST /reset-password) → depende de T023
- T026 (countByRoleAndStatus) → necesario para T019 y T021 (guard last ADMIN)

## Parallel Execution Example — US1

```
Phase 2 completa (T002–T015)
        │
        ├──[Agente A]── T016 (AdminUserService.searchUsers — Specification + paginación)
        │
        └──[Agente B]── T017 (AdminUserService.getUserDetail — perfil + actividad reciente)
                                │
                [merge A+B] T018 (AdminUserController GET /admin/users + GET /admin/users/{id})
```

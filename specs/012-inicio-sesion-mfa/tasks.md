# Tasks: AB-16 — Inicio de Sesión con MFA

**Input**: `specs/012-inicio-sesion-mfa/` (plan.md, spec.md, data-model.md, contracts/login-api.md, research.md)
**Module**: `auth` — `com.accioneselbosque.auth`
**Branch**: `012-inicio-sesion-mfa`
**Prerequisito**: AB-15 (InvestorRepository, MailService disponibles in-process)

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Puede correr en paralelo
- **[US1]**: Autenticación básica con credenciales (P1)
- **[US2]**: Segundo factor de autenticación MFA (P2)
- **[US3]**: Bloqueo por intentos fallidos consecutivos (P3)

---

## Phase 1: Setup — Migraciones DB

- [x] T001 Crear migración `backend/app/src/main/resources/db/migration/V12__add_investor_login_fields.sql` — `ALTER TABLE investor ADD COLUMN failed_attempts INT NOT NULL DEFAULT 0, ADD COLUMN locked_until TIMESTAMP NULL, ADD COLUMN totp_secret VARCHAR(64) NULL`
- [x] T002 Crear migración `backend/app/src/main/resources/db/migration/V13__create_otp_code_table.sql` — tabla `otp_code` (id BIGSERIAL PK, investor_id BIGINT NOT NULL FK → investor.id ON DELETE CASCADE, code VARCHAR(6) NOT NULL, channel VARCHAR(20) NOT NULL, expires_at TIMESTAMP NOT NULL, used_at TIMESTAMP NULL, created_at TIMESTAMP NOT NULL DEFAULT NOW()); índice `otp_code_investor_idx ON otp_code(investor_id, expires_at)`
- [x] T003 Crear migración `backend/app/src/main/resources/db/migration/V14__create_mfa_session_table.sql` — tabla `mfa_session` (id BIGSERIAL PK, session_token VARCHAR(36) NOT NULL UNIQUE, investor_id BIGINT NOT NULL FK → investor.id ON DELETE CASCADE, expires_at TIMESTAMP NOT NULL, completed BOOLEAN NOT NULL DEFAULT FALSE, created_at TIMESTAMP NOT NULL DEFAULT NOW()); índice `mfa_session_token_idx ON mfa_session(session_token)`

**Checkpoint Setup**: V12-V14 aplican sin errores; columnas y tablas verificadas.

---

## Phase 2: Foundational — Entidades, enums, excepciones

- [x] T004 [P] Añadir valores `SUSPENDED` y `BLOCKED` al enum `AccountStatus` en `backend/auth/src/main/java/com/accioneselbosque/auth/model/AccountStatus.java`
- [x] T005 [P] Crear entidad JPA `OtpCode` en `backend/auth/src/main/java/com/accioneselbosque/auth/model/OtpCode.java` — id (Long), investorId (Long), code (String), channel (String), expiresAt (LocalDateTime), usedAt (LocalDateTime nullable), createdAt
- [x] T006 [P] Crear entidad JPA `MfaSession` en `backend/auth/src/main/java/com/accioneselbosque/auth/model/MfaSession.java` — id (Long), sessionToken (String, UNIQUE), investorId (Long), expiresAt (LocalDateTime), completed (boolean), createdAt
- [x] T007 [P] Añadir campos a `Investor.java`: `failedAttempts` (int, default 0), `lockedUntil` (LocalDateTime nullable), `totpSecret` (String nullable)
- [x] T008 [P] Crear `OtpCodeRepository` en `backend/auth/src/main/java/com/accioneselbosque/auth/repository/OtpCodeRepository.java` — `findByInvestorIdAndUsedAtIsNullAndExpiresAtAfter(Long, LocalDateTime)`, `findTopByInvestorIdOrderByCreatedAtDesc(Long)`
- [x] T009 [P] Crear `MfaSessionRepository` en `backend/auth/src/main/java/com/accioneselbosque/auth/repository/MfaSessionRepository.java` — `findBySessionTokenAndExpiresAtAfterAndCompletedFalse(String, LocalDateTime)`
- [x] T010 Crear DTOs: `LoginRequest` (email, password), `LoginResponse` (sessionToken, channel), `MfaVerifyRequest` (sessionToken, otpCode), `MfaVerifyResponse` (accessToken, role) en `backend/auth/src/main/java/com/accioneselbosque/auth/dto/`
- [x] T011 Crear excepciones: `AccountLockedException` (→ 423), `AccountSuspendedException` (→ 403), `InvalidOtpException` (→ 401), `MfaSessionExpiredException` (→ 401); registrar en `GlobalExceptionHandler` en `backend/auth/src/main/java/com/accioneselbosque/auth/exception/GlobalExceptionHandler.java`

**Checkpoint Foundational**: Proyecto compila; OtpCode y MfaSession mapeados; AccountStatus tiene SUSPENDED y BLOCKED.

---

## Phase 3: User Story 1 — Autenticación básica con credenciales (P1) 🎯 MVP

**Goal**: `POST /auth/login` valida email + contraseña. Cuenta ACTIVE → crea MfaSession, envía OTP por correo, devuelve sessionToken. Mensajes de error genéricos. Cuenta PENDING/SUSPENDED/BLOCKED → mensajes específicos.

**Independent Test**: Login correcto con cuenta ACTIVE → 200 con sessionToken. Login incorrecto → 401 genérico. Cuenta PENDING → 403. Cuenta BLOCKED → 423.

- [x] T012 [P] [US1] Escribir `LoginControllerTest` en `backend/auth/src/test/java/com/accioneselbosque/auth/controller/LoginControllerTest.java` con `@WebMvcTest`: test login correcto → 200 sessionToken; credenciales incorrectas → 401; cuenta PENDING → 403; cuenta BLOCKED → 423; cuenta SUSPENDED → 403
- [x] T013 [P] [US1] Escribir `LoginServiceTest` en `backend/auth/src/test/java/com/accioneselbosque/auth/service/LoginServiceTest.java` con Mockito: test incremento de failedAttempts; test reset a 0 en login exitoso; test bloqueo cuando failedAttempts >= maxAttempts; test mensaje genérico sin revelar existencia del email
- [x] T014 [US1] Implementar `LoginService` en `backend/auth/src/main/java/com/accioneselbosque/auth/service/LoginService.java` — (1) buscar investor por email; (2) si no existe o contraseña incorrecta: incrementar failedAttempts (si existe), verificar umbral de bloqueo; (3) si cuenta no ACTIVE: lanzar excepción apropiada; (4) si bloqueada: lanzar `AccountLockedException`; (5) si OK: crear `MfaSession` (UUID random, TTL 5min), generar OTP 6 dígitos, persistir `OtpCode`, invocar `MailService.sendOtp(email, code)`; (6) retornar `LoginResponse` (depende de T004-T011)
- [x] T015 [US1] Implementar `LoginController` en `backend/auth/src/main/java/com/accioneselbosque/auth/controller/LoginController.java` — `POST /auth/login` sin autenticación requerida; delega a `LoginService.login()`, retorna 200 (depende de T014)

**Checkpoint US1**: Login correcto → sessionToken; error → mensaje genérico; bloqueo automático funcional.

---

## Phase 4: User Story 2 — Segundo factor MFA (P2)

**Goal**: `POST /auth/mfa/verify` valida el OTP. OTP correcto + MfaSession no expirada → emite JWT con rol. OTP incorrecto → 401. Sesión expirada → 401. `POST /auth/mfa/resend` reenvía OTP con rate limit.

**Independent Test**: Con sessionToken válida, enviar OTP correcto → 200 con accessToken y role. OTP incorrecto → 401. sessionToken expirada → 401. Reenvío → nuevo OTP válido.

- [x] T016 [P] [US2] Agregar tests a `LoginControllerTest`: `POST /auth/mfa/verify` con OTP correcto → 200 accessToken; OTP incorrecto → 401; sesión expirada → 401; `POST /auth/mfa/resend` → 200
- [x] T017 [US2] Implementar `MfaService` en `backend/auth/src/main/java/com/accioneselbosque/auth/service/MfaService.java` — (1) `verify(sessionToken, otpCode)`: buscar MfaSession activa por token; si no existe/expirada → `MfaSessionExpiredException`; buscar OtpCode válido (no usado, no expirado); si no existe → `InvalidOtpException`; marcar `usedAt = NOW()`, `session.completed = TRUE`; emitir JWT via `JwtService.generateToken(investorId, role)`; (2) `resend(sessionToken)`: verificar MfaSession activa; verificar rate limit (máx 3 reenvíos/sesión); generar nuevo OTP, invalidar el anterior; enviar por correo (depende de T014)
- [x] T018 [US2] Implementar `JwtService` en `backend/auth/src/main/java/com/accioneselbosque/auth/service/JwtService.java` — genera JWT con claims `sub=investorId`, `role=INVESTOR/BROKER/ADMIN`, exp según parámetro `auth.jwt_ttl_hours`; usa `JwtAuthenticationFilter` existente para validación
- [x] T019 [US2] Agregar `POST /auth/mfa/verify` y `POST /auth/mfa/resend` en `LoginController` (depende de T017)

**Checkpoint US2**: JWT emitido solo tras OTP correcto; OTP single-use confirmado; reenvío funcional.

---

## Phase 5: User Story 3 — Bloqueo por intentos fallidos (P3)

**Goal**: Tras N intentos fallidos (default 5, configurable), la cuenta se bloquea con `BLOCKED` y `lockedUntil = NOW() + 30min`. Tras el período de bloqueo, se desbloquea automáticamente. ADMIN puede desbloquear manualmente.

**Independent Test**: Configurar umbral a 3 intentos (mock). Tres logins fallidos consecutivos → cuarto intento devuelve 423 aunque las credenciales sean correctas. Tras `lockedUntil`, login correcto → 200.

- [x] T020 [P] [US3] Agregar tests a `LoginServiceTest`: tras 5 intentos fallidos: accountStatus = BLOCKED; sexto intento → 423; desbloqueo manual → failedAttempts = 0, lockedUntil = NULL, accountStatus = ACTIVE
- [x] T021 [US3] Actualizar `LoginService.login()` — leer umbral desde `GlobalParameterService.getInt("auth.max_login_attempts")` (fallback hardcoded 5); si `failedAttempts >= umbral`: `accountStatus = BLOCKED`, `lockedUntil = NOW() + lockDurationMinutes`, persistir; al verificar estado: si `accountStatus == BLOCKED && lockedUntil < NOW()`: auto-desbloquear (ACTIVE, failedAttempts=0) antes de procesar (depende de T014)
- [x] T022 [US3] Añadir endpoint en `AdminUserController` (ver AB-41) o exponer método en `LoginService.unlockAccount(Long investorId)` llamable desde `AdminUserService` — `accountStatus = ACTIVE`, `failedAttempts = 0`, `lockedUntil = NULL`

**Checkpoint US3**: Bloqueo automático funcional; auto-desbloqueo por TTL; desbloqueo manual disponible.

---

## Phase 6: Polish

- [x] T023 [P] Verificar que OTP de 6 dígitos es siempre numérico (no alfanumérico) y se hashea o se almacena en texto plano con TTL corto (decisión: texto plano aceptable en proyecto académico con TTL de 5 min)
- [x] T024 [P] Verificar que mensajes de error de login nunca revelan si el email existe: revisar `LoginService` — el mismo mensaje `INVALID_CREDENTIALS` se devuelve si email no encontrado O si contraseña incorrecta
- [x] T025 Ejecutar suite: `mvn test -pl backend/auth` — todos los tests de MFA pasan

---

## Dependencias clave

- T014 (LoginService) → depende de T004-T011 (entidades, excepciones, DTOs)
- T017 (MfaService.verify) → depende de T014 (MfaSession creada en el primer factor)
- T021 (bloqueo automático) → puede leer `GlobalParameterService` si AB-40 ya está implementado; de lo contrario usar constante 5
- US2 impl (T017-T019) → depende de US1 (T014-T015)
- US3 (T021) → modifica el mismo `LoginService` de US1 — implementar en secuencia

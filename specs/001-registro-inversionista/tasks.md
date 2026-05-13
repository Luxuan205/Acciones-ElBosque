# Tasks: AB-15 — Registro de Inversionista en la Plataforma

**Input**: `specs/001-registro-inversionista/` (plan.md, spec.md, data-model.md, contracts/registration-api.md, research.md, quickstart.md)
**Module**: `auth` — `com.accioneselbosque.auth`
**Branch**: `001-registro-inversionista`

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Puede correr en paralelo (archivos distintos, sin dependencias entre sí)
- **[US1]**: User Story 1 — Registro exitoso + verificación de correo (P1)
- **[US2]**: User Story 2 — Rechazo de datos inválidos o duplicados (P2)

---

## Phase 1: Setup — Infraestructura inicial

**Purpose**: Dependencias, esquema DB y plantilla de correo. Completar antes de cualquier código Java.

- [x] T001 Agregar `spring-boot-starter-mail` a `backend/auth/pom.xml` (dependencia ausente identificada en research.md)
- [x] T002 Crear migración Flyway `backend/auth/src/main/resources/db/migration/V1__create_investor_table.sql` con el SQL del data-model.md (schema `auth_db`, tabla `investor`, índices `idx_investor_email` e `idx_investor_document_number`)
- [x] T003 Crear migración Flyway `backend/auth/src/main/resources/db/migration/V2__create_verification_token_table.sql` con el SQL del data-model.md (tabla `verification_token`, FK `investor_id`, índices)
- [x] T004 Crear plantilla de correo HTML `backend/auth/src/main/resources/templates/email/verification-email.html` con variable `${verificationUrl}` para el enlace y `${fullName}` para el saludo

**Checkpoint Setup**: Flyway migrations válidas y pom.xml con spring-boot-starter-mail — `./mvnw compile` debe pasar.

---

## Phase 2: Foundational — Bloques base (bloquea todas las historias)

**Purpose**: Entidades JPA, repositorios, seguridad y manejo de errores. DEBE completarse antes de implementar cualquier User Story.

**⚠️ CRÍTICO**: Ningún trabajo de User Story puede empezar hasta completar esta fase.

- [x] T005 Crear enum `AccountStatus` en `backend/auth/src/main/java/com/accioneselbosque/auth/model/AccountStatus.java` con valores `PENDING`, `ACTIVE`, `INACTIVE`
- [x] T006 [P] Crear entidad JPA `Investor` en `backend/auth/src/main/java/com/accioneselbosque/auth/model/Investor.java` con campos: `id` (BIGSERIAL), `fullName`, `documentNumber`, `email`, `passwordHash`, `accountStatus` (@Enumerated STRING), `createdAt`, `updatedAt` (@PreUpdate)
- [x] T007 Crear entidad JPA `VerificationToken` en `backend/auth/src/main/java/com/accioneselbosque/auth/model/VerificationToken.java` con campos: `id`, `token` (UUID String), `investor` (@ManyToOne), `expiresAt`, `used`, `createdAt` (depende de T006)
- [x] T008 [P] Crear `InvestorRepository` en `backend/auth/src/main/java/com/accioneselbosque/auth/repository/InvestorRepository.java` extendiendo `JpaRepository<Investor, Long>` con métodos: `findByEmail(String)`, `findByDocumentNumber(String)`, `existsByEmail(String)`, `existsByDocumentNumber(String)`
- [x] T009 [P] Crear `VerificationTokenRepository` en `backend/auth/src/main/java/com/accioneselbosque/auth/repository/VerificationTokenRepository.java` con métodos: `findByToken(String)`, `findTopByInvestorOrderByCreatedAtDesc(Investor)`
- [x] T010 [P] Crear excepciones de dominio en `backend/auth/src/main/java/com/accioneselbosque/auth/exception/`: `DuplicateEmailException.java`, `DuplicateDocumentException.java`, `TokenExpiredException.java`, `TokenAlreadyUsedException.java` (todas extienden `RuntimeException`)
- [x] T011 Crear `GlobalExceptionHandler` en `backend/auth/src/main/java/com/accioneselbosque/auth/exception/GlobalExceptionHandler.java` con `@ControllerAdvice` — mapea cada excepción al código HTTP del contrato (400, 404, 409, 429)
- [x] T012 Crear `SecurityConfig` en `backend/auth/src/main/java/com/accioneselbosque/auth/config/SecurityConfig.java` con `@Bean SecurityFilterChain` que hace `permitAll()` sobre `/auth/register`, `/auth/verify`, `/auth/resend-verification` y requiere auth en el resto
- [x] T013 [P] Crear `MailConfig` en `backend/auth/src/main/java/com/accioneselbosque/auth/config/MailConfig.java` con `@Bean JavaMailSender` configurado desde `application.yaml` (host, port, username, password vía env vars)

**Checkpoint Foundational**: `./mvnw test -pl backend/auth` compilando sin errores; Flyway aplica V1 y V2 correctamente contra una BD de test.

---

## Phase 3: User Story 1 — Registro exitoso + verificación de correo (P1) 🎯 MVP

**Goal**: Un visitante puede registrarse con datos válidos, recibir el correo de verificación y activar su cuenta haciendo clic en el enlace.

**Independent Test**: `POST /auth/register` con datos válidos → 201, estado PENDING en DB, token en `verification_token`. `GET /auth/verify?token=<uuid>` → 200, estado ACTIVE en DB.

### Tests — User Story 1

> **ESCRIBIR PRIMERO — deben FALLAR antes de implementar**

- [x] T014 [P] [US1] Escribir `RegistrationControllerTest` en `backend/auth/src/test/java/com/accioneselbosque/auth/controller/RegistrationControllerTest.java` con `@WebMvcTest(RegistrationController.class)`: test `POST /auth/register` datos válidos → 201; test `GET /auth/verify?token` válido → 200
- [x] T015 [P] [US1] Escribir `RegistrationServiceTest` en `backend/auth/src/test/java/com/accioneselbosque/auth/service/RegistrationServiceTest.java` con `@ExtendWith(MockitoExtension.class)`: test que `register()` llama a `investorRepository.save()`, genera token y llama a `mailSender.send()`
- [x] T016 [P] [US1] Escribir `InvestorRepositoryTest` en `backend/auth/src/test/java/com/accioneselbosque/auth/repository/InvestorRepositoryTest.java` con `@DataJpaTest`: test `findByEmail` retorna Optional con el investor correcto; test `existsByEmail` retorna true para email existente

### Implementación — User Story 1

- [x] T017 [P] [US1] Crear `RegisterRequest` DTO en `backend/auth/src/main/java/com/accioneselbosque/auth/dto/RegisterRequest.java` con anotaciones Bean Validation: `@NotBlank @Size(max=150) fullName`, `@NotBlank @Pattern(regexp="^\\d{6,10}$") documentNumber`, `@NotBlank @Email email`, `@NotBlank @Size(min=8,max=72) password`, `@NotBlank confirmPassword`
- [x] T018 [P] [US1] Crear `RegisterResponse` DTO en `backend/auth/src/main/java/com/accioneselbosque/auth/dto/RegisterResponse.java` con campos `message` y `email`
- [x] T019 [US1] Implementar `VerificationTokenService` en `backend/auth/src/main/java/com/accioneselbosque/auth/service/VerificationTokenService.java` con métodos: `createToken(Investor)` → genera `UUID.randomUUID()`, TTL `now().plusHours(24)`, persiste y retorna token; `validateToken(String)` → busca token, verifica `!used && expiresAt > now()`, lanza `TokenExpiredException` o `TokenAlreadyUsedException` si aplica; `markUsed(VerificationToken)` → sets `used=true`
- [x] T020 [US1] Implementar `RegistrationService.register(RegisterRequest)` en `backend/auth/src/main/java/com/accioneselbosque/auth/service/RegistrationService.java`: (1) validar no-duplicado email/document, (2) hashear password con `BCryptPasswordEncoder(12)`, (3) crear y persistir `Investor` con status PENDING, (4) llamar `VerificationTokenService.createToken()`, (5) construir URL de verificación y enviar correo con `JavaMailSender` y plantilla HTML (depende de T019)
- [x] T021 [US1] Implementar `RegistrationService.verifyAccount(String token)`: (1) llamar `VerificationTokenService.validateToken()`, (2) obtener investor del token, (3) setear `accountStatus = ACTIVE`, (4) persistir, (5) llamar `markUsed()` (depende de T019, T020)
- [x] T022 [US1] Implementar `RegistrationController` en `backend/auth/src/main/java/com/accioneselbosque/auth/controller/RegistrationController.java` con: `POST /auth/register` → `@Valid @RequestBody RegisterRequest`, validar `password == confirmPassword`, delegar a `registrationService.register()`, retornar 201; `GET /auth/verify` → `@RequestParam String token`, delegar a `registrationService.verifyAccount()`, retornar 200 (depende de T020, T021)

**Checkpoint US1**: Tests T014–T016 pasan. `POST /auth/register` retorna 201, `GET /auth/verify` retorna 200. Flujos 1 y 2 del `quickstart.md` verifican correctamente en DB.

---

## Phase 4: User Story 2 — Rechazo de datos inválidos o duplicados (P2)

**Goal**: El sistema rechaza registros con email/documento duplicado (409), datos inválidos (400), tokens expirados (400) y límite de reenvío (429).

**Independent Test**: `POST /auth/register` con email duplicado → 409. `POST /auth/register` con contraseña corta → 400 con array `details`. `GET /auth/verify` con token expirado → 400. `POST /auth/resend-verification` dos veces en < 2 min → 429.

### Tests — User Story 2

> **ESCRIBIR PRIMERO — deben FALLAR antes de implementar**

- [x] T023 [P] [US2] Agregar tests a `RegistrationControllerTest`: `POST /auth/register` email duplicado → 409; documento duplicado → 409; contraseña sin mayúscula → 400 con `details`; contraseña ≠ confirmación → 400; `GET /auth/verify` token expirado → 400; `POST /auth/resend-verification` rate limiting → 429
- [x] T024 [P] [US2] Agregar tests a `RegistrationServiceTest`: `register()` lanza `DuplicateEmailException` cuando email ya existe; lanza `DuplicateDocumentException` cuando documento ya existe; `verifyAccount()` lanza `TokenExpiredException` para token con `expiresAt` pasado; `resendVerification()` lanza excepción si último token tiene < 2 min de antigüedad

### Implementación — User Story 2

- [x] T025 [US2] Agregar validación de duplicados en `RegistrationService.register()`: llamar `investorRepository.existsByEmail()` y `existsByDocumentNumber()` antes de guardar; lanzar `DuplicateEmailException` o `DuplicateDocumentException` según corresponda (depende de T020)
- [x] T026 [US2] Implementar `RegistrationService.resendVerification(String email)` en `backend/auth/src/main/java/com/accioneselbosque/auth/service/RegistrationService.java`: (1) buscar investor por email — si no existe, retornar 200 igualmente (anti-enumeración); (2) si cuenta ACTIVE, lanzar excepción → 400; (3) verificar que el último token tenga `createdAt < now() - 2 min` (rate limit) — si no, lanzar excepción → 429; (4) crear nuevo token y reenviar correo
- [x] T027 [US2] Implementar `RegistrationController POST /auth/resend-verification` en `RegistrationController.java`: `@RequestBody` con `email`, delegar a `registrationService.resendVerification()`, retornar siempre 200 (incluso si email no existe — anti-enumeración) (depende de T026)
- [x] T028 [US2] Agregar manejo en `GlobalExceptionHandler` para `MethodArgumentNotValidException` de Bean Validation: construir response body `{"error": "...", "details": [...]}` con mensajes por campo; mapear `DuplicateEmailException` → 409, `DuplicateDocumentException` → 409, `TokenExpiredException` → 400, `TokenAlreadyUsedException` → 409, rate-limit exception → 429 (depende de T011)

**Checkpoint US2**: Tests T023–T024 pasan. Flujos 2, 3 y 4 del `quickstart.md` retornan los códigos HTTP correctos. No se crean cuentas duplicadas.

---

## Phase 5: Polish & Cross-Cutting

**Purpose**: Seguridad, integración completa y validación end-to-end.

- [x] T029 [P] Verificar que `POST /auth/resend-verification` retorna 200 idéntico para email existente y no-existente (anti-enumeración SC de security). Ajustar respuesta si difiere.
- [ ] T030 Ejecutar todos los flujos del `quickstart.md` manualmente contra el servicio corriendo localmente con MailHog. Registrar resultado (pass/fail) como comentario en el PR.
- [ ] T031 [P] Ejecutar `./mvnw test -pl backend/auth` y confirmar que todos los tests del módulo pasan. Corregir cualquier fallo antes del merge.

---

## Dependencias y Orden de Ejecución

### Dependencias entre fases

- **Phase 1 (Setup)**: Sin dependencias — arrancar de inmediato
- **Phase 2 (Foundational)**: Depende de Phase 1 completada — **bloquea** las User Stories
- **Phase 3 (US1)**: Depende de Phase 2 completada — MVP entregable
- **Phase 4 (US2)**: Depende de Phase 2 completada; se integra con Phase 3 (agrega validaciones al servicio ya creado)
- **Phase 5 (Polish)**: Depende de Phase 3 y 4 completadas

### Dependencias dentro de cada fase

```
T001 (pom.xml)
    └─► T013 (MailConfig usa spring-boot-starter-mail)
    └─► T020 (RegistrationService usa JavaMailSender)

T002, T003 (Flyway migrations)
    └─► T006, T007 (entidades mapean a las tablas)

T005 (AccountStatus enum)
    └─► T006 (Investor usa AccountStatus)

T006 (Investor entity)
    └─► T007 (VerificationToken tiene FK a Investor)
    └─► T008 (InvestorRepository<Investor, Long>)

T007 (VerificationToken entity)
    └─► T009 (VerificationTokenRepository)

T008, T009 (Repositories)
    └─► T019 (VerificationTokenService los inyecta)
    └─► T020 (RegistrationService los inyecta)

T010 (Excepciones)
    └─► T011 (GlobalExceptionHandler maneja estas excepciones)
    └─► T019, T020, T026 (los servicios las lanzan)

T019 (VerificationTokenService)
    └─► T020 (RegistrationService.register() llama createToken())
    └─► T021 (RegistrationService.verifyAccount() llama validateToken())

T020, T021 (RegistrationService métodos)
    └─► T022 (RegistrationController los inyecta)
```

### Oportunidades de paralelismo

- **Phase 1**: T001, T002, T003, T004 — todos en paralelo
- **Phase 2**: T005 primero; luego T006 y T008+T009+T010+T013 en paralelo; T007 después de T006
- **Phase 3 tests**: T014, T015, T016 — todos en paralelo (escribir tests primero)
- **Phase 3 impl**: T017, T018 en paralelo; luego T019; luego T020; luego T021; luego T022
- **Phase 4 tests**: T023, T024 — en paralelo
- **Phase 4 impl**: T025 (modifica T020) → T026 → T027; T028 independiente de T025–T027

---

## Ejemplo de ejecución paralela — Phase 3

```text
# Paso 1: Escribir los 3 tests en paralelo (todos en archivos distintos)
T014 RegistrationControllerTest (happy paths)
T015 RegistrationServiceTest
T016 InvestorRepositoryTest

# Paso 2: Ejecutar tests → confirmar que FALLAN (sin implementación aún)
./mvnw test -pl backend/auth → debe fallar

# Paso 3: DTOs en paralelo
T017 RegisterRequest DTO
T018 RegisterResponse DTO

# Paso 4: Servicio de tokens (prerrequisito para el servicio principal)
T019 VerificationTokenService

# Paso 5: Servicio de registro
T020 RegistrationService.register()
T021 RegistrationService.verifyAccount()

# Paso 6: Controlador (une todo)
T022 RegistrationController

# Paso 7: Tests pasan → US1 completa
./mvnw test -pl backend/auth → verde
```

---

## Estrategia de implementación

### MVP (Solo User Story 1)

1. Phase 1: Setup (T001–T004)
2. Phase 2: Foundational (T005–T013)
3. Phase 3: US1 (T014–T022) — tests primero, luego implementación
4. **PARAR Y VALIDAR**: ejecutar quickstart.md Flujos 1 y 2
5. Demo-able: registro + verificación funcionales

### Entrega incremental

1. MVP (US1) → demo
2. Agregar US2 (T023–T028) → validaciones completas → demo
3. Polish (T029–T031) → listo para merge

### Estrategia de equipo (4 desarrolladores)

- **Dev A**: T001–T004 (Setup) → T011, T012 (Security + GlobalExceptionHandler)
- **Dev B**: T005–T007 (Entidades JPA) → T019 (VerificationTokenService)
- **Dev C**: T008–T010 (Repositorios + Excepciones) → T020–T021 (RegistrationService)
- **Dev D**: T013 (MailConfig) + T017–T018 (DTOs) → T022 (RegistrationController)
- Todos: tests (T014–T016 antes de impl) → review cruzado

---

## Notas

- `[P]` = archivos distintos, sin dependencias entre sí en ese momento
- Tests marcados `[P]` dentro de una historia deben escribirse y FALLAR antes de implementar
- BCrypt factor 12: configurar en `SecurityConfig.java` como `@Bean BCryptPasswordEncoder`
- Token UUID: `UUID.randomUUID().toString()` — no usar librerías externas
- Rate limit de reenvío (2 min): comparar `lastToken.createdAt.plusMinutes(2).isAfter(now())`
- Anti-enumeración en resend: mismo response 200 independientemente de si el email existe
- Ambiente de correo en dev: MailHog en puerto 1025 (ver `quickstart.md`)

# Tasks: AB-18 — Activación de Suscripción Premium

**Input**: `specs/013-suscripcion-premium/` (plan.md, spec.md, data-model.md, contracts/subscription-api.md)
**Module**: `auth` — `com.accioneselbosque.auth`
**Branch**: `013-suscripcion-premium`
**Prerequisito**: AB-15 (Investor con subscriptionType y subscriptionExpiresAt disponibles)

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Puede correr en paralelo
- **[US1]**: Activación STANDARD → PREMIUM (P1)
- **[US2]**: Consulta de estado de suscripción (P2)
- **[US3]**: Vencimiento y degradación automática (P3)

---

## Phase 1: Setup — Migraciones DB

- [x] T001 Crear migración `backend/app/src/main/resources/db/migration/V15__create_subscription_event_table.sql` — tabla `subscription_event` (id BIGSERIAL PK, investor_id BIGINT NOT NULL FK → investor.id ON DELETE CASCADE, event_type VARCHAR(30) NOT NULL, previous_type VARCHAR(20) NOT NULL, new_type VARCHAR(20) NOT NULL, expires_at TIMESTAMP NULL, triggered_by VARCHAR(20) NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT NOW()); índice `sub_event_investor_idx ON subscription_event(investor_id, created_at DESC)`

**Checkpoint Setup**: V15 aplica sin errores; tabla `subscription_event` verificada.

---

## Phase 2: Foundational — Entidades, repositorios, facades, DTOs

- [x] T002 [P] Crear entidad JPA `SubscriptionEvent` en `backend/auth/src/main/java/com/accioneselbosque/auth/model/SubscriptionEvent.java` — id (Long), investorId (Long), eventType (String), previousType (String), newType (String), expiresAt (LocalDateTime nullable), triggeredBy (String), createdAt
- [x] T003 [P] Crear `SubscriptionEventRepository` en `backend/auth/src/main/java/com/accioneselbosque/auth/repository/SubscriptionEventRepository.java` — `findTopByInvestorIdOrderByCreatedAtDesc(Long investorId)`
- [x] T004 [P] Crear `SubscriptionGate` facade en `backend/auth/src/main/java/com/accioneselbosque/auth/facade/SubscriptionGate.java` — `isPremiumActive(Long investorId)`: retorna TRUE si el investor tiene subscriptionType==PREMIUM y subscriptionExpiresAt > NOW(); BROKER y ADMIN siempre retornan TRUE
- [x] T005 [P] Crear DTOs `SubscriptionStatusResponse` y `ActivateSubscriptionResponse` en `backend/auth/src/main/java/com/accioneselbosque/auth/dto/` — según data-model.md

**Checkpoint Foundational**: Proyecto compila; SubscriptionEvent mapeado; SubscriptionGate inyectable.

---

## Phase 3: User Story 1 — Activación STANDARD → PREMIUM (P1) 🎯 MVP

**Goal**: `POST /subscriptions/activate` activa el plan PREMIUM para el inversionista autenticado: cambia `subscriptionType=PREMIUM`, `subscriptionExpiresAt=NOW()+30d`, persiste `SubscriptionEvent(ACTIVATED)`. Si ya es PREMIUM activo, devuelve estado actual sin crear evento.

**Independent Test**: Investor STANDARD → POST activate → 200 con expiresAt; Investor PREMIUM activo → 200 con mensaje ya-activo; Investor PREMIUM vencido → 200 renueva.

- [x] T006 [P] [US1] Escribir `SubscriptionControllerTest` en `backend/auth/src/test/java/com/accioneselbosque/auth/controller/SubscriptionControllerTest.java` con `@WebMvcTest`: activar STANDARD → 200 con subscriptionType=PREMIUM; activar ya-PREMIUM activo → 200 con mensaje; test acceso sin JWT → 401
- [x] T007 [P] [US1] Escribir `SubscriptionServiceTest` en `backend/auth/src/test/java/com/accioneselbosque/auth/service/SubscriptionServiceTest.java` con Mockito: activar STANDARD → subscriptionType=PREMIUM, expiresAt=NOW()+30d, evento persiste; activar PREMIUM vigente → devuelve estado sin crear evento; activar PREMIUM vencido → renueva desde NOW()
- [x] T008 [US1] Implementar `SubscriptionService` en `backend/auth/src/main/java/com/accioneselbosque/auth/service/SubscriptionService.java` — (1) buscar Investor por investorId del JWT; (2) si ya PREMIUM y expiresAt > NOW(): retornar estado actual; (3) si STANDARD o PREMIUM vencido: `subscriptionType=PREMIUM`, `subscriptionExpiresAt=NOW()+30d`, persistir investor, crear `SubscriptionEvent(ACTIVATED, triggeredBy=INVESTOR)`; (4) retornar `ActivateSubscriptionResponse` (depende de T002-T005)
- [x] T009 [US1] Implementar `SubscriptionController` en `backend/auth/src/main/java/com/accioneselbosque/auth/controller/SubscriptionController.java` — `POST /subscriptions/activate` (requiere JWT rol INVESTOR); delega a `SubscriptionService.activate()`; retorna 200 (depende de T008)

**Checkpoint US1**: Activación STANDARD→PREMIUM funcional; idempotente para PREMIUM vigente.

---

## Phase 4: User Story 2 — Consulta de estado (P2)

**Goal**: `GET /subscriptions/status` devuelve el tipo de suscripción actual, fecha de activación, fecha de expiración y días restantes. BROKER y ADMIN siempre ven isActive=true.

**Independent Test**: STANDARD → 200 con isActive=false, daysRemaining=0; PREMIUM activo → isActive=true, daysRemaining>0.

- [x] T010 [P] [US2] Agregar tests a `SubscriptionControllerTest`: GET /subscriptions/status con PREMIUM activo → 200 con daysRemaining>0; con STANDARD → 200 isActive=false; sin JWT → 401
- [x] T011 [US2] Implementar `SubscriptionService.getStatus(Long investorId)` en `backend/auth/src/main/java/com/accioneselbosque/auth/service/SubscriptionService.java` — calcular daysRemaining via `ChronoUnit.DAYS.between(NOW(), expiresAt)`; retornar `SubscriptionStatusResponse` (depende de T008)
- [x] T012 [US2] Agregar `GET /subscriptions/status` en `SubscriptionController` (depende de T011)

**Checkpoint US2**: GET status retorna datos correctos para STANDARD y PREMIUM.

---

## Phase 5: User Story 3 — Vencimiento y degradación automática (P3)

**Goal**: Job nocturno (`@Scheduled(cron="0 0 0 * * *")`) detecta investors con `subscriptionType=PREMIUM` y `subscriptionExpiresAt < NOW()` y los degrada a STANDARD, creando `SubscriptionEvent(EXPIRED, triggeredBy=SYSTEM_JOB)`.

**Independent Test**: Configurar expiresAt en el pasado (mock de reloj), correr job → subscriptionType=STANDARD, subscriptionExpiresAt=NULL, evento EXPIRED persiste.

- [x] T013 [P] [US3] Escribir `SubscriptionExpiryJobTest` en `backend/auth/src/test/java/com/accioneselbosque/auth/service/SubscriptionExpiryJobTest.java` con Mockito: job con 2 vencidas y 1 activa → 2 degradadas a STANDARD, 1 sin cambios; evento EXPIRED creado por cada vencida
- [x] T014 [US3] Implementar `SubscriptionExpiryJob` en `backend/auth/src/main/java/com/accioneselbosque/auth/service/SubscriptionExpiryJob.java` — `@Scheduled(cron="0 0 0 * * *")`: `SELECT * FROM investor WHERE subscription_type='PREMIUM' AND subscription_expires_at < NOW()`; para cada uno: `subscriptionType=STANDARD`, `subscriptionExpiresAt=NULL`, persistir, crear `SubscriptionEvent(EXPIRED, triggeredBy=SYSTEM_JOB)` (depende de T002-T003)

**Checkpoint US3**: Job degrada vencidas a STANDARD; genera eventos de auditoría.

---

## Phase 6: Polish

- [x] T015 [P] Verificar que `SubscriptionGate.isPremiumActive()` es inyectable desde `market-data`, `notifications` y `orders` sin dependencia circular — revisar que la interfaz sea pública y el módulo `auth` no dependa de los módulos receptores
- [x] T016 [P] Verificar que la activación es idempotente: llamadas repetidas con PREMIUM activo no crean eventos duplicados ni extienden la suscripción — revisar lógica en `SubscriptionService.activate()`
- [x] T017 Ejecutar suite: `mvn test -pl backend/auth` — todos los tests de suscripción pasan

---

## Dependencias clave

- T008 (SubscriptionService) → depende de T002-T005 (entidades, repos, DTOs)
- T011 (getStatus) → depende de T008 (SubscriptionService ya implementado)
- T014 (ExpiryJob) → depende de T002-T003 (SubscriptionEvent entity y repository)
- US2 (T011-T012) → implementar tras US1 (T008-T009)
- US3 (T014) → independiente de US1/US2, puede implementarse en paralelo si entidades están listas

# Tasks: AB-33 — Notificación de Estado de Órdenes

**Input**: `specs/019-notificacion-ordenes/` (plan.md, spec.md, data-model.md, research.md, contracts/notifications-api.md)
**Module**: `notifications` — `com.accioneselbosque.notifications`
**Branch**: `019-notificacion-ordenes`
**Prerequisito**: módulo `auth` con `InvestorPreferencesRepository.findByInvestorId()` disponible; módulo `orders` con entidad `Order` y evento de cambio de estado accesible

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Puede correr en paralelo
- **[US1]**: Notificación inmediata de cambio de estado de orden (P1)
- **[US2]**: Configuración del canal de notificación preferido (P2)
- **[US3]**: Historial de notificaciones enviadas (P3)

---

## Phase 1: Setup — Estructura del módulo Maven + Migraciones DB

- [x] T001 Crear `backend/notifications/pom.xml` — nuevo módulo Maven con dependencias: `spring-boot-starter-mail`, `spring-boot-starter-data-jpa`, `spring-retry`, `spring-aspects`; hereda del parent POM raíz; grupo `com.accioneselbosque`, artefacto `notifications`
- [x] T002 Registrar el módulo `notifications` en el `pom.xml` raíz del proyecto en la sección `<modules>` — añadir `<module>backend/notifications</module>`
- [x] T003 Crear migración `backend/app/src/main/resources/db/migration/V19__create_notification_table.sql` — tabla `notification`: columnas `id BIGSERIAL PK`, `investor_id BIGINT NOT NULL REFERENCES investor(id)`, `event_type VARCHAR(40) NOT NULL`, `channel VARCHAR(20) NOT NULL`, `subject VARCHAR(200) NOT NULL`, `body TEXT NOT NULL`, `status VARCHAR(20) NOT NULL DEFAULT 'PENDING'`, `reference_id BIGINT NULL`, `archived BOOLEAN NOT NULL DEFAULT FALSE`, `created_at TIMESTAMP NOT NULL DEFAULT NOW()`; índices `notif_investor_idx ON notification(investor_id, created_at DESC)` y `notif_status_idx ON notification(status, created_at)`
- [x] T004 Crear migración `backend/app/src/main/resources/db/migration/V20__create_notification_attempt_table.sql` — tabla `notification_attempt`: columnas `id BIGSERIAL PK`, `notification_id BIGINT NOT NULL REFERENCES notification(id) ON DELETE CASCADE`, `attempt_number INT NOT NULL`, `status VARCHAR(20) NOT NULL`, `error_message TEXT NULL`, `attempted_at TIMESTAMP NOT NULL DEFAULT NOW()`

**Checkpoint Setup**: Módulo Maven compila (`mvn package -pl backend/notifications`); migraciones V19–V20 aplican sin errores; tablas `notification` y `notification_attempt` visibles en BD.

---

## Phase 2: Foundational — Entidades JPA, repositorios, DTOs, excepciones

- [x] T005 [P] Crear enum `NotificationEventType` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/model/NotificationEventType.java` — valores: `ORDER_EXECUTED`, `ORDER_CANCELLED`, `ORDER_REJECTED`, `ORDER_QUEUED`, `PRICE_ALERT`, `MARKET_ALERT`, `SUBSCRIPTION_ACTIVATED`, `SUBSCRIPTION_EXPIRED`
- [x] T006 [P] Crear entidad JPA `Notification` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/model/Notification.java` — `@Entity @Table(name="notification")`; campos: `id`, `investorId`, `eventType` (`@Enumerated EnumType.STRING`), `channel` (String), `subject`, `body`, `status` (String), `referenceId`, `archived` (boolean), `createdAt`; `@PrePersist` para `createdAt`; Lombok `@Data @Builder @NoArgsConstructor @AllArgsConstructor`
- [x] T007 [P] Crear entidad JPA `NotificationAttempt` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/model/NotificationAttempt.java` — `@Entity @Table(name="notification_attempt")`; campos: `id`, `notificationId` (FK BIGINT), `attemptNumber`, `status` (String), `errorMessage`, `attemptedAt`; Lombok `@Data @Builder @NoArgsConstructor @AllArgsConstructor`
- [x] T008 [P] Crear `NotificationRepository` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/repository/NotificationRepository.java` — `JpaRepository<Notification, Long>`; métodos: `findByInvestorIdOrderByCreatedAtDesc(Long investorId, Pageable pageable)`, `findByInvestorIdAndEventTypeOrderByCreatedAtDesc(Long, String, Pageable)`, `findByStatusAndArchivedFalseAndCreatedAtBefore(String status, LocalDateTime cutoff)`
- [x] T009 [P] Crear `NotificationAttemptRepository` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/repository/NotificationAttemptRepository.java` — `JpaRepository<NotificationAttempt, Long>`; método: `findByNotificationIdOrderByAttemptNumberAsc(Long notificationId)`
- [x] T010 [P] Crear DTO `NotificationDto` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/dto/NotificationDto.java` — record con campos: `Long id`, `String eventType`, `String channel`, `String subject`, `String body`, `String status`, `Long referenceId`, `LocalDateTime createdAt`
- [x] T011 [P] Crear DTO `NotificationAttemptDto` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/dto/NotificationAttemptDto.java` — record con campos: `Integer attemptNumber`, `String status`, `LocalDateTime attemptedAt`
- [x] T012 [P] Crear DTO `NotificationDetailDto` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/dto/NotificationDetailDto.java` — record con campos del `NotificationDto` más `List<NotificationAttemptDto> attempts`
- [x] T013 [P] Crear DTO `PagedNotificationResponse` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/dto/PagedNotificationResponse.java` — record con campos: `List<NotificationDto> content`, `long totalElements`, `int page`, `int size`
- [x] T014 [P] Crear excepción `NotificationNotFoundException` (→ 404) en `backend/notifications/src/main/java/com/accioneselbosque/notifications/exception/NotificationNotFoundException.java` — extiende `RuntimeException`; constructor recibe `Long id`; mensaje: `"Notification not found or does not belong to investor: " + id`
- [x] T015 Crear `GlobalExceptionHandler` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/exception/GlobalExceptionHandler.java` — `@RestControllerAdvice`; maneja `NotificationNotFoundException` → 404; maneja `Exception` genérica → 500 (depende de T014)

**Checkpoint Foundational**: Entidades JPA mapean correctamente contra tablas V19–V20; repositorios inyectables; DTOs compilables; `GlobalExceptionHandler` activo.

---

## Phase 3: User Story 1 — Notificación inmediata de cambio de estado (P1) 🎯 MVP

**Goal**: Cuando una orden cambia de estado (EXECUTED, CANCELLED, REJECTED, QUEUED), `NotificationService.sendOrderStatusChange()` crea el registro en `notification`, determina el canal preferido del inversionista (default EMAIL), envía el email vía `EmailNotificationSender` con Spring Retry (máx 3 intentos, backoff exponencial), y registra cada intento en `notification_attempt`. El canal PUSH se implementa como stub que registra SKIPPED.

**Independent Test**: Publicar un `OrderStatusChangeEvent` con estado ORDER_EXECUTED → verificar que se crea una fila en `notification` con `status=SENT`, exactamente una fila en `notification_attempt` con `status=SUCCESS`, y que `JavaMailSender.send()` fue invocado exactamente una vez.

- [x] T016 [P] [US1] Crear clase `OrderStatusChangeEvent` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/model/OrderStatusChangeEvent.java` — POJO con campos: `Long investorId`, `Long orderId`, `NotificationEventType eventType`, `String stockSymbol`, `Integer quantity`, `BigDecimal executionPrice`, `BigDecimal commission`, `BigDecimal totalAmount`, `String cancellationReason`, `String rejectionReason`; Lombok `@Data @Builder @AllArgsConstructor @NoArgsConstructor`
- [x] T017 [P] [US1] Crear interfaz `NotificationSender` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/service/NotificationSender.java` — un único método `void send(Notification notification)`
- [x] T018 [US1] Implementar `EmailNotificationSender` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/service/EmailNotificationSender.java` — `implements NotificationSender`; inyecta `JavaMailSender`; método `send(Notification n)` anotado con `@Retryable(retryFor = {MailException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))`; construye `SimpleMailMessage`; método `@Recover` que recibe `MailException` y relanza `RuntimeException` para marcar FAILED (depende de T017)
- [x] T019 [US1] Implementar `PushNotificationSender` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/service/PushNotificationSender.java` — `implements NotificationSender`; stub MVP: log `"PUSH stub: skipped for notification {}"`, sin envío real, sin excepción (depende de T017)
- [x] T020 [US1] Implementar `NotificationService` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/service/NotificationService.java` — `@Service`; inyecta `NotificationRepository`, `NotificationAttemptRepository`, `EmailNotificationSender`, `PushNotificationSender`, `InvestorPreferencesRepository`; método `sendOrderStatusChange(OrderStatusChangeEvent event)` anotado con `@TransactionalEventListener(phase = AFTER_COMMIT)`: (1) resolver canal (default EMAIL si NONE); (2) construir `subject` y `body` según `eventType`; (3) si BOTH, crear dos `Notification`; (4) persistir con `status=PENDING`; (5) invocar sender; (6) crear `NotificationAttempt`; (7) actualizar `Notification.status` (depende de T005–T015, T016–T019)
- [x] T021 [US1] Crear `OrderEventListener` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/service/OrderEventListener.java` — `@Component`; método `onOrderStatusChange(@TransactionalEventListener OrderStatusChangeEvent event)` delega a `NotificationService.sendOrderStatusChange(event)` (depende de T020)
- [x] T022 [P] [US1] Crear `NotificationsConfig` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/NotificationsConfig.java` — `@Configuration @EnableRetry`
- [x] T023 [US1] Añadir configuración de mail en `backend/app/src/main/resources/application.yml` — propiedades bajo `spring.mail`: `host`, `port`, `username`, `password`, `properties.mail.smtp.auth=true`, `properties.mail.smtp.starttls.enable=true` (depende de T018)

**Checkpoint US1**: Publicar `OrderStatusChangeEvent(eventType=ORDER_EXECUTED)` → `notification.status=SENT`; `notification_attempt.status=SUCCESS`; `JavaMailSender.send()` invocado una vez; canal PUSH genera intento SKIPPED; fallo de email → 3 intentos registrados antes de FAILED.

---

## Phase 4: User Story 2 — Configuración del canal de notificación preferido (P2)

**Goal**: `NotificationService` respeta el canal preferido del inversionista (`EMAIL`, `PUSH`, `BOTH`). Si el canal es `NONE` o no está configurado, se usa EMAIL por defecto. Si el canal es `BOTH`, se crean dos registros `Notification` independientes.

**Independent Test**: Configurar `InvestorPreferences.notifChannel = PUSH` → ejecutar una orden → verificar que se crea exactamente una `Notification` con `channel=PUSH` y ninguna con `channel=EMAIL`.

- [x] T024 [P] [US2] Verificar existencia del enum `NotifChannel` en el módulo `auth` — si ya existe en `auth`, añadir dependencia al módulo `auth` en `backend/notifications/pom.xml` y reutilizarlo; si no, crearlo en `backend/notifications/src/main/java/com/accioneselbosque/notifications/model/NotifChannel.java` con valores `EMAIL`, `PUSH`, `BOTH`, `NONE`
- [x] T025 [US2] Implementar método privado `resolveChannels(Long investorId)` en `NotificationService` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/service/NotificationService.java` — retorna `List<String>`: consultar `InvestorPreferencesRepository`; si vacío o NONE → `["EMAIL"]`; si BOTH → `["EMAIL","PUSH"]`; caso contrario → `[channel.name()]` (depende de T020)
- [x] T026 [US2] Adaptar `sendOrderStatusChange()` en `NotificationService` — iterar sobre `resolveChannels()` para crear una `Notification` y su intento de entrega por cada canal activo en `backend/notifications/src/main/java/com/accioneselbosque/notifications/service/NotificationService.java` (depende de T025)

**Checkpoint US2**: Canal EMAIL → solo email enviado; canal PUSH → solo stub SKIPPED; canal BOTH → exactamente dos registros en `notification`; canal NONE → idéntico a EMAIL.

---

## Phase 5: User Story 3 — Historial de notificaciones (P3)

**Goal**: `GET /notifications` retorna la lista paginada de notificaciones del inversionista autenticado con soporte de filtrado por `eventType`. `GET /notifications/{id}` retorna el detalle incluyendo los intentos de entrega. Solo el propio inversionista puede ver sus notificaciones.

**Independent Test**: Crear 5 notificaciones para investor A y 3 para investor B → `GET /notifications` con JWT de A → `totalElements=5`; `GET /notifications/{id_de_B}` con JWT de A → 404 NOT_FOUND.

- [x] T027 [P] [US3] Implementar método `getHistory(Long investorId, int page, int size, String eventType)` en `NotificationService` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/service/NotificationService.java` — `PageRequest.of(page, size, Sort.by("createdAt").descending())`; filtrar por `eventType` si no null; mapear a `NotificationDto`; retornar `PagedNotificationResponse` (depende de T008, T010, T013)
- [x] T028 [P] [US3] Implementar método `getDetail(Long investorId, Long notificationId)` en `NotificationService` — cargar por id; si no existe o `notification.investorId != investorId` → lanzar `NotificationNotFoundException`; cargar intentos; mapear a `NotificationDetailDto` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/service/NotificationService.java` (depende de T009–T012, T014)
- [x] T029 [US3] Implementar `NotificationController` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/controller/NotificationController.java` — `@RestController @RequestMapping("/notifications") @PreAuthorize("hasRole('INVESTOR'")`; `GET /notifications` con params opcionales `eventType`, `page=0`, `size=20`; extrae `investorId` del JWT; `GET /notifications/{id}` delega a `getDetail()`; retorna 200 en ambos casos (depende de T027–T028)

**Checkpoint US3**: `GET /notifications` retorna página correcta con orden descendente; `GET /notifications/{id}` incluye lista de intentos; acceso a notificación ajena → 404.

---

## Phase 6: Polish

- [x] T030 [P] Implementar `NotificationArchivalJob` en `backend/notifications/src/main/java/com/accioneselbosque/notifications/service/NotificationArchivalJob.java` — `@Component`; método `archiveOldNotifications()` con `@Scheduled(cron = "0 0 2 1 * *")`; marca `archived=TRUE` las notificaciones con `created_at < NOW() - INTERVAL '12 months'`; registra en log cuántas filas archivadas
- [x] T031 [P] Externalizar configuración de reintentos en `backend/app/src/main/resources/application.yml` — añadir `notifications.retry.max-attempts=3`, `notifications.retry.initial-delay-ms=1000`, `notifications.retry.multiplier=2`; inyectar vía `@Value` en `EmailNotificationSender` reemplazando valores hardcodeados
- [x] T032 Ejecutar suite del módulo: `mvn test -pl backend/notifications` — confirmar flujos: ORDER_EXECUTED → SENT, ORDER_CANCELLED → SENT, fallo email → FAILED con 3 intentos, canal PUSH → SKIPPED

---

## Dependencias clave

- T001 → T002 (registrar módulo requiere que pom.xml del módulo exista)
- T003 (V19) → debe aplicar antes de T004 (V20 tiene FK hacia `notification.id`)
- T005–T007 (enums y entidades) → bloquean T008–T009; corren en paralelo entre sí
- T008–T014 (repositorios + DTOs + excepciones) → bloquean T015 y T020; corren en paralelo entre sí
- T017 (NotificationSender) → bloquea T018 (EmailSender) y T019 (PushSender)
- T016 + T017 + T018 + T019 + Phase 2 completa → bloquean T020 (NotificationService)
- T020 → bloquea T021 (OrderEventListener) y T025 (resolveChannels)
- T025 → bloquea T026
- T027 + T028 → bloquean T029 (NotificationController)
- T030 → depende de T008

## Parallel Execution Example — US1

```
Phase 2 completa (T005–T015)
        │
        ├── T016 (OrderStatusChangeEvent)
        ├── T017 (interfaz NotificationSender)   ──► T018 (EmailSender) ─┐
        │                                         └─► T019 (PushSender)  ─┤
        └── T022 (NotificationsConfig @EnableRetry)                       │
                                                                           ▼
                                                             T020 (NotificationService)
                                                                           │
                                                             T021 (OrderEventListener)
                                                                           │
                                                             T023 (application.yml mail)
```

# Research: AB-33 — Notificación de Estado de Órdenes

## Decisiones técnicas

### Canal PUSH en MVP

**Decision**: El canal PUSH se implementa como stub que registra el intento en `notification_attempt`
con `status = 'SKIPPED'`. La implementación real de push (Firebase, APNs) se deja para una fase
posterior.

**Rationale**: No hay frontend móvil en el proyecto académico. El stub permite que la lógica de
routing esté en su lugar sin infraestructura adicional.

### Reintentos automáticos

**Decision**: `Spring Retry` con `@Retryable` en `EmailNotificationSender.send()`. Máximo 3
intentos con backoff exponencial (1s, 2s, 4s). Si los 3 fallan, se registra como `FAILED` en
`notification_attempt`.

**Rationale**: Simple, sin colas de mensajes externas. Para el volumen académico es suficiente.

### Historial de retención 12 meses

**Decision**: Job `@Scheduled` mensual que archiva (marca como `archived = TRUE`) las
notificaciones con más de 12 meses. No se eliminan.

### Integración con módulos productores

**Decision**: Los módulos que generan eventos de orden (`orders`) llaman a
`NotificationFacade.sendOrderStatusChange(investorId, orderId, newStatus, details)` in-process
como una llamada de servicio normal. La llamada es sincrónica pero no bloquea el flujo principal
(se ejecuta al final de la transacción vía `@TransactionalEventListener`).

### Canal preferido

**Decision**: `NotificationService` consulta in-process al `auth` module:
`InvestorPreferencesRepository.findByInvestorId(id).notifChannel`. Si `NONE`, se usa EMAIL.

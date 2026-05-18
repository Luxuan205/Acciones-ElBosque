# Research: AB-38 — Auditoría y Cumplimiento Legal

## Decisiones técnicas

### Inmutabilidad del log

**Decision**: A nivel de repositorio, `AuditEventRepository` extiende `Repository<AuditEvent, Long>`
(no `JpaRepository`) — solo expone métodos de consulta, sin `save(entity)` genérico. El único
método de escritura es `AuditService.record()` que persiste un `AuditEvent` nuevo via `EntityManager.persist()` directamente. No hay `@Modifying` ni `DELETE` en ninguna query del repositorio.

**Rationale**: Previene eliminaciones accidentales incluso si un desarrollador tiene acceso al repositorio.
A nivel de DB, se puede añadir una política de RLS de PostgreSQL en producción.

### Escritura asíncrona

**Decision**: `@Async` en `AuditService.record()` para que la escritura del log no bloquee el
flujo principal. El timestamp almacenado es el del momento del evento (pasado como parámetro),
no el de la escritura.

**Rationale**: Cumple SC-002 (< 1 segundo) sin impactar el rendimiento de la operación auditada.
En caso de falla de escritura, se loggea el error (no se propaga al llamador).

### Tipos de eventos

Categorías iniciales:
- AUTH: `AUTH_SUCCESS`, `AUTH_FAILURE`, `AUTH_MFA_FAILED`, `ACCOUNT_LOCKED`, `ACCOUNT_UNLOCKED`
- ORDERS: `ORDER_CREATED`, `ORDER_CANCELLED`, `ORDER_EXECUTED`, `ORDER_REJECTED`
- PROFILE: `PROFILE_UPDATED`, `PASSWORD_CHANGED`
- SUBSCRIPTION: `SUBSCRIPTION_ACTIVATED`, `SUBSCRIPTION_EXPIRED`
- ADMIN: `USER_SUSPENDED`, `USER_UNSUSPENDED`, `ROLE_CHANGED`, `PARAMETER_CHANGED`
- ALERTS: `PRICE_ALERT_CREATED`, `PRICE_ALERT_TRIGGERED`

### Archivado

**Decision**: Campo `archived BOOLEAN` en `audit_event`. Job mensual marca `archived = TRUE` los
eventos con más de 5 años. El endpoint de consulta filtra por `archived = FALSE` por defecto;
incluye opción `includeArchived=true` para admins.

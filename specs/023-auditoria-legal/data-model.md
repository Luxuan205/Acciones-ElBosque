# Data Model: AB-38 — Auditoría y Cumplimiento Legal

## Nueva entidad

### AuditEvent
```
audit_event
├── id              BIGSERIAL     PK
├── event_type      VARCHAR(50)   NOT NULL  (ver AuditEventType)
├── investor_id     BIGINT        NULL FK → investor.id  (NULL para eventos de sistema)
├── performed_by    BIGINT        NULL FK → investor.id  (quién realizó la acción — puede ser admin)
├── reference_type  VARCHAR(30)   NULL      ('ORDER' | 'INVESTOR' | 'SUBSCRIPTION' | etc.)
├── reference_id    BIGINT        NULL      (ID del objeto afectado)
├── detail          JSONB         NULL      (datos adicionales: IP, valores previo/nuevo, etc.)
├── result          VARCHAR(20)   NOT NULL  ('SUCCESS' | 'FAILURE')
├── ip_address      VARCHAR(45)   NULL      (IPv4 o IPv6)
├── archived        BOOLEAN       NOT NULL DEFAULT FALSE
└── occurred_at     TIMESTAMP     NOT NULL  (timestamp del evento, no de la escritura)

INDEX audit_investor_idx ON audit_event(investor_id, occurred_at DESC)
INDEX audit_type_idx ON audit_event(event_type, occurred_at DESC)
INDEX audit_performed_by_idx ON audit_event(performed_by, occurred_at DESC)
```

## Flyway Migrations

### V27__create_audit_event_table.sql
```sql
CREATE TABLE audit_event (
    id             BIGSERIAL PRIMARY KEY,
    event_type     VARCHAR(50) NOT NULL,
    investor_id    BIGINT NULL REFERENCES investor(id),
    performed_by   BIGINT NULL REFERENCES investor(id),
    reference_type VARCHAR(30) NULL,
    reference_id   BIGINT NULL,
    detail         JSONB NULL,
    result         VARCHAR(20) NOT NULL,
    ip_address     VARCHAR(45) NULL,
    archived       BOOLEAN NOT NULL DEFAULT FALSE,
    occurred_at    TIMESTAMP NOT NULL
);
CREATE INDEX audit_investor_idx ON audit_event(investor_id, occurred_at DESC);
CREATE INDEX audit_type_idx ON audit_event(event_type, occurred_at DESC);
CREATE INDEX audit_performed_by_idx ON audit_event(performed_by, occurred_at DESC);
```

## Java — AuditEventRecord (facade input)

```java
record AuditEventRecord(
    AuditEventType eventType,
    Long investorId,        // usuario afectado (null si es evento de sistema)
    Long performedBy,       // quien realizó la acción (puede ser admin distinto al investor)
    String referenceType,
    Long referenceId,
    Map<String, Object> detail,
    AuditResult result,     // SUCCESS | FAILURE
    String ipAddress,
    Instant occurredAt
)
```

## Business Rules
- `AuditEventRepository` NO expone `delete()`, `deleteAll()`, ni `@Modifying` queries
- `archived = TRUE` solo lo puede establecer `AuditArchiveJob` (no accesible por API)
- El endpoint GET solo devuelve `archived = FALSE` por defecto (parámetro `includeArchived=true` para admin)
- BROKER: solo puede ver eventos donde `investor_id` sea uno de sus clientes (fuera de scope de este MVP)

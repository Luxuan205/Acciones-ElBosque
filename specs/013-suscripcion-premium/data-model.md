# Data Model: AB-18 — Activación de Suscripción Premium

## Entidades existentes (sin cambios de esquema)

### Investor (campos de suscripción ya en V8)
```
investor
├── subscription_type      VARCHAR(20) NOT NULL DEFAULT 'STANDARD'  ← existente
└── subscription_expires_at TIMESTAMP NULL                           ← existente
```

## Nueva entidad

### SubscriptionEvent
```
subscription_event
├── id               BIGSERIAL    PK
├── investor_id      BIGINT       NOT NULL FK → investor.id ON DELETE CASCADE
├── event_type       VARCHAR(30)  NOT NULL  ('ACTIVATED' | 'EXPIRED' | 'DOWNGRADED' | 'RENEWED')
├── previous_type    VARCHAR(20)  NOT NULL  ('STANDARD' | 'PREMIUM')
├── new_type         VARCHAR(20)  NOT NULL  ('STANDARD' | 'PREMIUM')
├── expires_at       TIMESTAMP    NULL      (nueva fecha de expiración tras ACTIVATED/RENEWED)
├── triggered_by     VARCHAR(20)  NOT NULL  ('INVESTOR' | 'SYSTEM_JOB' | 'ADMIN')
└── created_at       TIMESTAMP    NOT NULL DEFAULT NOW()

INDEX sub_event_investor_idx ON subscription_event(investor_id, created_at DESC)
```

## Flyway Migrations

### V15__create_subscription_event_table.sql
```sql
CREATE TABLE subscription_event (
    id            BIGSERIAL PRIMARY KEY,
    investor_id   BIGINT NOT NULL REFERENCES investor(id) ON DELETE CASCADE,
    event_type    VARCHAR(30) NOT NULL,
    previous_type VARCHAR(20) NOT NULL,
    new_type      VARCHAR(20) NOT NULL,
    expires_at    TIMESTAMP NULL,
    triggered_by  VARCHAR(20) NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX sub_event_investor_idx ON subscription_event(investor_id, created_at DESC);
```

## Java DTOs

```java
record SubscriptionStatusResponse(
    String subscriptionType,   // 'STANDARD' | 'PREMIUM'
    LocalDateTime activatedAt, // null si STANDARD
    LocalDateTime expiresAt,   // null si STANDARD
    boolean isActive,
    long daysRemaining         // 0 si STANDARD o expirada
)

record ActivateSubscriptionResponse(
    String subscriptionType,   // 'PREMIUM'
    LocalDateTime activatedAt,
    LocalDateTime expiresAt
)
```

## Business Rules
- Solo se activa si `subscriptionType == STANDARD` (o si la PREMIUM ya venció)
- Si ya es PREMIUM activa: devolver estado actual sin crear nuevo evento (FR-008)
- Al activar: `subscriptionType = PREMIUM`, `subscriptionExpiresAt = NOW() + 30 días`
- Job de degradación: `UPDATE investor SET subscription_type='STANDARD', subscription_expires_at=NULL WHERE subscription_type='PREMIUM' AND subscription_expires_at < NOW()`
- BROKER y ADMIN: siempre considerados como premium en `SubscriptionGate.isPremiumActive()`

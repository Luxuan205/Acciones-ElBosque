# Data Model: AB-33 — Notificación de Estado de Órdenes

## Nuevas entidades

### Notification
```
notification
├── id            BIGSERIAL     PK
├── investor_id   BIGINT        NOT NULL FK → investor.id
├── event_type    VARCHAR(40)   NOT NULL  ('ORDER_EXECUTED' | 'ORDER_CANCELLED' | 'ORDER_REJECTED' | 'ORDER_QUEUED' | 'PRICE_ALERT' | 'MARKET_ALERT' | 'SUBSCRIPTION_ACTIVATED' | 'SUBSCRIPTION_EXPIRED')
├── channel       VARCHAR(20)   NOT NULL  ('EMAIL' | 'PUSH')
├── subject       VARCHAR(200)  NOT NULL
├── body          TEXT          NOT NULL
├── status        VARCHAR(20)   NOT NULL  ('PENDING' | 'SENT' | 'FAILED' | 'SKIPPED')
├── reference_id  BIGINT        NULL      (order_id, alert_id, etc.)
├── archived      BOOLEAN       NOT NULL DEFAULT FALSE
└── created_at    TIMESTAMP     NOT NULL DEFAULT NOW()

INDEX notif_investor_idx ON notification(investor_id, created_at DESC)
INDEX notif_status_idx ON notification(status, created_at)
```

### NotificationAttempt
```
notification_attempt
├── id               BIGSERIAL   PK
├── notification_id  BIGINT      NOT NULL FK → notification.id ON DELETE CASCADE
├── attempt_number   INT         NOT NULL
├── status           VARCHAR(20) NOT NULL  ('SUCCESS' | 'FAILED' | 'SKIPPED')
├── error_message    TEXT        NULL
└── attempted_at     TIMESTAMP   NOT NULL DEFAULT NOW()
```

## Flyway Migrations

### V21__create_notification_table.sql
```sql
CREATE TABLE notification (
    id           BIGSERIAL PRIMARY KEY,
    investor_id  BIGINT NOT NULL REFERENCES investor(id),
    event_type   VARCHAR(40) NOT NULL,
    channel      VARCHAR(20) NOT NULL,
    subject      VARCHAR(200) NOT NULL,
    body         TEXT NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reference_id BIGINT NULL,
    archived     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX notif_investor_idx ON notification(investor_id, created_at DESC);
CREATE INDEX notif_status_idx ON notification(status, created_at);
```

### V22__create_notification_attempt_table.sql
```sql
CREATE TABLE notification_attempt (
    id              BIGSERIAL PRIMARY KEY,
    notification_id BIGINT NOT NULL REFERENCES notification(id) ON DELETE CASCADE,
    attempt_number  INT NOT NULL,
    status          VARCHAR(20) NOT NULL,
    error_message   TEXT NULL,
    attempted_at    TIMESTAMP NOT NULL DEFAULT NOW()
);
```

## Java DTOs

```java
record NotificationDto(
    Long id,
    String eventType,
    String channel,
    String subject,
    String status,
    Long referenceId,
    LocalDateTime createdAt
)
```

## Business Rules
- Canal EMAIL: envío vía `JavaMailSender`; reintentos máx 3
- Canal PUSH: stub (SKIPPED) en MVP
- Canal BOTH: se crean dos entradas en `notification` (una EMAIL, una PUSH)
- `NotifChannel.NONE` en preferencias → canal EMAIL por defecto
- Archivado mensual de notificaciones con más de 12 meses

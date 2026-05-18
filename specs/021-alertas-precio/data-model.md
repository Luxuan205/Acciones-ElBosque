# Data Model: AB-35 — Alertas de Precio Personalizadas

## Nueva entidad

### PriceAlert
```
price_alert
├── id              BIGSERIAL       PK
├── investor_id     BIGINT          NOT NULL FK → investor.id ON DELETE CASCADE
├── symbol          VARCHAR(20)     NOT NULL
├── alert_type      VARCHAR(20)     NOT NULL  ('ABSOLUTE' | 'PERCENTAGE')
├── direction       VARCHAR(10)     NULL      ('ABOVE' | 'BELOW') — solo para ABSOLUTE
├── trigger_value   DECIMAL(18,2)   NOT NULL  (precio absoluto o % de variación)
├── reference_price DECIMAL(18,2)   NULL      (precio al crear — solo para PERCENTAGE)
├── status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE'  ('ACTIVE' | 'TRIGGERED' | 'INACTIVE' | 'SUSPENDED')
├── triggered_at    TIMESTAMP       NULL
└── created_at      TIMESTAMP       NOT NULL DEFAULT NOW()

INDEX price_alert_investor_idx ON price_alert(investor_id, status)
INDEX price_alert_symbol_status_idx ON price_alert(symbol, status)
```

## Flyway Migrations

### V24__create_price_alert_table.sql
```sql
CREATE TABLE price_alert (
    id              BIGSERIAL PRIMARY KEY,
    investor_id     BIGINT NOT NULL REFERENCES investor(id) ON DELETE CASCADE,
    symbol          VARCHAR(20) NOT NULL,
    alert_type      VARCHAR(20) NOT NULL,
    direction       VARCHAR(10) NULL,
    trigger_value   DECIMAL(18,2) NOT NULL,
    reference_price DECIMAL(18,2) NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    triggered_at    TIMESTAMP NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX price_alert_investor_idx ON price_alert(investor_id, status);
CREATE INDEX price_alert_symbol_status_idx ON price_alert(symbol, status);
```

## Java DTOs

```java
record CreatePriceAlertRequest(
    @NotBlank @Size(max=20) String symbol,
    @NotNull PriceAlertType alertType,    // ABSOLUTE | PERCENTAGE
    String direction,                      // ABOVE | BELOW (requerido para ABSOLUTE)
    @DecimalMin("0.01") BigDecimal triggerValue
)

record PriceAlertDto(
    Long id,
    String symbol,
    String alertType,
    String direction,
    BigDecimal triggerValue,
    BigDecimal referencePrice,
    String status,
    LocalDateTime triggeredAt,
    LocalDateTime createdAt
)
```

## Business Rules
- Gate PREMIUM: `SubscriptionGate.isPremiumActive(investorId)` = false → HTTP 403
- Máx alertas ACTIVE por usuario: 20 (configurable en AB-40)
- Si `alertType = PERCENTAGE`: `referencePrice` = precio actual al crear; no requiere `direction`
- Al dispararse: `status = TRIGGERED`, `triggered_at = NOW()`; se envía notificación via `NotificationService`
- SUSPENDED: al vencer suscripción premium; no se evalúan; se reactivan al renovar

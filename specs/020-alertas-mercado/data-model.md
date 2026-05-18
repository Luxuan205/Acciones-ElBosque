# Data Model: AB-34 — Alertas de Mercado

## Nueva entidad

### MarketAlertSubscription
```
market_alert_subscription
├── id              BIGSERIAL     PK
├── investor_id     BIGINT        NOT NULL FK → investor.id ON DELETE CASCADE
├── alert_type      VARCHAR(30)   NOT NULL  ('MARKET_OPEN' | 'MARKET_CLOSE' | 'TRADING_SUSPENDED' | 'UNUSUAL_VOLUME')
├── symbol          VARCHAR(20)   NULL      (NULL para alertas globales OPEN/CLOSE)
├── threshold_value DECIMAL(10,2) NULL      (solo UNUSUAL_VOLUME — porcentaje de volumen)
├── active          BOOLEAN       NOT NULL DEFAULT TRUE
└── created_at      TIMESTAMP     NOT NULL DEFAULT NOW()

INDEX market_alert_investor_idx ON market_alert_subscription(investor_id, active)
INDEX market_alert_type_symbol_idx ON market_alert_subscription(alert_type, symbol, active)
```

## Flyway Migrations

### V23__create_market_alert_subscription_table.sql
```sql
CREATE TABLE market_alert_subscription (
    id              BIGSERIAL PRIMARY KEY,
    investor_id     BIGINT NOT NULL REFERENCES investor(id) ON DELETE CASCADE,
    alert_type      VARCHAR(30) NOT NULL,
    symbol          VARCHAR(20) NULL,
    threshold_value DECIMAL(10,2) NULL,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX market_alert_investor_idx ON market_alert_subscription(investor_id, active);
CREATE INDEX market_alert_type_symbol_idx ON market_alert_subscription(alert_type, symbol, active);
```

## Java DTOs

```java
record CreateMarketAlertRequest(
    @NotNull MarketAlertType alertType,
    @Size(max=20) String symbol,          // requerido para TRADING_SUSPENDED y UNUSUAL_VOLUME
    BigDecimal thresholdValue             // requerido para UNUSUAL_VOLUME (porcentaje)
)

record MarketAlertSubscriptionDto(
    Long id,
    String alertType,
    String symbol,
    BigDecimal thresholdValue,
    boolean active,
    LocalDateTime createdAt
)
```

## Business Rules
- MARKET_OPEN y MARKET_CLOSE: `symbol = NULL`, `threshold_value = NULL`
- TRADING_SUSPENDED: `symbol` requerido; `threshold_value = NULL`
- UNUSUAL_VOLUME: `symbol` requerido; `threshold_value` requerido (% de volumen promedio)
- Una suscripción desactivada no genera alertas pero conserva su configuración
- Un suscriptor puede recibir múltiples alertas del mismo tipo si tiene múltiples suscripciones activas

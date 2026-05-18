# Data Model: AB-22 — Stop-Loss y Take-Profit

## Nueva entidad

### ConditionalOrder
```
conditional_order
├── id              BIGSERIAL       PK
├── investor_id     BIGINT          NOT NULL FK → investor.id
├── type            VARCHAR(20)     NOT NULL  ('STOP_LOSS' | 'TAKE_PROFIT')
├── symbol          VARCHAR(20)     NOT NULL
├── quantity        INT             NOT NULL  CHECK (quantity > 0)
├── trigger_price   DECIMAL(18,2)   NOT NULL  (precio que activa la orden)
├── status          VARCHAR(20)     NOT NULL  ('ACTIVE' | 'TRIGGERED' | 'CANCELLED')
├── oco_partner_id  BIGINT          NULL FK → conditional_order.id  (par OCO)
├── triggered_order_id BIGINT       NULL FK → market_order.id  (orden generada al activarse)
├── created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
└── updated_at      TIMESTAMP       NOT NULL DEFAULT NOW()

INDEX cond_order_investor_idx ON conditional_order(investor_id, status)
INDEX cond_order_symbol_status_idx ON conditional_order(symbol, status)
```

## Flyway Migrations

### V19__create_conditional_order_table.sql
```sql
CREATE TABLE conditional_order (
    id                 BIGSERIAL PRIMARY KEY,
    investor_id        BIGINT NOT NULL REFERENCES investor(id),
    type               VARCHAR(20) NOT NULL,
    symbol             VARCHAR(20) NOT NULL,
    quantity           INT NOT NULL CHECK (quantity > 0),
    trigger_price      DECIMAL(18,2) NOT NULL,
    status             VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    oco_partner_id     BIGINT NULL REFERENCES conditional_order(id),
    triggered_order_id BIGINT NULL REFERENCES market_order(id),
    created_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX cond_order_investor_idx ON conditional_order(investor_id, status);
CREATE INDEX cond_order_symbol_status_idx ON conditional_order(symbol, status);
```

## Java DTOs

```java
record CreateStopLossRequest(
    @NotBlank String symbol,
    @Min(1) int quantity,
    @DecimalMin("0.01") BigDecimal triggerPrice,
    Long takeProfitId   // opcional — para vincular OCO
)

record ConditionalOrderResponse(
    Long id,
    String type,          // 'STOP_LOSS' | 'TAKE_PROFIT'
    String symbol,
    int quantity,
    BigDecimal triggerPrice,
    String status,
    Long ocoPartnerId,
    LocalDateTime createdAt
)
```

## Business Rules
- STOP_LOSS activo: se activa cuando `currentPrice <= triggerPrice`
- TAKE_PROFIT activo: se activa cuando `currentPrice >= triggerPrice`
- Al activarse: `status = TRIGGERED`, se genera MARKET_SELL, el OCO partner pasa a CANCELLED
- Si el inversionista cierra la posición manualmente: todos los ConditionalOrder ACTIVE para ese symbol se cancelan

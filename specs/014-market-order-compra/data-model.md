# Data Model: AB-19 — Generación de Market Order de Compra

## Nuevas entidades (compartidas con AB-20 al AB-23)

### Order
```
market_order
├── id              BIGSERIAL       PK
├── investor_id     BIGINT          NOT NULL FK → investor.id
├── order_type      VARCHAR(20)     NOT NULL  ('MARKET_BUY' | 'MARKET_SELL' | 'LIMIT_BUY' | 'LIMIT_SELL')
├── status          VARCHAR(20)     NOT NULL  ('QUEUED' | 'PENDING' | 'EXECUTED' | 'CANCELLED' | 'REJECTED')
├── symbol          VARCHAR(20)     NOT NULL
├── quantity        INT             NOT NULL  CHECK (quantity > 0)
├── estimated_price DECIMAL(18,2)   NOT NULL  (precio al momento de crear la orden)
├── execution_price DECIMAL(18,2)   NULL      (precio real de ejecución — se llena al ejecutar)
├── commission      DECIMAL(18,2)   NOT NULL
├── total_estimated DECIMAL(18,2)   NOT NULL  (quantity * estimated_price + commission)
├── limit_price     DECIMAL(18,2)   NULL      (solo para LIMIT_BUY / LIMIT_SELL)
├── expires_at      TIMESTAMP       NULL      (solo para limit orders con GTD)
├── rejection_reason VARCHAR(200)   NULL
├── created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
└── updated_at      TIMESTAMP       NOT NULL DEFAULT NOW()

INDEX order_investor_idx ON market_order(investor_id, created_at DESC)
INDEX order_status_idx ON market_order(status, symbol)
```

### BalanceReservation
```
balance_reservation
├── id          BIGSERIAL     PK
├── order_id    BIGINT        NOT NULL UNIQUE FK → market_order.id ON DELETE CASCADE
├── investor_id BIGINT        NOT NULL FK → investor.id
├── amount      DECIMAL(18,2) NOT NULL  (monto reservado = total_estimated)
├── released    BOOLEAN       NOT NULL DEFAULT FALSE
├── created_at  TIMESTAMP     NOT NULL DEFAULT NOW()
└── released_at TIMESTAMP     NULL

INDEX balance_res_investor_idx ON balance_reservation(investor_id, released)
```

## Flyway Migrations

### V16__create_order_table.sql
```sql
CREATE TABLE market_order (
    id               BIGSERIAL PRIMARY KEY,
    investor_id      BIGINT NOT NULL REFERENCES investor(id),
    order_type       VARCHAR(20) NOT NULL,
    status           VARCHAR(20) NOT NULL,
    symbol           VARCHAR(20) NOT NULL,
    quantity         INT NOT NULL CHECK (quantity > 0),
    estimated_price  DECIMAL(18,2) NOT NULL,
    execution_price  DECIMAL(18,2) NULL,
    commission       DECIMAL(18,2) NOT NULL,
    total_estimated  DECIMAL(18,2) NOT NULL,
    limit_price      DECIMAL(18,2) NULL,
    expires_at       TIMESTAMP NULL,
    rejection_reason VARCHAR(200) NULL,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX order_investor_idx ON market_order(investor_id, created_at DESC);
CREATE INDEX order_status_idx ON market_order(status, symbol);
```

### V17__create_balance_reservation_table.sql
```sql
CREATE TABLE balance_reservation (
    id          BIGSERIAL PRIMARY KEY,
    order_id    BIGINT NOT NULL UNIQUE REFERENCES market_order(id) ON DELETE CASCADE,
    investor_id BIGINT NOT NULL REFERENCES investor(id),
    amount      DECIMAL(18,2) NOT NULL,
    released    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    released_at TIMESTAMP NULL
);
CREATE INDEX balance_res_investor_idx ON balance_reservation(investor_id, released);
```

## Java DTOs

```java
record PlaceMarketBuyRequest(
    @NotBlank @Size(max=20) String symbol,
    @Min(1) int quantity
)

record CommissionBreakdown(
    BigDecimal estimatedPrice,
    BigDecimal quantity,
    BigDecimal commission,
    BigDecimal totalEstimated
)

record OrderResponse(
    Long orderId,
    String status,             // 'PENDING' | 'QUEUED'
    String symbol,
    int quantity,
    CommissionBreakdown breakdown,
    LocalDateTime createdAt
)
```

## Business Rules
- Saldo disponible = saldo total − suma de `balance_reservation.amount` WHERE `released = FALSE`
- Si saldo disponible < `total_estimated`: REJECTED con mensaje de fondos insuficientes
- Tasa de comisión: 0.3% del valor bruto (configurable en parámetros globales AB-40)
- Si mercado abierto → status = PENDING; si mercado cerrado → status = QUEUED (previa confirmación)
- Al QUEUED: se reserva saldo igualmente para evitar doble gasto

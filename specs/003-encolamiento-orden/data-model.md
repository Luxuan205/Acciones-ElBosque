# Data Model: AB-24 — Encolamiento de Orden Fuera de Horario Bursátil

## Entities

### Order
```
Order
├── id              UUID            PK
├── investorId      UUID            NOT NULL  (investor who owns the order)
├── brokerId        UUID            NULL      (set by broker orders feature, AB-32)
├── symbol          VARCHAR(20)     NOT NULL
├── quantity        INTEGER         NOT NULL  CHECK > 0
├── orderType       VARCHAR(10)     NOT NULL  CHECK IN ('BUY','SELL')
├── status          VARCHAR(20)     NOT NULL  CHECK IN ('QUEUED','ACTIVE','EXECUTED','FAILED','CANCELLED')
├── unitPrice       DECIMAL(18,2)   NOT NULL  (price at time of order placement)
├── grossValue      DECIMAL(18,2)   NOT NULL
├── commissionRate  DECIMAL(5,4)    NOT NULL  (e.g. 0.0150 for 1.50%)
├── commissionAmt   DECIMAL(18,2)   NOT NULL
├── netTotal        DECIMAL(18,2)   NOT NULL
├── createdAt       TIMESTAMP       NOT NULL DEFAULT now()
└── processedAt     TIMESTAMP       NULL      (set when ACTIVE/EXECUTED/FAILED)
```

### OrderStatus (Java Enum)
```java
enum OrderStatus {
    QUEUED,     // placed outside market hours, awaiting processing
    ACTIVE,     // accepted and submitted to market
    EXECUTED,   // trade completed
    FAILED,     // processing failed (insufficient funds, market error)
    CANCELLED   // cancelled by investor while QUEUED
}
```

## Relationships
- `Order` many-to-one `Investor` (by `investorId`, not FK to avoid cross-module dependency)
- Max 10 orders per investor in QUEUED status (enforced in service layer)

## Validation Rules
- `symbol`: not blank, uppercase, 1–20 chars
- `quantity`: positive integer
- `orderType`: BUY or SELL
- Queue limit: max 10 QUEUED orders per investor (validated before insert)
- Cancellation: only allowed when `status == QUEUED`

## Flyway Migrations

### V1__create_order_table.sql
```sql
CREATE TABLE "order" (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    investor_id     UUID            NOT NULL,
    broker_id       UUID,
    symbol          VARCHAR(20)     NOT NULL,
    quantity        INTEGER         NOT NULL CHECK (quantity > 0),
    order_type      VARCHAR(10)     NOT NULL CHECK (order_type IN ('BUY','SELL')),
    status          VARCHAR(20)     NOT NULL DEFAULT 'QUEUED'
                                    CHECK (status IN ('QUEUED','ACTIVE','EXECUTED','FAILED','CANCELLED')),
    unit_price      DECIMAL(18,2)   NOT NULL,
    gross_value     DECIMAL(18,2)   NOT NULL,
    commission_rate DECIMAL(5,4)    NOT NULL,
    commission_amt  DECIMAL(18,2)   NOT NULL,
    net_total       DECIMAL(18,2)   NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    processed_at    TIMESTAMP
);

CREATE INDEX idx_order_investor_status ON "order"(investor_id, status);
CREATE INDEX idx_order_status_created  ON "order"(status, created_at ASC);
```

Note: `"order"` is quoted because `ORDER` is a reserved SQL keyword.

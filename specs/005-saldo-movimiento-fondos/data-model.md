# Data Model: AB-26 — Consulta de Saldo y Movimiento de Fondos

## Entities

### AccountBalance
```
AccountBalance
├── id            UUID          PK
├── investorId    UUID          NOT NULL UNIQUE  (one balance per investor)
├── totalBalance  DECIMAL(18,2) NOT NULL DEFAULT 0.00
├── currency      VARCHAR(3)    NOT NULL DEFAULT 'COP'
├── updatedAt     TIMESTAMP     NOT NULL DEFAULT now()
└── createdAt     TIMESTAMP     NOT NULL DEFAULT now()
```

### FundMovement
```
FundMovement
├── id             UUID          PK
├── investorId     UUID          NOT NULL
├── type           VARCHAR(15)   NOT NULL  CHECK IN ('DEPOSIT','WITHDRAWAL','PURCHASE','SALE','COMMISSION')
├── amount         DECIMAL(18,2) NOT NULL  CHECK != 0
├── balanceAfter   DECIMAL(18,2) NOT NULL  (account balance after applying this movement)
├── currency       VARCHAR(3)    NOT NULL DEFAULT 'COP'
├── description    VARCHAR(200)  NULL      (optional human-readable context)
├── orderId        UUID          NULL      (FK to order when movement is PURCHASE/SALE/COMMISSION)
└── createdAt      TIMESTAMP     NOT NULL DEFAULT now()
```

### MovementType (Java Enum)
```java
enum MovementType {
    DEPOSIT,       // investor adds funds
    WITHDRAWAL,    // investor withdraws funds
    PURCHASE,      // funds deducted for stock purchase
    SALE,          // funds credited from stock sale
    COMMISSION     // commission fee deducted
}
```

## Computed Values (not persisted)
- `reservedBalance`: SUM of `net_total` WHERE `investor_id = ? AND status IN ('ACTIVE','QUEUED')` — queried from order table in OrderRepository (in-process)
- `availableBalance = totalBalance - reservedBalance`

## Relationships
- `AccountBalance` 1-to-1 `Investor` (by investorId)
- `FundMovement` many-to-1 `Investor` (by investorId)
- `FundMovement.orderId` references `order.id` (no FK constraint — cross-module reference)

## Validation Rules
- `amount`: non-zero; positive for DEPOSIT/SALE; negative for WITHDRAWAL/PURCHASE/COMMISSION (sign convention enforced in service)
- `balanceAfter`: computed by service before insert; must not be negative (service validates funds before PURCHASE/WITHDRAWAL)
- Pagination: page size fixed at 20; `from` and `to` are ISO-8601 dates

## Flyway Migrations

### V1__create_account_balance_table.sql
```sql
CREATE TABLE account_balance (
    id             UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    investor_id    UUID          NOT NULL UNIQUE,
    total_balance  DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    currency       VARCHAR(3)    NOT NULL DEFAULT 'COP',
    updated_at     TIMESTAMP     NOT NULL DEFAULT NOW(),
    created_at     TIMESTAMP     NOT NULL DEFAULT NOW()
);
```

### V2__create_fund_movement_table.sql
```sql
CREATE TABLE fund_movement (
    id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    investor_id   UUID          NOT NULL,
    type          VARCHAR(15)   NOT NULL
                                CHECK (type IN ('DEPOSIT','WITHDRAWAL','PURCHASE','SALE','COMMISSION')),
    amount        DECIMAL(18,2) NOT NULL CHECK (amount != 0),
    balance_after DECIMAL(18,2) NOT NULL,
    currency      VARCHAR(3)    NOT NULL DEFAULT 'COP',
    description   VARCHAR(200),
    order_id      UUID,
    created_at    TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_fund_movement_investor_created
    ON fund_movement(investor_id, created_at DESC);
```

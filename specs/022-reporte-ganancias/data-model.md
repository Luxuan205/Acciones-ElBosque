# Data Model: AB-37 — Reporte de Ganancias y Pérdidas

## Nuevas entidades

### Position
```
position
├── id                  BIGSERIAL       PK
├── investor_id         BIGINT          NOT NULL FK → investor.id
├── symbol              VARCHAR(20)     NOT NULL
├── current_quantity    INT             NOT NULL DEFAULT 0
├── avg_purchase_price  DECIMAL(18,2)   NOT NULL
├── cash_balance        DECIMAL(18,4)   NOT NULL DEFAULT 0  (saldo disponible del inversionista)
├── created_at          TIMESTAMP       NOT NULL DEFAULT NOW()
└── updated_at          TIMESTAMP       NOT NULL DEFAULT NOW()

UNIQUE (investor_id, symbol)
INDEX position_investor_idx ON position(investor_id)
```

Nota: `cash_balance` solo se almacena en la posición del símbolo `CASH` (symbol = 'CASH') o en
un registro especial. Alternativa: tabla separada `investor_balance`. Para el MVP, se usa un
registro especial con `symbol = '_CASH'`.

### Transaction
```
transaction
├── id                  BIGSERIAL       PK
├── investor_id         BIGINT          NOT NULL FK → investor.id
├── order_id            BIGINT          NOT NULL FK → market_order.id
├── transaction_type    VARCHAR(20)     NOT NULL  ('BUY' | 'SELL')
├── symbol              VARCHAR(20)     NOT NULL
├── quantity            INT             NOT NULL
├── execution_price     DECIMAL(18,2)   NOT NULL
├── commission          DECIMAL(18,2)   NOT NULL
├── gross_amount        DECIMAL(18,2)   NOT NULL  (quantity × execution_price)
├── net_amount          DECIMAL(18,2)   NOT NULL  (gross_amount ± commission)
├── realized_gain       DECIMAL(18,2)   NULL      (solo para SELL — neto después de comisión)
├── avg_price_at_time   DECIMAL(18,2)   NULL      (PPP al momento de la venta — para SELL)
└── executed_at         TIMESTAMP       NOT NULL DEFAULT NOW()

INDEX transaction_investor_idx ON transaction(investor_id, executed_at DESC)
INDEX transaction_symbol_idx ON transaction(investor_id, symbol, executed_at DESC)
```

## Flyway Migrations

### V25__create_position_table.sql
```sql
CREATE TABLE position (
    id                 BIGSERIAL PRIMARY KEY,
    investor_id        BIGINT NOT NULL REFERENCES investor(id),
    symbol             VARCHAR(20) NOT NULL,
    current_quantity   INT NOT NULL DEFAULT 0,
    avg_purchase_price DECIMAL(18,2) NOT NULL DEFAULT 0,
    cash_balance       DECIMAL(18,4) NOT NULL DEFAULT 0,
    created_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_position_investor_symbol UNIQUE (investor_id, symbol)
);
CREATE INDEX position_investor_idx ON position(investor_id);
```

### V26__create_transaction_table.sql
```sql
CREATE TABLE transaction (
    id               BIGSERIAL PRIMARY KEY,
    investor_id      BIGINT NOT NULL REFERENCES investor(id),
    order_id         BIGINT NOT NULL REFERENCES market_order(id),
    transaction_type VARCHAR(20) NOT NULL,
    symbol           VARCHAR(20) NOT NULL,
    quantity         INT NOT NULL,
    execution_price  DECIMAL(18,2) NOT NULL,
    commission       DECIMAL(18,2) NOT NULL,
    gross_amount     DECIMAL(18,2) NOT NULL,
    net_amount       DECIMAL(18,2) NOT NULL,
    realized_gain    DECIMAL(18,2) NULL,
    avg_price_at_time DECIMAL(18,2) NULL,
    executed_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX transaction_investor_idx ON transaction(investor_id, executed_at DESC);
```

## Java DTOs

```java
record PositionDto(
    String symbol,
    int currentQuantity,
    BigDecimal avgPurchasePrice,
    BigDecimal currentPrice,
    BigDecimal unrealizedGain,
    BigDecimal unrealizedGainPct
)

record PortfolioReportDto(
    BigDecimal totalInvested,
    BigDecimal totalMarketValue,
    BigDecimal totalUnrealizedGain,
    BigDecimal totalRealizedGain,
    List<PositionDto> positions,
    List<TransactionDto> transactions
)
```

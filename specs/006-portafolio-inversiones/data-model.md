# Data Model: AB-27 — Visualización de Portafolio de Inversiones

## Entities

### Position
```
Position
├── id           UUID          PK
├── investorId   UUID          NOT NULL
├── symbol       VARCHAR(20)   NOT NULL
├── name         VARCHAR(100)  NOT NULL  (stock name cached at buy time)
├── quantity     INTEGER       NOT NULL  CHECK > 0
├── avgBuyPrice  DECIMAL(18,2) NOT NULL  CHECK > 0  (weighted average)
├── currency     VARCHAR(3)    NOT NULL DEFAULT 'COP'
├── updatedAt    TIMESTAMP     NOT NULL DEFAULT now()
└── createdAt    TIMESTAMP     NOT NULL DEFAULT now()

UNIQUE (investor_id, symbol)
```

## Java DTOs (not persisted — enriched at query time)

### PositionDto
```java
record PositionDto(
    String symbol,
    String name,
    int    quantity,
    BigDecimal avgBuyPrice,
    BigDecimal currentPrice,      // from StockSnapshotService (in-process)
    BigDecimal positionValue,     // quantity * currentPrice
    BigDecimal pnlAmount,         // (currentPrice - avgBuyPrice) * quantity
    BigDecimal pnlPercent,        // pnlAmount / (avgBuyPrice * quantity) * 100
    BigDecimal dayChange,         // from StockSnapshot.dayChange
    BigDecimal dayChangePct,      // from StockSnapshot.dayChangePct
    String currency
)
```

### PortfolioSummaryDto
```java
record PortfolioSummaryDto(
    BigDecimal totalValue,     // SUM of positionValue across all positions
    BigDecimal totalPnl,       // SUM of pnlAmount
    BigDecimal totalPnlPct,    // totalPnl / totalCost * 100
    BigDecimal totalDayChange, // SUM of (dayChange * quantity)
    int positionCount
)
```

## Calculation Rules

### Weighted Average Buy Price
```
newAvgBuyPrice = (existingQty * existingAvgBuyPrice + addedQty * addedPrice)
               / (existingQty + addedQty)
```
Applied in `PositionCalculator.recalculateAvgPrice()` each time a BUY order executes.

### Position Value and P&L
```
positionValue = quantity * currentPrice
pnlAmount     = (currentPrice - avgBuyPrice) * quantity
pnlPercent    = (pnlAmount / (avgBuyPrice * quantity)) * 100
```

### Sell (position reduction)
When a SELL executes: `position.quantity -= soldQuantity`. If `quantity == 0`, the Position row is deleted (closed position).

## Validation Rules
- `symbol + investorId`: unique per UNIQUE constraint
- `quantity`: always positive; 0 means closed (row deleted)
- `currentPrice` fallback: if `StockSnapshotService` returns null, `currentPrice = avgBuyPrice` (safe fallback, P&L = 0)

## Flyway Migrations

### V3__create_position_table.sql
```sql
CREATE TABLE position (
    id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    investor_id   UUID          NOT NULL,
    symbol        VARCHAR(20)   NOT NULL,
    name          VARCHAR(100)  NOT NULL,
    quantity      INTEGER       NOT NULL CHECK (quantity > 0),
    avg_buy_price DECIMAL(18,2) NOT NULL CHECK (avg_buy_price > 0),
    currency      VARCHAR(3)    NOT NULL DEFAULT 'COP',
    updated_at    TIMESTAMP     NOT NULL DEFAULT NOW(),
    created_at    TIMESTAMP     NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_position_investor_symbol UNIQUE (investor_id, symbol)
);

CREATE INDEX idx_position_investor ON position(investor_id);
```

# Data Model: AB-36 — Watchlist de Acciones (Funcionalidad Premium)

## Entities

### Watchlist
```
Watchlist
├── id          UUID      PK
├── investorId  UUID      NOT NULL UNIQUE  (one watchlist per investor)
└── createdAt   TIMESTAMP NOT NULL DEFAULT now()
```

Note: Created on-demand when the first entry is added. Never deleted when subscription expires.

### WatchlistEntry
```
WatchlistEntry
├── id           UUID          PK
├── watchlistId  UUID          NOT NULL  FK → watchlist.id ON DELETE CASCADE
├── symbol       VARCHAR(20)   NOT NULL
├── addedAt      TIMESTAMP     NOT NULL DEFAULT now()
└── priceAtAdded DECIMAL(18,2) NOT NULL  (market price when the entry was added)

UNIQUE (watchlist_id, symbol)
```

## Java DTOs (not persisted — enriched at query time)

### WatchlistEntryRequest
```java
record WatchlistEntryRequest(
    @NotBlank @Size(max = 20) String symbol
)
```

### WatchlistEntryDto
```java
record WatchlistEntryDto(
    String     symbol,
    String     name,              // from StockSnapshotService (in-process)
    BigDecimal currentPrice,      // from StockSnapshotService; null if no snapshot
    BigDecimal priceAtAdded,      // stored at add time
    BigDecimal dayChange,         // from StockSnapshot; null if no snapshot
    BigDecimal dayChangePct,      // from StockSnapshot; null if no snapshot
    LocalDateTime lastUpdated,    // StockSnapshot.updatedAt; null if no snapshot
    LocalDateTime addedAt
)
```

### WatchlistResponse
```java
record WatchlistResponse(
    UUID watchlistId,
    int  entryCount,              // size of entries list
    int  maxEntries,              // always 50
    List<WatchlistEntryDto> entries
)
```

## Relationships
- `Watchlist` 1-to-1 `Investor` (by investorId)
- `Watchlist` 1-to-many `WatchlistEntry` (CASCADE DELETE — if watchlist deleted, entries go too)
- `WatchlistEntry.symbol` references `stock_snapshot.symbol` logically (no FK — cross-module)

## Business Rules
- Max 50 entries per watchlist (validated before insert; HTTP 422 if exceeded)
- Duplicate symbol rejected by UNIQUE constraint (HTTP 409)
- Access blocked for non-premium investors (HTTP 403 from PremiumSubscriptionGate)
- Watchlist and entries NOT deleted on subscription expiry

## Validation Rules
- `symbol`: not blank, max 20 chars; must exist in `stock_snapshot` (validated against StockSnapshotService)
- Premium check: `investor.subscriptionType == 'PREMIUM' && investor.subscriptionExpiresAt > NOW()`

## Flyway Migrations

### V3__create_watchlist_table.sql
```sql
CREATE TABLE watchlist (
    id          UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    investor_id UUID      NOT NULL UNIQUE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
```

### V4__create_watchlist_entry_table.sql
```sql
CREATE TABLE watchlist_entry (
    id             UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    watchlist_id   UUID          NOT NULL REFERENCES watchlist(id) ON DELETE CASCADE,
    symbol         VARCHAR(20)   NOT NULL,
    added_at       TIMESTAMP     NOT NULL DEFAULT NOW(),
    price_at_added DECIMAL(18,2) NOT NULL,

    CONSTRAINT uq_watchlist_symbol UNIQUE (watchlist_id, symbol)
);

CREATE INDEX idx_watchlist_entry_watchlist ON watchlist_entry(watchlist_id);
```

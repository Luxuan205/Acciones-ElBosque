# Data Model: AB-28 — Dashboard de Comportamiento de Acciones

## Entities

### StockSnapshot
```
StockSnapshot
├── id             UUID          PK
├── symbol         VARCHAR(20)   NOT NULL UNIQUE
├── name           VARCHAR(100)  NOT NULL
├── currentPrice   DECIMAL(18,2) NOT NULL
├── previousClose  DECIMAL(18,2) NOT NULL
├── dayChange      DECIMAL(18,2) NOT NULL  (currentPrice - previousClose)
├── dayChangePct   DECIMAL(7,4)  NOT NULL  (dayChange / previousClose * 100)
├── volume         BIGINT        NOT NULL DEFAULT 0
└── updatedAt      TIMESTAMP     NOT NULL DEFAULT now()
```

### IntradayPricePoint
```
IntradayPricePoint
├── id        UUID          PK
├── symbol    VARCHAR(20)   NOT NULL
├── timestamp TIMESTAMP     NOT NULL
├── price     DECIMAL(18,2) NOT NULL
└── volume    BIGINT        NOT NULL DEFAULT 0

UNIQUE (symbol, timestamp)
```

### StockSortField (Java Enum — not persisted)
```java
enum StockSortField {
    name,
    dayChangePct,
    currentPrice,
    volume
}
```

## Relationships
- `StockSnapshot` 1-to-many `IntradayPricePoint` (by symbol string — no FK for performance)
- One `StockSnapshot` row per symbol (upsert on each `MarketDataIngestor` run)

## Validation Rules
- `symbol`: uppercase, alphanumeric, 1–20 chars (query param)
- `sort` query param: must be one of `{name_asc, name_desc, dayChangePct_asc, dayChangePct_desc}` — validated in controller
- Intraday interval: 5 minutes; purged at market close via `@Scheduled`

## Dev Seed Data (via Flyway)
Initial 10 Colombian market symbols with representative prices:

| symbol     | name                           | price (COP) |
|-----------|--------------------------------|-------------|
| PFBCOLOM  | Bancolombia Preferencial        | 39,500      |
| NUTRESA   | Grupo Nutresa                   | 68,200      |
| ISA       | Interconexión Eléctrica S.A.   | 22,100      |
| ECOPETROL | Ecopetrol S.A.                  | 1,950       |
| CEMARGOS  | Cementos Argos                  | 7,800       |
| GRUPOSURA | Grupo de Inversiones Suramericana| 18,300     |
| ÉXITO     | Grupo Éxito                     | 13,700      |
| ETB       | Empresa de Telecomunicaciones   | 410         |
| PFDAVVNDA | Davivienda Preferencial         | 52,600      |
| CLH       | Constructora Conconcreto        | 2,350       |

## Flyway Migrations

### V1__create_stock_snapshot_table.sql
```sql
CREATE TABLE stock_snapshot (
    id             UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    symbol         VARCHAR(20)   NOT NULL UNIQUE,
    name           VARCHAR(100)  NOT NULL,
    current_price  DECIMAL(18,2) NOT NULL,
    previous_close DECIMAL(18,2) NOT NULL,
    day_change     DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    day_change_pct DECIMAL(7,4)  NOT NULL DEFAULT 0.0000,
    volume         BIGINT        NOT NULL DEFAULT 0,
    updated_at     TIMESTAMP     NOT NULL DEFAULT NOW()
);

INSERT INTO stock_snapshot (id, symbol, name, current_price, previous_close) VALUES
    (gen_random_uuid(), 'PFBCOLOM',  'Bancolombia Preferencial',          39500.00, 39500.00),
    (gen_random_uuid(), 'NUTRESA',   'Grupo Nutresa',                      68200.00, 68200.00),
    (gen_random_uuid(), 'ISA',       'Interconexion Electrica S.A.',       22100.00, 22100.00),
    (gen_random_uuid(), 'ECOPETROL', 'Ecopetrol S.A.',                      1950.00,  1950.00),
    (gen_random_uuid(), 'CEMARGOS',  'Cementos Argos',                      7800.00,  7800.00),
    (gen_random_uuid(), 'GRUPOSURA', 'Grupo de Inversiones Suramericana',  18300.00, 18300.00),
    (gen_random_uuid(), 'EXITO',     'Grupo Exito',                        13700.00, 13700.00),
    (gen_random_uuid(), 'ETB',       'Empresa de Telecomunicaciones',         410.00,   410.00),
    (gen_random_uuid(), 'PFDAVVNDA', 'Davivienda Preferencial',            52600.00, 52600.00),
    (gen_random_uuid(), 'CLH',       'Constructora Conconcreto',            2350.00,  2350.00);
```

### V2__create_intraday_price_point_table.sql
```sql
CREATE TABLE intraday_price_point (
    id        UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    symbol    VARCHAR(20)   NOT NULL,
    timestamp TIMESTAMP     NOT NULL,
    price     DECIMAL(18,2) NOT NULL,
    volume    BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT uq_intraday_symbol_ts UNIQUE (symbol, timestamp)
);

CREATE INDEX idx_intraday_symbol_ts ON intraday_price_point(symbol, timestamp DESC);
```

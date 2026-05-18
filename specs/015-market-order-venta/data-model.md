# Data Model: AB-20 — Generación de Market Order de Venta

## Entidades reutilizadas

- `market_order` (V16) — se usa con `order_type = 'MARKET_SELL'`
- `balance_reservation` (V17) — NO aplica para ventas (se usa `title_reservation`)

## Nueva entidad

### TitleReservation
```
title_reservation
├── id          BIGSERIAL     PK
├── order_id    BIGINT        NOT NULL UNIQUE FK → market_order.id ON DELETE CASCADE
├── investor_id BIGINT        NOT NULL FK → investor.id
├── symbol      VARCHAR(20)   NOT NULL
├── quantity    INT           NOT NULL  CHECK (quantity > 0)
├── released    BOOLEAN       NOT NULL DEFAULT FALSE
├── created_at  TIMESTAMP     NOT NULL DEFAULT NOW()
└── released_at TIMESTAMP     NULL

INDEX title_res_investor_symbol_idx ON title_reservation(investor_id, symbol, released)
```

## Flyway Migrations

### V18__create_title_reservation_table.sql
```sql
CREATE TABLE title_reservation (
    id          BIGSERIAL PRIMARY KEY,
    order_id    BIGINT NOT NULL UNIQUE REFERENCES market_order(id) ON DELETE CASCADE,
    investor_id BIGINT NOT NULL REFERENCES investor(id),
    symbol      VARCHAR(20) NOT NULL,
    quantity    INT NOT NULL CHECK (quantity > 0),
    released    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    released_at TIMESTAMP NULL
);
CREATE INDEX title_res_investor_symbol_idx ON title_reservation(investor_id, symbol, released);
```

## Java DTOs

```java
record PlaceMarketSellRequest(
    @NotBlank @Size(max=20) String symbol,
    @Min(1) int quantity
)

// OrderResponse reutilizado de AB-19 — mismo DTO
// total_estimated para SELL = cantidad × precioEstimado − comisión (monto neto a recibir)
```

## Business Rules
- Títulos disponibles para venta = títulos en portafolio − reservas activas del mismo símbolo
- Si títulos disponibles < quantity: REJECTED con mensaje de títulos insuficientes
- Tasa de comisión: misma regla que MARKET_BUY (0.3% del valor bruto, configurable)
- `total_estimated` = netAmount (lo que recibirá el inversionista)

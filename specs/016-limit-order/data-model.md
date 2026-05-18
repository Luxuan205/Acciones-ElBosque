# Data Model: AB-21 — Colocación de Limit Order

## Entidades reutilizadas (sin cambios de esquema)

### market_order (V16)
Los campos relevantes para limit orders ya existen:
```
market_order
├── order_type       VARCHAR(20)   → 'LIMIT_BUY' | 'LIMIT_SELL'
├── limit_price      DECIMAL(18,2) → precio objetivo (NOT NULL para limit orders)
└── expires_at       TIMESTAMP     → NULL = GTC; fecha = GTD
```

- Para LIMIT_BUY: se reserva saldo en `balance_reservation`
- Para LIMIT_SELL: se reservan títulos en `title_reservation`

## No se requieren nuevas tablas en este spec.

## Java DTOs

```java
record PlaceLimitBuyRequest(
    @NotBlank @Size(max=20) String symbol,
    @Min(1) int quantity,
    @DecimalMin("0.01") BigDecimal limitPrice,    // precio máximo a pagar
    LocalDate expiresAt                            // null = GTC; fecha = GTD
)

record PlaceLimitSellRequest(
    @NotBlank @Size(max=20) String symbol,
    @Min(1) int quantity,
    @DecimalMin("0.01") BigDecimal limitPrice,    // precio mínimo a aceptar
    LocalDate expiresAt
)
```

## Business Rules
- LIMIT_BUY: se ejecuta cuando `currentPrice <= limitPrice`
- LIMIT_SELL: se ejecuta cuando `currentPrice >= limitPrice`
- GTD expirada: `status = CANCELLED`, recursos liberados, notificación enviada
- El job de evaluación ignora órdenes fuera de horario bursátil
- La reserva de saldo/títulos ocurre al crear la orden, independientemente del precio límite

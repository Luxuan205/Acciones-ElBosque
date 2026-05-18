# Data Model: AB-23 — Cancelación de Orden

## Modificaciones a entidades existentes

### market_order (modificación)
```
market_order ← añadir columnas
├── version              BIGINT        NOT NULL DEFAULT 0   ← optimistic lock
└── cancellation_reason  VARCHAR(200)  NULL
```

## Flyway Migrations

### V20__add_cancellation_fields_to_order.sql
```sql
ALTER TABLE market_order
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN cancellation_reason VARCHAR(200) NULL;
```

## Java DTOs

```java
record CancellationResponse(
    Long orderId,
    String previousStatus,    // 'PENDING' | 'QUEUED'
    String newStatus,         // 'CANCELLED'
    String resourcesReleased, // 'BALANCE' | 'TITLES' | 'NONE' (para condicionales)
    BigDecimal amountReleased,// saldo liberado (null para ventas/condicionales)
    Integer titlesReleased,   // títulos liberados (null para compras/condicionales)
    LocalDateTime cancelledAt
)

record BulkCancellationResponse(
    int totalRequested,
    int totalCancelled,
    int totalFailed,
    List<CancellationResponse> cancelled,
    List<BulkCancellationFailure> failed
)

record BulkCancellationFailure(
    Long orderId,
    String currentStatus,    // 'EXECUTED', 'ALREADY_CANCELLED'
    String reason
)
```

## Business Rules
- Estados cancelables: PENDING, QUEUED
- Estados no cancelables: EXECUTED, CANCELLED, REJECTED
- Al cancelar un PENDING de MARKET_BUY: liberar `balance_reservation`
- Al cancelar un PENDING de MARKET_SELL: liberar `title_reservation`
- Al cancelar un LIMIT_BUY/SELL: liberar reserva correspondiente
- Race condition: si `OptimisticLockException` → devolver estado actual de la orden al cliente
- Cancelación masiva: procesar en lote; reportar éxitos y fracasos sin rollback global

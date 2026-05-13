# Data Model: AB-32 — Generación y Firma de Órdenes a Nombre del Cliente

## Entities

### Order (extended — broker_id field)
The `order` table already exists (created in AB-24). This feature adds `broker_id`
via a Flyway ALTER migration.

```
Order (additions only)
└── brokerId  UUID  NULL  (NULL = direct investor order; NOT NULL = broker-placed order)
```

Full `order` table schema: see `specs/003-encolamiento-orden/data-model.md`.

### Distinction: Direct vs Broker Orders
```
broker_id IS NULL     → direct order placed by investor
broker_id IS NOT NULL → order placed by broker on behalf of investor
```

## Java DTOs

### BrokerOrderRequest
```java
record BrokerOrderRequest(
    @NotNull UUID   clientId,      // investorId of the client
    @NotBlank String symbol,
    @Positive int   quantity,
    @NotBlank String orderType,    // "BUY" or "SELL"
    @Positive @DecimalMin("0.01") BigDecimal unitPrice
)
```

### BrokerOrderHistoryResponse
```java
record BrokerOrderHistoryResponse(
    UUID       orderId,
    String     clientName,          // investor.fullName (resolved in-process)
    UUID       clientId,
    String     brokerName,          // broker's investor.fullName (resolved in-process)
    String     symbol,
    int        quantity,
    String     orderType,
    String     status,
    BigDecimal netTotal,
    LocalDateTime createdAt,
    LocalDateTime processedAt
)
```

## Relationships
- `Order.investorId` = the client (order owner, receives the trade)
- `Order.brokerId` = the broker who placed it (author, for auditing)
- `BrokerClientAssignment` validates the broker–client relationship before order creation

## Validation Rules
- `clientId`: must be in broker's assigned clients (`BrokerAssignmentValidator`)
- `symbol`: not blank, uppercase
- `quantity`: positive integer
- `orderType`: BUY or SELL
- `unitPrice`: positive, scale ≤ 2
- Commission rate determined by `client's` subscriptionType (not the broker's)
- Broker-placed orders follow the same queue and limit rules as direct orders

## Flyway Migrations

### V2__add_broker_id_to_order.sql
```sql
ALTER TABLE "order"
    ADD COLUMN broker_id UUID;

CREATE INDEX idx_order_broker ON "order"(broker_id)
    WHERE broker_id IS NOT NULL;
```

Note: No FK constraint on `broker_id` to avoid cross-module schema coupling.
The application layer guarantees referential integrity via `BrokerAssignmentValidator`.

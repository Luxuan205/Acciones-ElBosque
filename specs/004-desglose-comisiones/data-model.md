# Data Model: AB-25 — Visualización y Desglose de Comisiones

## Entities

### CommissionRate
```
CommissionRate
├── id                UUID          PK
├── subscriptionType  VARCHAR(20)   NOT NULL UNIQUE  CHECK IN ('STANDARD','PREMIUM')
└── ratePercent       DECIMAL(5,2)  NOT NULL  (e.g. 1.50 for 1.50%, 0.80 for 0.80%)
```

Note: `OrderPreviewResponse` is a pure DTO — it is NOT persisted. The commission rate
is calculated server-side on both preview and order confirmation to prevent tampering.

## Java DTOs (not persisted)

### OrderPreviewRequest
```java
record OrderPreviewRequest(
    @NotBlank String symbol,
    @Positive int quantity,
    @Positive @DecimalMin("0.01") BigDecimal unitPrice,
    @NotBlank String orderType     // "BUY" or "SELL"
)
```

### OrderPreviewResponse
```java
record OrderPreviewResponse(
    String   symbol,
    int      quantity,
    BigDecimal unitPrice,
    BigDecimal grossValue,         // quantity * unitPrice
    String   subscriptionType,     // STANDARD or PREMIUM
    BigDecimal ratePercent,        // e.g. 1.50
    BigDecimal commissionAmount,   // grossValue * (ratePercent / 100)
    BigDecimal netTotal            // BUY: grossValue + commissionAmount
                                   // SELL: grossValue - commissionAmount
)
```

## Calculation Rules
- `grossValue = quantity * unitPrice`
- `commissionAmount = grossValue.multiply(ratePercent.divide(100, 4, HALF_UP))`
- BUY `netTotal = grossValue + commissionAmount` (investor pays more)
- SELL `netTotal = grossValue - commissionAmount` (investor receives less)
- All BigDecimal operations use `RoundingMode.HALF_UP` with scale 2

## Validation Rules
- `quantity`: positive integer > 0
- `unitPrice`: positive, scale ≤ 2
- `orderType`: must be BUY or SELL
- `subscriptionType` resolved from JWT claim; not provided by client

## Flyway Migrations

### V2__create_commission_rate_table.sql
```sql
CREATE TABLE commission_rate (
    id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_type VARCHAR(20)   NOT NULL UNIQUE
                                    CHECK (subscription_type IN ('STANDARD','PREMIUM')),
    rate_percent      DECIMAL(5,2)  NOT NULL CHECK (rate_percent > 0)
);

INSERT INTO commission_rate (id, subscription_type, rate_percent) VALUES
    (gen_random_uuid(), 'STANDARD', 1.50),
    (gen_random_uuid(), 'PREMIUM',  0.80);
```

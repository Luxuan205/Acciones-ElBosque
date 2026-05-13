# Data Model: AB-31 — Gestión de Clientes Asignados por el Comisionista

## Entities

### BrokerClientAssignment
```
BrokerClientAssignment
├── id          UUID      PK
├── brokerId    UUID      NOT NULL  (references investor.id where role=BROKER)
├── investorId  UUID      NOT NULL  (references investor.id where role=INVESTOR)
├── assignedAt  TIMESTAMP NOT NULL DEFAULT now()
├── active      BOOLEAN   NOT NULL DEFAULT true
└── deactivatedAt TIMESTAMP NULL   (set when admin deactivates assignment)

UNIQUE (broker_id, investor_id)
```

Note: `brokerId` and `investorId` reference the same `investor` table (auth_db schema).
Both are UUID foreign keys but no FK constraint is enforced at DB level to allow independent
evolution of the investor entity.

## Java DTOs (not persisted)

### ClientSummaryDto
```java
record ClientSummaryDto(
    UUID   investorId,
    String fullName,
    String email,
    String accountStatus,           // ACTIVE, INACTIVE, PENDING
    BigDecimal availableBalance,    // from portfolio (in-process)
    int    activeOrdersCount,       // from order (in-process)
    LocalDateTime assignedAt
)
```

### ClientDetailDto
```java
record ClientDetailDto(
    UUID   investorId,
    String fullName,
    String email,
    String phone,
    String accountStatus,
    BigDecimal totalBalance,
    BigDecimal availableBalance,
    PortfolioSummaryDto portfolioSummary,   // from portfolio
    List<OrderResponse> recentOrders        // last 5 orders from order
)
```

## Relationships
- `BrokerClientAssignment` many-to-1 `Investor` via `brokerId` (broker side)
- `BrokerClientAssignment` many-to-1 `Investor` via `investorId` (client side)
- One broker can have many clients; one investor can be assigned to at most one broker (enforced by admin convention, not DB constraint in this sprint)

## Validation Rules
- `search` query param: optional, max 100 chars; matched against `fullName` (ILIKE)
- `status` query param: optional, must be one of ACTIVE, INACTIVE, PENDING
- `investorId` path param: must belong to requesting broker's assigned clients (service-layer check)
- Only users with role BROKER can access these endpoints

## Flyway Migrations

### V6__create_broker_client_assignment_table.sql
```sql
CREATE TABLE broker_client_assignment (
    id              UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    broker_id       UUID      NOT NULL,
    investor_id     UUID      NOT NULL,
    assigned_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    active          BOOLEAN   NOT NULL DEFAULT TRUE,
    deactivated_at  TIMESTAMP,

    CONSTRAINT uq_broker_investor UNIQUE (broker_id, investor_id)
);

CREATE INDEX idx_bca_broker_active ON broker_client_assignment(broker_id, active);
CREATE INDEX idx_bca_investor      ON broker_client_assignment(investor_id);
```

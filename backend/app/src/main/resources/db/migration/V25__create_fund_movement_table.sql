CREATE TABLE fund_movement (
    id           BIGSERIAL PRIMARY KEY,
    investor_id  BIGINT NOT NULL REFERENCES investor(id),
    type         VARCHAR(15) NOT NULL CHECK (type IN ('DEPOSIT','WITHDRAWAL','PURCHASE','SALE','COMMISSION')),
    amount       DECIMAL(18,2) NOT NULL,
    balance_after DECIMAL(18,2),
    currency     VARCHAR(3) NOT NULL DEFAULT 'COP',
    description  VARCHAR(200),
    order_id     BIGINT NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_fund_movement_investor_created ON fund_movement(investor_id, created_at DESC);

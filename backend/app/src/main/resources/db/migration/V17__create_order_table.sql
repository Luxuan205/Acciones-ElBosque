CREATE TABLE IF NOT EXISTS market_order (
    id               BIGSERIAL PRIMARY KEY,
    investor_id      BIGINT NOT NULL REFERENCES investor(id),
    order_type       VARCHAR(20) NOT NULL,
    status           VARCHAR(20) NOT NULL,
    symbol           VARCHAR(20) NOT NULL,
    quantity         INT NOT NULL CHECK (quantity > 0),
    estimated_price  DECIMAL(18,2) NOT NULL,
    execution_price  DECIMAL(18,2) NULL,
    commission       DECIMAL(18,2) NOT NULL,
    total_estimated  DECIMAL(18,2) NOT NULL,
    limit_price      DECIMAL(18,2) NULL,
    expires_at       TIMESTAMP NULL,
    rejection_reason VARCHAR(200) NULL,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS order_investor_idx ON market_order(investor_id, created_at DESC);
CREATE INDEX IF NOT EXISTS order_status_idx ON market_order(status, symbol);

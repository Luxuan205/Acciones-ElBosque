CREATE TABLE position (
    id                 BIGSERIAL PRIMARY KEY,
    investor_id        BIGINT NOT NULL REFERENCES investor(id),
    symbol             VARCHAR(20) NOT NULL,
    current_quantity   INT NOT NULL DEFAULT 0 CHECK (current_quantity >= 0),
    avg_purchase_price DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    cash_balance       DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    created_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_position_investor_symbol UNIQUE (investor_id, symbol)
);
CREATE INDEX position_investor_idx ON position(investor_id);

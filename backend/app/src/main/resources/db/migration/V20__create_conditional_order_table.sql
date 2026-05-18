CREATE TABLE IF NOT EXISTS conditional_order (
    id BIGSERIAL PRIMARY KEY,
    investor_id BIGINT NOT NULL REFERENCES investor(id),
    type VARCHAR(20) NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    trigger_price DECIMAL(18,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    oco_partner_id BIGINT NULL REFERENCES conditional_order(id),
    triggered_order_id BIGINT NULL REFERENCES market_order(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS cond_order_investor_idx ON conditional_order(investor_id, status);
CREATE INDEX IF NOT EXISTS cond_order_symbol_status_idx ON conditional_order(symbol, status);

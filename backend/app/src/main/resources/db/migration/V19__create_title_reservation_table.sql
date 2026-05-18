CREATE TABLE IF NOT EXISTS title_reservation (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE REFERENCES market_order(id) ON DELETE CASCADE,
    investor_id BIGINT NOT NULL REFERENCES investor(id),
    symbol VARCHAR(20) NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    released BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    released_at TIMESTAMP NULL
);
CREATE INDEX IF NOT EXISTS title_res_investor_symbol_idx ON title_reservation(investor_id, symbol, released);

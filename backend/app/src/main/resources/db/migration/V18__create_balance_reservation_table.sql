CREATE TABLE IF NOT EXISTS balance_reservation (
    id          BIGSERIAL PRIMARY KEY,
    order_id    BIGINT NOT NULL UNIQUE REFERENCES market_order(id) ON DELETE CASCADE,
    investor_id BIGINT NOT NULL REFERENCES investor(id),
    amount      DECIMAL(18,2) NOT NULL,
    released    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    released_at TIMESTAMP NULL
);
CREATE INDEX IF NOT EXISTS balance_res_investor_idx ON balance_reservation(investor_id, released);

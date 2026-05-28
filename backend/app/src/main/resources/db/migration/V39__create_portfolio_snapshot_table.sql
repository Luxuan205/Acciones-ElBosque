CREATE TABLE portfolio_snapshot (
    id             BIGSERIAL PRIMARY KEY,
    investor_id    BIGINT NOT NULL REFERENCES investor(id),
    snapshot_date  DATE NOT NULL,
    total_value    DECIMAL(18,2) NOT NULL,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_snapshot_investor_date UNIQUE (investor_id, snapshot_date)
);
CREATE INDEX snapshot_investor_date_idx ON portfolio_snapshot(investor_id, snapshot_date DESC);

CREATE TABLE IF NOT EXISTS subscription_event (
    id BIGSERIAL PRIMARY KEY,
    investor_id BIGINT NOT NULL REFERENCES investor(id) ON DELETE CASCADE,
    event_type VARCHAR(30) NOT NULL,
    previous_type VARCHAR(20) NOT NULL,
    new_type VARCHAR(20) NOT NULL,
    expires_at TIMESTAMP NULL,
    triggered_by VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS sub_event_investor_idx ON subscription_event(investor_id, created_at DESC);

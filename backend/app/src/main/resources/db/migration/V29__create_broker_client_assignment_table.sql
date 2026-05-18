CREATE TABLE broker_client_assignment (
    id             BIGSERIAL PRIMARY KEY,
    broker_id      BIGINT NOT NULL REFERENCES investor(id),
    investor_id    BIGINT NOT NULL REFERENCES investor(id),
    assigned_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    active         BOOLEAN NOT NULL DEFAULT TRUE,
    deactivated_at TIMESTAMP NULL,
    CONSTRAINT uq_broker_client UNIQUE (broker_id, investor_id)
);
CREATE INDEX idx_bca_broker_active ON broker_client_assignment(broker_id, active);
CREATE INDEX idx_bca_investor ON broker_client_assignment(investor_id);

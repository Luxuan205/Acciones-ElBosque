CREATE TABLE audit_event (
    id             BIGSERIAL PRIMARY KEY,
    event_type     VARCHAR(50) NOT NULL,
    investor_id    BIGINT NULL REFERENCES investor(id),
    performed_by   BIGINT NULL REFERENCES investor(id),
    reference_type VARCHAR(50) NULL,
    reference_id   BIGINT NULL,
    detail         TEXT NULL,
    result         VARCHAR(10) NOT NULL DEFAULT 'SUCCESS',
    ip_address     VARCHAR(45) NULL,
    archived       BOOLEAN NOT NULL DEFAULT FALSE,
    occurred_at    TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX audit_investor_idx ON audit_event(investor_id, occurred_at DESC);
CREATE INDEX audit_type_idx ON audit_event(event_type, occurred_at DESC);
CREATE INDEX audit_performed_by_idx ON audit_event(performed_by, occurred_at DESC);

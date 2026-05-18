CREATE TABLE price_alert (
    id              BIGSERIAL PRIMARY KEY,
    investor_id     BIGINT NOT NULL REFERENCES investor(id),
    symbol          VARCHAR(20) NOT NULL,
    alert_type      VARCHAR(20) NOT NULL CHECK (alert_type IN ('ABSOLUTE','PERCENTAGE')),
    threshold       NUMERIC(18,4) NOT NULL,
    reference_price NUMERIC(18,4) NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    triggered_at    TIMESTAMP NULL
);
CREATE INDEX price_alert_investor_idx ON price_alert(investor_id, status);

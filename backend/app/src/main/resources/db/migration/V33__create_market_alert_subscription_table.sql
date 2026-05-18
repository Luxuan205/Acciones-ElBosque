CREATE TABLE market_alert_subscription (
    id           BIGSERIAL PRIMARY KEY,
    investor_id  BIGINT NOT NULL REFERENCES investor(id),
    alert_type   VARCHAR(30) NOT NULL,
    symbol       VARCHAR(20) NULL,
    threshold    DECIMAL(18,4) NULL,
    active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX market_alert_investor_idx ON market_alert_subscription(investor_id, active);

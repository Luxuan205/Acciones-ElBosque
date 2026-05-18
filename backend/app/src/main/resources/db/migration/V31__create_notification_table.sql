CREATE TABLE notification (
    id           BIGSERIAL PRIMARY KEY,
    investor_id  BIGINT NOT NULL REFERENCES investor(id),
    event_type   VARCHAR(40) NOT NULL,
    channel      VARCHAR(20) NOT NULL,
    subject      VARCHAR(200) NOT NULL,
    body         TEXT NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reference_id BIGINT NULL,
    archived     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX notif_investor_idx ON notification(investor_id, created_at DESC);
CREATE INDEX notif_status_idx ON notification(status, created_at);

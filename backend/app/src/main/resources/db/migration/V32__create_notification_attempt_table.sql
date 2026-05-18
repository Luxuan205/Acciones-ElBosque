CREATE TABLE notification_attempt (
    id              BIGSERIAL PRIMARY KEY,
    notification_id BIGINT NOT NULL REFERENCES notification(id) ON DELETE CASCADE,
    attempt_number  INT NOT NULL,
    status          VARCHAR(20) NOT NULL,
    error_message   TEXT NULL,
    attempted_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

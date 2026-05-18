CREATE TABLE IF NOT EXISTS otp_code (
    id BIGSERIAL PRIMARY KEY,
    investor_id BIGINT NOT NULL REFERENCES investor(id) ON DELETE CASCADE,
    code VARCHAR(6) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS otp_code_investor_idx ON otp_code(investor_id, expires_at);

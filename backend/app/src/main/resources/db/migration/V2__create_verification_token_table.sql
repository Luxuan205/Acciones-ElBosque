CREATE TABLE IF NOT EXISTS verification_token (
    id          BIGSERIAL PRIMARY KEY,
    token       VARCHAR(36)  NOT NULL UNIQUE,
    investor_id BIGINT       NOT NULL REFERENCES investor(id) ON DELETE CASCADE,
    expires_at  TIMESTAMP    NOT NULL,
    used        BOOLEAN      NOT NULL DEFAULT false,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_verification_token_token       ON verification_token(token);
CREATE INDEX IF NOT EXISTS idx_verification_token_investor_id ON verification_token(investor_id);

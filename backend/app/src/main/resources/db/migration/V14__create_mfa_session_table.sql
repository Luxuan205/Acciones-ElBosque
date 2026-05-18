CREATE TABLE IF NOT EXISTS mfa_session (
    id BIGSERIAL PRIMARY KEY,
    session_token VARCHAR(36) NOT NULL UNIQUE,
    investor_id BIGINT NOT NULL REFERENCES investor(id) ON DELETE CASCADE,
    expires_at TIMESTAMP NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS mfa_session_token_idx ON mfa_session(session_token);

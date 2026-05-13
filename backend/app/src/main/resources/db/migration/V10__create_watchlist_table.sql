CREATE TABLE watchlist (
    id          UUID   PRIMARY KEY DEFAULT gen_random_uuid(),
    investor_id BIGINT NOT NULL UNIQUE REFERENCES investor(id),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

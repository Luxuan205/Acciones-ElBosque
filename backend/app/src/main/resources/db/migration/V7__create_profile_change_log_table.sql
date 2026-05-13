CREATE TABLE IF NOT EXISTS profile_change_log (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    investor_id BIGINT NOT NULL REFERENCES investor(id) ON DELETE CASCADE,
    field_name  VARCHAR(100) NOT NULL,
    old_value   TEXT,
    new_value   TEXT,
    changed_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_profile_change_log_investor_id ON profile_change_log(investor_id);

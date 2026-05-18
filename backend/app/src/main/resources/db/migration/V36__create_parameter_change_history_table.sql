CREATE TABLE parameter_change_history (
    id BIGSERIAL PRIMARY KEY,
    parameter_key VARCHAR(100) NOT NULL REFERENCES global_parameter(key),
    previous_value VARCHAR(500) NOT NULL,
    new_value VARCHAR(500) NOT NULL,
    changed_by BIGINT NOT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    reason VARCHAR(300) NULL
);

CREATE INDEX param_history_key_idx ON parameter_change_history(parameter_key, changed_at DESC);

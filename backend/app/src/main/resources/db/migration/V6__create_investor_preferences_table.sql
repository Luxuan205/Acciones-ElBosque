CREATE TABLE IF NOT EXISTS investor_preferences (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    investor_id     BIGINT NOT NULL UNIQUE REFERENCES investor(id) ON DELETE CASCADE,
    notif_channel   VARCHAR(10) NOT NULL DEFAULT 'EMAIL'
                        CHECK (notif_channel IN ('EMAIL', 'SMS', 'NONE')),
    language        VARCHAR(5) NOT NULL DEFAULT 'es'
                        CHECK (language IN ('es', 'en')),
    updated_at      TIMESTAMP DEFAULT NOW()
);

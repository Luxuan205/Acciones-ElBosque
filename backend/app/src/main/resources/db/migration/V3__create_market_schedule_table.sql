CREATE TABLE IF NOT EXISTS market_schedule (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    open_time    TIME    NOT NULL,
    close_time   TIME    NOT NULL,
    working_days INTEGER NOT NULL CHECK (working_days BETWEEN 1 AND 127),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by   UUID
);
INSERT INTO market_schedule (id, open_time, close_time, working_days)
VALUES (gen_random_uuid(), '09:00:00', '15:30:00', 31)
ON CONFLICT DO NOTHING;

CREATE TABLE IF NOT EXISTS market_holiday (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    date        DATE         NOT NULL UNIQUE,
    description VARCHAR(200) NOT NULL,
    type        VARCHAR(20)  NOT NULL DEFAULT 'NATIONAL' CHECK (type IN ('NATIONAL','REGIONAL','SPECIAL')),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_market_holiday_date ON market_holiday(date);

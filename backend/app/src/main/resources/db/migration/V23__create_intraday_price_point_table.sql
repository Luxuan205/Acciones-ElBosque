CREATE TABLE intraday_price_point (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    symbol    VARCHAR(20) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    price     DECIMAL(18,2) NOT NULL,
    volume    BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_intraday_symbol_ts UNIQUE (symbol, timestamp)
);
CREATE INDEX idx_intraday_symbol_ts ON intraday_price_point(symbol, timestamp DESC);

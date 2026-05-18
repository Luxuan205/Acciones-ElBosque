CREATE TABLE transaction (
    id               BIGSERIAL PRIMARY KEY,
    investor_id      BIGINT NOT NULL REFERENCES investor(id),
    order_id         BIGINT NULL,
    transaction_type VARCHAR(10) NOT NULL CHECK (transaction_type IN ('BUY','SELL')),
    symbol           VARCHAR(20) NOT NULL,
    quantity         INT NOT NULL,
    execution_price  DECIMAL(18,2) NOT NULL,
    commission       DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    gross_amount     DECIMAL(18,2) NOT NULL,
    net_amount       DECIMAL(18,2) NOT NULL,
    realized_gain    DECIMAL(18,2) NULL,
    avg_price_at_time DECIMAL(18,2) NULL,
    executed_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX transaction_investor_idx ON transaction(investor_id, executed_at DESC);
CREATE INDEX transaction_symbol_idx ON transaction(symbol, executed_at DESC);

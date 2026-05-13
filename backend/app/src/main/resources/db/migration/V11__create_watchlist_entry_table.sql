CREATE TABLE watchlist_entry (
    id             UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    watchlist_id   UUID          NOT NULL REFERENCES watchlist(id) ON DELETE CASCADE,
    symbol         VARCHAR(20)   NOT NULL,
    added_at       TIMESTAMP     NOT NULL DEFAULT NOW(),
    price_at_added DECIMAL(18,2) NOT NULL,

    CONSTRAINT uq_watchlist_symbol UNIQUE (watchlist_id, symbol)
);

CREATE INDEX idx_watchlist_entry_watchlist ON watchlist_entry(watchlist_id);

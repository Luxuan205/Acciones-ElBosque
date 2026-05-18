ALTER TABLE market_order ADD COLUMN IF NOT EXISTS broker_id BIGINT NULL;
CREATE INDEX IF NOT EXISTS idx_order_broker ON market_order(broker_id) WHERE broker_id IS NOT NULL;

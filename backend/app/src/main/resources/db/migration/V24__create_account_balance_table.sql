CREATE TABLE account_balance (
    id           BIGSERIAL PRIMARY KEY,
    investor_id  BIGINT UNIQUE NOT NULL REFERENCES investor(id),
    total_balance DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    currency     VARCHAR(3) NOT NULL DEFAULT 'COP',
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

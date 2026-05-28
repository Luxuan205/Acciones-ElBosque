-- Reset investor initial balance to 5,000,000
UPDATE investor SET available_balance = 5000000.00;

-- Seed or reset account_balance for all investors to 5,000,000
INSERT INTO account_balance (investor_id, total_balance, currency, created_at, updated_at)
SELECT id, 5000000.00, 'COP', NOW(), NOW()
FROM investor
ON CONFLICT (investor_id) DO UPDATE
    SET total_balance = 5000000.00,
        updated_at    = NOW();

ALTER TABLE investor
    ADD COLUMN subscription_type VARCHAR(20) NOT NULL DEFAULT 'STANDARD',
    ADD COLUMN subscription_expires_at TIMESTAMP NULL;

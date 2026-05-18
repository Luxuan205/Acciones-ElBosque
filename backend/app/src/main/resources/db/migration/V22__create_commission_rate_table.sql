CREATE TABLE commission_rate (
    subscription_type VARCHAR(20) PRIMARY KEY,
    rate_percent      DECIMAL(5,2) NOT NULL CHECK (rate_percent > 0)
);

INSERT INTO commission_rate (subscription_type, rate_percent) VALUES
    ('STANDARD', 1.50),
    ('PREMIUM',  0.80);

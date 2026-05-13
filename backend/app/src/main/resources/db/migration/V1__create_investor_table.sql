CREATE TABLE IF NOT EXISTS investor (
    id              BIGSERIAL PRIMARY KEY,
    full_name       VARCHAR(150)    NOT NULL,
    document_number VARCHAR(10)     NOT NULL UNIQUE,
    email           VARCHAR(255)    NOT NULL UNIQUE,
    password_hash   VARCHAR(60)     NOT NULL,
    account_status  VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_investor_email           ON investor(email);
CREATE INDEX IF NOT EXISTS idx_investor_document_number ON investor(document_number);

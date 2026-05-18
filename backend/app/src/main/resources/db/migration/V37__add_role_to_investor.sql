ALTER TABLE investor ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'INVESTOR';
UPDATE investor SET role = 'ADMIN' WHERE email = 'admin@accioneselbosque.com';
CREATE INDEX IF NOT EXISTS investor_role_status_idx ON investor(role, account_status);

CREATE TABLE global_parameter (
    key VARCHAR(100) PRIMARY KEY,
    value VARCHAR(500) NOT NULL,
    data_type VARCHAR(20) NOT NULL,
    category VARCHAR(50) NOT NULL,
    description VARCHAR(300) NOT NULL,
    min_value VARCHAR(100) NULL,
    max_value VARCHAR(100) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

INSERT INTO global_parameter (key, value, data_type, category, description, min_value, max_value) VALUES
('max_login_attempts', '5', 'INTEGER', 'Seguridad', 'Número máximo de intentos de login fallidos antes de bloquear la cuenta', '1', '20'),
('token_ttl_minutes', '60', 'INTEGER', 'Seguridad', 'Tiempo de vida en minutos del token de verificación', '5', '1440'),
('otp_ttl_minutes', '10', 'INTEGER', 'Seguridad', 'Tiempo de vida en minutos del código OTP', '1', '60'),
('premium_duration_days', '30', 'INTEGER', 'Suscripciones', 'Duración en días de la suscripción premium', '1', '365'),
('audit_retention_years', '5', 'INTEGER', 'Auditoría', 'Años de retención de eventos de auditoría', '1', '10'),
('limit_order_default_ttl_days', '90', 'INTEGER', 'Trading', 'TTL por defecto en días de las órdenes límite', '1', '365'),
('max_price_alerts_per_user', '10', 'INTEGER', 'Trading', 'Número máximo de alertas de precio por usuario', '1', '50');

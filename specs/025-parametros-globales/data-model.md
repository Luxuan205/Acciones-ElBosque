# Data Model: AB-40 — Gestión de Parámetros Globales del Sistema

## Nuevas entidades

### GlobalParameter
```
global_parameter
├── id          BIGSERIAL     PK
├── key         VARCHAR(80)   NOT NULL UNIQUE
├── value       VARCHAR(200)  NOT NULL
├── data_type   VARCHAR(20)   NOT NULL  ('INT' | 'DECIMAL' | 'STRING' | 'BOOLEAN')
├── category    VARCHAR(30)   NOT NULL  ('AUTH' | 'SUBSCRIPTION' | 'TRADING' | 'AUDIT')
├── description VARCHAR(300)  NOT NULL
├── min_value   VARCHAR(50)   NULL      (rango mínimo como string para cualquier tipo)
├── max_value   VARCHAR(50)   NULL
├── version     BIGINT        NOT NULL DEFAULT 0  (optimistic lock)
└── updated_at  TIMESTAMP     NOT NULL DEFAULT NOW()
```

### ParameterChangeHistory
```
parameter_change_history
├── id              BIGSERIAL     PK
├── parameter_key   VARCHAR(80)   NOT NULL
├── previous_value  VARCHAR(200)  NOT NULL
├── new_value       VARCHAR(200)  NOT NULL
├── changed_by      BIGINT        NOT NULL FK → investor.id  (siempre un ADMIN)
└── changed_at      TIMESTAMP     NOT NULL DEFAULT NOW()

INDEX param_history_key_idx ON parameter_change_history(parameter_key, changed_at DESC)
```

## Flyway Migrations

### V28__create_global_parameter_table.sql
```sql
CREATE TABLE global_parameter (
    id          BIGSERIAL PRIMARY KEY,
    key         VARCHAR(80) NOT NULL UNIQUE,
    value       VARCHAR(200) NOT NULL,
    data_type   VARCHAR(20) NOT NULL,
    category    VARCHAR(30) NOT NULL,
    description VARCHAR(300) NOT NULL,
    min_value   VARCHAR(50) NULL,
    max_value   VARCHAR(50) NULL,
    version     BIGINT NOT NULL DEFAULT 0,
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Seed data
INSERT INTO global_parameter (key, value, data_type, category, description, min_value, max_value) VALUES
('auth.max_login_attempts', '5', 'INT', 'AUTH', 'Intentos fallidos antes de bloquear la cuenta', '3', '10'),
('auth.lock_duration_minutes', '30', 'INT', 'AUTH', 'Duración del bloqueo de cuenta en minutos', '5', '1440'),
('auth.otp_ttl_minutes', '5', 'INT', 'AUTH', 'Vigencia del código OTP en minutos', '2', '15'),
('auth.jwt_ttl_hours', '8', 'INT', 'AUTH', 'Vigencia del token JWT en horas', '1', '24'),
('subscription.premium_duration_days', '30', 'INT', 'SUBSCRIPTION', 'Duración de la suscripción PREMIUM en días', '7', '365'),
('trading.commission_rate_pct', '0.3', 'DECIMAL', 'TRADING', 'Tasa de comisión como porcentaje del valor bruto', '0.0', '5.0'),
('trading.max_price_alerts_per_user', '20', 'INT', 'TRADING', 'Máximo de alertas de precio activas por usuario PREMIUM', '1', '100'),
('audit.active_retention_years', '5', 'INT', 'AUDIT', 'Años de retención en el log de auditoría activo', '1', '20');
```

### V29__create_parameter_change_history_table.sql
```sql
CREATE TABLE parameter_change_history (
    id            BIGSERIAL PRIMARY KEY,
    parameter_key VARCHAR(80) NOT NULL,
    previous_value VARCHAR(200) NOT NULL,
    new_value     VARCHAR(200) NOT NULL,
    changed_by    BIGINT NOT NULL REFERENCES investor(id),
    changed_at    TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX param_history_key_idx ON parameter_change_history(parameter_key, changed_at DESC);
```

## Java DTOs

```java
record GlobalParameterDto(
    String key,
    String value,
    String dataType,
    String category,
    String description,
    String minValue,
    String maxValue
)

record UpdateParameterRequest(
    @NotBlank String value
)
```

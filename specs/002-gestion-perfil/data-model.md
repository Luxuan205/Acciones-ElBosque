# Data Model: AB-17 — Gestión de Perfil de Usuario

## Entities

### Investor (existing, extended)
```
Investor
├── id              UUID          PK
├── fullName        VARCHAR(100)  NOT NULL
├── documentNumber  VARCHAR(20)   NOT NULL UNIQUE  [READ-ONLY after registration]
├── email           VARCHAR(150)  NOT NULL UNIQUE  [READ-ONLY after registration]
├── passwordHash    VARCHAR(255)  NOT NULL
├── phone           VARCHAR(25)   NULL             [NEW — V3 migration]
├── accountStatus   VARCHAR(20)   NOT NULL  CHECK IN ('PENDING','ACTIVE','INACTIVE')
├── createdAt       TIMESTAMP     NOT NULL DEFAULT now()
└── updatedAt       TIMESTAMP     NOT NULL DEFAULT now()
```

### InvestorPreferences
```
InvestorPreferences
├── id              UUID          PK
├── investorId      UUID          NOT NULL UNIQUE  FK → investor.id
├── notifChannel    VARCHAR(10)   NOT NULL DEFAULT 'EMAIL'  CHECK IN ('EMAIL','SMS','NONE')
├── language        VARCHAR(5)    NOT NULL DEFAULT 'es'     CHECK IN ('es','en')
└── updatedAt       TIMESTAMP     NOT NULL DEFAULT now()
```

### ProfileChangeLog
```
ProfileChangeLog
├── id          UUID          PK
├── investorId  UUID          NOT NULL  FK → investor.id
├── fieldName   VARCHAR(50)   NOT NULL
├── oldValue    VARCHAR(500)  NULL
├── newValue    VARCHAR(500)  NULL
└── changedAt   TIMESTAMP     NOT NULL DEFAULT now()
```
Note: For password changes, `oldValue` and `newValue` are stored as `[REDACTED]`.

## Relationships
- `Investor` 1-to-1 `InvestorPreferences` (created on-demand)
- `Investor` 1-to-many `ProfileChangeLog`

## Validation Rules
- `fullName`: 2–100 chars, not blank
- `phone`: optional; if provided matches `^\+?[0-9\s\-]{7,20}$`
- `notifChannel`: must be one of EMAIL, SMS, NONE
- `language`: must be one of es, en
- `currentPassword` (ChangePasswordRequest): must match stored BCrypt hash
- `newPassword`: 8–72 chars, same rules as registration
- `confirmNewPassword`: must equal `newPassword`

## Flyway Migrations

### V3__add_phone_to_investor.sql
```sql
ALTER TABLE investor
    ADD COLUMN phone VARCHAR(25);
```

### V4__create_investor_preferences_table.sql
```sql
CREATE TABLE investor_preferences (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    investor_id     UUID        NOT NULL UNIQUE REFERENCES investor(id) ON DELETE CASCADE,
    notif_channel   VARCHAR(10) NOT NULL DEFAULT 'EMAIL'
                                CHECK (notif_channel IN ('EMAIL','SMS','NONE')),
    language        VARCHAR(5)  NOT NULL DEFAULT 'es'
                                CHECK (language IN ('es','en')),
    updated_at      TIMESTAMP   NOT NULL DEFAULT NOW()
);
```

### V5__create_profile_change_log_table.sql
```sql
CREATE TABLE profile_change_log (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    investor_id UUID        NOT NULL REFERENCES investor(id) ON DELETE CASCADE,
    field_name  VARCHAR(50) NOT NULL,
    old_value   VARCHAR(500),
    new_value   VARCHAR(500),
    changed_at  TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_profile_change_log_investor ON profile_change_log(investor_id);
```

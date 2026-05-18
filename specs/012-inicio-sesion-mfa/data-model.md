# Data Model: AB-16 — Inicio de Sesión con MFA

## Cambios a entidades existentes

### Investor (modificación)
```
Investor  ← tabla existente
├── failed_attempts   INT       NOT NULL DEFAULT 0    ← AÑADIR
├── locked_until      TIMESTAMP NULL                  ← AÑADIR
└── totp_secret       VARCHAR(64) NULL                ← AÑADIR (para canal TOTP)
```

### AccountStatus (enum — añadir valores)
```
PENDING   → cuenta registrada, correo no verificado
ACTIVE    → cuenta operativa
INACTIVE  → desactivación voluntaria
SUSPENDED → suspendida por administrador (AB-41)    ← AÑADIR
BLOCKED   → bloqueada automáticamente por fuerza bruta ← AÑADIR
```

## Nuevas entidades

### OtpCode
```
otp_code
├── id          BIGSERIAL     PK
├── investor_id BIGINT        NOT NULL FK → investor.id ON DELETE CASCADE
├── code        VARCHAR(6)    NOT NULL
├── channel     VARCHAR(20)   NOT NULL  ('EMAIL' | 'TOTP')
├── expires_at  TIMESTAMP     NOT NULL
├── used_at     TIMESTAMP     NULL      (NULL = no usado)
└── created_at  TIMESTAMP     NOT NULL DEFAULT NOW()

INDEX otp_code_investor_idx ON otp_code(investor_id, expires_at)
```

### MfaSession
```
mfa_session
├── id              BIGSERIAL     PK
├── session_token   VARCHAR(36)   NOT NULL UNIQUE  (UUID)
├── investor_id     BIGINT        NOT NULL FK → investor.id ON DELETE CASCADE
├── expires_at      TIMESTAMP     NOT NULL  (created_at + 5 min)
├── completed       BOOLEAN       NOT NULL DEFAULT FALSE
└── created_at      TIMESTAMP     NOT NULL DEFAULT NOW()

INDEX mfa_session_token_idx ON mfa_session(session_token)
```

## Flyway Migrations

### V12__add_investor_login_fields.sql
```sql
ALTER TABLE investor
    ADD COLUMN failed_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN locked_until TIMESTAMP NULL,
    ADD COLUMN totp_secret VARCHAR(64) NULL;
```

### V13__create_otp_code_table.sql
```sql
CREATE TABLE otp_code (
    id          BIGSERIAL PRIMARY KEY,
    investor_id BIGINT NOT NULL REFERENCES investor(id) ON DELETE CASCADE,
    code        VARCHAR(6) NOT NULL,
    channel     VARCHAR(20) NOT NULL,
    expires_at  TIMESTAMP NOT NULL,
    used_at     TIMESTAMP NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX otp_code_investor_idx ON otp_code(investor_id, expires_at);
```

### V14__create_mfa_session_table.sql
```sql
CREATE TABLE mfa_session (
    id            BIGSERIAL PRIMARY KEY,
    session_token VARCHAR(36) NOT NULL UNIQUE,
    investor_id   BIGINT NOT NULL REFERENCES investor(id) ON DELETE CASCADE,
    expires_at    TIMESTAMP NOT NULL,
    completed     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX mfa_session_token_idx ON mfa_session(session_token);
```

## Java DTOs

```java
record LoginRequest(
    @NotBlank @Email String email,
    @NotBlank String password
)

record LoginResponse(
    String sessionToken,  // token pre-auth para enviar el OTP
    String channel        // 'EMAIL' | 'TOTP' — canal por donde se envió el OTP
)

record MfaVerifyRequest(
    @NotBlank String sessionToken,
    @NotBlank @Size(min=6, max=6) String otpCode
)

record MfaVerifyResponse(
    String accessToken,
    String role
)
```

## Business Rules
- `failedAttempts` se incrementa en cada intento fallido (primer o segundo factor)
- `failedAttempts` se resetea a 0 en login exitoso
- Si `failedAttempts >= maxAttempts` (default 5), se pone `lockedUntil = NOW() + 30min` y `accountStatus = BLOCKED`
- OTP expira en 5 minutos; `usedAt` se marca al validarlo para evitar replay
- `MfaSession` expira en 5 minutos; el JWT solo se emite cuando `completed = TRUE`
- Mensajes de error genéricos: nunca revelar si el email existe

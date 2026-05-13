# Data Model: AB-15 — Registro de Inversionista

**Phase 1 output** | **Date**: 2026-05-10 | **Schema**: `auth_db`

## Entidades JPA

### Investor

Representa la cuenta de un inversionista registrado en la plataforma.

| Campo | Tipo Java | Tipo SQL | Restricciones | Descripción |
|-------|-----------|----------|---------------|-------------|
| `id` | `Long` | `BIGSERIAL` | PK, NOT NULL | Identificador interno |
| `fullName` | `String` | `VARCHAR(150)` | NOT NULL | Nombre completo |
| `documentNumber` | `String` | `VARCHAR(10)` | NOT NULL, UNIQUE | Cédula colombiana |
| `email` | `String` | `VARCHAR(255)` | NOT NULL, UNIQUE | Correo electrónico |
| `passwordHash` | `String` | `VARCHAR(60)` | NOT NULL | Hash BCrypt (60 chars) |
| `accountStatus` | `AccountStatus` (enum) | `VARCHAR(20)` | NOT NULL, DEFAULT 'PENDING' | PENDING / ACTIVE / INACTIVE |
| `createdAt` | `LocalDateTime` | `TIMESTAMP` | NOT NULL, DEFAULT NOW() | Fecha de registro |
| `updatedAt` | `LocalDateTime` | `TIMESTAMP` | NOT NULL | Última modificación |

**Enum `AccountStatus`**: `PENDING`, `ACTIVE`, `INACTIVE`

### VerificationToken

Token único enviado por correo para verificar la cuenta del inversionista.

| Campo | Tipo Java | Tipo SQL | Restricciones | Descripción |
|-------|-----------|----------|---------------|-------------|
| `id` | `Long` | `BIGSERIAL` | PK, NOT NULL | Identificador interno |
| `token` | `String` | `VARCHAR(36)` | NOT NULL, UNIQUE | UUID v4 |
| `investor` | `Investor` | `BIGINT` (FK) | NOT NULL | Dueño del token |
| `expiresAt` | `LocalDateTime` | `TIMESTAMP` | NOT NULL | NOW() + 24h |
| `used` | `boolean` | `BOOLEAN` | NOT NULL, DEFAULT false | true tras verificación exitosa |
| `createdAt` | `LocalDateTime` | `TIMESTAMP` | NOT NULL, DEFAULT NOW() | Fecha de creación |

**Relación**: `VerificationToken` → `Investor` (Many-to-One; un inversionista puede
tener varios tokens si solicita reenvío).

---

## Migraciones Flyway

### V1__create_investor_table.sql

```sql
CREATE SCHEMA IF NOT EXISTS auth_db;

CREATE TABLE auth_db.investor (
    id              BIGSERIAL PRIMARY KEY,
    full_name       VARCHAR(150)    NOT NULL,
    document_number VARCHAR(10)     NOT NULL UNIQUE,
    email           VARCHAR(255)    NOT NULL UNIQUE,
    password_hash   VARCHAR(60)     NOT NULL,
    account_status  VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_investor_email          ON auth_db.investor(email);
CREATE INDEX idx_investor_document_number ON auth_db.investor(document_number);
```

### V2__create_verification_token_table.sql

```sql
CREATE TABLE auth_db.verification_token (
    id          BIGSERIAL PRIMARY KEY,
    token       VARCHAR(36)  NOT NULL UNIQUE,
    investor_id BIGINT       NOT NULL REFERENCES auth_db.investor(id) ON DELETE CASCADE,
    expires_at  TIMESTAMP    NOT NULL,
    used        BOOLEAN      NOT NULL DEFAULT false,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_verification_token_token       ON auth_db.verification_token(token);
CREATE INDEX idx_verification_token_investor_id ON auth_db.verification_token(investor_id);
```

---

## Diagrama de relaciones

```
auth_db.investor (1) ────< auth_db.verification_token
  id PK                       id PK
  full_name                   token UNIQUE
  document_number UNIQUE       investor_id FK → investor.id
  email UNIQUE                 expires_at
  password_hash                used
  account_status               created_at
  created_at
  updated_at
```

---

## Reglas de negocio reflejadas en el modelo

- `document_number` y `email` son UNIQUE a nivel de DB — garantía de no duplicados
  independiente de la validación de aplicación.
- `account_status` DEFAULT 'PENDING' — ningún inversionista nace activo.
- `password_hash` VARCHAR(60) — exactamente el largo del hash BCrypt de Spring Security.
- `token` VARCHAR(36) — exactamente el largo de un UUID v4 (`xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`).
- `ON DELETE CASCADE` en `verification_token.investor_id` — si se elimina un
  inversionista, sus tokens se eliminan automáticamente.

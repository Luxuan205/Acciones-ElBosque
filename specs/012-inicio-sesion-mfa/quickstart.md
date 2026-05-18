# Quickstart: AB-16 — Inicio de Sesión con MFA

**Prerequisites**
- Service running on `http://localhost:8080`
- At least one ACTIVE user account with MFA enabled (OTP via EMAIL)
- A PENDING (unverified) user account for negative-path tests
- Environment variable `$INVESTOR_TOKEN` — a valid JWT obtained after completing both MFA factors

---

## Flow 1: Happy path — login step 1 returns sessionToken

```bash
curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "investor@elbosque.edu.co", "password": "S3cur3P@ss"}' | jq .
```

Expected HTTP 200:
```json
{
  "sessionToken": "550e8400-e29b-41d4-a716-446655440000",
  "channel": "EMAIL"
}
```

> Save the `sessionToken` value — it is required for step 2 (MFA verify) and resend.

---

## Flow 2: Happy path — MFA verify step 2 returns JWT

Replace `482931` with the OTP received in the email for the session above.

```bash
curl -s -X POST http://localhost:8080/auth/mfa/verify \
  -H "Content-Type: application/json" \
  -d '{"sessionToken": "550e8400-e29b-41d4-a716-446655440000", "otpCode": "482931"}' | jq .
```

Expected HTTP 200:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "role": "INVESTOR"
}
```

> The `accessToken` is the Bearer JWT used in all authenticated endpoints. Export it: `export INVESTOR_TOKEN=<accessToken>`

---

## Flow 3: Resend OTP — rate-limit not yet exceeded

```bash
curl -s -X POST http://localhost:8080/auth/mfa/resend \
  -H "Content-Type: application/json" \
  -d '{"sessionToken": "550e8400-e29b-41d4-a716-446655440000"}' | jq .
```

Expected HTTP 200:
```json
{
  "channel": "EMAIL",
  "message": "Código reenviado exitosamente."
}
```

---

## Flow 4: Validation error — missing required field

```bash
curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "investor@elbosque.edu.co"}' | jq .
```

Expected HTTP 400:
```json
{
  "error": "VALIDATION_ERROR",
  "message": "Campos requeridos faltantes o con formato inválido"
}
```

---

## Flow 5: Invalid credentials — wrong password

```bash
curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "investor@elbosque.edu.co", "password": "WrongPassword1"}' | jq .
```

Expected HTTP 401:
```json
{
  "error": "INVALID_CREDENTIALS",
  "message": "Credenciales incorrectas o cuenta no encontrada."
}
```

> The same response is returned for an email that does not exist — the system deliberately avoids revealing whether the email is registered (FR-003).

---

## Flow 6: Account not yet verified — PENDING state

```bash
curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "pending@elbosque.edu.co", "password": "S3cur3P@ss"}' | jq .
```

Expected HTTP 403:
```json
{
  "error": "ACCOUNT_PENDING",
  "message": "Debes verificar tu correo electrónico antes de acceder."
}
```

---

## Flow 7: Account locked after repeated failed attempts

Trigger 5 consecutive failed login attempts to lock the account, then attempt with correct credentials.

```bash
# Trigger 5 failures (run this block 5 times or script it)
for i in 1 2 3 4 5; do
  curl -s -X POST http://localhost:8080/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email": "investor@elbosque.edu.co", "password": "BadPass'$i'"}' | jq .email
done

# Now attempt with correct credentials
curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "investor@elbosque.edu.co", "password": "S3cur3P@ss"}' | jq .
```

Expected HTTP 423 (after the 5th failure and on all subsequent attempts until unlocked):
```json
{
  "error": "ACCOUNT_LOCKED",
  "message": "Cuenta bloqueada temporalmente. Intente de nuevo más tarde."
}
```

---

## Flow 8: Expired or invalid OTP code

Use a valid sessionToken but submit a wrong or already-used OTP.

```bash
curl -s -X POST http://localhost:8080/auth/mfa/verify \
  -H "Content-Type: application/json" \
  -d '{"sessionToken": "550e8400-e29b-41d4-a716-446655440000", "otpCode": "000000"}' | jq .
```

Expected HTTP 401:
```json
{
  "error": "INVALID_OTP",
  "message": "Código incorrecto o expirado."
}
```

---

## SQL Verification Queries

```sql
-- Confirm login attempts are being audited (FR-008, SC-004)
SELECT user_id, result, ip_address, attempted_at
FROM login_audit
ORDER BY attempted_at DESC
LIMIT 10;

-- Check account lock status after repeated failures
SELECT id, email, status, failed_attempts, locked_until
FROM users
WHERE email = 'investor@elbosque.edu.co';

-- Inspect active pre-auth sessions
SELECT session_token, user_id, channel, created_at, expires_at
FROM mfa_sessions
WHERE expires_at > NOW()
ORDER BY created_at DESC;
```

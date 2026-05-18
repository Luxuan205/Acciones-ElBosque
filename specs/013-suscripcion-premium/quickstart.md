# Quickstart: AB-18 — Suscripción Premium

**Prerequisites**
- Service running on `http://localhost:8080`
- `$INVESTOR_TOKEN` — JWT for a user with role INVESTOR and current subscriptionType STANDARD
- `$PREMIUM_TOKEN` — JWT for a user that already has an active PREMIUM subscription
- `$ADMIN_TOKEN` — JWT for a user with role ADMIN (for negative-path role check)

---

## Flow 1: Happy path — consult status for a STANDARD user

```bash
curl -s -X GET http://localhost:8080/subscriptions/status \
  -H "Authorization: Bearer $INVESTOR_TOKEN" | jq .
```

Expected HTTP 200:
```json
{
  "subscriptionType": "STANDARD",
  "activatedAt": null,
  "expiresAt": null,
  "isActive": false,
  "daysRemaining": 0
}
```

---

## Flow 2: Happy path — activate PREMIUM subscription

```bash
curl -s -X POST http://localhost:8080/subscriptions/activate \
  -H "Authorization: Bearer $INVESTOR_TOKEN" | jq .
```

Expected HTTP 200:
```json
{
  "subscriptionType": "PREMIUM",
  "activatedAt": "2026-05-14T10:00:00Z",
  "expiresAt": "2026-06-13T10:00:00Z"
}
```

> After activation, `subscriptionType` is immediately PREMIUM and the expiry is 30 days from `activatedAt` (SC-001).

---

## Flow 3: Consult status after activation — PREMIUM active

```bash
curl -s -X GET http://localhost:8080/subscriptions/status \
  -H "Authorization: Bearer $INVESTOR_TOKEN" | jq .
```

Expected HTTP 200:
```json
{
  "subscriptionType": "PREMIUM",
  "activatedAt": "2026-05-14T10:00:00Z",
  "expiresAt": "2026-06-13T10:00:00Z",
  "isActive": true,
  "daysRemaining": 30
}
```

---

## Flow 4: Attempt to activate when already PREMIUM

Using `$PREMIUM_TOKEN` (user who already has an active subscription):

```bash
curl -s -X POST http://localhost:8080/subscriptions/activate \
  -H "Authorization: Bearer $PREMIUM_TOKEN" | jq .
```

Expected HTTP 200 (idempotent — no double charge, existing subscription info returned):
```json
{
  "subscriptionType": "PREMIUM",
  "activatedAt": "2026-04-13T09:00:00Z",
  "expiresAt": "2026-05-13T09:00:00Z",
  "message": "Ya tiene una suscripción PREMIUM activa."
}
```

---

## Flow 5: Unauthenticated request — missing JWT

```bash
curl -s -X GET http://localhost:8080/subscriptions/status | jq .
```

Expected HTTP 401:
```json
{
  "error": "UNAUTHORIZED",
  "message": "JWT ausente o inválido"
}
```

---

## Flow 6: Wrong role — ADMIN token on investor-only endpoint

```bash
curl -s -X POST http://localhost:8080/subscriptions/activate \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq .
```

Expected HTTP 403:
```json
{
  "error": "FORBIDDEN",
  "message": "Rol no es INVESTOR"
}
```

---

## Flow 7: Simulate expired subscription — status after expiry

Set a subscription's `expires_at` to the past in the DB, then consult status:

```sql
-- Manually expire a subscription for testing
UPDATE subscriptions
SET expires_at = NOW() - INTERVAL '1 day'
WHERE user_id = (SELECT id FROM users WHERE email = 'investor@elbosque.edu.co');
```

```bash
curl -s -X GET http://localhost:8080/subscriptions/status \
  -H "Authorization: Bearer $INVESTOR_TOKEN" | jq .
```

Expected HTTP 200 (degraded back to STANDARD):
```json
{
  "subscriptionType": "STANDARD",
  "activatedAt": null,
  "expiresAt": null,
  "isActive": false,
  "daysRemaining": 0
}
```

---

## SQL Verification Queries

```sql
-- Check a user's current subscription state
SELECT u.email, s.subscription_type, s.activated_at, s.expires_at, s.is_active
FROM subscriptions s
JOIN users u ON u.id = s.user_id
WHERE u.email = 'investor@elbosque.edu.co';

-- Find all PREMIUM subscriptions expiring in the next 7 days (for renewal reminders)
SELECT u.email, s.expires_at,
       EXTRACT(DAY FROM s.expires_at - NOW()) AS days_remaining
FROM subscriptions s
JOIN users u ON u.id = s.user_id
WHERE s.subscription_type = 'PREMIUM'
  AND s.expires_at BETWEEN NOW() AND NOW() + INTERVAL '7 days';

-- Verify the auto-downgrade job ran (subscriptions past expiry with type still PREMIUM)
SELECT COUNT(*) AS should_be_zero
FROM subscriptions
WHERE subscription_type = 'PREMIUM'
  AND expires_at < NOW();
```

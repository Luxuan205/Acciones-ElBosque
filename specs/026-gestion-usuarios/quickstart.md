# Quickstart: AB-41 — Gestión de Usuarios por el Administrador

**Prerequisites**
- Service running on port 8080
- JWT token: `$ADMIN_TOKEN` (role: ADMIN, user: admin@accioneselbosque.com)
- JWT token: `$INVESTOR_TOKEN` (role: INVESTOR, user: investor@test.com)
- DB seeded with users: investor ID 42 (ACTIVE/PREMIUM), investor ID 55 (BLOCKED/BASIC), investor ID 77 (ACTIVE/BASIC)

---

## Flow 1: Search users with no filters (paginated list)

```bash
curl -s -X GET "http://localhost:8080/admin/users?page=0&size=20" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq .
```

Expected 200:
```json
{
  "content": [
    {
      "id": 42,
      "fullName": "Ana López",
      "email": "ana@example.com",
      "accountStatus": "ACTIVE",
      "role": "INVESTOR",
      "subscriptionType": "PREMIUM",
      "createdAt": "2026-03-01T09:00:00Z"
    },
    {
      "id": 55,
      "fullName": "Carlos Mendoza",
      "email": "carlos.mendoza@test.com",
      "accountStatus": "BLOCKED",
      "role": "INVESTOR",
      "subscriptionType": "BASIC",
      "createdAt": "2026-04-10T11:00:00Z"
    }
  ],
  "totalElements": 157,
  "page": 0,
  "size": 20
}
```

---

## Flow 2: Search users by email

```bash
curl -s -X GET "http://localhost:8080/admin/users?email=ana%40example.com&page=0&size=20" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq .
```

Expected 200:
```json
{
  "content": [
    {
      "id": 42,
      "fullName": "Ana López",
      "email": "ana@example.com",
      "accountStatus": "ACTIVE",
      "role": "INVESTOR",
      "subscriptionType": "PREMIUM",
      "createdAt": "2026-03-01T09:00:00Z"
    }
  ],
  "totalElements": 1,
  "page": 0,
  "size": 20
}
```

DB verification:
```sql
SELECT id, full_name, email, account_status, role, subscription_type
FROM users WHERE email = 'ana@example.com';
```

---

## Flow 3: Filter users by account status and subscription type

```bash
curl -s -X GET "http://localhost:8080/admin/users?accountStatus=ACTIVE&subscriptionType=PREMIUM&page=0&size=20" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq .
```

Expected 200:
```json
{
  "content": [
    {
      "id": 42,
      "fullName": "Ana López",
      "email": "ana@example.com",
      "accountStatus": "ACTIVE",
      "role": "INVESTOR",
      "subscriptionType": "PREMIUM",
      "createdAt": "2026-03-01T09:00:00Z"
    }
  ],
  "totalElements": 183,
  "page": 0,
  "size": 20
}
```

---

## Flow 4: View the full profile of a specific user

```bash
curl -s -X GET http://localhost:8080/admin/users/42 \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq .
```

Expected 200:
```json
{
  "id": 42,
  "fullName": "Ana López",
  "email": "ana@example.com",
  "documentNumber": "1234567890",
  "phone": "+573001234567",
  "accountStatus": "ACTIVE",
  "role": "INVESTOR",
  "subscriptionType": "PREMIUM",
  "subscriptionExpiresAt": "2026-06-01T00:00:00Z",
  "createdAt": "2026-03-01T09:00:00Z",
  "recentActivity": [
    {
      "eventType": "AUTH_SUCCESS",
      "occurredAt": "2026-05-13T10:00:00Z",
      "result": "SUCCESS"
    },
    {
      "eventType": "ORDER_CREATED",
      "occurredAt": "2026-05-12T14:23:00Z",
      "result": "SUCCESS"
    }
  ]
}
```

---

## Flow 5: Suspend an active user

```bash
curl -s -X PATCH http://localhost:8080/admin/users/42/status \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"newStatus": "SUSPENDED", "reason": "Actividad sospechosa detectada"}' | jq .
```

Expected 200:
```json
{
  "id": 42,
  "previousStatus": "ACTIVE",
  "newStatus": "SUSPENDED",
  "reason": "Actividad sospechosa detectada",
  "updatedAt": "2026-05-14T15:30:00Z"
}
```

Verify the session is now invalid by using the investor's token:
```bash
curl -s -X GET "http://localhost:8080/portfolio/positions" \
  -H "Authorization: Bearer $INVESTOR_TOKEN" | jq .
```

Expected 401 or 403 (suspended user cannot access protected resources):
```json
{
  "error": "ACCOUNT_SUSPENDED",
  "message": "Tu cuenta ha sido suspendida. Contacta al soporte."
}
```

DB verification:
```sql
SELECT id, account_status, suspension_reason, updated_at
FROM users WHERE id = 42;
```

---

## Flow 6: Unblock a locked account

```bash
curl -s -X PATCH http://localhost:8080/admin/users/55/status \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"newStatus": "ACTIVE", "reason": "Identidad verificada por soporte"}' | jq .
```

Expected 200:
```json
{
  "id": 55,
  "previousStatus": "BLOCKED",
  "newStatus": "ACTIVE",
  "reason": "Identidad verificada por soporte",
  "updatedAt": "2026-05-14T15:35:00Z"
}
```

---

## Flow 7: Prevent suspending the only active administrator

```bash
# Attempt to suspend the only admin (assuming only one ADMIN exists in the system)
curl -s -X PATCH http://localhost:8080/admin/users/1/status \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"newStatus": "SUSPENDED", "reason": "Test"}' | jq .
```

Expected 409:
```json
{
  "error": "LAST_ADMIN",
  "message": "No se puede suspender al único administrador activo."
}
```

---

## Flow 8: Promote a user from INVESTOR to BROKER

```bash
curl -s -X PATCH http://localhost:8080/admin/users/77/role \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"newRole": "BROKER", "reason": "Asignado como broker regional Bogotá"}' | jq .
```

Expected 200:
```json
{
  "id": 77,
  "previousRole": "INVESTOR",
  "newRole": "BROKER",
  "effectiveOnNextLogin": true
}
```

DB verification:
```sql
SELECT id, role, updated_at FROM users WHERE id = 77;
```

---

## Flow 9: Promote a user to ADMIN (requires confirmation token)

```bash
curl -s -X PATCH http://localhost:8080/admin/users/77/role \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"newRole": "ADMIN", "reason": "Designado administrador regional", "adminConfirmation": "CONFIRM_ADMIN_ROLE"}' | jq .
```

Expected 200:
```json
{
  "id": 77,
  "previousRole": "BROKER",
  "newRole": "ADMIN",
  "effectiveOnNextLogin": true
}
```

Attempt the same request without the confirmation field:
```bash
curl -s -X PATCH http://localhost:8080/admin/users/77/role \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"newRole": "ADMIN", "reason": "Sin confirmacion"}' | jq .
```

Expected 400:
```json
{
  "error": "CONFIRMATION_REQUIRED",
  "message": "La asignación del rol ADMIN requiere el campo 'adminConfirmation' con valor 'CONFIRM_ADMIN_ROLE'."
}
```

---

## Flow 10: Trigger a password reset for a user

```bash
curl -s -X POST http://localhost:8080/admin/users/42/reset-password \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq .
```

Expected 200:
```json
{
  "message": "Enlace de restablecimiento enviado al correo del usuario."
}
```

Verify the audit log entry was created:
```sql
SELECT event_type, performed_by, reference_type, reference_id, result, occurred_at
FROM audit_events
WHERE event_type = 'PASSWORD_RESET_REQUESTED'
  AND reference_id = 42
ORDER BY occurred_at DESC
LIMIT 1;
```

---

## Flow 11: Reject access from a non-ADMIN role

```bash
curl -s -X GET "http://localhost:8080/admin/users?page=0&size=20" \
  -H "Authorization: Bearer $INVESTOR_TOKEN" | jq .
```

Expected 403:
```json
{
  "error": "FORBIDDEN",
  "message": "Acceso denegado. Se requiere rol ADMIN."
}
```

# Quickstart: AB-17 — Gestión de Perfil de Usuario

**Prerequisites**
- auth-security-service running on port 8081
- Valid JWT for an ACTIVE investor (obtained via POST /auth/login)
- Replace `$TOKEN` with your Bearer token and `$INV_ID` with the investor UUID

---

## Flow 1: View profile

```bash
curl -s -X GET http://localhost:8080/auth/profile \
  -H "Authorization: Bearer $TOKEN" | jq .
```

Expected: 200 with `fullName`, `email`, `documentNumber`, `phone`, `accountStatus`.

---

## Flow 2: Update personal data (name and phone)

```bash
curl -s -X PUT http://localhost:8080/auth/profile/personal \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "María García López",
    "phone": "+57 310 123 4567"
  }' | jq .
```

Expected: 200 with updated `fullName` and `phone`.

Verify audit log:
```sql
SELECT field_name, old_value, new_value, changed_at
FROM profile_change_log
WHERE investor_id = '<INV_ID>'
ORDER BY changed_at DESC;
```

---

## Flow 3: Change password

```bash
curl -s -X PUT http://localhost:8080/auth/change-password \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "currentPassword": "CurrentPass123!",
    "newPassword": "NewSecurePass456!",
    "confirmNewPassword": "NewSecurePass456!"
  }' | jq .
```

Expected: 200 `{"message": "Password changed successfully"}`.

Test wrong current password:
```bash
curl -s -X PUT http://localhost:8080/auth/change-password \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "currentPassword": "WrongPassword",
    "newPassword": "NewSecurePass456!",
    "confirmNewPassword": "NewSecurePass456!"
  }' | jq .
```

Expected: 401 `INVALID_CURRENT_PASSWORD`.

Verify password changed (new password redacted in log):
```sql
SELECT field_name, old_value, new_value, changed_at
FROM profile_change_log
WHERE investor_id = '<INV_ID>' AND field_name = 'password';
-- old_value and new_value should both be '[REDACTED]'
```

---

## Flow 4: Update preferences

```bash
curl -s -X PUT http://localhost:8080/auth/preferences \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "notifChannel": "SMS",
    "language": "en"
  }' | jq .
```

Expected: 200 with `"notifChannel": "SMS"` and `"language": "en"`.

Test invalid enum:
```bash
curl -s -X PUT http://localhost:8080/auth/preferences \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"notifChannel": "WHATSAPP"}' | jq .
```

Expected: 400 `VALIDATION_ERROR`.

---

## Flow 5: Read-only fields (email, documentNumber)

Attempt to send email in the update request:
```bash
curl -s -X PUT http://localhost:8080/auth/profile/personal \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Test",
    "email": "hacker@evil.com"
  }' | jq .
```

Expected: 200 but email is unchanged (field is ignored). Verify:
```sql
SELECT email FROM investor WHERE id = '<INV_ID>';
-- Must be the original email
```

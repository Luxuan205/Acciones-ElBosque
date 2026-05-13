# API Contract: Profile Management (AB-17)

**Base URL**: `/auth`  
**Auth**: Bearer JWT (role: INVESTOR)  
**Module**: `auth-security-service`

---

## GET /auth/profile

Returns the authenticated investor's profile data.

**Response 200**
```json
{
  "investorId": "uuid",
  "fullName": "María García López",
  "email": "maria@example.com",
  "documentNumber": "1098765432",
  "phone": "+57 310 123 4567",
  "accountStatus": "ACTIVE",
  "createdAt": "2026-01-15T10:30:00Z"
}
```

Note: `email` and `documentNumber` are read-only (shown but cannot be updated).

---

## PUT /auth/profile/personal

Updates editable personal data (name and/or phone).

**Request**
```json
{
  "fullName": "María García López",
  "phone": "+57 310 123 4567"
}
```

Both fields are optional individually; at least one must be provided.

**Validations**
- `fullName`: 2–100 characters if provided
- `phone`: matches `^\+?[0-9\s\-]{7,20}$` if provided; null clears the field

**Response 200**
```json
{
  "investorId": "uuid",
  "fullName": "María García López",
  "phone": "+57 310 123 4567",
  "updatedAt": "2026-05-10T14:22:00Z"
}
```

**Response 400** — Validation error
```json
{
  "error": "VALIDATION_ERROR",
  "message": "fullName must be between 2 and 100 characters"
}
```

---

## PUT /auth/change-password

Changes the investor's password after verifying the current one.

**Request**
```json
{
  "currentPassword": "OldPassword123!",
  "newPassword": "NewPassword456!",
  "confirmNewPassword": "NewPassword456!"
}
```

**Validations**
- `currentPassword`: must match stored BCrypt hash
- `newPassword`: 8–72 characters
- `confirmNewPassword`: must equal `newPassword`

**Response 200**
```json
{
  "message": "Password changed successfully"
}
```

**Response 400** — Passwords do not match
```json
{
  "error": "VALIDATION_ERROR",
  "message": "confirmNewPassword does not match newPassword"
}
```

**Response 401** — Current password incorrect
```json
{
  "error": "INVALID_CURRENT_PASSWORD",
  "message": "Current password is incorrect"
}
```

---

## GET /auth/preferences

Returns the investor's notification and language preferences.

**Response 200**
```json
{
  "notifChannel": "EMAIL",
  "language": "es",
  "updatedAt": "2026-05-01T09:00:00Z"
}
```

Default values if preferences have never been set: `notifChannel = EMAIL`, `language = es`.

---

## PUT /auth/preferences

Updates notification and/or language preferences.

**Request**
```json
{
  "notifChannel": "SMS",
  "language": "en"
}
```

**Validations**
- `notifChannel`: one of `EMAIL`, `SMS`, `NONE`
- `language`: one of `es`, `en`

**Response 200**
```json
{
  "notifChannel": "SMS",
  "language": "en",
  "updatedAt": "2026-05-10T14:30:00Z"
}
```

**Response 400** — Invalid enum value
```json
{
  "error": "VALIDATION_ERROR",
  "message": "notifChannel must be one of: EMAIL, SMS, NONE"
}
```

---

## Common Error Responses

| Status | Error Code          | Description                          |
|--------|---------------------|--------------------------------------|
| 401    | UNAUTHORIZED        | Missing or invalid JWT               |
| 403    | FORBIDDEN           | JWT valid but role is not INVESTOR   |

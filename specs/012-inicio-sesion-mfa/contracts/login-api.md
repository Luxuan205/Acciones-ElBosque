# API Contract: Inicio de Sesión con MFA (AB-16)

**Base URL**: `/auth`
**Auth**: Sin autenticación requerida en los endpoints de login
**Module**: `auth`

---

## POST /auth/login

Primer factor de autenticación: valida email y contraseña.

**Request**
```json
{ "email": "investor@example.com", "password": "S3cur3P@ss" }
```

**Response 200 — Credenciales correctas, MFA requerido**
```json
{ "sessionToken": "550e8400-e29b-41d4-a716-446655440000", "channel": "EMAIL" }
```

**Response 401 — Credenciales incorrectas o cuenta no encontrada**
```json
{ "error": "INVALID_CREDENTIALS", "message": "Credenciales incorrectas o cuenta no encontrada." }
```

**Response 403 — Cuenta PENDING**
```json
{ "error": "ACCOUNT_PENDING", "message": "Debes verificar tu correo electrónico antes de acceder." }
```

**Response 423 — Cuenta bloqueada temporalmente**
```json
{ "error": "ACCOUNT_LOCKED", "message": "Cuenta bloqueada temporalmente. Intente de nuevo más tarde." }
```

---

## POST /auth/mfa/verify

Segundo factor: valida el OTP enviado al canal configurado.

**Request**
```json
{ "sessionToken": "550e8400-e29b-41d4-a716-446655440000", "otpCode": "482931" }
```

**Response 200 — Autenticación completa**
```json
{ "accessToken": "eyJhbGci...", "role": "INVESTOR" }
```

**Response 401 — OTP incorrecto o expirado**
```json
{ "error": "INVALID_OTP", "message": "Código incorrecto o expirado." }
```

**Response 401 — Sesión de pre-autenticación expirada**
```json
{ "error": "SESSION_EXPIRED", "message": "Sesión expirada. Por favor inicie sesión nuevamente." }
```

---

## POST /auth/mfa/resend

Reenvía el OTP al canal configurado del usuario.

**Request**
```json
{ "sessionToken": "550e8400-e29b-41d4-a716-446655440000" }
```

**Response 200**
```json
{ "channel": "EMAIL", "message": "Código reenviado exitosamente." }
```

**Response 429 — Demasiadas solicitudes de reenvío**
```json
{ "error": "RESEND_LIMIT", "message": "Demasiadas solicitudes. Espere antes de solicitar otro código." }
```

---

## Common Error Responses

| Status | Error Code         | Description                                          |
|--------|--------------------|------------------------------------------------------|
| 400    | VALIDATION_ERROR   | Campos requeridos faltantes o con formato inválido   |
| 401    | INVALID_CREDENTIALS| Email/contraseña incorrectos (genérico)              |
| 401    | INVALID_OTP        | Código OTP incorrecto o expirado                     |
| 401    | SESSION_EXPIRED    | Sesión de pre-autenticación vencida                  |
| 403    | ACCOUNT_PENDING    | Cuenta no verificada                                 |
| 403    | ACCOUNT_SUSPENDED  | Cuenta suspendida por administrador                  |
| 423    | ACCOUNT_LOCKED     | Cuenta bloqueada por intentos fallidos               |
| 429    | RESEND_LIMIT       | Límite de reenvíos de OTP excedido                   |

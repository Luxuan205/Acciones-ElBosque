# API Contract: Gestión de Usuarios por el Administrador (AB-41)

**Base URL**: `/admin/users`
**Auth**: Bearer JWT (role: ADMIN)
**Module**: `auth`

---

## GET /admin/users

Busca y lista usuarios con filtros.

**Query Params**: `email`, `accountStatus`, `role`, `subscriptionType`, `page`, `size`

**Response 200**
```json
{
  "content": [
    { "id": 42, "fullName": "Ana López", "email": "ana@example.com", "accountStatus": "ACTIVE", "role": "INVESTOR", "subscriptionType": "PREMIUM", "createdAt": "2026-03-01T09:00:00Z" }
  ],
  "totalElements": 157,
  "page": 0,
  "size": 20
}
```

---

## GET /admin/users/{id}

Perfil completo con actividad reciente.

**Response 200**
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
    { "eventType": "AUTH_SUCCESS", "occurredAt": "2026-05-13T10:00:00Z", "result": "SUCCESS" }
  ]
}
```

---

## PATCH /admin/users/{id}/status

**Request**: `{ "newStatus": "SUSPENDED", "reason": "Actividad sospechosa detectada" }`

**Response 200**: `{ "id": 42, "previousStatus": "ACTIVE", "newStatus": "SUSPENDED", "reason": "...", "updatedAt": "..." }`

**Response 409 — Último admin**
```json
{ "error": "LAST_ADMIN", "message": "No se puede suspender al único administrador activo." }
```

---

## PATCH /admin/users/{id}/role

**Request**: `{ "newRole": "BROKER", "reason": "Asignado como broker regional" }`

Para cambio a ADMIN: `{ "newRole": "ADMIN", "reason": "...", "adminConfirmation": "CONFIRM_ADMIN_ROLE" }`

**Response 200**: `{ "id": 42, "previousRole": "INVESTOR", "newRole": "BROKER", "effectiveOnNextLogin": true }`

---

## POST /admin/users/{id}/reset-password

Inicia el proceso de restablecimiento de contraseña (el link va al correo del usuario).

**Request**: sin body

**Response 200**: `{ "message": "Enlace de restablecimiento enviado al correo del usuario." }`

---

## Common Error Responses

| Status | Error Code    | Description                             |
|--------|---------------|-----------------------------------------|
| 401    | UNAUTHORIZED  | JWT ausente o inválido                  |
| 403    | FORBIDDEN     | Rol no es ADMIN                         |
| 404    | NOT_FOUND     | Usuario no encontrado                   |
| 409    | LAST_ADMIN    | Acción dejaría el sistema sin admins    |
| 409    | ALREADY_SUSPENDED | Usuario ya está suspendido          |

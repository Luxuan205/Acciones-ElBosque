# API Contract: Suscripción Premium (AB-18)

**Base URL**: `/subscriptions`
**Auth**: Bearer JWT (role: INVESTOR)
**Module**: `auth`

---

## POST /subscriptions/activate

Activa el plan PREMIUM para el inversionista autenticado. El pago es externo; este endpoint
asume que ya fue confirmado por la pasarela de pago.

**Request**: sin body

**Response 200 — Activación exitosa**
```json
{
  "subscriptionType": "PREMIUM",
  "activatedAt": "2026-05-13T10:00:00Z",
  "expiresAt": "2026-06-12T10:00:00Z"
}
```

**Response 200 — Ya tiene PREMIUM activo**
```json
{
  "subscriptionType": "PREMIUM",
  "activatedAt": "2026-04-13T09:00:00Z",
  "expiresAt": "2026-05-13T09:00:00Z",
  "message": "Ya tiene una suscripción PREMIUM activa."
}
```

---

## GET /subscriptions/status

Consulta el estado actual de la suscripción del inversionista autenticado.

**Response 200 — PREMIUM activo**
```json
{
  "subscriptionType": "PREMIUM",
  "activatedAt": "2026-05-13T10:00:00Z",
  "expiresAt": "2026-06-12T10:00:00Z",
  "isActive": true,
  "daysRemaining": 30
}
```

**Response 200 — STANDARD**
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

## Common Error Responses

| Status | Error Code    | Description                        |
|--------|---------------|------------------------------------|
| 401    | UNAUTHORIZED  | JWT ausente o inválido             |
| 403    | FORBIDDEN     | Rol no es INVESTOR                 |

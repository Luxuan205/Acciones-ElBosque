# API Contract: Parámetros Globales (AB-40)

**Base URL**: `/config/parameters`
**Auth**: Bearer JWT (role: ADMIN)
**Module**: `configuration`

---

## GET /config/parameters

Lista todos los parámetros globales agrupados por categoría.

**Response 200**
```json
{
  "AUTH": [
    { "key": "auth.max_login_attempts", "value": "5", "dataType": "INT", "description": "Intentos fallidos antes de bloquear la cuenta", "minValue": "3", "maxValue": "10" }
  ],
  "TRADING": [
    { "key": "trading.commission_rate_pct", "value": "0.3", "dataType": "DECIMAL", "description": "Tasa de comisión como % del valor bruto", "minValue": "0.0", "maxValue": "5.0" }
  ]
}
```

---

## PUT /config/parameters/{key}

Modifica el valor de un parámetro.

**Request**: `{ "value": "7" }`

**Response 200**
```json
{ "key": "auth.max_login_attempts", "previousValue": "5", "newValue": "7", "updatedAt": "2026-05-13T16:00:00Z" }
```

**Response 400 — Valor fuera de rango**
```json
{ "error": "INVALID_VALUE", "message": "El valor debe estar entre 3 y 10." }
```

**Response 409 — Conflicto de escritura**
```json
{ "error": "CONFLICT", "message": "El parámetro fue modificado por otro administrador. Valor actual: 6" }
```

---

## GET /config/parameters/{key}/history

Historial de cambios de un parámetro.

**Response 200**
```json
[
  { "previousValue": "5", "newValue": "7", "changedBy": "admin@accioneselbosque.com", "changedAt": "2026-05-13T16:00:00Z" },
  { "previousValue": "3", "newValue": "5", "changedBy": "admin@accioneselbosque.com", "changedAt": "2026-05-01T09:00:00Z" }
]
```

---

## Common Error Responses

| Status | Error Code    | Description                              |
|--------|---------------|------------------------------------------|
| 401    | UNAUTHORIZED  | JWT ausente o inválido                   |
| 403    | FORBIDDEN     | Rol no es ADMIN                          |
| 404    | NOT_FOUND     | Clave de parámetro no existe             |
| 400    | INVALID_VALUE | Valor fuera del rango permitido          |
| 409    | CONFLICT      | Modificación concurrente — reintentar    |

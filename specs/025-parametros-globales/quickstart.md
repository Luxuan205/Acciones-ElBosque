# Quickstart: AB-40 — Parámetros Globales del Sistema

**Prerequisites**
- Service running on port 8080
- JWT token: `$ADMIN_TOKEN` (role: ADMIN, user: admin@accioneselbosque.com)
- JWT token: `$INVESTOR_TOKEN` (role: INVESTOR, user: investor@test.com)
- System initialized with default parameter values

---

## Flow 1: List all global parameters grouped by category

```bash
curl -s -X GET http://localhost:8080/config/parameters \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq .
```

Expected 200:
```json
{
  "AUTH": [
    {
      "key": "auth.max_login_attempts",
      "value": "5",
      "dataType": "INT",
      "description": "Intentos fallidos antes de bloquear la cuenta",
      "minValue": "3",
      "maxValue": "10"
    },
    {
      "key": "auth.token_expiry_minutes",
      "value": "60",
      "dataType": "INT",
      "description": "Tiempo de vigencia del JWT en minutos",
      "minValue": "15",
      "maxValue": "1440"
    }
  ],
  "TRADING": [
    {
      "key": "trading.commission_rate_pct",
      "value": "0.3",
      "dataType": "DECIMAL",
      "description": "Tasa de comisión como % del valor bruto",
      "minValue": "0.0",
      "maxValue": "5.0"
    },
    {
      "key": "trading.limit_order_expiry_days",
      "value": "30",
      "dataType": "INT",
      "description": "Días de vigencia por defecto de una limit order",
      "minValue": "1",
      "maxValue": "90"
    }
  ],
  "SUBSCRIPTIONS": [
    {
      "key": "subscriptions.premium_duration_days",
      "value": "30",
      "dataType": "INT",
      "description": "Duración de la suscripción premium en días",
      "minValue": "7",
      "maxValue": "365"
    }
  ],
  "AUDIT": [
    {
      "key": "audit.active_retention_days",
      "value": "365",
      "dataType": "INT",
      "description": "Días de retención en el log activo antes de archivar",
      "minValue": "90",
      "maxValue": "1825"
    }
  ]
}
```

DB verification:
```sql
SELECT category, key, value, data_type FROM system_parameters ORDER BY category, key;
```

---

## Flow 2: Update a parameter value within valid range

```bash
curl -s -X PUT http://localhost:8080/config/parameters/auth.max_login_attempts \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"value": "7"}' | jq .
```

Expected 200:
```json
{
  "key": "auth.max_login_attempts",
  "previousValue": "5",
  "newValue": "7",
  "updatedAt": "2026-05-14T16:00:00Z"
}
```

DB verification:
```sql
SELECT key, value, updated_at, updated_by
FROM system_parameters
WHERE key = 'auth.max_login_attempts';
```

---

## Flow 3: Reject a parameter value outside the allowed range

```bash
curl -s -X PUT http://localhost:8080/config/parameters/auth.max_login_attempts \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"value": "2"}' | jq .
```

Expected 400:
```json
{
  "error": "INVALID_VALUE",
  "message": "El valor debe estar entre 3 y 10."
}
```

```bash
# Also test upper bound
curl -s -X PUT http://localhost:8080/config/parameters/auth.max_login_attempts \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"value": "99"}' | jq .
```

Expected 400:
```json
{
  "error": "INVALID_VALUE",
  "message": "El valor debe estar entre 3 y 10."
}
```

---

## Flow 4: Reject update for a non-existent parameter key

```bash
curl -s -X PUT http://localhost:8080/config/parameters/trading.nonexistent_param \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"value": "100"}' | jq .
```

Expected 404:
```json
{
  "error": "NOT_FOUND",
  "message": "El parámetro 'trading.nonexistent_param' no existe."
}
```

---

## Flow 5: View change history for a specific parameter

```bash
curl -s -X GET http://localhost:8080/config/parameters/auth.max_login_attempts/history \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq .
```

Expected 200:
```json
[
  {
    "previousValue": "5",
    "newValue": "7",
    "changedBy": "admin@accioneselbosque.com",
    "changedAt": "2026-05-14T16:00:00Z"
  },
  {
    "previousValue": "3",
    "newValue": "5",
    "changedBy": "admin@accioneselbosque.com",
    "changedAt": "2026-05-01T09:00:00Z"
  }
]
```

DB verification:
```sql
SELECT previous_value, new_value, changed_by, changed_at
FROM system_parameter_history
WHERE parameter_key = 'auth.max_login_attempts'
ORDER BY changed_at DESC;
```

---

## Flow 6: Revert a parameter to its previous value (using history)

The revert is performed by issuing a new PUT with the desired previous value obtained from the history endpoint.

```bash
# Read history to find the previous value
PREV=$(curl -s http://localhost:8080/config/parameters/auth.max_login_attempts/history \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.[0].previousValue')

# Apply the revert as a new change
curl -s -X PUT http://localhost:8080/config/parameters/auth.max_login_attempts \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"value\": \"$PREV\"}" | jq .
```

Expected 200:
```json
{
  "key": "auth.max_login_attempts",
  "previousValue": "7",
  "newValue": "5",
  "updatedAt": "2026-05-14T16:05:00Z"
}
```

The revert creates a new entry in the history — it does not delete the previous change record.

---

## Flow 7: Reject access from a non-ADMIN role

```bash
curl -s -X GET http://localhost:8080/config/parameters \
  -H "Authorization: Bearer $INVESTOR_TOKEN" | jq .
```

Expected 403:
```json
{
  "error": "FORBIDDEN",
  "message": "Acceso denegado. Se requiere rol ADMIN."
}
```

---

## Flow 8: Conflict on concurrent write (optimistic locking)

Simulate two administrators updating the same parameter simultaneously. The second write should fail if the first has already been persisted.

```bash
# Second admin (same token scenario — in practice use a second admin session)
curl -s -X PUT http://localhost:8080/config/parameters/auth.max_login_attempts \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"value": "6"}' | jq .
```

Expected 409 (when a concurrent modification was detected):
```json
{
  "error": "CONFLICT",
  "message": "El parámetro fue modificado por otro administrador. Valor actual: 6"
}
```

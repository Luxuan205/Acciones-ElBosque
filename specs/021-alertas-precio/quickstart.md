# Quickstart: AB-35 — Alertas de Precio Personalizadas

**Prerequisites**
- Service running on port 8080
- JWT token: `$PREMIUM_TOKEN` (role: INVESTOR, active PREMIUM subscription)
- JWT token: `$BASIC_TOKEN` (role: INVESTOR, BASIC subscription — for error flows)
- Current reference prices: ECOPETROL ~1972 COP, PFBCOLOM ~39500 COP, NUTRESA ~68200 COP
- Alert IDs referenced below: `401` (ECOPETROL ABSOLUTE), `402` (PFBCOLOM PERCENTAGE)

---

## Flow 1: Create an absolute price alert — notify when ECOPETROL rises above 2100

```bash
curl -s -X POST http://localhost:8080/notifications/price-alerts \
  -H "Authorization: Bearer $PREMIUM_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "ECOPETROL",
    "alertType": "ABSOLUTE",
    "direction": "ABOVE",
    "triggerValue": 2100.00
  }' | jq .
```

Expected 201:
```json
{
  "id": 401,
  "symbol": "ECOPETROL",
  "alertType": "ABSOLUTE",
  "direction": "ABOVE",
  "triggerValue": 2100.00,
  "referencePrice": null,
  "status": "ACTIVE"
}
```

---

## Flow 2: Create an absolute price alert — notify when PFBCOLOM falls below 37000

```bash
curl -s -X POST http://localhost:8080/notifications/price-alerts \
  -H "Authorization: Bearer $PREMIUM_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "PFBCOLOM",
    "alertType": "ABSOLUTE",
    "direction": "BELOW",
    "triggerValue": 37000.00
  }' | jq .
```

Expected 201:
```json
{
  "id": 403,
  "symbol": "PFBCOLOM",
  "alertType": "ABSOLUTE",
  "direction": "BELOW",
  "triggerValue": 37000.00,
  "referencePrice": null,
  "status": "ACTIVE"
}
```

---

## Flow 3: Create a percentage-change alert for PFBCOLOM (any direction, 5% swing)

The system records the current price as `referencePrice` at creation time.

```bash
curl -s -X POST http://localhost:8080/notifications/price-alerts \
  -H "Authorization: Bearer $PREMIUM_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "PFBCOLOM",
    "alertType": "PERCENTAGE",
    "triggerValue": 5.0
  }' | jq .
```

Expected 201:
```json
{
  "id": 402,
  "symbol": "PFBCOLOM",
  "alertType": "PERCENTAGE",
  "direction": null,
  "triggerValue": 5.0,
  "referencePrice": 39500.00,
  "status": "ACTIVE"
}
```

---

## Flow 4: List all price alerts for the investor

```bash
curl -s http://localhost:8080/notifications/price-alerts \
  -H "Authorization: Bearer $PREMIUM_TOKEN" | jq .
```

Expected 200:
```json
[
  {
    "id": 401,
    "symbol": "ECOPETROL",
    "alertType": "ABSOLUTE",
    "direction": "ABOVE",
    "triggerValue": 2100.00,
    "referencePrice": null,
    "status": "ACTIVE"
  },
  {
    "id": 402,
    "symbol": "PFBCOLOM",
    "alertType": "PERCENTAGE",
    "direction": null,
    "triggerValue": 5.0,
    "referencePrice": 39500.00,
    "status": "TRIGGERED",
    "triggeredAt": "2026-05-14T10:00:00Z"
  },
  {
    "id": 403,
    "symbol": "PFBCOLOM",
    "alertType": "ABSOLUTE",
    "direction": "BELOW",
    "triggerValue": 37000.00,
    "referencePrice": null,
    "status": "ACTIVE"
  }
]
```

---

## Flow 5: Modify the trigger value of an active alert

Raise the ECOPETROL upper target from 2100 to 2200.

```bash
curl -s -X PUT http://localhost:8080/notifications/price-alerts/401 \
  -H "Authorization: Bearer $PREMIUM_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"triggerValue": 2200.00}' | jq .
```

Expected 200:
```json
{
  "id": 401,
  "symbol": "ECOPETROL",
  "alertType": "ABSOLUTE",
  "direction": "ABOVE",
  "triggerValue": 2200.00,
  "referencePrice": null,
  "status": "ACTIVE"
}
```

---

## Flow 6: Reactivate a triggered alert (PFBCOLOM percentage alert)

After alert 402 was triggered, the investor wants to monitor future swings again.

```bash
curl -s -X PATCH http://localhost:8080/notifications/price-alerts/402/reactivate \
  -H "Authorization: Bearer $PREMIUM_TOKEN" | jq .
```

Expected 200:
```json
{
  "id": 402,
  "symbol": "PFBCOLOM",
  "alertType": "PERCENTAGE",
  "direction": null,
  "triggerValue": 5.0,
  "referencePrice": 39500.00,
  "status": "ACTIVE"
}
```

---

## Flow 7: Delete a price alert permanently

```bash
curl -s -X DELETE http://localhost:8080/notifications/price-alerts/403 \
  -H "Authorization: Bearer $PREMIUM_TOKEN" | jq .
```

Expected 204 (empty body).

---

## Flow 8: Attempt to create an alert without a PREMIUM subscription (error case)

```bash
curl -s -X POST http://localhost:8080/notifications/price-alerts \
  -H "Authorization: Bearer $BASIC_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "NUTRESA",
    "alertType": "ABSOLUTE",
    "direction": "ABOVE",
    "triggerValue": 70000.00
  }' | jq .
```

Expected 403:
```json
{
  "error": "PREMIUM_REQUIRED",
  "message": "Esta funcionalidad requiere suscripción PREMIUM activa."
}
```

---

## Flow 9: Exceed the maximum alert limit (error case)

After reaching 20 active alerts, any new creation returns:

```bash
curl -s -X POST http://localhost:8080/notifications/price-alerts \
  -H "Authorization: Bearer $PREMIUM_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "CEMARGOS",
    "alertType": "ABSOLUTE",
    "direction": "ABOVE",
    "triggerValue": 8500.00
  }' | jq .
```

Expected 422:
```json
{
  "error": "ALERT_LIMIT_REACHED",
  "message": "Ha alcanzado el límite máximo de alertas activas (20)."
}
```

---

## Optional SQL verification

```sql
-- Confirm all price alerts and their statuses for the investor
SELECT id, symbol, alert_type, direction, trigger_value, reference_price, status, triggered_at
FROM price_alerts
WHERE investor_id = <YOUR_INVESTOR_ID>
ORDER BY id;

-- Count active alerts (must be <= 20 for PREMIUM)
SELECT COUNT(*) AS active_alerts
FROM price_alerts
WHERE investor_id = <YOUR_INVESTOR_ID>
  AND status = 'ACTIVE';
```

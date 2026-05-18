# Quickstart: AB-34 — Alertas de Mercado

**Prerequisites**
- Service running on port 8080
- JWT token: `$INVESTOR_TOKEN` (role: INVESTOR)
- Subscription IDs referenced below: `301` (MARKET_OPEN), `302` (UNUSUAL_VOLUME for ECOPETROL)

---

## Flow 1: Subscribe to market open alerts (no symbol required)

```bash
curl -s -X POST http://localhost:8080/notifications/market-alerts \
  -H "Authorization: Bearer $INVESTOR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"alertType": "MARKET_OPEN"}' | jq .
```

Expected 201:
```json
{
  "id": 301,
  "alertType": "MARKET_OPEN",
  "symbol": null,
  "thresholdValue": null,
  "active": true
}
```

---

## Flow 2: Subscribe to unusual volume alert for ECOPETROL with threshold

Subscribe to be notified when ECOPETROL trading volume exceeds 150% of its daily average.

```bash
curl -s -X POST http://localhost:8080/notifications/market-alerts \
  -H "Authorization: Bearer $INVESTOR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"alertType": "UNUSUAL_VOLUME", "symbol": "ECOPETROL", "thresholdValue": 150.0}' | jq .
```

Expected 201:
```json
{
  "id": 302,
  "alertType": "UNUSUAL_VOLUME",
  "symbol": "ECOPETROL",
  "thresholdValue": 150.0,
  "active": true
}
```

---

## Flow 3: Subscribe to trading suspension alert for PFBCOLOM

```bash
curl -s -X POST http://localhost:8080/notifications/market-alerts \
  -H "Authorization: Bearer $INVESTOR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"alertType": "TRADING_SUSPENDED", "symbol": "PFBCOLOM"}' | jq .
```

Expected 201:
```json
{
  "id": 303,
  "alertType": "TRADING_SUSPENDED",
  "symbol": "PFBCOLOM",
  "thresholdValue": null,
  "active": true
}
```

---

## Flow 4: List all active market alert subscriptions

```bash
curl -s http://localhost:8080/notifications/market-alerts \
  -H "Authorization: Bearer $INVESTOR_TOKEN" | jq .
```

Expected 200:
```json
[
  {
    "id": 301,
    "alertType": "MARKET_OPEN",
    "symbol": null,
    "thresholdValue": null,
    "active": true
  },
  {
    "id": 302,
    "alertType": "UNUSUAL_VOLUME",
    "symbol": "ECOPETROL",
    "thresholdValue": 150.0,
    "active": true
  },
  {
    "id": 303,
    "alertType": "TRADING_SUSPENDED",
    "symbol": "PFBCOLOM",
    "thresholdValue": null,
    "active": true
  }
]
```

---

## Flow 5: Update the threshold on an unusual volume alert

Raise the ECOPETROL unusual volume threshold from 150% to 200%.

```bash
curl -s -X PUT http://localhost:8080/notifications/market-alerts/302 \
  -H "Authorization: Bearer $INVESTOR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"thresholdValue": 200.0, "active": true}' | jq .
```

Expected 200:
```json
{
  "id": 302,
  "alertType": "UNUSUAL_VOLUME",
  "symbol": "ECOPETROL",
  "thresholdValue": 200.0,
  "active": true
}
```

---

## Flow 6: Temporarily deactivate a market open alert

```bash
curl -s -X PUT http://localhost:8080/notifications/market-alerts/301 \
  -H "Authorization: Bearer $INVESTOR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"active": false}' | jq .
```

Expected 200:
```json
{
  "id": 301,
  "alertType": "MARKET_OPEN",
  "symbol": null,
  "thresholdValue": null,
  "active": false
}
```

---

## Flow 7: Delete a market alert subscription permanently

```bash
curl -s -X DELETE http://localhost:8080/notifications/market-alerts/303 \
  -H "Authorization: Bearer $INVESTOR_TOKEN" | jq .
```

Expected 204 (empty body).

---

## Flow 8: Attempt to create UNUSUAL_VOLUME alert without required fields (error cases)

Missing `symbol`:
```bash
curl -s -X POST http://localhost:8080/notifications/market-alerts \
  -H "Authorization: Bearer $INVESTOR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"alertType": "UNUSUAL_VOLUME", "thresholdValue": 150.0}' | jq .
```

Expected 400:
```json
{
  "error": "SYMBOL_REQUIRED",
  "message": "Symbol requerido para alertas de tipo UNUSUAL_VOLUME."
}
```

Missing `thresholdValue`:
```bash
curl -s -X POST http://localhost:8080/notifications/market-alerts \
  -H "Authorization: Bearer $INVESTOR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"alertType": "UNUSUAL_VOLUME", "symbol": "NUTRESA"}' | jq .
```

Expected 400:
```json
{
  "error": "THRESHOLD_REQUIRED",
  "message": "threshold_value requerido para alertas de tipo UNUSUAL_VOLUME."
}
```

---

## Optional SQL verification

```sql
-- Confirm all subscriptions for the investor and their active state
SELECT id, alert_type, symbol, threshold_value, active
FROM market_alert_subscriptions
WHERE investor_id = <YOUR_INVESTOR_ID>
ORDER BY id;

-- Check how many alerts were triggered for ECOPETROL unusual volume
SELECT COUNT(*) AS triggered_alerts
FROM notification_events
WHERE investor_id = <YOUR_INVESTOR_ID>
  AND alert_type = 'UNUSUAL_VOLUME'
  AND symbol = 'ECOPETROL';
```

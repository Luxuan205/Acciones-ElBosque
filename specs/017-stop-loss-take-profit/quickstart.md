# Quickstart: AB-22 — Stop-Loss y Take-Profit

**Prerequisites**
- Service running on port 8080
- JWT token: `$INVESTOR_TOKEN` (role: INVESTOR, with an active position in ECOPETROL)
- Active portfolio position: at least 50 shares of ECOPETROL (current price ~1972 COP)

---

## Flow 1: Create a stop-loss to limit downside on an ECOPETROL position

```bash
curl -s -X POST http://localhost:8080/orders/conditional/stop-loss \
  -H "Authorization: Bearer $INVESTOR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"symbol": "ECOPETROL", "quantity": 50, "triggerPrice": 1850.00}' | jq .
```

Expected 201:
```json
{
  "id": 201,
  "type": "STOP_LOSS",
  "symbol": "ECOPETROL",
  "quantity": 50,
  "triggerPrice": 1850.00,
  "status": "ACTIVE",
  "ocoPartnerId": null,
  "createdAt": "2026-05-14T09:00:00Z"
}
```

---

## Flow 2: Create a take-profit linked to the stop-loss (OCO pair)

Use the `id` returned in Flow 1 as `stopLossId` to form a one-cancels-other pair.

```bash
curl -s -X POST http://localhost:8080/orders/conditional/take-profit \
  -H "Authorization: Bearer $INVESTOR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"symbol": "ECOPETROL", "quantity": 50, "triggerPrice": 2200.00, "stopLossId": 201}' | jq .
```

Expected 201:
```json
{
  "id": 202,
  "type": "TAKE_PROFIT",
  "symbol": "ECOPETROL",
  "quantity": 50,
  "triggerPrice": 2200.00,
  "status": "ACTIVE",
  "ocoPartnerId": 201,
  "createdAt": "2026-05-14T09:01:00Z"
}
```

---

## Flow 3: List all active conditional orders

```bash
curl -s -X GET http://localhost:8080/orders/conditional \
  -H "Authorization: Bearer $INVESTOR_TOKEN" | jq .
```

Expected 200:
```json
[
  {
    "id": 201,
    "type": "STOP_LOSS",
    "symbol": "ECOPETROL",
    "quantity": 50,
    "triggerPrice": 1850.00,
    "status": "ACTIVE",
    "ocoPartnerId": 202
  },
  {
    "id": 202,
    "type": "TAKE_PROFIT",
    "symbol": "ECOPETROL",
    "quantity": 50,
    "triggerPrice": 2200.00,
    "status": "ACTIVE",
    "ocoPartnerId": 201
  }
]
```

---

## Flow 4: Create a standalone stop-loss for PFBCOLOM (no OCO partner)

```bash
curl -s -X POST http://localhost:8080/orders/conditional/stop-loss \
  -H "Authorization: Bearer $INVESTOR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"symbol": "PFBCOLOM", "quantity": 20, "triggerPrice": 37000.00}' | jq .
```

Expected 201:
```json
{
  "id": 203,
  "type": "STOP_LOSS",
  "symbol": "PFBCOLOM",
  "quantity": 20,
  "triggerPrice": 37000.00,
  "status": "ACTIVE",
  "ocoPartnerId": null,
  "createdAt": "2026-05-14T09:05:00Z"
}
```

---

## Flow 5: Modify the trigger price of an active conditional order

Raise the ECOPETROL stop-loss from 1850 to 1900 to lock in more downside protection.

```bash
curl -s -X PUT http://localhost:8080/orders/conditional/201 \
  -H "Authorization: Bearer $INVESTOR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"triggerPrice": 1900.00}' | jq .
```

Expected 200:
```json
{
  "id": 201,
  "type": "STOP_LOSS",
  "symbol": "ECOPETROL",
  "quantity": 50,
  "triggerPrice": 1900.00,
  "status": "ACTIVE",
  "ocoPartnerId": 202,
  "createdAt": "2026-05-14T09:00:00Z"
}
```

---

## Flow 6: Cancel a single conditional order

```bash
curl -s -X DELETE http://localhost:8080/orders/conditional/203 \
  -H "Authorization: Bearer $INVESTOR_TOKEN" | jq .
```

Expected 200:
```json
{
  "id": 203,
  "status": "CANCELLED",
  "cancelledAt": "2026-05-14T10:00:00Z"
}
```

---

## Flow 7: Attempt to modify an already-triggered order (error case)

```bash
curl -s -X PUT http://localhost:8080/orders/conditional/201 \
  -H "Authorization: Bearer $INVESTOR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"triggerPrice": 1750.00}' | jq .
```

Expected 409 (if the order was already triggered):
```json
{
  "error": "ALREADY_TRIGGERED",
  "message": "No se puede modificar una orden ya activada."
}
```

---

## Flow 8: Attempt to set a stop-loss on a symbol with no position (error case)

```bash
curl -s -X POST http://localhost:8080/orders/conditional/stop-loss \
  -H "Authorization: Bearer $INVESTOR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"symbol": "NUTRESA", "quantity": 10, "triggerPrice": 65000.00}' | jq .
```

Expected 422:
```json
{
  "error": "NO_POSITION",
  "message": "El usuario no tiene posición en ese símbolo."
}
```

---

## Optional SQL verification

```sql
-- Confirm both OCO orders share each other's partner ID
SELECT id, type, symbol, trigger_price, status, oco_partner_id
FROM conditional_orders
WHERE investor_id = <YOUR_INVESTOR_ID>
ORDER BY created_at DESC;
```

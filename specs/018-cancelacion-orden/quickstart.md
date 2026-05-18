# Quickstart: AB-23 — Cancelación de Orden

**Prerequisites**
- Service running on port 8080
- JWT token: `$INVESTOR_TOKEN` (role: INVESTOR)
- At least one order in PENDING or QUEUED state (e.g., a limit buy order for ECOPETROL)
- Order IDs referenced below: `1044` (PENDING buy), `1045` (PENDING sell), `1030` (already EXECUTED)

---

## Flow 1: Cancel a single PENDING buy order (balance released)

```bash
curl -s -X DELETE http://localhost:8080/orders/1044 \
  -H "Authorization: Bearer $INVESTOR_TOKEN" | jq .
```

Expected 200:
```json
{
  "orderId": 1044,
  "previousStatus": "PENDING",
  "newStatus": "CANCELLED",
  "resourcesReleased": "BALANCE",
  "amountReleased": 19779.16,
  "titlesReleased": null,
  "cancelledAt": "2026-05-14T10:00:00Z"
}
```

---

## Flow 2: Cancel a PENDING sell order (titles released back to portfolio)

```bash
curl -s -X DELETE http://localhost:8080/orders/1045 \
  -H "Authorization: Bearer $INVESTOR_TOKEN" | jq .
```

Expected 200:
```json
{
  "orderId": 1045,
  "previousStatus": "PENDING",
  "newStatus": "CANCELLED",
  "resourcesReleased": "TITLES",
  "amountReleased": null,
  "titlesReleased": 10,
  "cancelledAt": "2026-05-14T10:01:00Z"
}
```

---

## Flow 3: Attempt to cancel an already-executed order (error case)

```bash
curl -s -X DELETE http://localhost:8080/orders/1030 \
  -H "Authorization: Bearer $INVESTOR_TOKEN" | jq .
```

Expected 409:
```json
{
  "error": "ORDER_NOT_CANCELLABLE",
  "currentStatus": "EXECUTED",
  "message": "La orden ya fue ejecutada y no puede cancelarse."
}
```

---

## Flow 4: Attempt to cancel an order that belongs to another user (error case)

```bash
curl -s -X DELETE http://localhost:8080/orders/9999 \
  -H "Authorization: Bearer $INVESTOR_TOKEN" | jq .
```

Expected 404:
```json
{
  "error": "ORDER_NOT_FOUND",
  "message": "Orden no encontrada."
}
```

---

## Flow 5: Bulk cancel all active orders

Use this when reacting quickly to market volatility.

```bash
curl -s -X DELETE "http://localhost:8080/orders?bulk=true" \
  -H "Authorization: Bearer $INVESTOR_TOKEN" | jq .
```

Expected 200:
```json
{
  "totalRequested": 5,
  "totalCancelled": 4,
  "totalFailed": 1,
  "cancelled": [
    { "orderId": 1046, "newStatus": "CANCELLED", "amountReleased": 39500.00 },
    { "orderId": 1047, "newStatus": "CANCELLED", "amountReleased": 68200.00 },
    { "orderId": 1048, "newStatus": "CANCELLED", "amountReleased": 7888.00 },
    { "orderId": 1049, "newStatus": "CANCELLED", "amountReleased": null }
  ],
  "failed": [
    { "orderId": 1030, "currentStatus": "EXECUTED", "reason": "ORDER_NOT_CANCELLABLE" }
  ]
}
```

---

## Flow 6: Bulk cancel when investor has no active orders

```bash
curl -s -X DELETE "http://localhost:8080/orders?bulk=true" \
  -H "Authorization: Bearer $INVESTOR_TOKEN" | jq .
```

Expected 200 (empty result set):
```json
{
  "totalRequested": 0,
  "totalCancelled": 0,
  "totalFailed": 0,
  "cancelled": [],
  "failed": []
}
```

---

## Flow 7: Cancel without a valid JWT (error case)

```bash
curl -s -X DELETE http://localhost:8080/orders/1044 | jq .
```

Expected 401:
```json
{
  "error": "UNAUTHORIZED",
  "message": "JWT ausente o inválido."
}
```

---

## Optional SQL verification

```sql
-- Confirm the order is now CANCELLED and its reserved resources were released
SELECT id, status, resources_released, amount_released, titles_released, cancelled_at
FROM orders
WHERE id = 1044;

-- Verify investor available balance was restored
SELECT available_balance
FROM investor_accounts
WHERE investor_id = <YOUR_INVESTOR_ID>;
```

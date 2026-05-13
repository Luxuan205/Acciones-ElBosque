# Quickstart: AB-24 — Encolamiento de Orden Fuera de Horario Bursátil

**Prerequisites**
- order running on port 8082; configuration-service running on port 8085
- Valid JWT for an ACTIVE investor
- Market is **closed** for queue testing (set in configuration-service or run these flows after 15:30 UTC-5)
- Replace `$TOKEN` with your Bearer token

---

## Flow 1: Place order outside market hours → QUEUED

```bash
curl -s -X POST http://localhost:8080/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "PFBCOLOM",
    "quantity": 10,
    "orderType": "BUY",
    "unitPrice": 39500.00
  }' | jq .
```

Expected: 201 with `"status": "QUEUED"`.

Verify in DB:
```sql
SELECT id, investor_id, symbol, status, created_at
FROM "order"
WHERE symbol = 'PFBCOLOM'
ORDER BY created_at DESC LIMIT 1;
-- status should be 'QUEUED'
```

---

## Flow 2: View queued orders

```bash
curl -s -X GET "http://localhost:8080/orders?status=QUEUED" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

Expected: 200 with the list of QUEUED orders for the investor.

---

## Flow 3: Cancel a queued order

```bash
# Replace ORDER_ID with the UUID from Flow 1
ORDER_ID="<uuid>"

curl -s -X DELETE "http://localhost:8080/orders/$ORDER_ID/cancel" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

Expected: 200 with `"status": "CANCELLED"`.

Attempt to cancel again (already cancelled):
```bash
curl -s -X DELETE "http://localhost:8080/orders/$ORDER_ID/cancel" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

Expected: 409 `ORDER_NOT_CANCELLABLE`.

---

## Flow 4: Queue limit (max 10)

Place 10 orders in a row then try an 11th:
```bash
for i in {1..10}; do
  curl -s -X POST http://localhost:8080/orders \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"symbol\":\"PFBCOLOM\",\"quantity\":1,\"orderType\":\"BUY\",\"unitPrice\":39500.00}" \
    | jq -r '.status'
done

# 11th order should be rejected
curl -s -X POST http://localhost:8080/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"symbol":"PFBCOLOM","quantity":1,"orderType":"BUY","unitPrice":39500.00}' | jq .
```

Expected for 11th: 422 `QUEUE_LIMIT_REACHED`.

---

## Flow 5: Scheduler processes queue at market open

1. Ensure you have QUEUED orders from Flow 1
2. Open market via configuration-service:

```bash
curl -s -X PUT http://localhost:8080/config/market/schedule \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"openTime":"00:00","closeTime":"23:59","workingDays":["MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY","SUNDAY"]}' | jq .
```

3. Wait up to 60 seconds for the scheduler to run, then check:

```sql
SELECT id, status, processed_at FROM "order"
WHERE investor_id = '<INV_ID>' AND status = 'ACTIVE'
ORDER BY processed_at DESC;
-- Previously QUEUED orders should now be ACTIVE
```

# Quickstart: AB-32 — Generación y Firma de Órdenes a Nombre del Cliente

**Prerequisites**
- order running on port 8082; auth-security-service on port 8081
- Broker JWT: `$BROKER_TOKEN` (role BROKER)
- Investor A assigned to the broker; Investor B NOT assigned
- Seed assignment:

```sql
INSERT INTO broker_client_assignment (id, broker_id, investor_id, active) VALUES
  (gen_random_uuid(), '<BROKER_ID>', '<INVESTOR_A_ID>', true);
-- investor_b is NOT assigned to this broker
```

---

## Flow 1: Broker places order for assigned client

```bash
curl -s -X POST http://localhost:8080/orders/broker \
  -H "Authorization: Bearer $BROKER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "<INVESTOR_A_ID>",
    "symbol": "PFBCOLOM",
    "quantity": 5,
    "orderType": "BUY",
    "unitPrice": 39500.00
  }' | jq .
```

Expected 201 with `clientId`, `brokerId`, `clientName`, `brokerName`, and `status` (QUEUED or ACTIVE).

Verify authorship in DB:
```sql
SELECT id, investor_id, broker_id, symbol, status
FROM "order"
WHERE investor_id = '<INVESTOR_A_ID>'
ORDER BY created_at DESC LIMIT 1;
-- broker_id must NOT be NULL
```

---

## Flow 2: Broker attempts order for unassigned client (403)

```bash
curl -s -X POST http://localhost:8080/orders/broker \
  -H "Authorization: Bearer $BROKER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "<INVESTOR_B_ID>",
    "symbol": "PFBCOLOM",
    "quantity": 5,
    "orderType": "BUY",
    "unitPrice": 39500.00
  }' | jq .
```

Expected: 403 `ACCESS_DENIED`.

---

## Flow 3: Insufficient client funds

```bash
# Set client's available balance to a small amount:
UPDATE account_balance SET total_balance = 1000.00 WHERE investor_id = '<INVESTOR_A_ID>';

curl -s -X POST http://localhost:8080/orders/broker \
  -H "Authorization: Bearer $BROKER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "<INVESTOR_A_ID>",
    "symbol": "PFBCOLOM",
    "quantity": 100,
    "orderType": "BUY",
    "unitPrice": 39500.00
  }' | jq .
```

Expected: 400 `INSUFFICIENT_FUNDS`.

---

## Flow 4: View broker order history

```bash
curl -s "http://localhost:8080/orders/broker/history" \
  -H "Authorization: Bearer $BROKER_TOKEN" | jq '.content[] | {client: .clientName, symbol: .symbol, status: .status}'
```

Expected: All orders placed by this broker, with client names.

Filter by specific client:
```bash
curl -s "http://localhost:8080/orders/broker/history?clientId=<INVESTOR_A_ID>" \
  -H "Authorization: Bearer $BROKER_TOKEN" | jq '.totalElements'
```

---

## Flow 5: Filter history by date range

```bash
curl -s "http://localhost:8080/orders/broker/history?from=2026-05-01&to=2026-05-10" \
  -H "Authorization: Bearer $BROKER_TOKEN" | jq '[.content[] | {createdAt: .createdAt, symbol: .symbol}]'
```

Expected: Only orders created between May 1–10, 2026.

---

## Flow 6: Non-broker cannot use broker endpoint

```bash
curl -s -X POST http://localhost:8080/orders/broker \
  -H "Authorization: Bearer $INVESTOR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"clientId":"uuid","symbol":"PFBCOLOM","quantity":1,"orderType":"BUY","unitPrice":39500.00}' | jq .
```

Expected: 403 `FORBIDDEN`.

---

## Flow 7: Verify traceability — direct vs broker orders

```sql
SELECT id, investor_id, broker_id,
       CASE WHEN broker_id IS NULL THEN 'DIRECT' ELSE 'BROKER' END AS order_type,
       symbol, status
FROM "order"
WHERE investor_id = '<INVESTOR_A_ID>'
ORDER BY created_at DESC;
-- broker_id IS NOT NULL for all orders placed by the broker
```

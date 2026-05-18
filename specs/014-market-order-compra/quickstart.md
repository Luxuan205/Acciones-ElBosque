# Quickstart: AB-19 — Market Order de Compra

**Prerequisites**
- Service running on `http://localhost:8080`
- `$INVESTOR_TOKEN` — JWT for a user with role INVESTOR and sufficient COP balance (>= 20 000 COP)
- `$BROKE_TOKEN` — JWT for a user with role INVESTOR with 0 or near-0 COP balance
- Market data module (AB-28) returning live or stubbed prices
- Colombian stock symbols available in catalog: ECOPETROL, PFBCOLOM, NUTRESA, CEMARGOS

---

## Flow 1: Happy path — preview buy order before confirming

```bash
curl -s -X GET "http://localhost:8080/orders/market/buy/preview?symbol=ECOPETROL&quantity=10" \
  -H "Authorization: Bearer $INVESTOR_TOKEN" | jq .
```

Expected HTTP 200:
```json
{
  "symbol": "ECOPETROL",
  "quantity": 10,
  "estimatedPrice": 1972.00,
  "commission": 59.16,
  "totalEstimated": 19779.16,
  "marketOpen": true
}
```

> `totalEstimated` = (estimatedPrice × quantity) + commission. Verify the maths before confirming.

---

## Flow 2: Happy path — place market buy order (market open)

```bash
curl -s -X POST http://localhost:8080/orders/market/buy \
  -H "Authorization: Bearer $INVESTOR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"symbol": "ECOPETROL", "quantity": 10}' | jq .
```

Expected HTTP 201:
```json
{
  "orderId": 1042,
  "status": "PENDING",
  "symbol": "ECOPETROL",
  "quantity": 10,
  "breakdown": {
    "estimatedPrice": 1972.00,
    "quantity": 10,
    "commission": 59.16,
    "totalEstimated": 19779.16
  },
  "createdAt": "2026-05-14T14:30:00Z"
}
```

> Status `PENDING` means the order is live in the market. Save `orderId` to track its progress.

---

## Flow 3: Happy path — preview a larger purchase (PFBCOLOM)

```bash
curl -s -X GET "http://localhost:8080/orders/market/buy/preview?symbol=PFBCOLOM&quantity=5" \
  -H "Authorization: Bearer $INVESTOR_TOKEN" | jq .
```

Expected HTTP 200:
```json
{
  "symbol": "PFBCOLOM",
  "quantity": 5,
  "estimatedPrice": 39500.00,
  "commission": 592.50,
  "totalEstimated": 198062.50,
  "marketOpen": true
}
```

---

## Flow 4: Order queued — placed outside market hours

To test this flow the service must report `marketOpen: false` (e.g. after 16:00 COT on a weekday or during a holiday).

```bash
curl -s -X POST http://localhost:8080/orders/market/buy \
  -H "Authorization: Bearer $INVESTOR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"symbol": "NUTRESA", "quantity": 3}' | jq .
```

Expected HTTP 201 (status QUEUED when market is closed):
```json
{
  "orderId": 1043,
  "status": "QUEUED",
  "symbol": "NUTRESA",
  "quantity": 3,
  "breakdown": {
    "estimatedPrice": 68200.00,
    "quantity": 3,
    "commission": 614.00,
    "totalEstimated": 205214.00
  },
  "message": "El mercado está cerrado. La orden se ejecutará en la próxima apertura.",
  "createdAt": "2026-05-14T20:00:00Z"
}
```

---

## Flow 5: Insufficient balance — order rejected

```bash
curl -s -X POST http://localhost:8080/orders/market/buy \
  -H "Authorization: Bearer $BROKE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"symbol": "ECOPETROL", "quantity": 10}' | jq .
```

Expected HTTP 422:
```json
{
  "error": "INSUFFICIENT_BALANCE",
  "message": "Saldo insuficiente para cubrir el total de la compra."
}
```

---

## Flow 6: Unknown symbol — catalog lookup fails

```bash
curl -s -X GET "http://localhost:8080/orders/market/buy/preview?symbol=FAKESTOCK&quantity=1" \
  -H "Authorization: Bearer $INVESTOR_TOKEN" | jq .
```

Expected HTTP 404:
```json
{
  "error": "SYMBOL_NOT_FOUND",
  "message": "No se encontró la acción: FAKESTOCK"
}
```

---

## Flow 7: Unauthenticated request

```bash
curl -s -X POST http://localhost:8080/orders/market/buy \
  -H "Content-Type: application/json" \
  -d '{"symbol": "ECOPETROL", "quantity": 10}' | jq .
```

Expected HTTP 401:
```json
{
  "error": "UNAUTHORIZED",
  "message": "JWT ausente o inválido"
}
```

---

## SQL Verification Queries

```sql
-- Confirm order was persisted with correct initial status
SELECT id, investor_id, symbol, quantity, status, created_at
FROM orders
WHERE symbol = 'ECOPETROL'
ORDER BY created_at DESC
LIMIT 5;

-- Verify balance was reserved after order placement (FR-005)
SELECT available_balance, reserved_balance
FROM accounts
WHERE investor_id = (SELECT id FROM users WHERE email = 'investor@elbosque.edu.co');

-- Check audit log entry for the order (FR-008)
SELECT user_id, action, symbol, quantity, order_status, logged_at
FROM order_audit_log
ORDER BY logged_at DESC
LIMIT 5;

-- Count QUEUED orders waiting for next market open
SELECT COUNT(*) AS queued_orders
FROM orders
WHERE status = 'QUEUED';
```

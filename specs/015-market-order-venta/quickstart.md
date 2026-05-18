# Quickstart: AB-20 — Market Order de Venta

**Prerequisites**
- Service running on `http://localhost:8080`
- `$INVESTOR_TOKEN` — JWT for a user with role INVESTOR who holds at least 20 PFBCOLOM titles and 50 ECOPETROL titles in their portfolio
- `$BROKE_TOKEN` — JWT for a user with role INVESTOR who holds 0 titles of any stock
- Colombian stock symbols available in catalog: PFBCOLOM, ECOPETROL, NUTRESA, CEMARGOS

---

## Flow 1: Happy path — preview sell order before confirming

```bash
curl -s -X GET "http://localhost:8080/orders/market/sell/preview?symbol=PFBCOLOM&quantity=5" \
  -H "Authorization: Bearer $INVESTOR_TOKEN" | jq .
```

Expected HTTP 200:
```json
{
  "symbol": "PFBCOLOM",
  "quantity": 5,
  "estimatedPrice": 39500.00,
  "commission": 592.50,
  "netAmount": 196907.50,
  "availableTitles": 20,
  "marketOpen": true
}
```

> `netAmount` = (estimatedPrice × quantity) − commission. `availableTitles` shows unreserved titles only.

---

## Flow 2: Happy path — place market sell order (market open)

```bash
curl -s -X POST http://localhost:8080/orders/market/sell \
  -H "Authorization: Bearer $INVESTOR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"symbol": "PFBCOLOM", "quantity": 5}' | jq .
```

Expected HTTP 201:
```json
{
  "orderId": 1044,
  "status": "PENDING",
  "symbol": "PFBCOLOM",
  "quantity": 5,
  "breakdown": {
    "estimatedPrice": 39500.00,
    "quantity": 5,
    "commission": 592.50,
    "netAmount": 196907.50
  },
  "createdAt": "2026-05-14T14:45:00Z"
}
```

> Status `PENDING` means the order is live in the market. The 5 titles are now reserved in the portfolio (FR-005).

---

## Flow 3: Happy path — sell a different stock (ECOPETROL)

```bash
curl -s -X GET "http://localhost:8080/orders/market/sell/preview?symbol=ECOPETROL&quantity=20" \
  -H "Authorization: Bearer $INVESTOR_TOKEN" | jq .
```

Expected HTTP 200:
```json
{
  "symbol": "ECOPETROL",
  "quantity": 20,
  "estimatedPrice": 1972.00,
  "commission": 118.32,
  "netAmount": 39321.68,
  "availableTitles": 50,
  "marketOpen": true
}
```

```bash
curl -s -X POST http://localhost:8080/orders/market/sell \
  -H "Authorization: Bearer $INVESTOR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"symbol": "ECOPETROL", "quantity": 20}' | jq .
```

Expected HTTP 201:
```json
{
  "orderId": 1045,
  "status": "PENDING",
  "symbol": "ECOPETROL",
  "quantity": 20,
  "breakdown": {
    "estimatedPrice": 1972.00,
    "quantity": 20,
    "commission": 118.32,
    "netAmount": 39321.68
  },
  "createdAt": "2026-05-14T14:46:00Z"
}
```

---

## Flow 4: Order queued — placed outside market hours

To test this flow the service must report `marketOpen: false` (e.g. after 16:00 COT or on a weekend).

```bash
curl -s -X POST http://localhost:8080/orders/market/sell \
  -H "Authorization: Bearer $INVESTOR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"symbol": "PFBCOLOM", "quantity": 3}' | jq .
```

Expected HTTP 201 (status QUEUED when market is closed):
```json
{
  "orderId": 1046,
  "status": "QUEUED",
  "symbol": "PFBCOLOM",
  "quantity": 3,
  "breakdown": {
    "estimatedPrice": 39500.00,
    "quantity": 3,
    "commission": 355.50,
    "netAmount": 118144.50
  },
  "message": "El mercado está cerrado. La orden se ejecutará en la próxima apertura.",
  "createdAt": "2026-05-14T20:00:00Z"
}
```

---

## Flow 5: Insufficient titles — order rejected

```bash
curl -s -X POST http://localhost:8080/orders/market/sell \
  -H "Authorization: Bearer $BROKE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"symbol": "PFBCOLOM", "quantity": 100}' | jq .
```

Expected HTTP 422:
```json
{
  "error": "INSUFFICIENT_TITLES",
  "message": "No tiene suficientes títulos disponibles de PFBCOLOM."
}
```

---

## Flow 6: Selling more titles than owned

Even with a valid portfolio, attempting to sell beyond available quantity must fail.

```bash
curl -s -X POST http://localhost:8080/orders/market/sell \
  -H "Authorization: Bearer $INVESTOR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"symbol": "PFBCOLOM", "quantity": 9999}' | jq .
```

Expected HTTP 422:
```json
{
  "error": "INSUFFICIENT_TITLES",
  "message": "No tiene suficientes títulos disponibles de PFBCOLOM."
}
```

---

## Flow 7: Unknown symbol

```bash
curl -s -X GET "http://localhost:8080/orders/market/sell/preview?symbol=GHOSTCORP&quantity=1" \
  -H "Authorization: Bearer $INVESTOR_TOKEN" | jq .
```

Expected HTTP 404:
```json
{
  "error": "SYMBOL_NOT_FOUND",
  "message": "No se encontró la acción: GHOSTCORP"
}
```

---

## Flow 8: Unauthenticated request

```bash
curl -s -X POST http://localhost:8080/orders/market/sell \
  -H "Content-Type: application/json" \
  -d '{"symbol": "PFBCOLOM", "quantity": 5}' | jq .
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
-- Confirm sell order was persisted with correct initial status
SELECT id, investor_id, symbol, quantity, order_type, status, created_at
FROM orders
WHERE symbol = 'PFBCOLOM' AND order_type = 'MARKET_SELL'
ORDER BY created_at DESC
LIMIT 5;

-- Verify titles were reserved after sell order placement (FR-005)
SELECT symbol, total_titles, reserved_titles,
       (total_titles - reserved_titles) AS available_titles
FROM portfolio
WHERE investor_id = (SELECT id FROM users WHERE email = 'investor@elbosque.edu.co')
  AND symbol = 'PFBCOLOM';

-- Check audit log entry for the sell order (FR-008)
SELECT user_id, action, symbol, quantity, order_status, logged_at
FROM order_audit_log
WHERE action = 'MARKET_SELL'
ORDER BY logged_at DESC
LIMIT 5;

-- Double-sell guard: total reserved should never exceed total holdings
SELECT symbol,
       SUM(quantity) AS total_reserved,
       MAX(total_titles) AS portfolio_total
FROM orders o
JOIN portfolio p ON p.investor_id = o.investor_id AND p.symbol = o.symbol
WHERE o.status IN ('PENDING', 'QUEUED')
  AND o.order_type = 'MARKET_SELL'
GROUP BY symbol;
```

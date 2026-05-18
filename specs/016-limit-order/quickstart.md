# Quickstart: AB-21 — Limit Orders (Compra y Venta)

**Prerequisites**
- Service running on `http://localhost:8080`
- `$INVESTOR_TOKEN` — JWT for a user with role INVESTOR, sufficient COP balance (>= 200 000 COP) and at least 15 PFBCOLOM titles in portfolio
- `$BROKE_TOKEN` — JWT for a user with role INVESTOR with 0 balance and 0 titles
- Colombian stock symbols available in catalog: ECOPETROL (~1972 COP), PFBCOLOM (~39500 COP), NUTRESA (~68200 COP), CEMARGOS, GRUPOSURA, ISA

---

## Flow 1: Happy path — place limit buy order (price below market)

The investor wants to buy 100 ECOPETROL titles only if the price drops to 1900 COP. The order stays PENDING until that condition is met.

```bash
curl -s -X POST http://localhost:8080/orders/limit/buy \
  -H "Authorization: Bearer $INVESTOR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "ECOPETROL",
    "quantity": 100,
    "limitPrice": 1900.00,
    "expiresAt": "2026-06-01"
  }' | jq .
```

Expected HTTP 201:
```json
{
  "orderId": 1050,
  "status": "PENDING",
  "orderType": "LIMIT_BUY",
  "symbol": "ECOPETROL",
  "quantity": 100,
  "limitPrice": 1900.00,
  "expiresAt": "2026-06-01",
  "breakdown": {
    "estimatedPrice": 1900.00,
    "commission": 570.00,
    "totalReserved": 190570.00
  },
  "createdAt": "2026-05-14T10:00:00Z"
}
```

> `totalReserved` = (limitPrice × quantity) + commission. This amount is immediately deducted from `available_balance` and moved to `reserved_balance` (FR-003, SC-001).

---

## Flow 2: Happy path — place limit sell order (GTC — no expiry)

The investor wants to sell 10 PFBCOLOM titles but only at 42 000 COP or above. No expiry date means this is a Good Till Cancelled order.

```bash
curl -s -X POST http://localhost:8080/orders/limit/sell \
  -H "Authorization: Bearer $INVESTOR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "PFBCOLOM",
    "quantity": 10,
    "limitPrice": 42000.00,
    "expiresAt": null
  }' | jq .
```

Expected HTTP 201:
```json
{
  "orderId": 1051,
  "status": "PENDING",
  "orderType": "LIMIT_SELL",
  "symbol": "PFBCOLOM",
  "quantity": 10,
  "limitPrice": 42000.00,
  "expiresAt": null,
  "validity": "GTC",
  "breakdown": {
    "estimatedPrice": 42000.00,
    "commission": 1260.00,
    "netEstimated": 418740.00
  },
  "createdAt": "2026-05-14T10:05:00Z"
}
```

> `netEstimated` = (limitPrice × quantity) − commission. `validity: "GTC"` is returned when `expiresAt` is null. The 10 titles are immediately reserved in the portfolio (FR-004, SC-001).

---

## Flow 3: Limit buy with GTD expiry — NUTRESA at target price

```bash
curl -s -X POST http://localhost:8080/orders/limit/buy \
  -H "Authorization: Bearer $INVESTOR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "NUTRESA",
    "quantity": 3,
    "limitPrice": 65000.00,
    "expiresAt": "2026-05-21"
  }' | jq .
```

Expected HTTP 201:
```json
{
  "orderId": 1052,
  "status": "PENDING",
  "orderType": "LIMIT_BUY",
  "symbol": "NUTRESA",
  "quantity": 3,
  "limitPrice": 65000.00,
  "expiresAt": "2026-05-21",
  "breakdown": {
    "estimatedPrice": 65000.00,
    "commission": 585.00,
    "totalReserved": 195585.00
  },
  "createdAt": "2026-05-14T10:10:00Z"
}
```

---

## Flow 4: Insufficient balance — limit buy rejected

```bash
curl -s -X POST http://localhost:8080/orders/limit/buy \
  -H "Authorization: Bearer $BROKE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "ECOPETROL",
    "quantity": 1000,
    "limitPrice": 1900.00,
    "expiresAt": "2026-06-01"
  }' | jq .
```

Expected HTTP 422:
```json
{
  "error": "INSUFFICIENT_BALANCE",
  "message": "Saldo insuficiente para cubrir el total de la limit order de compra."
}
```

---

## Flow 5: Insufficient titles — limit sell rejected

```bash
curl -s -X POST http://localhost:8080/orders/limit/sell \
  -H "Authorization: Bearer $BROKE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "PFBCOLOM",
    "quantity": 500,
    "limitPrice": 40000.00,
    "expiresAt": null
  }' | jq .
```

Expected HTTP 422:
```json
{
  "error": "INSUFFICIENT_TITLES",
  "message": "Títulos disponibles insuficientes"
}
```

---

## Flow 6: Invalid limit price — zero or negative value

```bash
curl -s -X POST http://localhost:8080/orders/limit/buy \
  -H "Authorization: Bearer $INVESTOR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "ECOPETROL",
    "quantity": 10,
    "limitPrice": -500.00,
    "expiresAt": null
  }' | jq .
```

Expected HTTP 400:
```json
{
  "error": "INVALID_LIMIT_PRICE",
  "message": "El precio límite debe ser mayor a cero."
}
```

---

## Flow 7: Unauthenticated request

```bash
curl -s -X POST http://localhost:8080/orders/limit/buy \
  -H "Content-Type: application/json" \
  -d '{"symbol": "ECOPETROL", "quantity": 10, "limitPrice": 1900.00, "expiresAt": null}' | jq .
```

Expected HTTP 401:
```json
{
  "error": "UNAUTHORIZED",
  "message": "JWT ausente o inválido"
}
```

---

## Flow 8: Simulate GTD expiry — verify auto-cancellation

Set a limit order's `expires_at` to the past and trigger or wait for the expiry job (runs within 5 minutes per SC-003).

```sql
-- Manually expire a limit order for testing (use the orderId from Flow 3)
UPDATE orders
SET expires_at = NOW() - INTERVAL '1 minute'
WHERE id = 1052 AND status = 'PENDING';
```

After the expiry job runs:

```bash
# Assuming a GET /orders/{orderId} endpoint exists for status checks
curl -s -X GET http://localhost:8080/orders/1052 \
  -H "Authorization: Bearer $INVESTOR_TOKEN" | jq .status
```

Expected value: `"CANCELLED"`

Verify funds were released:

```sql
SELECT available_balance, reserved_balance
FROM accounts
WHERE investor_id = (SELECT id FROM users WHERE email = 'investor@elbosque.edu.co');
-- reserved_balance should decrease by totalReserved from the cancelled order
```

---

## SQL Verification Queries

```sql
-- All active (PENDING) limit orders for an investor
SELECT id, order_type, symbol, quantity, limit_price, expires_at, created_at
FROM orders
WHERE investor_id = (SELECT id FROM users WHERE email = 'investor@elbosque.edu.co')
  AND status = 'PENDING'
ORDER BY created_at DESC;

-- Verify balance reservation for LIMIT_BUY (FR-003)
SELECT available_balance, reserved_balance,
       (available_balance + reserved_balance) AS total_balance
FROM accounts
WHERE investor_id = (SELECT id FROM users WHERE email = 'investor@elbosque.edu.co');

-- Verify title reservation for LIMIT_SELL (FR-004)
SELECT symbol, total_titles, reserved_titles,
       (total_titles - reserved_titles) AS available_titles
FROM portfolio
WHERE investor_id = (SELECT id FROM users WHERE email = 'investor@elbosque.edu.co')
  AND symbol = 'PFBCOLOM';

-- Expired orders that should have been auto-cancelled (should be 0)
SELECT COUNT(*) AS missed_expirations
FROM orders
WHERE status = 'PENDING'
  AND expires_at IS NOT NULL
  AND expires_at < NOW();

-- Audit log for limit order placements (FR-009)
SELECT user_id, action, symbol, quantity, limit_price, order_status, logged_at
FROM order_audit_log
WHERE action IN ('LIMIT_BUY', 'LIMIT_SELL')
ORDER BY logged_at DESC
LIMIT 10;
```

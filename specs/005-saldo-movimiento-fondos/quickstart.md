# Quickstart: AB-26 — Consulta de Saldo y Movimiento de Fondos

**Prerequisites**
- portfolio running on port 8083
- Valid JWT for an ACTIVE investor with funds (obtained after deposit)
- Replace `$TOKEN` with Bearer token and `$INV_ID` with investor UUID

---

## Flow 1: Check balance

```bash
curl -s -X GET http://localhost:8080/portfolio/balance \
  -H "Authorization: Bearer $TOKEN" | jq .
```

Expected 200:
```json
{
  "investorId": "uuid",
  "totalBalance": 5000000.00,
  "reservedBalance": 0.00,
  "availableBalance": 5000000.00,
  "currency": "COP",
  "updatedAt": "2026-01-15T10:00:00Z"
}
```

---

## Flow 2: Get fund movement history (all movements)

```bash
curl -s -X GET "http://localhost:8080/portfolio/movements" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

Expected 200 with paginated list of all movements:
```json
{
  "content": [...],
  "totalElements": 1,
  "totalPages": 1,
  "page": 0,
  "size": 20
}
```

---

## Flow 3: Filter movements by date range

```bash
curl -s -X GET "http://localhost:8080/portfolio/movements?from=2026-01-01&to=2026-05-10" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

Expected: Only movements within the specified date range.

---

## Flow 4: Verify reserved balance updates with order placement

1. Check balance before placing order:
```bash
curl -s http://localhost:8080/portfolio/balance \
  -H "Authorization: Bearer $TOKEN" | jq '{available: .availableBalance}'
```

2. Place a QUEUED order (market closed) via order:
```bash
curl -s -X POST http://localhost:8080/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"symbol":"PFBCOLOM","quantity":5,"orderType":"BUY","unitPrice":39500.00}' | jq .
```

3. Check balance again — `reservedBalance` should increase:
```bash
curl -s http://localhost:8080/portfolio/balance \
  -H "Authorization: Bearer $TOKEN" | jq '{total: .totalBalance, reserved: .reservedBalance, available: .availableBalance}'
```

Expected: `reservedBalance = 200462.50` (5 × 39500 × 1.015); `availableBalance` decreased by same amount.

---

## Flow 5: Pagination

```bash
curl -s -X GET "http://localhost:8080/portfolio/movements?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN" | jq '{total: .totalElements, pages: .totalPages, page: .page}'
```

---

## Flow 6: Isolation — cannot see another investor's balance

```bash
# Using investor A's token to request investor B's data (not possible — endpoint uses JWT)
# The endpoint always returns the authenticated investor's own data
curl -s http://localhost:8080/portfolio/balance \
  -H "Authorization: Bearer $TOKEN_INVESTOR_A" | jq '.investorId'
# Must return investor A's ID, never investor B's
```

---

## Flow 7: Verify balanceAfter in movements

```sql
SELECT type, amount, balance_after, created_at
FROM fund_movement
WHERE investor_id = '<INV_ID>'
ORDER BY created_at ASC;
-- Each row's balance_after should equal previous balance_after + amount
```

# Quickstart: AB-27 — Visualización de Portafolio de Inversiones

**Prerequisites**
- portfolio running on port 8083
- market-data running on port 8084 (for price enrichment)
- Investor has executed at least one BUY order (position data seeded or via order)
- Replace `$TOKEN` with Bearer token and `$INV_ID` with investor UUID

---

## Flow 1: Empty portfolio

```bash
curl -s http://localhost:8080/portfolio/holdings \
  -H "Authorization: Bearer $TOKEN" | jq .
```

Expected (new investor with no positions):
```json
{
  "investorId": "uuid",
  "positions": [],
  "currency": "COP"
}
```

---

## Flow 2: Portfolio with positions

Seed a test position directly (or let order execution populate it):
```sql
INSERT INTO position (id, investor_id, symbol, name, quantity, avg_buy_price, currency)
VALUES (gen_random_uuid(), '<INV_ID>', 'PFBCOLOM', 'Bancolombia Preferencial', 10, 38900.00, 'COP');
```

```bash
curl -s http://localhost:8080/portfolio/holdings \
  -H "Authorization: Bearer $TOKEN" | jq .
```

Expected:
```json
{
  "investorId": "uuid",
  "positions": [
    {
      "symbol": "PFBCOLOM",
      "name": "Bancolombia Preferencial",
      "quantity": 10,
      "avgBuyPrice": 38900.00,
      "currentPrice": 39500.00,
      "positionValue": 395000.00,
      "pnlAmount": 6000.00,
      "pnlPercent": 1.54,
      "dayChange": 350.00,
      "dayChangePct": 0.89,
      "currency": "COP"
    }
  ],
  "currency": "COP"
}
```

Verify P&L calculation: `(39500 - 38900) × 10 = 6000.00`; `6000 / (38900 × 10) × 100 = 1.54%`.

---

## Flow 3: Portfolio summary

```bash
curl -s http://localhost:8080/portfolio/summary \
  -H "Authorization: Bearer $TOKEN" | jq .
```

Expected (with the PFBCOLOM position above):
```json
{
  "investorId": "uuid",
  "totalValue": 395000.00,
  "totalCost": 389000.00,
  "totalPnl": 6000.00,
  "totalPnlPct": 1.54,
  "totalDayChange": 3500.00,
  "positionCount": 1,
  "currency": "COP"
}
```

---

## Flow 4: Weighted average price — multiple buys

Simulate a second buy at a different price:
```sql
-- Simulate execution: first buy 10 at 38900, then 5 more at 40000
-- weighted avg = (10 * 38900 + 5 * 40000) / 15 = 39266.67
UPDATE position
SET quantity = 15, avg_buy_price = 39266.67
WHERE investor_id = '<INV_ID>' AND symbol = 'PFBCOLOM';
```

```bash
curl -s http://localhost:8080/portfolio/holdings \
  -H "Authorization: Bearer $TOKEN" | jq '.positions[0] | {avgBuyPrice, quantity, pnlAmount}'
```

Expected: `avgBuyPrice = 39266.67`, `pnlAmount = (39500 - 39266.67) × 15 = 3499.95`.

---

## Flow 5: Isolation test

```bash
# Token from investor A cannot see investor B's holdings
# The endpoint uses the investorId from JWT — returns only the authenticated investor's data
curl -s http://localhost:8080/portfolio/holdings \
  -H "Authorization: Bearer $TOKEN_INVESTOR_A" | jq '.investorId'
# Must return investor A's UUID
```

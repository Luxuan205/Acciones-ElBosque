# Quickstart: AB-28 — Dashboard de Comportamiento de Acciones

**Prerequisites**
- market-data running on port 8084
- Flyway seed data applied (10 Colombian stocks pre-loaded by V1 migration)
- Replace `$TOKEN` with any valid Bearer token (any authenticated role)

---

## Flow 1: List all stocks (default sort by name)

```bash
curl -s "http://localhost:8080/market/stocks" \
  -H "Authorization: Bearer $TOKEN" | jq '{marketOpen: .marketOpen, count: (.stocks | length), first: .stocks[0].symbol}'
```

Expected: 200 with `marketOpen: true/false`, 10 stocks listed.

---

## Flow 2: Search by symbol

```bash
curl -s "http://localhost:8080/market/stocks?search=ECO" \
  -H "Authorization: Bearer $TOKEN" | jq '.stocks[] | {symbol, name}'
```

Expected: Returns `ECOPETROL` (symbol matches "ECO").

---

## Flow 3: Search by name

```bash
curl -s "http://localhost:8080/market/stocks?search=bancolombia" \
  -H "Authorization: Bearer $TOKEN" | jq '.stocks[] | {symbol, name}'
```

Expected: Returns `PFBCOLOM` (name contains "Bancolombia", case-insensitive).

---

## Flow 4: Sort by daily change percentage (best performers first)

```bash
curl -s "http://localhost:8080/market/stocks?sort=dayChangePct_desc" \
  -H "Authorization: Bearer $TOKEN" | jq '[.stocks[] | {symbol, dayChangePct}]'
```

Expected: Stocks ordered by `dayChangePct` descending.

---

## Flow 5: Get stock detail

```bash
curl -s "http://localhost:8080/market/stocks/PFBCOLOM" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

Expected 200:
```json
{
  "symbol": "PFBCOLOM",
  "name": "Bancolombia Preferencial",
  "currentPrice": 39500.00,
  "previousClose": 39150.00,
  "dayChange": 350.00,
  "dayChangePct": 0.89,
  "volume": 85000,
  "updatedAt": "2026-05-10T14:55:00Z",
  "marketOpen": true,
  "stale": false
}
```

Test non-existent symbol:
```bash
curl -s "http://localhost:8080/market/stocks/FAKE" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

Expected: 404 `SYMBOL_NOT_FOUND`.

---

## Flow 6: Get intraday price points

```bash
curl -s "http://localhost:8080/market/stocks/PFBCOLOM/intraday" \
  -H "Authorization: Bearer $TOKEN" | jq '{symbol: .symbol, pointCount: (.points | length), first: .points[0]}'
```

Expected during market hours: Multiple points at 5-minute intervals.
Expected after market close: `"points": []`.

---

## Flow 7: Verify stale indicator when market is closed

Close the market via configuration-service:
```bash
curl -s -X PUT http://localhost:8080/config/market/schedule \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"openTime":"00:00","closeTime":"00:01","workingDays":["MONDAY"]}' | jq .
```

Then check a stock:
```bash
curl -s "http://localhost:8080/market/stocks/PFBCOLOM" \
  -H "Authorization: Bearer $TOKEN" | jq '{marketOpen: .marketOpen, stale: .stale}'
```

Expected: `"marketOpen": false`, `"stale": true`.

---

## Flow 8: Verify price update (scheduler runs every 60s)

```bash
# Check current price
curl -s "http://localhost:8080/market/stocks/ECOPETROL" \
  -H "Authorization: Bearer $TOKEN" | jq '{price: .currentPrice, updatedAt: .updatedAt}'

# Wait 65 seconds
sleep 65

# Check again — price should have changed (dev mode: ±2% random variation)
curl -s "http://localhost:8080/market/stocks/ECOPETROL" \
  -H "Authorization: Bearer $TOKEN" | jq '{price: .currentPrice, updatedAt: .updatedAt}'
```

# Quickstart: AB-36 — Watchlist de Acciones (Funcionalidad Premium)

**Prerequisites**
- market-data running on port 8084
- Premium investor JWT: `$PREMIUM_TOKEN`
- Standard (non-premium) investor JWT: `$STD_TOKEN`
- Stock seed data applied (PFBCOLOM, NUTRESA, ECOPETROL, etc.)

---

## Flow 1: Non-premium investor cannot access watchlist (403)

```bash
curl -s http://localhost:8080/watchlist \
  -H "Authorization: Bearer $STD_TOKEN" | jq .
```

Expected: 403 `PREMIUM_REQUIRED`.

---

## Flow 2: Get empty watchlist (first access — created on-demand)

```bash
curl -s http://localhost:8080/watchlist \
  -H "Authorization: Bearer $PREMIUM_TOKEN" | jq .
```

Expected 200:
```json
{
  "watchlistId": "uuid",
  "entryCount": 0,
  "maxEntries": 50,
  "entries": []
}
```

Verify watchlist created in DB:
```sql
SELECT id, investor_id, created_at FROM watchlist
WHERE investor_id = '<INVESTOR_ID>';
```

---

## Flow 3: Add a stock to the watchlist

```bash
curl -s -X POST http://localhost:8080/watchlist/entries \
  -H "Authorization: Bearer $PREMIUM_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"symbol": "PFBCOLOM"}' | jq .
```

Expected 201:
```json
{
  "symbol": "PFBCOLOM",
  "name": "Bancolombia Preferencial",
  "priceAtAdded": 39500.00,
  "addedAt": "2026-05-10T14:30:00Z",
  "entryCount": 1,
  "maxEntries": 50
}
```

---

## Flow 4: Get watchlist with enriched prices

```bash
curl -s http://localhost:8080/watchlist \
  -H "Authorization: Bearer $PREMIUM_TOKEN" | jq .
```

Expected: 1 entry with `currentPrice`, `dayChange`, `dayChangePct`, and `lastUpdated` from market data.

---

## Flow 5: Duplicate symbol rejected

```bash
curl -s -X POST http://localhost:8080/watchlist/entries \
  -H "Authorization: Bearer $PREMIUM_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"symbol": "PFBCOLOM"}' | jq .
```

Expected: 409 `SYMBOL_ALREADY_IN_WATCHLIST`.

---

## Flow 6: Unknown symbol rejected

```bash
curl -s -X POST http://localhost:8080/watchlist/entries \
  -H "Authorization: Bearer $PREMIUM_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"symbol": "FAKESYMBOL"}' | jq .
```

Expected: 400 `SYMBOL_NOT_FOUND`.

---

## Flow 7: Remove a stock from watchlist

```bash
curl -s -X DELETE "http://localhost:8080/watchlist/entries/PFBCOLOM" \
  -H "Authorization: Bearer $PREMIUM_TOKEN" | jq .
```

Expected 200:
```json
{
  "symbol": "PFBCOLOM",
  "removedAt": "2026-05-10T15:00:00Z",
  "entryCount": 0,
  "maxEntries": 50
}
```

Attempt to remove again:
```bash
curl -s -X DELETE "http://localhost:8080/watchlist/entries/PFBCOLOM" \
  -H "Authorization: Bearer $PREMIUM_TOKEN" | jq .
```

Expected: 404 `SYMBOL_NOT_IN_WATCHLIST`.

---

## Flow 8: Watchlist limit (max 50 entries)

Add 50 entries, then try a 51st:
```bash
SYMBOLS=("PFBCOLOM" "NUTRESA" "ISA" "ECOPETROL" "CEMARGOS" "GRUPOSURA" "EXITO" "ETB" "PFDAVVNDA" "CLH")
# For a real 50-entry test, use 50 distinct valid symbols

curl -s -X POST http://localhost:8080/watchlist/entries \
  -H "Authorization: Bearer $PREMIUM_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"symbol": "EXTRA_SYMBOL"}' | jq .
```

Expected when at limit: 422 `WATCHLIST_LIMIT_REACHED`.

---

## Flow 9: Watchlist preserved after subscription expiry

```sql
-- Simulate subscription expiry
UPDATE investor
SET subscription_expires_at = NOW() - INTERVAL '1 day'
WHERE id = '<INVESTOR_ID>';
```

```bash
curl -s http://localhost:8080/watchlist \
  -H "Authorization: Bearer $PREMIUM_TOKEN" | jq .
```

Expected: 403 `PREMIUM_REQUIRED` (access blocked).

Verify watchlist data still exists in DB:
```sql
SELECT we.symbol, we.added_at, we.price_at_added
FROM watchlist_entry we
JOIN watchlist w ON w.id = we.watchlist_id
WHERE w.investor_id = '<INVESTOR_ID>';
-- Data must still be present despite expired subscription
```

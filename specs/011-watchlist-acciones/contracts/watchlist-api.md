# API Contract: Watchlist (AB-36)

**Base URL**: `/watchlist`  
**Auth**: Bearer JWT (role: INVESTOR, active PREMIUM subscription)  
**Module**: `market-data`

All endpoints require an active PREMIUM subscription. Non-premium investors receive
HTTP 403 on all watchlist endpoints.

---

## GET /watchlist

Returns the authenticated investor's watchlist with current market prices.

**Response 200 — Watchlist with entries**
```json
{
  "watchlistId": "uuid",
  "investorId": "uuid",
  "entryCount": 2,
  "maxEntries": 50,
  "entries": [
    {
      "symbol": "PFBCOLOM",
      "name": "Bancolombia Preferencial",
      "currentPrice": 39500.00,
      "priceAtAdded": 38200.00,
      "dayChange": 350.00,
      "dayChangePct": 0.89,
      "lastUpdated": "2026-05-10T14:55:00Z",
      "addedAt": "2026-04-15T11:30:00Z"
    },
    {
      "symbol": "NUTRESA",
      "name": "Grupo Nutresa",
      "currentPrice": 68200.00,
      "priceAtAdded": 67000.00,
      "dayChange": -200.00,
      "dayChangePct": -0.29,
      "lastUpdated": "2026-05-10T14:55:00Z",
      "addedAt": "2026-04-20T09:10:00Z"
    }
  ]
}
```

**Response 200 — Empty watchlist**
```json
{
  "watchlistId": "uuid",
  "investorId": "uuid",
  "entryCount": 0,
  "maxEntries": 50,
  "entries": []
}
```

Note: If a stock has no snapshot (e.g., symbol removed from catalog), `currentPrice`,
`dayChange`, `dayChangePct`, and `lastUpdated` will be `null`.

---

## POST /watchlist/entries

Adds a stock symbol to the investor's watchlist.

**Request**
```json
{
  "symbol": "ECOPETROL"
}
```

**Validations**
- `symbol`: not blank, max 20 chars; must exist in the market catalog (StockSnapshot)
- Watchlist must have fewer than 50 entries
- Symbol must not already be in the watchlist

**Response 201**
```json
{
  "symbol": "ECOPETROL",
  "name": "Ecopetrol S.A.",
  "priceAtAdded": 1972.00,
  "addedAt": "2026-05-10T14:30:00Z",
  "entryCount": 3,
  "maxEntries": 50
}
```

**Response 400** — Symbol not found in catalog
```json
{
  "error": "SYMBOL_NOT_FOUND",
  "message": "No stock found with symbol: XYZ"
}
```

**Response 409** — Symbol already in watchlist
```json
{
  "error": "SYMBOL_ALREADY_IN_WATCHLIST",
  "message": "ECOPETROL is already in your watchlist"
}
```

**Response 422** — Watchlist limit reached
```json
{
  "error": "WATCHLIST_LIMIT_REACHED",
  "message": "Watchlist limit reached (max 50 symbols). Remove a symbol before adding a new one."
}
```

---

## DELETE /watchlist/entries/{symbol}

Removes a stock symbol from the investor's watchlist.

**Path Params**
- `symbol` (string): the stock ticker to remove (e.g., `PFBCOLOM`)

**Response 200**
```json
{
  "symbol": "PFBCOLOM",
  "removedAt": "2026-05-10T15:00:00Z",
  "entryCount": 1,
  "maxEntries": 50
}
```

**Response 404** — Symbol not in watchlist
```json
{
  "error": "SYMBOL_NOT_IN_WATCHLIST",
  "message": "PFBCOLOM is not in your watchlist"
}
```

---

## Common Error Responses

| Status | Error Code          | Description                                  |
|--------|---------------------|----------------------------------------------|
| 401    | UNAUTHORIZED        | Missing or invalid JWT                       |
| 403    | FORBIDDEN           | Role is not INVESTOR                         |
| 403    | PREMIUM_REQUIRED    | Investor does not have an active PREMIUM subscription |

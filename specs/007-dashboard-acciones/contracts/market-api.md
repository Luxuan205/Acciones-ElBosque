# API Contract: Market Dashboard (AB-28)

**Base URL**: `/market`  
**Auth**: Bearer JWT (any authenticated role)  
**Module**: `market-data`

---

## GET /market/stocks

Returns a list of all available stocks with current price and day variation.
Supports search by symbol/name and sorting.

**Query Params**
- `search` (optional): substring to search in `symbol` or `name` (case-insensitive)
- `sort` (optional, default `name_asc`): one of `name_asc`, `name_desc`, `dayChangePct_asc`, `dayChangePct_desc`

**Response 200**
```json
{
  "marketOpen": true,
  "stocks": [
    {
      "symbol": "ECOPETROL",
      "name": "Ecopetrol S.A.",
      "currentPrice": 1972.00,
      "previousClose": 1950.00,
      "dayChange": 22.00,
      "dayChangePct": 1.13,
      "volume": 1250000,
      "updatedAt": "2026-05-10T14:55:00Z",
      "stale": false
    },
    {
      "symbol": "PFBCOLOM",
      "name": "Bancolombia Preferencial",
      "currentPrice": 39500.00,
      "previousClose": 39150.00,
      "dayChange": 350.00,
      "dayChangePct": 0.89,
      "volume": 85000,
      "updatedAt": "2026-05-10T14:55:00Z",
      "stale": false
    }
  ]
}
```

When market is closed, `"marketOpen": false` and `"stale": true` on all entries.

**Response 400** — Invalid sort parameter
```json
{
  "error": "VALIDATION_ERROR",
  "message": "sort must be one of: name_asc, name_desc, dayChangePct_asc, dayChangePct_desc"
}
```

---

## GET /market/stocks/{symbol}

Returns detailed information for a single stock.

**Path Params**
- `symbol` (string): stock ticker (e.g., `PFBCOLOM`)

**Response 200**
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

**Response 404** — Symbol not found
```json
{
  "error": "SYMBOL_NOT_FOUND",
  "message": "No stock found with symbol: XYZ"
}
```

---

## GET /market/stocks/{symbol}/intraday

Returns intraday price points (5-minute intervals) for the current trading session.

**Path Params**
- `symbol` (string): stock ticker

**Response 200**
```json
{
  "symbol": "PFBCOLOM",
  "date": "2026-05-10",
  "interval": "5min",
  "points": [
    {
      "timestamp": "2026-05-10T09:00:00-05:00",
      "price": 39150.00,
      "volume": 3200
    },
    {
      "timestamp": "2026-05-10T09:05:00-05:00",
      "price": 39280.00,
      "volume": 4100
    }
  ]
}
```

When market is closed: `"points": []` with a note that data is available only during
active trading sessions.

**Response 404** — Symbol not found
```json
{
  "error": "SYMBOL_NOT_FOUND",
  "message": "No stock found with symbol: XYZ"
}
```

---

## Common Error Responses

| Status | Error Code   | Description               |
|--------|--------------|---------------------------|
| 401    | UNAUTHORIZED | Missing or invalid JWT    |

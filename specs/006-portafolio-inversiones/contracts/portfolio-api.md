# API Contract: Investment Portfolio (AB-27)

**Base URL**: `/portfolio`  
**Auth**: Bearer JWT (role: INVESTOR)  
**Module**: `portfolio`

---

## GET /portfolio/holdings

Returns all open positions in the authenticated investor's portfolio, enriched with
current market prices.

**Response 200 — With positions**
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
    },
    {
      "symbol": "NUTRESA",
      "name": "Grupo Nutresa",
      "quantity": 5,
      "avgBuyPrice": 67500.00,
      "currentPrice": 68200.00,
      "positionValue": 341000.00,
      "pnlAmount": 3500.00,
      "pnlPercent": 1.04,
      "dayChange": -200.00,
      "dayChangePct": -0.29,
      "currency": "COP"
    }
  ],
  "currency": "COP"
}
```

**Response 200 — Empty portfolio**
```json
{
  "investorId": "uuid",
  "positions": [],
  "currency": "COP"
}
```

Note on `currentPrice` fallback: if a stock has no `StockSnapshot` record (e.g., delisted),
`currentPrice` equals `avgBuyPrice`, and `pnlAmount` / `pnlPercent` will be `0.00`.

---

## GET /portfolio/summary

Returns aggregate totals for the investor's portfolio.

**Response 200**
```json
{
  "investorId": "uuid",
  "totalValue": 736000.00,
  "totalCost": 726500.00,
  "totalPnl": 9500.00,
  "totalPnlPct": 1.31,
  "totalDayChange": 5500.00,
  "positionCount": 2,
  "currency": "COP"
}
```

**Response 200 — Empty portfolio**
```json
{
  "investorId": "uuid",
  "totalValue": 0.00,
  "totalCost": 0.00,
  "totalPnl": 0.00,
  "totalPnlPct": 0.00,
  "totalDayChange": 0.00,
  "positionCount": 0,
  "currency": "COP"
}
```

Calculation:
- `totalValue = SUM(positionValue)` where `positionValue = quantity × currentPrice`
- `totalCost = SUM(quantity × avgBuyPrice)`
- `totalPnl = totalValue - totalCost`
- `totalPnlPct = totalPnl / totalCost × 100` (0.00 if `totalCost == 0`)
- `totalDayChange = SUM(dayChange × quantity)`

---

## Common Error Responses

| Status | Error Code   | Description                              |
|--------|--------------|------------------------------------------|
| 401    | UNAUTHORIZED | Missing or invalid JWT                   |
| 403    | FORBIDDEN    | Role is not INVESTOR                     |
| 403    | FORBIDDEN    | Requesting another investor's portfolio  |

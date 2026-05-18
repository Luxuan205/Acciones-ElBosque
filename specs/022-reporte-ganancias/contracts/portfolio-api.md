# API Contract: Portafolio y Reporte de Ganancias (AB-37)

**Base URL**: `/portfolio`
**Auth**: Bearer JWT (role: INVESTOR)
**Module**: `portfolio`

---

## GET /portfolio/positions

Posiciones abiertas actuales del inversionista.

**Response 200**
```json
{
  "positions": [
    { "symbol": "ECOPETROL", "currentQuantity": 100, "avgPurchasePrice": 1920.00, "currentPrice": 1972.00, "unrealizedGain": 5200.00, "unrealizedGainPct": 2.71 },
    { "symbol": "PFBCOLOM", "currentQuantity": 20, "avgPurchasePrice": 38000.00, "currentPrice": 39500.00, "unrealizedGain": 30000.00, "unrealizedGainPct": 3.95 }
  ],
  "totalInvested": 952000.00,
  "totalMarketValue": 987200.00,
  "totalUnrealizedGain": 35200.00
}
```

---

## GET /portfolio/report

Reporte de ganancias y pérdidas filtrado por período.

**Query Params**: `period=MONTH` (`TODAY` | `WEEK` | `MONTH` | `YEAR` | `CUSTOM`)
Para `CUSTOM`: `from=2026-01-01&to=2026-05-13`

**Response 200**
```json
{
  "period": "MONTH",
  "from": "2026-04-13",
  "to": "2026-05-13",
  "totalRealizedGain": 12500.00,
  "positions": [ ... ],
  "transactions": [
    { "transactionType": "SELL", "symbol": "ECOPETROL", "quantity": 50, "executionPrice": 1985.00, "commission": 297.75, "grossAmount": 99250.00, "netAmount": 98952.25, "realizedGain": 3252.25, "executedAt": "2026-05-10T11:30:00Z" }
  ]
}
```

---

## GET /portfolio/report/export

Exporta el reporte como CSV.

**Query Params**: igual que GET /portfolio/report

**Response 200**: `Content-Type: text/csv`, `Content-Disposition: attachment; filename="portfolio-report.csv"`

---

## Common Error Responses

| Status | Error Code    | Description               |
|--------|---------------|---------------------------|
| 401    | UNAUTHORIZED  | JWT ausente o inválido    |
| 400    | INVALID_PERIOD| Período de fechas inválido|

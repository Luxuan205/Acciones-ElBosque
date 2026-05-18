# Quickstart: AB-37 — Reporte de Ganancias y Pérdidas

**Prerequisites**
- Service running on port 8080
- JWT token: `$INVESTOR_TOKEN` (role: INVESTOR, user: investor@test.com)
- Portfolio with at least 2 open positions (ECOPETROL, PFBCOLOM) and at least one past SELL transaction

---

## Flow 1: View current open positions with unrealized gains

```bash
curl -s -X GET http://localhost:8080/portfolio/positions \
  -H "Authorization: Bearer $INVESTOR_TOKEN" | jq .
```

Expected 200:
```json
{
  "positions": [
    {
      "symbol": "ECOPETROL",
      "currentQuantity": 100,
      "avgPurchasePrice": 1920.00,
      "currentPrice": 1972.00,
      "unrealizedGain": 5200.00,
      "unrealizedGainPct": 2.71
    },
    {
      "symbol": "PFBCOLOM",
      "currentQuantity": 20,
      "avgPurchasePrice": 38000.00,
      "currentPrice": 39500.00,
      "unrealizedGain": 30000.00,
      "unrealizedGainPct": 3.95
    }
  ],
  "totalInvested": 952000.00,
  "totalMarketValue": 987200.00,
  "totalUnrealizedGain": 35200.00
}
```

DB verification:
```sql
SELECT symbol, quantity, avg_purchase_price
FROM portfolio_positions
WHERE investor_id = 42 AND status = 'OPEN';
```

---

## Flow 2: Generate P&L report for the current month

```bash
curl -s -X GET "http://localhost:8080/portfolio/report?period=MONTH" \
  -H "Authorization: Bearer $INVESTOR_TOKEN" | jq .
```

Expected 200:
```json
{
  "period": "MONTH",
  "from": "2026-04-14",
  "to": "2026-05-14",
  "totalRealizedGain": 12500.00,
  "positions": [
    {
      "symbol": "ECOPETROL",
      "currentQuantity": 100,
      "avgPurchasePrice": 1920.00,
      "currentPrice": 1972.00,
      "unrealizedGain": 5200.00,
      "unrealizedGainPct": 2.71
    }
  ],
  "transactions": [
    {
      "transactionType": "SELL",
      "symbol": "ECOPETROL",
      "quantity": 50,
      "executionPrice": 1985.00,
      "commission": 297.75,
      "grossAmount": 99250.00,
      "netAmount": 98952.25,
      "realizedGain": 3252.25,
      "executedAt": "2026-05-10T11:30:00Z"
    }
  ]
}
```

---

## Flow 3: Generate P&L report for today

```bash
curl -s -X GET "http://localhost:8080/portfolio/report?period=TODAY" \
  -H "Authorization: Bearer $INVESTOR_TOKEN" | jq .
```

Expected 200:
```json
{
  "period": "TODAY",
  "from": "2026-05-14",
  "to": "2026-05-14",
  "totalRealizedGain": 0.00,
  "positions": [],
  "transactions": []
}
```

Note: An empty report is valid when there are no transactions on the current day. The response body must not be null.

---

## Flow 4: Generate P&L report for a custom date range

```bash
curl -s -X GET "http://localhost:8080/portfolio/report?period=CUSTOM&from=2026-01-01&to=2026-03-31" \
  -H "Authorization: Bearer $INVESTOR_TOKEN" | jq .
```

Expected 200:
```json
{
  "period": "CUSTOM",
  "from": "2026-01-01",
  "to": "2026-03-31",
  "totalRealizedGain": 47800.00,
  "positions": [],
  "transactions": [
    {
      "transactionType": "SELL",
      "symbol": "NUTRESA",
      "quantity": 30,
      "executionPrice": 41200.00,
      "commission": 370.80,
      "grossAmount": 1236000.00,
      "netAmount": 1235629.20,
      "realizedGain": 47800.00,
      "executedAt": "2026-02-15T09:45:00Z"
    }
  ]
}
```

DB verification:
```sql
SELECT symbol, quantity, execution_price, commission, realized_gain, executed_at
FROM trade_transactions
WHERE investor_id = 42
  AND transaction_type = 'SELL'
  AND executed_at BETWEEN '2026-01-01' AND '2026-03-31 23:59:59';
```

---

## Flow 5: Reject invalid period parameter

```bash
curl -s -X GET "http://localhost:8080/portfolio/report?period=YESTERDAY" \
  -H "Authorization: Bearer $INVESTOR_TOKEN" | jq .
```

Expected 400:
```json
{
  "error": "INVALID_PERIOD",
  "message": "El período 'YESTERDAY' no es válido. Use: TODAY, WEEK, MONTH, YEAR, CUSTOM."
}
```

---

## Flow 6: Reject CUSTOM period request missing date parameters

```bash
curl -s -X GET "http://localhost:8080/portfolio/report?period=CUSTOM&from=2026-01-01" \
  -H "Authorization: Bearer $INVESTOR_TOKEN" | jq .
```

Expected 400:
```json
{
  "error": "INVALID_PERIOD",
  "message": "El parámetro 'to' es requerido cuando el período es CUSTOM."
}
```

---

## Flow 7: Export P&L report as CSV

```bash
curl -s -X GET "http://localhost:8080/portfolio/report/export?period=MONTH" \
  -H "Authorization: Bearer $INVESTOR_TOKEN" \
  -o portfolio-report.csv \
  -w "%{http_code} | Content-Type: %{content_type}"
```

Expected 200 with headers:
```
Content-Type: text/csv
Content-Disposition: attachment; filename="portfolio-report.csv"
```

Expected CSV preview (first lines):
```
transactionType,symbol,quantity,executionPrice,commission,grossAmount,netAmount,realizedGain,executedAt
SELL,ECOPETROL,50,1985.00,297.75,99250.00,98952.25,3252.25,2026-05-10T11:30:00Z
```

---

## Flow 8: Reject unauthenticated request

```bash
curl -s -X GET "http://localhost:8080/portfolio/positions" | jq .
```

Expected 401:
```json
{
  "error": "UNAUTHORIZED",
  "message": "JWT ausente o inválido."
}
```

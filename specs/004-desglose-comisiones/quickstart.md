# Quickstart: AB-25 — Visualización y Desglose de Comisiones

**Prerequisites**
- order running on port 8082
- Valid JWT for an investor (STANDARD subscription `$TOKEN_STD`, PREMIUM subscription `$TOKEN_PRE`)
- Replace tokens with your Bearer tokens

---

## Flow 1: Preview order — STANDARD subscriber

```bash
curl -s -X POST http://localhost:8080/orders/preview \
  -H "Authorization: Bearer $TOKEN_STD" \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "PFBCOLOM",
    "quantity": 10,
    "orderType": "BUY",
    "unitPrice": 39500.00
  }' | jq .
```

Expected 200:
```json
{
  "symbol": "PFBCOLOM",
  "quantity": 10,
  "orderType": "BUY",
  "unitPrice": 39500.00,
  "grossValue": 395000.00,
  "subscriptionType": "STANDARD",
  "ratePercent": 1.50,
  "commissionAmount": 5925.00,
  "netTotal": 400925.00
}
```

Verify calculation: `395000 × 0.015 = 5925.00`; `395000 + 5925 = 400925.00`.

---

## Flow 2: Preview order — PREMIUM subscriber

```bash
curl -s -X POST http://localhost:8080/orders/preview \
  -H "Authorization: Bearer $TOKEN_PRE" \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "PFBCOLOM",
    "quantity": 10,
    "orderType": "BUY",
    "unitPrice": 39500.00
  }' | jq .
```

Expected 200: `"ratePercent": 0.80`, `"commissionAmount": 3160.00`, `"netTotal": 398160.00`.

Verify: `395000 × 0.008 = 3160.00`; `395000 + 3160 = 398160.00`.

---

## Flow 3: Preview SELL order (commission deducted from proceeds)

```bash
curl -s -X POST http://localhost:8080/orders/preview \
  -H "Authorization: Bearer $TOKEN_STD" \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "PFBCOLOM",
    "quantity": 10,
    "orderType": "SELL",
    "unitPrice": 39500.00
  }' | jq .
```

Expected: `"netTotal": 389075.00` (`395000 - 5925`).

---

## Flow 4: Confirm order (verify anti-tampering)

Place the actual order — server recalculates, does not use client preview figures:

```bash
curl -s -X POST http://localhost:8080/orders \
  -H "Authorization: Bearer $TOKEN_STD" \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "PFBCOLOM",
    "quantity": 10,
    "orderType": "BUY",
    "unitPrice": 39500.00
  }' | jq '{commissionAmt: .commissionAmt, netTotal: .netTotal}'
```

Expected: `commissionAmt = 5925.00`, `netTotal = 400925.00` — matches the preview.

---

## Flow 5: Validation errors

Missing required field:
```bash
curl -s -X POST http://localhost:8080/orders/preview \
  -H "Authorization: Bearer $TOKEN_STD" \
  -H "Content-Type: application/json" \
  -d '{"symbol":"PFBCOLOM","quantity":0,"orderType":"BUY","unitPrice":39500.00}' | jq .
```

Expected: 400 `VALIDATION_ERROR` (quantity must be positive).

---

## Flow 6: Verify commission rates in DB

```sql
SELECT subscription_type, rate_percent FROM commission_rate ORDER BY rate_percent DESC;
-- Should show: STANDARD 1.50, PREMIUM 0.80
```

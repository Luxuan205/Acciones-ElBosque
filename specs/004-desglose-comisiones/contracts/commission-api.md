# API Contract: Commission Preview (AB-25)

**Base URL**: `/orders`  
**Auth**: Bearer JWT (role: INVESTOR or BROKER)  
**Module**: `order`

---

## POST /orders/preview

Calculates and returns the full cost breakdown for a prospective order.
The response is informational only — no order is created and nothing is persisted.

The commission rate is determined by the investor's subscription type extracted from
the JWT. The client does NOT provide the rate.

**Request**
```json
{
  "symbol": "PFBCOLOM",
  "quantity": 10,
  "orderType": "BUY",
  "unitPrice": 39500.00
}
```

**Validations**
- `symbol`: not blank
- `quantity`: positive integer ≥ 1
- `orderType`: `BUY` or `SELL`
- `unitPrice`: positive decimal, scale ≤ 2

**Response 200 — Preview (STANDARD subscriber, BUY)**
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

**Response 200 — Preview (PREMIUM subscriber, BUY)**
```json
{
  "symbol": "PFBCOLOM",
  "quantity": 10,
  "orderType": "BUY",
  "unitPrice": 39500.00,
  "grossValue": 395000.00,
  "subscriptionType": "PREMIUM",
  "ratePercent": 0.80,
  "commissionAmount": 3160.00,
  "netTotal": 398160.00
}
```

**Response 200 — Preview (SELL)**
For SELL orders, commissions are deducted FROM the proceeds:
```json
{
  "symbol": "PFBCOLOM",
  "quantity": 10,
  "orderType": "SELL",
  "unitPrice": 39500.00,
  "grossValue": 395000.00,
  "subscriptionType": "STANDARD",
  "ratePercent": 1.50,
  "commissionAmount": 5925.00,
  "netTotal": 389075.00
}
```

Note: `netTotal` for SELL = `grossValue - commissionAmount` (investor receives less).

**Response 400** — Validation error
```json
{
  "error": "VALIDATION_ERROR",
  "message": "quantity must be a positive integer"
}
```

---

## Calculation Formulas

| Field             | Formula                                              |
|-------------------|------------------------------------------------------|
| `grossValue`      | `quantity × unitPrice`                               |
| `commissionAmount`| `grossValue × (ratePercent / 100)` (HALF_UP, 2 dp)  |
| `netTotal` (BUY)  | `grossValue + commissionAmount`                      |
| `netTotal` (SELL) | `grossValue - commissionAmount`                      |

Commission rates (from `commission_rate` table):
- `STANDARD`: 1.50%
- `PREMIUM`: 0.80%

---

## Important Note on Order Confirmation

When the investor proceeds to create the order (`POST /orders`), the server **recalculates**
the commission using the same formulas. The preview figures shown here are not passed back
by the client — the server does not trust the client's preview data. This guarantees that
`amount charged == amount displayed` (SC-002).

---

## Common Error Responses

| Status | Error Code   | Description                  |
|--------|--------------|------------------------------|
| 401    | UNAUTHORIZED | Missing or invalid JWT       |
| 403    | FORBIDDEN    | JWT role not allowed         |

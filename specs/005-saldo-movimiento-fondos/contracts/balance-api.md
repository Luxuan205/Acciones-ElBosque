# API Contract: Balance & Fund Movements (AB-26)

**Base URL**: `/portfolio`  
**Auth**: Bearer JWT (role: INVESTOR)  
**Module**: `portfolio`

---

## GET /portfolio/balance

Returns the authenticated investor's current account balance summary.

**Response 200**
```json
{
  "investorId": "uuid",
  "totalBalance": 5000000.00,
  "reservedBalance": 400925.00,
  "availableBalance": 4599075.00,
  "currency": "COP",
  "updatedAt": "2026-05-10T14:25:00Z"
}
```

- `totalBalance`: total funds in the account
- `reservedBalance`: sum of `netTotal` for orders in status `ACTIVE` or `QUEUED`
- `availableBalance`: `totalBalance - reservedBalance`

---

## GET /portfolio/movements

Returns a paginated list of fund movements with optional date-range filter.

**Query Params**
- `from` (optional): ISO-8601 date — `2026-01-01`
- `to` (optional): ISO-8601 date — `2026-05-10` (inclusive, end of day)
- `page` (optional, default 0): page number (0-based)
- `size` (optional, default 20, max 20): page size

**Response 200**
```json
{
  "content": [
    {
      "movementId": "uuid",
      "type": "PURCHASE",
      "amount": -400925.00,
      "balanceAfter": 4599075.00,
      "currency": "COP",
      "description": "Buy 10 PFBCOLOM @ 39500.00",
      "orderId": "uuid",
      "createdAt": "2026-05-10T14:20:00Z"
    },
    {
      "movementId": "uuid",
      "type": "DEPOSIT",
      "amount": 5000000.00,
      "balanceAfter": 5000000.00,
      "currency": "COP",
      "description": "Initial deposit",
      "orderId": null,
      "createdAt": "2026-01-15T10:00:00Z"
    }
  ],
  "totalElements": 2,
  "totalPages": 1,
  "page": 0,
  "size": 20
}
```

Movement types and their sign convention:

| type         | amount sign | description                      |
|--------------|------------|----------------------------------|
| `DEPOSIT`    | positive   | Funds added to account           |
| `WITHDRAWAL` | negative   | Funds withdrawn                  |
| `PURCHASE`   | negative   | Deducted for stock buy           |
| `SALE`       | positive   | Credited from stock sale         |
| `COMMISSION` | negative   | Commission fee (charged separately) |

**Response 400** — Invalid date format
```json
{
  "error": "VALIDATION_ERROR",
  "message": "from must be a valid ISO-8601 date (yyyy-MM-dd)"
}
```

**Response 400** — `from` is after `to`
```json
{
  "error": "VALIDATION_ERROR",
  "message": "from must not be after to"
}
```

---

## Common Error Responses

| Status | Error Code   | Description                              |
|--------|--------------|------------------------------------------|
| 401    | UNAUTHORIZED | Missing or invalid JWT                   |
| 403    | FORBIDDEN    | Role is not INVESTOR                     |
| 403    | FORBIDDEN    | Requesting another investor's data       |

# API Contract: Limit Order (AB-21)

**Base URL**: `/orders`
**Auth**: Bearer JWT (role: INVESTOR)
**Module**: `orders`

---

## POST /orders/limit/buy

**Request**
```json
{ "symbol": "ECOPETROL", "quantity": 100, "limitPrice": 1900.00, "expiresAt": "2026-06-01" }
```

**Response 201**
```json
{
  "orderId": 1050,
  "status": "PENDING",
  "orderType": "LIMIT_BUY",
  "symbol": "ECOPETROL",
  "quantity": 100,
  "limitPrice": 1900.00,
  "expiresAt": "2026-06-01",
  "breakdown": { "estimatedPrice": 1900.00, "commission": 570.00, "totalReserved": 190570.00 },
  "createdAt": "2026-05-13T10:00:00Z"
}
```

---

## POST /orders/limit/sell

**Request**
```json
{ "symbol": "PFBCOLOM", "quantity": 10, "limitPrice": 42000.00, "expiresAt": null }
```

**Response 201**
```json
{
  "orderId": 1051,
  "status": "PENDING",
  "orderType": "LIMIT_SELL",
  "symbol": "PFBCOLOM",
  "quantity": 10,
  "limitPrice": 42000.00,
  "expiresAt": null,
  "validity": "GTC",
  "breakdown": { "estimatedPrice": 42000.00, "commission": 1260.00, "netEstimated": 418740.00 },
  "createdAt": "2026-05-13T10:05:00Z"
}
```

---

## Common Error Responses

| Status | Error Code           | Description                          |
|--------|----------------------|--------------------------------------|
| 401    | UNAUTHORIZED         | JWT ausente o inválido               |
| 422    | INSUFFICIENT_BALANCE | Saldo insuficiente (para LIMIT_BUY)  |
| 422    | INSUFFICIENT_TITLES  | Títulos insuficientes (para LIMIT_SELL) |
| 400    | INVALID_LIMIT_PRICE  | Precio límite inválido (≤ 0)         |

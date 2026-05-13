# API Contract: Order Queue (AB-24)

**Base URL**: `/orders`  
**Auth**: Bearer JWT (role: INVESTOR)  
**Module**: `order`

---

## POST /orders

Places a new order. If the market is closed, the order is persisted with status `QUEUED`;
if open, with status `ACTIVE`.

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
- `symbol`: not blank, max 20 chars
- `quantity`: positive integer ≥ 1
- `orderType`: `BUY` or `SELL`
- `unitPrice`: positive, scale ≤ 2
- Investor must not already have 10 QUEUED orders (HTTP 422)

**Response 201 — Order created (market closed → QUEUED)**
```json
{
  "orderId": "uuid",
  "symbol": "PFBCOLOM",
  "quantity": 10,
  "orderType": "BUY",
  "status": "QUEUED",
  "unitPrice": 39500.00,
  "grossValue": 395000.00,
  "commissionRate": 1.50,
  "commissionAmt": 5925.00,
  "netTotal": 400925.00,
  "createdAt": "2026-05-10T20:00:00Z",
  "processedAt": null
}
```

**Response 201 — Order created (market open → ACTIVE)**
Same body with `"status": "ACTIVE"`.

**Response 400** — Validation error
```json
{
  "error": "VALIDATION_ERROR",
  "message": "quantity must be a positive integer"
}
```

**Response 422** — Queue limit reached
```json
{
  "error": "QUEUE_LIMIT_REACHED",
  "message": "Maximum of 10 queued orders per investor. Cancel an existing order to place a new one."
}
```

---

## DELETE /orders/{orderId}/cancel

Cancels an order. Only orders in status `QUEUED` can be cancelled.

**Path Params**
- `orderId` (UUID): the order to cancel

**Response 200**
```json
{
  "orderId": "uuid",
  "status": "CANCELLED",
  "cancelledAt": "2026-05-10T20:05:00Z"
}
```

**Response 404** — Order not found or not owned by requester
```json
{
  "error": "ORDER_NOT_FOUND",
  "message": "Order not found"
}
```

**Response 409** — Order is not in QUEUED status
```json
{
  "error": "ORDER_NOT_CANCELLABLE",
  "message": "Only QUEUED orders can be cancelled. Current status: ACTIVE"
}
```

---

## GET /orders

Lists the authenticated investor's orders, optionally filtered by status.

**Query Params**
- `status` (optional): `QUEUED`, `ACTIVE`, `EXECUTED`, `FAILED`, `CANCELLED`
- `page` (optional, default 0): page number
- `size` (optional, default 20): page size

**Response 200**
```json
{
  "content": [
    {
      "orderId": "uuid",
      "symbol": "PFBCOLOM",
      "quantity": 10,
      "orderType": "BUY",
      "status": "QUEUED",
      "netTotal": 400925.00,
      "createdAt": "2026-05-10T20:00:00Z",
      "processedAt": null
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "page": 0,
  "size": 20
}
```

---

## Common Error Responses

| Status | Error Code   | Description                   |
|--------|--------------|-------------------------------|
| 401    | UNAUTHORIZED | Missing or invalid JWT        |
| 403    | FORBIDDEN    | Role is not INVESTOR          |

# API Contract: Broker Order Management (AB-32)

**Base URL**: `/orders`  
**Auth**: Bearer JWT (role: BROKER)  
**Module**: `order`

---

## POST /orders/broker

Places an order on behalf of an assigned client. The broker must be assigned to the
client; otherwise the request is rejected. The order is placed using the client's
subscription type for commission calculation.

**Request**
```json
{
  "clientId": "uuid",
  "symbol": "PFBCOLOM",
  "quantity": 10,
  "orderType": "BUY",
  "unitPrice": 39500.00
}
```

**Validations**
- `clientId`: must be in broker's assigned clients list (checked in-process via BrokerAssignmentValidator)
- `symbol`: not blank, max 20 chars
- `quantity`: positive integer ≥ 1
- `orderType`: `BUY` or `SELL`
- `unitPrice`: positive decimal, scale ≤ 2
- Client must have sufficient available balance for BUY orders
- Client must not already have 10 QUEUED orders

**Response 201 — Order placed (market closed → QUEUED)**
```json
{
  "orderId": "uuid",
  "clientId": "uuid",
  "clientName": "Carlos Rodríguez Pérez",
  "brokerId": "uuid",
  "brokerName": "Felipe Torres Herrera",
  "symbol": "PFBCOLOM",
  "quantity": 10,
  "orderType": "BUY",
  "status": "QUEUED",
  "unitPrice": 39500.00,
  "grossValue": 395000.00,
  "commissionRate": 1.50,
  "commissionAmt": 5925.00,
  "netTotal": 400925.00,
  "createdAt": "2026-05-10T20:00:00Z"
}
```

**Response 403** — Client not assigned to this broker
```json
{
  "error": "ACCESS_DENIED",
  "message": "Client uuid is not assigned to this broker"
}
```

**Response 400** — Insufficient client funds (BUY)
```json
{
  "error": "INSUFFICIENT_FUNDS",
  "message": "Client's available balance (COP 250,000.00) is insufficient for this order (COP 400,925.00)"
}
```

**Response 422** — Client's queue limit reached
```json
{
  "error": "QUEUE_LIMIT_REACHED",
  "message": "Client already has 10 queued orders. Cancel one before placing a new order."
}
```

---

## GET /orders/broker/history

Returns the paginated history of orders placed by the authenticated broker,
optionally filtered by client and date range.

**Query Params**
- `clientId` (optional): filter by specific client (UUID)
- `from` (optional): ISO-8601 date start — `2026-01-01`
- `to` (optional): ISO-8601 date end — `2026-05-10` (inclusive)
- `page` (optional, default 0)
- `size` (optional, default 20)

**Response 200**
```json
{
  "brokerId": "uuid",
  "content": [
    {
      "orderId": "uuid",
      "clientId": "uuid",
      "clientName": "Carlos Rodríguez Pérez",
      "brokerName": "Felipe Torres Herrera",
      "symbol": "PFBCOLOM",
      "quantity": 10,
      "orderType": "BUY",
      "status": "EXECUTED",
      "netTotal": 400925.00,
      "createdAt": "2026-05-08T10:15:00Z",
      "processedAt": "2026-05-09T09:02:00Z"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "page": 0,
  "size": 20
}
```

**Response 400** — Invalid date range
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
| 403    | FORBIDDEN    | Role is not BROKER                       |
| 403    | ACCESS_DENIED| Client not assigned to requesting broker |

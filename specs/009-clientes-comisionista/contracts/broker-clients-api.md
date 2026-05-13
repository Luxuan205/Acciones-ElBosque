# API Contract: Broker Client Management (AB-31)

**Base URL**: `/brokers/me`  
**Auth**: Bearer JWT (role: BROKER)  
**Module**: `auth-security-service`

---

## GET /brokers/me/clients

Returns the list of clients assigned to the authenticated broker. Supports search
by name and filtering by account status.

**Query Params**
- `search` (optional): substring matched against `fullName` (case-insensitive)
- `status` (optional): `ACTIVE`, `INACTIVE`, `PENDING`
- `page` (optional, default 0)
- `size` (optional, default 20)

**Response 200**
```json
{
  "brokerId": "uuid",
  "content": [
    {
      "investorId": "uuid",
      "fullName": "Carlos Rodríguez Pérez",
      "email": "carlos@example.com",
      "accountStatus": "ACTIVE",
      "availableBalance": 4599075.00,
      "activeOrdersCount": 2,
      "assignedAt": "2026-01-20T09:00:00Z"
    },
    {
      "investorId": "uuid",
      "fullName": "Ana Martínez López",
      "email": "ana@example.com",
      "accountStatus": "ACTIVE",
      "availableBalance": 12500000.00,
      "activeOrdersCount": 0,
      "assignedAt": "2026-02-05T14:30:00Z"
    }
  ],
  "totalElements": 2,
  "totalPages": 1,
  "page": 0,
  "size": 20
}
```

**Response 200 — No clients assigned**
```json
{
  "brokerId": "uuid",
  "content": [],
  "totalElements": 0,
  "totalPages": 0,
  "page": 0,
  "size": 20
}
```

**Response 400** — Invalid status value
```json
{
  "error": "VALIDATION_ERROR",
  "message": "status must be one of: ACTIVE, INACTIVE, PENDING"
}
```

---

## GET /brokers/me/clients/{investorId}

Returns detailed information about one of the broker's assigned clients,
including portfolio summary and recent orders.

**Path Params**
- `investorId` (UUID): the client's investor ID

**Response 200**
```json
{
  "investorId": "uuid",
  "fullName": "Carlos Rodríguez Pérez",
  "email": "carlos@example.com",
  "phone": "+57 315 678 9012",
  "accountStatus": "ACTIVE",
  "totalBalance": 5000000.00,
  "availableBalance": 4599075.00,
  "assignedAt": "2026-01-20T09:00:00Z",
  "portfolioSummary": {
    "totalValue": 736000.00,
    "totalPnl": 9500.00,
    "totalPnlPct": 1.31,
    "positionCount": 2
  },
  "recentOrders": [
    {
      "orderId": "uuid",
      "symbol": "PFBCOLOM",
      "quantity": 10,
      "orderType": "BUY",
      "status": "EXECUTED",
      "netTotal": 400925.00,
      "createdAt": "2026-05-08T10:15:00Z"
    }
  ]
}
```

Note: `recentOrders` shows the last 5 orders. An empty array is returned if the client
has placed no orders.

**Response 403** — Client not assigned to this broker
```json
{
  "error": "ACCESS_DENIED",
  "message": "Client is not assigned to this broker"
}
```

**Response 404** — Investor not found
```json
{
  "error": "INVESTOR_NOT_FOUND",
  "message": "No investor found with the given ID"
}
```

---

## Common Error Responses

| Status | Error Code   | Description                            |
|--------|--------------|----------------------------------------|
| 401    | UNAUTHORIZED | Missing or invalid JWT                 |
| 403    | FORBIDDEN    | Role is not BROKER                     |
| 403    | ACCESS_DENIED| Requested client not assigned to broker|

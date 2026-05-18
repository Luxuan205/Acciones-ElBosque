# API Contract: Stop-Loss y Take-Profit (AB-22)

**Base URL**: `/orders/conditional`
**Auth**: Bearer JWT (role: INVESTOR)
**Module**: `orders`

---

## POST /orders/conditional/stop-loss

**Request**
```json
{ "symbol": "ECOPETROL", "quantity": 50, "triggerPrice": 1850.00 }
```

**Response 201**
```json
{ "id": 201, "type": "STOP_LOSS", "symbol": "ECOPETROL", "quantity": 50, "triggerPrice": 1850.00, "status": "ACTIVE", "ocoPartnerId": null, "createdAt": "2026-05-13T11:00:00Z" }
```

---

## POST /orders/conditional/take-profit

**Request**
```json
{ "symbol": "ECOPETROL", "quantity": 50, "triggerPrice": 2200.00, "stopLossId": 201 }
```

**Response 201**
```json
{ "id": 202, "type": "TAKE_PROFIT", "symbol": "ECOPETROL", "quantity": 50, "triggerPrice": 2200.00, "status": "ACTIVE", "ocoPartnerId": 201, "createdAt": "2026-05-13T11:01:00Z" }
```

---

## GET /orders/conditional

Lista todas las órdenes condicionales activas del inversionista.

**Response 200**
```json
[
  { "id": 201, "type": "STOP_LOSS", "symbol": "ECOPETROL", "quantity": 50, "triggerPrice": 1850.00, "status": "ACTIVE", "ocoPartnerId": 202 },
  { "id": 202, "type": "TAKE_PROFIT", "symbol": "ECOPETROL", "quantity": 50, "triggerPrice": 2200.00, "status": "ACTIVE", "ocoPartnerId": 201 }
]
```

---

## PUT /orders/conditional/{id}

Modifica el precio de activación de una orden condicional activa.

**Request**: `{ "triggerPrice": 1880.00 }`

**Response 200**: ConditionalOrderResponse actualizado

---

## DELETE /orders/conditional/{id}

Cancela una orden condicional activa.

**Response 200**: `{ "id": 201, "status": "CANCELLED", "cancelledAt": "2026-05-13T12:00:00Z" }`

---

## Common Error Responses

| Status | Error Code           | Description                                    |
|--------|----------------------|------------------------------------------------|
| 404    | NOT_FOUND            | Orden condicional no encontrada o no del usuario |
| 409    | ALREADY_TRIGGERED    | No se puede modificar una orden ya activada    |
| 422    | NO_POSITION          | El usuario no tiene posición en ese símbolo    |

# API Contract: Cancelación de Orden (AB-23)

**Base URL**: `/orders`
**Auth**: Bearer JWT (role: INVESTOR)
**Module**: `orders`

---

## DELETE /orders/{orderId}

Cancela una orden activa (PENDING o QUEUED) del inversionista autenticado.

**Response 200 — Cancelación exitosa**
```json
{
  "orderId": 1044,
  "previousStatus": "PENDING",
  "newStatus": "CANCELLED",
  "resourcesReleased": "BALANCE",
  "amountReleased": 19779.16,
  "titlesReleased": null,
  "cancelledAt": "2026-05-13T15:00:00Z"
}
```

**Response 409 — Orden ya ejecutada**
```json
{ "error": "ORDER_NOT_CANCELLABLE", "currentStatus": "EXECUTED", "message": "La orden ya fue ejecutada y no puede cancelarse." }
```

**Response 404 — Orden no encontrada o no pertenece al usuario**
```json
{ "error": "ORDER_NOT_FOUND", "message": "Orden no encontrada." }
```

---

## DELETE /orders?bulk=true

Cancela todas las órdenes activas (PENDING y QUEUED) del inversionista.

**Response 200**
```json
{
  "totalRequested": 5,
  "totalCancelled": 4,
  "totalFailed": 1,
  "cancelled": [ { "orderId": 1044, "newStatus": "CANCELLED", "amountReleased": 19779.16 } ],
  "failed": [ { "orderId": 1030, "currentStatus": "EXECUTED", "reason": "ORDER_NOT_CANCELLABLE" } ]
}
```

---

## Common Error Responses

| Status | Error Code           | Description                           |
|--------|----------------------|---------------------------------------|
| 401    | UNAUTHORIZED         | JWT ausente o inválido                |
| 404    | ORDER_NOT_FOUND      | Orden no encontrada                   |
| 409    | ORDER_NOT_CANCELLABLE| La orden no está en estado cancelable |

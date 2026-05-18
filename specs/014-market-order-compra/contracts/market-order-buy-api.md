# API Contract: Market Order de Compra (AB-19)

**Base URL**: `/orders`
**Auth**: Bearer JWT (role: INVESTOR)
**Module**: `orders`

---

## GET /orders/market/buy/preview

Vista previa del desglose de comisiones antes de confirmar.

**Query Params**: `symbol=ECOPETROL&quantity=10`

**Response 200**
```json
{
  "symbol": "ECOPETROL",
  "quantity": 10,
  "estimatedPrice": 1972.00,
  "commission": 59.16,
  "totalEstimated": 19779.16,
  "marketOpen": true
}
```

**Response 404 — Símbolo no encontrado**
```json
{ "error": "SYMBOL_NOT_FOUND", "message": "No se encontró la acción: XYZ" }
```

---

## POST /orders/market/buy

Confirma y coloca la market order de compra.

**Request**
```json
{ "symbol": "ECOPETROL", "quantity": 10 }
```

**Response 201 — Orden creada (mercado abierto)**
```json
{
  "orderId": 1042,
  "status": "PENDING",
  "symbol": "ECOPETROL",
  "quantity": 10,
  "breakdown": {
    "estimatedPrice": 1972.00,
    "quantity": 10,
    "commission": 59.16,
    "totalEstimated": 19779.16
  },
  "createdAt": "2026-05-13T14:30:00Z"
}
```

**Response 201 — Orden encolada (mercado cerrado)**
```json
{
  "orderId": 1043,
  "status": "QUEUED",
  "symbol": "ECOPETROL",
  "quantity": 10,
  "breakdown": { "estimatedPrice": 1972.00, "quantity": 10, "commission": 59.16, "totalEstimated": 19779.16 },
  "message": "El mercado está cerrado. La orden se ejecutará en la próxima apertura.",
  "createdAt": "2026-05-13T20:00:00Z"
}
```

**Response 422 — Saldo insuficiente**
```json
{ "error": "INSUFFICIENT_BALANCE", "message": "Saldo insuficiente para cubrir el total de la compra." }
```

---

## Common Error Responses

| Status | Error Code           | Description                             |
|--------|----------------------|-----------------------------------------|
| 401    | UNAUTHORIZED         | JWT ausente o inválido                  |
| 403    | FORBIDDEN            | Rol no es INVESTOR                      |
| 404    | SYMBOL_NOT_FOUND     | Símbolo no encontrado en el catálogo    |
| 422    | INSUFFICIENT_BALANCE | Saldo disponible insuficiente           |

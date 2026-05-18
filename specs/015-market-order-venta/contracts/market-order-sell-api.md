# API Contract: Market Order de Venta (AB-20)

**Base URL**: `/orders`
**Auth**: Bearer JWT (role: INVESTOR)
**Module**: `orders`

---

## GET /orders/market/sell/preview

Vista previa del desglose antes de confirmar la venta.

**Query Params**: `symbol=PFBCOLOM&quantity=5`

**Response 200**
```json
{
  "symbol": "PFBCOLOM",
  "quantity": 5,
  "estimatedPrice": 39500.00,
  "commission": 592.50,
  "netAmount": 196907.50,
  "availableTitles": 20,
  "marketOpen": true
}
```

---

## POST /orders/market/sell

Confirma y coloca la market order de venta.

**Request**
```json
{ "symbol": "PFBCOLOM", "quantity": 5 }
```

**Response 201 — Orden creada (mercado abierto)**
```json
{
  "orderId": 1044,
  "status": "PENDING",
  "symbol": "PFBCOLOM",
  "quantity": 5,
  "breakdown": {
    "estimatedPrice": 39500.00,
    "quantity": 5,
    "commission": 592.50,
    "netAmount": 196907.50
  },
  "createdAt": "2026-05-13T14:45:00Z"
}
```

**Response 422 — Títulos insuficientes**
```json
{ "error": "INSUFFICIENT_TITLES", "message": "No tiene suficientes títulos disponibles de PFBCOLOM." }
```

---

## Common Error Responses

| Status | Error Code          | Description                            |
|--------|---------------------|----------------------------------------|
| 401    | UNAUTHORIZED        | JWT ausente o inválido                 |
| 403    | FORBIDDEN           | Rol no es INVESTOR                     |
| 404    | SYMBOL_NOT_FOUND    | Símbolo no encontrado en el catálogo   |
| 422    | INSUFFICIENT_TITLES | Títulos disponibles insuficientes      |

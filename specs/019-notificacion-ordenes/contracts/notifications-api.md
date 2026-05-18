# API Contract: Notificaciones de Órdenes (AB-33)

**Base URL**: `/notifications`
**Auth**: Bearer JWT (role: INVESTOR)
**Module**: `notifications`

---

## GET /notifications

Historial de notificaciones del inversionista autenticado.

**Query Params**: `page=0&size=20&eventType=ORDER_EXECUTED` (opcionales)

**Response 200**
```json
{
  "content": [
    {
      "id": 5001,
      "eventType": "ORDER_EXECUTED",
      "channel": "EMAIL",
      "subject": "Tu orden #1042 fue ejecutada",
      "status": "SENT",
      "referenceId": 1042,
      "createdAt": "2026-05-13T14:32:00Z"
    }
  ],
  "totalElements": 42,
  "page": 0,
  "size": 20
}
```

---

## GET /notifications/{id}

Detalle de una notificación específica.

**Response 200**
```json
{
  "id": 5001,
  "eventType": "ORDER_EXECUTED",
  "channel": "EMAIL",
  "subject": "Tu orden #1042 fue ejecutada",
  "body": "Tu market order de compra de 10 títulos de ECOPETROL fue ejecutada a $1.975 COP...",
  "status": "SENT",
  "referenceId": 1042,
  "attempts": [
    { "attemptNumber": 1, "status": "SUCCESS", "attemptedAt": "2026-05-13T14:32:01Z" }
  ],
  "createdAt": "2026-05-13T14:32:00Z"
}
```

---

## Common Error Responses

| Status | Error Code    | Description                          |
|--------|---------------|--------------------------------------|
| 401    | UNAUTHORIZED  | JWT ausente o inválido               |
| 404    | NOT_FOUND     | Notificación no encontrada o no propia |

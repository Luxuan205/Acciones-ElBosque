# API Contract: Alertas de Precio (AB-35)

**Base URL**: `/notifications/price-alerts`
**Auth**: Bearer JWT (role: INVESTOR, suscripción PREMIUM activa)
**Module**: `notifications`

---

## GET /notifications/price-alerts

**Response 200**
```json
[
  { "id": 401, "symbol": "ECOPETROL", "alertType": "ABSOLUTE", "direction": "ABOVE", "triggerValue": 2100.00, "referencePrice": null, "status": "ACTIVE" },
  { "id": 402, "symbol": "PFBCOLOM", "alertType": "PERCENTAGE", "direction": null, "triggerValue": 5.0, "referencePrice": 39500.00, "status": "TRIGGERED", "triggeredAt": "2026-05-12T10:00:00Z" }
]
```

---

## POST /notifications/price-alerts

**Request — Alerta absoluta**
```json
{ "symbol": "ECOPETROL", "alertType": "ABSOLUTE", "direction": "ABOVE", "triggerValue": 2100.00 }
```

**Request — Alerta porcentual**
```json
{ "symbol": "PFBCOLOM", "alertType": "PERCENTAGE", "triggerValue": 5.0 }
```

**Response 201**: PriceAlertDto

**Response 403 — No premium**
```json
{ "error": "PREMIUM_REQUIRED", "message": "Esta funcionalidad requiere suscripción PREMIUM activa." }
```

**Response 422 — Límite de alertas alcanzado**
```json
{ "error": "ALERT_LIMIT_REACHED", "message": "Ha alcanzado el límite máximo de alertas activas (20)." }
```

---

## PATCH /notifications/price-alerts/{id}/reactivate

Reactiva una alerta en estado TRIGGERED o INACTIVE.

**Response 200**: PriceAlertDto con `status: "ACTIVE"`

---

## PUT /notifications/price-alerts/{id}

Modifica el valor de activación de una alerta ACTIVE.

**Request**: `{ "triggerValue": 2200.00 }`

---

## DELETE /notifications/price-alerts/{id}

**Response 204**

---

## Common Error Responses

| Status | Error Code         | Description                              |
|--------|--------------------|------------------------------------------|
| 403    | PREMIUM_REQUIRED   | Suscripción PREMIUM inactiva o vencida   |
| 404    | NOT_FOUND          | Alerta no encontrada                     |
| 422    | ALERT_LIMIT_REACHED| Límite de alertas activas alcanzado      |

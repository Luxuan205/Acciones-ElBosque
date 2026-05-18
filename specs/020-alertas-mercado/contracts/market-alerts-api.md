# API Contract: Alertas de Mercado (AB-34)

**Base URL**: `/notifications/market-alerts`
**Auth**: Bearer JWT (role: INVESTOR)
**Module**: `notifications`

---

## GET /notifications/market-alerts

Lista suscripciones de alertas de mercado del inversionista.

**Response 200**
```json
[
  { "id": 301, "alertType": "MARKET_OPEN", "symbol": null, "thresholdValue": null, "active": true },
  { "id": 302, "alertType": "UNUSUAL_VOLUME", "symbol": "ECOPETROL", "thresholdValue": 150.0, "active": true }
]
```

---

## POST /notifications/market-alerts

Crea una nueva suscripción.

**Request**
```json
{ "alertType": "UNUSUAL_VOLUME", "symbol": "ECOPETROL", "thresholdValue": 150.0 }
```

**Response 201**: MarketAlertSubscriptionDto

---

## PUT /notifications/market-alerts/{id}

Modifica umbral o activa/desactiva suscripción.

**Request**: `{ "thresholdValue": 200.0, "active": true }`

**Response 200**: MarketAlertSubscriptionDto actualizado

---

## DELETE /notifications/market-alerts/{id}

Elimina una suscripción de alerta.

**Response 204**

---

## Common Error Responses

| Status | Error Code      | Description                                   |
|--------|-----------------|-----------------------------------------------|
| 400    | SYMBOL_REQUIRED | Symbol requerido para TRADING_SUSPENDED/UNUSUAL_VOLUME |
| 400    | THRESHOLD_REQUIRED | threshold_value requerido para UNUSUAL_VOLUME |
| 404    | NOT_FOUND       | Suscripción no encontrada o no pertenece al usuario |

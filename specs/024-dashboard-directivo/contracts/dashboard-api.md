# API Contract: Dashboard Directivo (AB-39)

**Base URL**: `/admin`
**Auth**: Bearer JWT (role: ADMIN)
**Module**: `app`

---

## GET /admin/dashboard

Métricas operativas en tiempo real.

**Response 200**
```json
{
  "marketOpen": true,
  "activeOrders": 142,
  "todayVolume": 85420000.00,
  "connectedUsers": 38,
  "generatedAt": "2026-05-13T14:30:00Z"
}
```

---

## GET /admin/dashboard/summary

Resumen financiero del período seleccionado.

**Query Params**: `period=THIS_MONTH` (`TODAY` | `THIS_WEEK` | `THIS_MONTH`)

**Response 200**
```json
{
  "period": "THIS_MONTH",
  "from": "2026-05-01",
  "to": "2026-05-13",
  "transactionVolume": 1240000000.00,
  "commissionRevenue": 3720000.00,
  "newRegistrations": 47,
  "activePremiumSubscriptions": 183
}
```

---

## Common Error Responses

| Status | Error Code  | Description                      |
|--------|-------------|----------------------------------|
| 401    | UNAUTHORIZED| JWT ausente o inválido           |
| 403    | FORBIDDEN   | Rol no es ADMIN                  |

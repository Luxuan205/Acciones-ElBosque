# API Contract: Auditoría y Cumplimiento Legal (AB-38)

**Base URL**: `/audit`
**Auth**: Bearer JWT (role: ADMIN)
**Module**: `audit-compliance`

---

## GET /audit/events

Consulta el log de auditoría con filtros.

**Query Params**:
- `investorId` (Long, opcional)
- `eventType` (String, opcional) — ej. `AUTH_FAILURE`
- `result` (String, opcional) — `SUCCESS` | `FAILURE`
- `from` (ISO date, opcional)
- `to` (ISO date, opcional)
- `includeArchived` (boolean, default false)
- `page`, `size` (paginación)

**Response 200**
```json
{
  "content": [
    {
      "id": 10001,
      "eventType": "AUTH_FAILURE",
      "investorId": 42,
      "performedBy": 42,
      "referenceType": null,
      "referenceId": null,
      "detail": { "reason": "INVALID_PASSWORD", "attempt": 3 },
      "result": "FAILURE",
      "ipAddress": "192.168.1.100",
      "archived": false,
      "occurredAt": "2026-05-13T10:15:00Z"
    }
  ],
  "totalElements": 5420,
  "page": 0,
  "size": 50
}
```

---

## GET /audit/events/export

Exporta el log filtrado como CSV.

**Query Params**: igual que GET /audit/events

**Response 200**: `Content-Type: text/csv`, `Content-Disposition: attachment; filename="audit-log.csv"`

---

## Common Error Responses

| Status | Error Code    | Description                 |
|--------|---------------|-----------------------------|
| 401    | UNAUTHORIZED  | JWT ausente o inválido      |
| 403    | FORBIDDEN     | Rol no es ADMIN             |

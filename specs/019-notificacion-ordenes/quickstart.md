# Quickstart: AB-33 — Notificaciones de Órdenes

**Prerequisites**
- Service running on port 8080
- JWT token: `$INVESTOR_TOKEN` (role: INVESTOR)
- At least one executed or cancelled order for the investor (to generate notification records)
- Notification IDs referenced below: `5001` (ORDER_EXECUTED), `5002` (ORDER_CANCELLED)

---

## Flow 1: List all notifications (paginated)

```bash
curl -s "http://localhost:8080/notifications?page=0&size=20" \
  -H "Authorization: Bearer $INVESTOR_TOKEN" | jq .
```

Expected 200:
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
      "createdAt": "2026-05-14T09:32:00Z"
    },
    {
      "id": 5002,
      "eventType": "ORDER_CANCELLED",
      "channel": "EMAIL",
      "subject": "Tu orden #1044 fue cancelada",
      "status": "SENT",
      "referenceId": 1044,
      "createdAt": "2026-05-14T10:00:00Z"
    }
  ],
  "totalElements": 2,
  "page": 0,
  "size": 20
}
```

---

## Flow 2: Filter notifications by event type

```bash
curl -s "http://localhost:8080/notifications?eventType=ORDER_EXECUTED&page=0&size=10" \
  -H "Authorization: Bearer $INVESTOR_TOKEN" | jq .
```

Expected 200 (only ORDER_EXECUTED events):
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
      "createdAt": "2026-05-14T09:32:00Z"
    }
  ],
  "totalElements": 1,
  "page": 0,
  "size": 10
}
```

---

## Flow 3: Get full detail of a specific notification

```bash
curl -s http://localhost:8080/notifications/5001 \
  -H "Authorization: Bearer $INVESTOR_TOKEN" | jq .
```

Expected 200:
```json
{
  "id": 5001,
  "eventType": "ORDER_EXECUTED",
  "channel": "EMAIL",
  "subject": "Tu orden #1042 fue ejecutada",
  "body": "Tu market order de compra de 10 títulos de ECOPETROL fue ejecutada a $1.972 COP. Comisión: $197,20. Monto total: $19.917,20 COP.",
  "status": "SENT",
  "referenceId": 1042,
  "attempts": [
    {
      "attemptNumber": 1,
      "status": "SUCCESS",
      "attemptedAt": "2026-05-14T09:32:01Z"
    }
  ],
  "createdAt": "2026-05-14T09:32:00Z"
}
```

---

## Flow 4: Get detail of an ORDER_CANCELLED notification

```bash
curl -s http://localhost:8080/notifications/5002 \
  -H "Authorization: Bearer $INVESTOR_TOKEN" | jq .
```

Expected 200:
```json
{
  "id": 5002,
  "eventType": "ORDER_CANCELLED",
  "channel": "EMAIL",
  "subject": "Tu orden #1044 fue cancelada",
  "body": "Tu orden límite de compra #1044 (10 títulos de PFBCOLOM a $39.500 COP) fue cancelada. Saldo liberado: $395.000 COP.",
  "status": "SENT",
  "referenceId": 1044,
  "attempts": [
    {
      "attemptNumber": 1,
      "status": "SUCCESS",
      "attemptedAt": "2026-05-14T10:00:01Z"
    }
  ],
  "createdAt": "2026-05-14T10:00:00Z"
}
```

---

## Flow 5: View a notification with a failed delivery attempt (retry visible)

```bash
curl -s http://localhost:8080/notifications/5003 \
  -H "Authorization: Bearer $INVESTOR_TOKEN" | jq .
```

Expected 200 (delivery failed and retried):
```json
{
  "id": 5003,
  "eventType": "ORDER_REJECTED",
  "channel": "EMAIL",
  "subject": "Tu orden #1050 fue rechazada",
  "body": "Tu order de compra de 5 títulos de NUTRESA fue rechazada: saldo insuficiente.",
  "status": "FAILED",
  "referenceId": 1050,
  "attempts": [
    {
      "attemptNumber": 1,
      "status": "FAILED",
      "attemptedAt": "2026-05-14T11:00:01Z"
    },
    {
      "attemptNumber": 2,
      "status": "FAILED",
      "attemptedAt": "2026-05-14T11:05:01Z"
    },
    {
      "attemptNumber": 3,
      "status": "FAILED",
      "attemptedAt": "2026-05-14T11:10:01Z"
    }
  ],
  "createdAt": "2026-05-14T11:00:00Z"
}
```

---

## Flow 6: Attempt to access another investor's notification (error case)

```bash
curl -s http://localhost:8080/notifications/9999 \
  -H "Authorization: Bearer $INVESTOR_TOKEN" | jq .
```

Expected 404:
```json
{
  "error": "NOT_FOUND",
  "message": "Notificación no encontrada."
}
```

---

## Flow 7: Access without a valid JWT (error case)

```bash
curl -s http://localhost:8080/notifications | jq .
```

Expected 401:
```json
{
  "error": "UNAUTHORIZED",
  "message": "JWT ausente o inválido."
}
```

---

## Optional SQL verification

```sql
-- Count notifications by event type for the investor
SELECT event_type, status, COUNT(*) AS total
FROM notifications
WHERE investor_id = <YOUR_INVESTOR_ID>
GROUP BY event_type, status
ORDER BY total DESC;

-- Check all delivery attempts for a specific notification
SELECT attempt_number, status, attempted_at
FROM notification_attempts
WHERE notification_id = 5003
ORDER BY attempt_number;
```

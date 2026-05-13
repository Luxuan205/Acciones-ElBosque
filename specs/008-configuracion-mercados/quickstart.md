# Quickstart: AB-29 — Gestión de Horarios y Configuración de Mercados

**Prerequisites**
- configuration-service running on port 8085
- Admin JWT: `$ADMIN_TOKEN`; any valid JWT: `$TOKEN`
- Flyway seed applied: Mon–Fri 09:00–15:30 UTC-5

---

## Flow 1: Check current market status (any authenticated user)

```bash
curl -s http://localhost:8080/config/market/status \
  -H "Authorization: Bearer $TOKEN" | jq .
```

Expected during Mon–Fri 09:00–15:30 (Bogotá time):
```json
{
  "status": "OPEN",
  "today": "2026-05-10",
  "currentTime": "11:30:00",
  "timezone": "America/Bogota",
  "nextClose": "15:30:00",
  "nextOpen": null,
  "isHoliday": false,
  "holidayName": null
}
```

---

## Flow 2: Get market schedule (admin only)

```bash
curl -s http://localhost:8080/config/market/schedule \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq .
```

Expected:
```json
{
  "openTime": "09:00",
  "closeTime": "15:30",
  "workingDays": ["MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY"],
  "timezone": "America/Bogota"
}
```

---

## Flow 3: Update market schedule

```bash
curl -s -X PUT http://localhost:8080/config/market/schedule \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "openTime": "09:30",
    "closeTime": "16:00",
    "workingDays": ["MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY"]
  }' | jq .
```

Expected: 200 with `"openTime": "09:30"` and `"closeTime": "16:00"`.

Test non-admin access:
```bash
curl -s -X PUT http://localhost:8080/config/market/schedule \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"openTime":"10:00","closeTime":"14:00","workingDays":["MONDAY"]}' | jq .
```

Expected: 403 `FORBIDDEN`.

---

## Flow 4: Add a holiday

```bash
curl -s -X POST http://localhost:8080/config/market/holidays \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "date": "2026-08-07",
    "description": "Batalla de Boyaca",
    "type": "NATIONAL"
  }' | jq .
```

Expected: 201 with the holiday data.

Attempt to add the same date again:
```bash
curl -s -X POST http://localhost:8080/config/market/holidays \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"date":"2026-08-07","description":"Duplicate","type":"NATIONAL"}' | jq .
```

Expected: 409 `HOLIDAY_ALREADY_EXISTS`.

---

## Flow 5: Check market status on a holiday

Set current date to a holiday for testing (or use a system date mock):
```sql
-- Verify holiday is stored
SELECT * FROM market_holiday WHERE date = '2026-08-07';
```

When the system date is 2026-08-07:
```bash
curl -s http://localhost:8080/config/market/status \
  -H "Authorization: Bearer $TOKEN" | jq '{status: .status, isHoliday: .isHoliday, holidayName: .holidayName}'
```

Expected: `"status": "CLOSED"`, `"isHoliday": true`, `"holidayName": "Batalla de Boyaca"`.

---

## Flow 6: Delete a holiday

```bash
HOLIDAY_ID=$(curl -s http://localhost:8080/config/market/holidays \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.holidays[] | select(.date == "2026-08-07") | .id')

curl -s -X DELETE "http://localhost:8080/config/market/holidays/$HOLIDAY_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN" -w "\nHTTP %{http_code}"
```

Expected: HTTP 204 (no body).

Test invalid ID:
```bash
curl -s -X DELETE "http://localhost:8080/config/market/holidays/00000000-0000-0000-0000-000000000000" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq .
```

Expected: 404 `HOLIDAY_NOT_FOUND`.

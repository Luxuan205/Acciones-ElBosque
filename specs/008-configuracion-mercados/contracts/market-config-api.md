# API Contract: Market Configuration (AB-29)

**Base URL**: `/config/market`  
**Auth**: Bearer JWT  
**Module**: `configuration-service`

Read endpoints (`GET /config/market/status`) are accessible to all authenticated roles.
Write endpoints (`PUT`, `POST`, `DELETE`) require role **ADMIN**.

---

## GET /config/market/status

Returns the current market status. Consumed by `order` and other modules in-process,
and exposed via REST for frontend display.

**Auth**: Any authenticated role

**Response 200 — Market open**
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

**Response 200 — Market closed (after hours)**
```json
{
  "status": "CLOSED",
  "today": "2026-05-10",
  "currentTime": "18:00:00",
  "timezone": "America/Bogota",
  "nextClose": null,
  "nextOpen": "2026-05-11T09:00:00",
  "isHoliday": false,
  "holidayName": null
}
```

**Response 200 — Market closed (holiday)**
```json
{
  "status": "CLOSED",
  "today": "2026-05-01",
  "currentTime": "10:00:00",
  "timezone": "America/Bogota",
  "nextClose": null,
  "nextOpen": "2026-05-02T09:00:00",
  "isHoliday": true,
  "holidayName": "Día del Trabajo"
}
```

---

## GET /config/market/schedule

Returns the current market schedule configuration.

**Auth**: ADMIN

**Response 200**
```json
{
  "openTime": "09:00",
  "closeTime": "15:30",
  "workingDays": ["MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY"],
  "timezone": "America/Bogota",
  "updatedAt": "2026-01-01T00:00:00Z"
}
```

---

## PUT /config/market/schedule

Updates the market schedule. Changes apply from the next trading session.

**Auth**: ADMIN

**Request**
```json
{
  "openTime": "09:30",
  "closeTime": "16:00",
  "workingDays": ["MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY"]
}
```

**Validations**
- `openTime`: valid HH:mm; must be before `closeTime`
- `closeTime`: valid HH:mm; must be after `openTime`
- `workingDays`: non-empty array; each value must be a valid `DayOfWeek`

**Response 200**
```json
{
  "message": "Market schedule updated. Changes apply from next trading session.",
  "openTime": "09:30",
  "closeTime": "16:00",
  "workingDays": ["MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY"],
  "updatedAt": "2026-05-10T14:00:00Z"
}
```

**Response 400** — Invalid time range
```json
{
  "error": "VALIDATION_ERROR",
  "message": "openTime must be before closeTime"
}
```

---

## GET /config/market/holidays

Returns the list of configured market holidays.

**Auth**: ADMIN

**Query Params**
- `year` (optional): 4-digit year; defaults to current year

**Response 200**
```json
{
  "holidays": [
    {
      "id": "uuid",
      "date": "2026-05-01",
      "description": "Día del Trabajo",
      "type": "NATIONAL"
    },
    {
      "id": "uuid",
      "date": "2026-12-25",
      "description": "Navidad",
      "type": "NATIONAL"
    }
  ]
}
```

---

## POST /config/market/holidays

Adds a new market holiday.

**Auth**: ADMIN

**Request**
```json
{
  "date": "2026-08-07",
  "description": "Batalla de Boyacá",
  "type": "NATIONAL"
}
```

**Validations**
- `date`: ISO-8601 date; must be unique (no duplicate dates)
- `description`: not blank, max 200 chars
- `type`: one of `NATIONAL`, `REGIONAL`, `SPECIAL`

**Response 201**
```json
{
  "id": "uuid",
  "date": "2026-08-07",
  "description": "Batalla de Boyacá",
  "type": "NATIONAL",
  "createdAt": "2026-05-10T14:05:00Z"
}
```

**Response 409** — Holiday already exists for that date
```json
{
  "error": "HOLIDAY_ALREADY_EXISTS",
  "message": "A holiday is already configured for 2026-08-07"
}
```

---

## DELETE /config/market/holidays/{holidayId}

Removes a configured holiday.

**Auth**: ADMIN

**Path Params**
- `holidayId` (UUID): the holiday to delete

**Response 204** — No content (deleted successfully)

**Response 404**
```json
{
  "error": "HOLIDAY_NOT_FOUND",
  "message": "Holiday not found"
}
```

---

## Common Error Responses

| Status | Error Code   | Description                      |
|--------|--------------|----------------------------------|
| 401    | UNAUTHORIZED | Missing or invalid JWT           |
| 403    | FORBIDDEN    | Role is not ADMIN                |

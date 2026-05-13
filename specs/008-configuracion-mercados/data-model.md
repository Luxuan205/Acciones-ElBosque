# Data Model: AB-29 — Gestión de Horarios y Configuración de Mercados

## Entities

### MarketSchedule
```
MarketSchedule
├── id           UUID     PK
├── openTime     TIME     NOT NULL  (without timezone; interpreted as UTC-5 / America/Bogota)
├── closeTime    TIME     NOT NULL
├── workingDays  INTEGER  NOT NULL  (bitmask: bit0=Mon, bit1=Tue, bit2=Wed, bit3=Thu, bit4=Fri)
│                                   value 31 (11111b) = Mon-Fri
├── updatedAt    TIMESTAMP NOT NULL DEFAULT now()
└── updatedBy    UUID      NULL     (investorId of the admin who last modified)
```

Note: Only one row exists in this table (single market). Flyway V1 inserts the initial seed.

### MarketHoliday
```
MarketHoliday
├── id          UUID        PK
├── date        DATE        NOT NULL UNIQUE
├── description VARCHAR(200) NOT NULL
├── type        VARCHAR(20)  NOT NULL DEFAULT 'NATIONAL'  CHECK IN ('NATIONAL','REGIONAL','SPECIAL')
└── createdAt   TIMESTAMP    NOT NULL DEFAULT now()
```

### MarketStatusDto (Java — not persisted)
```java
record MarketStatusDto(
    String    status,      // "OPEN" or "CLOSED"
    LocalDate today,
    LocalTime currentTime, // UTC-5
    LocalTime nextOpen,    // null if market is open right now
    LocalTime nextClose,   // null if market is closed
    boolean   isHoliday,
    String    holidayName  // null if not a holiday
)
```

## workingDays Bitmask Mapping
| DayOfWeek | Bit | Value |
|-----------|-----|-------|
| MONDAY    |  0  |   1   |
| TUESDAY   |  1  |   2   |
| WEDNESDAY |  2  |   4   |
| THURSDAY  |  3  |   8   |
| FRIDAY    |  4  |  16   |

Mon–Fri = 1+2+4+8+16 = **31**

## MarketStatus Logic
```
isMarketOpen():
  today = LocalDate.now(ZoneId.of("America/Bogota"))
  if today is in MarketHoliday → CLOSED
  if DayOfWeek(today) not in workingDays bitmask → CLOSED
  currentTime = LocalTime.now(ZoneId.of("America/Bogota"))
  if currentTime >= openTime && currentTime < closeTime → OPEN
  else → CLOSED
```

## Validation Rules
- `openTime`: must be before `closeTime`
- `closeTime`: must be after `openTime`; max 23:59
- `workingDays`: 1–31 (at least one day must be set)
- `date` (holiday): ISO-8601 date; must be future date; unique
- `description`: not blank, max 200 chars
- `type`: one of NATIONAL, REGIONAL, SPECIAL
- Only users with role ADMIN may call PUT/POST/DELETE endpoints

## Flyway Migrations

### V1__create_market_schedule_table.sql
```sql
CREATE TABLE market_schedule (
    id           UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    open_time    TIME      NOT NULL,
    close_time   TIME      NOT NULL,
    working_days INTEGER   NOT NULL CHECK (working_days BETWEEN 1 AND 127),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by   UUID
);

-- Seed: Monday–Friday 09:00–15:30 (UTC-5 / America/Bogota)
INSERT INTO market_schedule (id, open_time, close_time, working_days) VALUES
    (gen_random_uuid(), '09:00:00', '15:30:00', 31);
```

### V2__create_market_holiday_table.sql
```sql
CREATE TABLE market_holiday (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    date        DATE         NOT NULL UNIQUE,
    description VARCHAR(200) NOT NULL,
    type        VARCHAR(20)  NOT NULL DEFAULT 'NATIONAL'
                             CHECK (type IN ('NATIONAL','REGIONAL','SPECIAL')),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_market_holiday_date ON market_holiday(date);
```

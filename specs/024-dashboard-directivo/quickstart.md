# Quickstart: AB-39 — Dashboard Directivo

**Prerequisites**
- Service running on port 8080
- JWT token: `$ADMIN_TOKEN` (role: ADMIN, user: admin@accioneselbosque.com)
- JWT token: `$INVESTOR_TOKEN` (role: INVESTOR, user: investor@test.com)
- Market open during business hours (Mon–Fri, 09:00–16:00 COT)

---

## Flow 1: Load real-time operational metrics

```bash
curl -s -X GET http://localhost:8080/admin/dashboard \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq .
```

Expected 200:
```json
{
  "marketOpen": true,
  "activeOrders": 142,
  "todayVolume": 85420000.00,
  "connectedUsers": 38,
  "generatedAt": "2026-05-14T14:30:00Z"
}
```

DB verification:
```sql
-- Confirm active orders count
SELECT COUNT(*) FROM orders WHERE status IN ('PENDING', 'PARTIALLY_FILLED');

-- Confirm today's volume
SELECT SUM(gross_amount) FROM trade_transactions
WHERE DATE(executed_at) = CURDATE();
```

---

## Flow 2: Load financial summary for today

```bash
curl -s -X GET "http://localhost:8080/admin/dashboard/summary?period=TODAY" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq .
```

Expected 200:
```json
{
  "period": "TODAY",
  "from": "2026-05-14",
  "to": "2026-05-14",
  "transactionVolume": 85420000.00,
  "commissionRevenue": 256260.00,
  "newRegistrations": 3,
  "activePremiumSubscriptions": 183
}
```

---

## Flow 3: Load financial summary for the current week

```bash
curl -s -X GET "http://localhost:8080/admin/dashboard/summary?period=THIS_WEEK" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq .
```

Expected 200:
```json
{
  "period": "THIS_WEEK",
  "from": "2026-05-11",
  "to": "2026-05-14",
  "transactionVolume": 412750000.00,
  "commissionRevenue": 1238250.00,
  "newRegistrations": 19,
  "activePremiumSubscriptions": 183
}
```

DB verification:
```sql
SELECT SUM(gross_amount) AS volume, SUM(commission) AS commission_revenue
FROM trade_transactions
WHERE executed_at >= DATE_TRUNC('week', CURRENT_DATE);

SELECT COUNT(*) AS new_registrations
FROM investors
WHERE created_at >= DATE_TRUNC('week', CURRENT_DATE);
```

---

## Flow 4: Load financial summary for the current month

```bash
curl -s -X GET "http://localhost:8080/admin/dashboard/summary?period=THIS_MONTH" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq .
```

Expected 200:
```json
{
  "period": "THIS_MONTH",
  "from": "2026-05-01",
  "to": "2026-05-14",
  "transactionVolume": 1240000000.00,
  "commissionRevenue": 3720000.00,
  "newRegistrations": 47,
  "activePremiumSubscriptions": 183
}
```

---

## Flow 5: Verify metrics refresh — poll twice and compare timestamps

```bash
# First call
curl -s -X GET http://localhost:8080/admin/dashboard \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq .generatedAt

# Wait up to 60 seconds and call again
curl -s -X GET http://localhost:8080/admin/dashboard \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq .generatedAt
```

Expected: `generatedAt` timestamps should differ by at most 60 seconds, confirming the auto-refresh SLA defined in SC-002.

---

## Flow 6: Reject access from a non-ADMIN role (investor)

```bash
curl -s -X GET http://localhost:8080/admin/dashboard \
  -H "Authorization: Bearer $INVESTOR_TOKEN" | jq .
```

Expected 403:
```json
{
  "error": "FORBIDDEN",
  "message": "Acceso denegado. Se requiere rol ADMIN."
}
```

---

## Flow 7: Reject unauthenticated request to summary endpoint

```bash
curl -s -X GET "http://localhost:8080/admin/dashboard/summary?period=TODAY" | jq .
```

Expected 401:
```json
{
  "error": "UNAUTHORIZED",
  "message": "JWT ausente o inválido."
}
```

---

## Flow 8: Confirm dashboard metrics match source modules

```bash
# Dashboard today volume
DASH_VOL=$(curl -s "http://localhost:8080/admin/dashboard/summary?period=TODAY" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq .transactionVolume)

# Portfolio report volume for same day (as proxy check)
echo "Dashboard volume today: $DASH_VOL"
```

Expected: `transactionVolume` in the dashboard matches the sum of `grossAmount` fields for all SELL/BUY transactions executed today. Any discrepancy should trigger a system alert (FR-002).

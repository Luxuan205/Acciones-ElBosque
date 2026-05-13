# Quickstart: AB-31 — Gestión de Clientes Asignados por el Comisionista

**Prerequisites**
- auth-security-service running on port 8081
- Broker JWT: `$BROKER_TOKEN`; another broker JWT: `$BROKER2_TOKEN`
- At least 2 investors assigned to Broker 1, none to Broker 2
- Seed assignment data:

```sql
-- Assign investor_a and investor_b to broker_1
INSERT INTO broker_client_assignment (id, broker_id, investor_id, active) VALUES
  (gen_random_uuid(), '<BROKER1_ID>', '<INVESTOR_A_ID>', true),
  (gen_random_uuid(), '<BROKER1_ID>', '<INVESTOR_B_ID>', true);
-- broker_2 has no clients assigned
```

---

## Flow 1: List all assigned clients

```bash
curl -s "http://localhost:8080/brokers/me/clients" \
  -H "Authorization: Bearer $BROKER_TOKEN" | jq '{total: .totalElements, clients: [.content[] | {name: .fullName, status: .accountStatus}]}'
```

Expected: 200 with 2 clients (only Broker 1's clients).

---

## Flow 2: Search clients by name

```bash
curl -s "http://localhost:8080/brokers/me/clients?search=carlos" \
  -H "Authorization: Bearer $BROKER_TOKEN" | jq '.content[] | {name: .fullName}'
```

Expected: Returns only clients whose name contains "carlos" (case-insensitive).

---

## Flow 3: Filter by account status

```bash
curl -s "http://localhost:8080/brokers/me/clients?status=ACTIVE" \
  -H "Authorization: Bearer $BROKER_TOKEN" | jq '[.content[] | .accountStatus]'
```

Expected: All returned clients have `"accountStatus": "ACTIVE"`.

---

## Flow 4: Get client detail

```bash
curl -s "http://localhost:8080/brokers/me/clients/<INVESTOR_A_ID>" \
  -H "Authorization: Bearer $BROKER_TOKEN" | jq .
```

Expected 200 with full client detail including `portfolioSummary` and `recentOrders`.

---

## Flow 5: Isolation — broker cannot access other broker's client

```bash
# Broker 2 attempts to access Broker 1's client
curl -s "http://localhost:8080/brokers/me/clients/<INVESTOR_A_ID>" \
  -H "Authorization: Bearer $BROKER2_TOKEN" | jq .
```

Expected: 403 `ACCESS_DENIED`.

---

## Flow 6: Non-broker cannot access endpoint

```bash
# Investor JWT attempting to access broker endpoint
curl -s "http://localhost:8080/brokers/me/clients" \
  -H "Authorization: Bearer $INVESTOR_TOKEN" | jq .
```

Expected: 403 `FORBIDDEN`.

---

## Flow 7: Empty client list (broker with no assignments)

```bash
curl -s "http://localhost:8080/brokers/me/clients" \
  -H "Authorization: Bearer $BROKER2_TOKEN" | jq '{total: .totalElements, empty: (.content | length == 0)}'
```

Expected: `"total": 0`, `"empty": true`.

---

## Flow 8: Verify client not visible after assignment deactivation

```sql
-- Admin deactivates an assignment
UPDATE broker_client_assignment
SET active = false, deactivated_at = NOW()
WHERE broker_id = '<BROKER1_ID>' AND investor_id = '<INVESTOR_B_ID>';
```

```bash
curl -s "http://localhost:8080/brokers/me/clients" \
  -H "Authorization: Bearer $BROKER_TOKEN" | jq '.totalElements'
```

Expected: Returns 1 (investor_b no longer visible, only investor_a).

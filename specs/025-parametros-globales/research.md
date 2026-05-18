# Research: AB-40 — Gestión de Parámetros Globales del Sistema

## Decisiones técnicas

### Parámetros iniciales y sus rangos

| Key | Default | Tipo | Rango | Descripción |
|-----|---------|------|-------|-------------|
| `auth.max_login_attempts` | 5 | INT | 3–10 | Intentos antes de bloquear |
| `auth.lock_duration_minutes` | 30 | INT | 5–1440 | Duración del bloqueo en minutos |
| `auth.otp_ttl_minutes` | 5 | INT | 2–15 | Vigencia del OTP |
| `auth.jwt_ttl_hours` | 8 | INT | 1–24 | Vigencia del JWT |
| `subscription.premium_duration_days` | 30 | INT | 7–365 | Duración de la suscripción PREMIUM |
| `trading.commission_rate_pct` | 0.3 | DECIMAL | 0.0–5.0 | Tasa de comisión (% del valor bruto) |
| `trading.max_price_alerts_per_user` | 20 | INT | 1–100 | Máximo de alertas de precio por usuario PREMIUM |
| `audit.active_retention_years` | 5 | INT | 1–20 | Años de retención en log activo |

### Caché de parámetros

**Decision**: `@Cacheable(value = "parameters", key = "#key")` con TTL de 60 segundos en
Spring Cache (ConcurrentMapCacheManager para el MVP; Caffeine en producción).
`@CacheEvict(value = "parameters", key = "#key")` al actualizar.

**Rationale**: Evita consultar la base de datos en cada petición que necesite un parámetro.

### Inmutabilidad del historial

**Decision**: `ParameterChangeHistoryRepository` no expone `delete()` ni `deleteAll()`.
Cada modificación genera un nuevo registro de historial; el historial nunca se modifica.

### Conflicto de escritura simultánea

**Decision**: Optimistic locking con `@Version` en `GlobalParameter`. Si dos admins modifican
el mismo parámetro, el segundo recibe `OptimisticLockException` → HTTP 409 con el valor actual.

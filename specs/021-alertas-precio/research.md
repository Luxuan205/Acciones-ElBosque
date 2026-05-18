# Research: AB-35 — Alertas de Precio Personalizadas

## Decisiones técnicas

### ABSOLUTE vs. PERCENTAGE

**Decision**: Campo `alert_type` discrimina el tipo.
- ABSOLUTE: `trigger_value` = precio absoluto (ej. 2000.00 COP). Condición: `currentPrice >= trigger_value` (para alerta de sube) o `currentPrice <= trigger_value` (para alerta de baja).
- PERCENTAGE: `trigger_value` = porcentaje (ej. 5.0 = 5%). `reference_price` almacena el precio al momento de crear la alerta. Condición: `|currentPrice - referencePrice| / referencePrice * 100 >= trigger_value`.

**Decision adicional**: Para alertas ABSOLUTE, el campo `direction` indica si es alerta de precio alto (`ABOVE`) o bajo (`BELOW`).

### Estado TRIGGERED — una sola notificación por activación

**Decision**: Al dispararse, `status = TRIGGERED`. El job no re-evalúa alertas en TRIGGERED.
El usuario debe reactivarlas manualmente (`PATCH /price-alerts/{id}/reactivate`).

**Rationale**: Evita spam de notificaciones si el precio oscila alrededor del umbral.

### Suspensión por vencimiento de suscripción

**Decision**: El job de degradación de suscripciones en `auth` (`SubscriptionExpiryJob`) publica
un `ApplicationEvent` con el `investorId`. `PriceAlertService` lo escucha y pone todas sus
alertas ACTIVE en SUSPENDED. Las alertas SUSPENDED no se evalúan.

### Número máximo de alertas por usuario

**Decision**: Configurable en parámetros globales (AB-40). Default: 20 alertas activas por usuario.

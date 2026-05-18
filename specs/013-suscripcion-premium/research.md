# Research: AB-18 — Activación de Suscripción Premium

## Decisiones técnicas

### Duración de suscripción

**Decision**: 30 días calendario desde el momento de activación (configurable vía parámetro global AB-40).

**Rationale**: Valor definido en el spec. La duración se leerá de `GlobalParameter` cuando AB-40 esté implementado; por ahora, constante de 30 días en `SubscriptionService`.

### Job de degradación periódica

**Decision**: `@Scheduled(cron = "0 0 0 * * *")` — se ejecuta a medianoche UTC todos los días.
Consulta todos los `investor` con `subscriptionType = PREMIUM` y `subscriptionExpiresAt < NOW()`,
y los degrada a STANDARD.

**Rationale**: Spring Scheduling ya está en el classpath de Spring Boot. No requiere infraestructura adicional (Quartz, etc.) para el alcance académico.

**Alternatives considered**: Quartz Scheduler para persistencia del estado del job — descartado por complejidad innecesaria.

### Facade SubscriptionGate

**Decision**: Interface Java pública en `auth` que expone `isPremiumActive(Long investorId): boolean`.
Los módulos `market-data` y `notifications` la usan in-process inyectando el bean.

**Rationale**: Cumple el Principio I de la constitución — comunicación in-process, no HTTP.

### Historial de suscripción

**Decision**: Tabla `subscription_event` con un registro por cada cambio de estado
(ACTIVATED, EXPIRED, MANUALLY_UPGRADED, DOWNGRADED).

**Rationale**: Necesario para auditoría (FR-008 del spec) y para el dashboard directivo (AB-39)
que muestra suscripciones activas.

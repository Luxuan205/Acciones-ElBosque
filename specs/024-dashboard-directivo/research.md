# Research: AB-39 — Dashboard Directivo

## Decisiones técnicas

### Cálculo en tiempo real vs. caché

**Decision**: Sin caché en el MVP. Cada petición al dashboard ejecuta las consultas en tiempo real.

**Rationale**: El volumen académico es bajo (pocos administradores, pocas peticiones simultáneas).
En producción se añadiría caché con TTL de 60 segundos.

### Período del resumen financiero

**Decision**: El dashboard soporta tres períodos: `TODAY`, `THIS_WEEK`, `THIS_MONTH`.
Implementado como `@RequestParam period` con enum.

### Métricas de usuarios "conectados"

**Decision**: Se usa `mfa_session.completed = TRUE AND created_at > NOW() - 1 HOUR` como proxy
de "usuario activo en la última hora". No requiere estado de sesión WebSocket.

### Ingresos por comisiones

**Decision**: `SUM(commission)` de `market_order` WHERE `status = 'EXECUTED'` AND `created_at`
en el período. Son ingresos estimados (las comisiones son revenue de la plataforma).

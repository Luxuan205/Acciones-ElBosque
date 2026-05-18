# Research: AB-34 — Alertas de Mercado

## Decisiones técnicas

### Mecanismo de eventos in-process

**Decision**: `ApplicationEventPublisher` de Spring. El módulo `market-data` publica
`MarketEvent` (POJO) cuando detecta un cambio de estado del mercado. El módulo `notifications`
tiene un `@EventListener` que lo recibe y busca los suscriptores.

**Rationale**: Cumple el Principio I de la constitución (in-process). No requiere Kafka ni RabbitMQ.
Es suficiente para el volumen académico.

### Tipos de alertas de mercado soportados inicialmente

- `MARKET_OPEN`: mercado abre (lunes-viernes en horario configurado)
- `MARKET_CLOSE`: mercado cierra
- `TRADING_SUSPENDED`: negociación suspendida para un símbolo específico
- `UNUSUAL_VOLUME`: volumen supera umbral configurable por el usuario (% del volumen promedio)

### Umbral para UNUSUAL_VOLUME

**Decision**: El umbral se almacena como `threshold_value` en la suscripción (% sobre el volumen
promedio de los últimos 5 días). El módulo `market-data` calcula el volumen actual y publica el
evento cuando supera el umbral de algún suscriptor.

**Rationale**: Simplicidad — umbral por suscripción, calculado en `market-data` que ya tiene
acceso a `stock_snapshot`.

### Suscripciones globales vs. por símbolo

**Decision**: `MARKET_OPEN` y `MARKET_CLOSE` son globales (sin símbolo asociado). `TRADING_SUSPENDED`
y `UNUSUAL_VOLUME` son por símbolo específico.

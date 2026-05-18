# Research: AB-21 — Colocación de Limit Order

## Decisiones técnicas

### Evaluación de condiciones de precio

**Decision**: Job `@Scheduled` que corre cada 30 segundos durante horario bursátil. Consulta todas
las órdenes PENDING de tipo LIMIT_BUY/LIMIT_SELL y compara su `limit_price` con el precio actual
del snapshot de `market-data`. Si la condición se cumple, la orden pasa a EXECUTED.

**Rationale**: Para el alcance académico, polling periódico es suficiente. En producción real se
usaría un bus de eventos de precios.

**Alternatives considered**: WebSocket/SSE con precios en tiempo real — descartado por complejidad
de infraestructura no justificada.

### GTC vs. GTD

**Decision**: `expires_at = NULL` → GTC (permanece hasta ejecución o cancelación manual).
`expires_at = <fecha>` → GTD (se cancela automáticamente si pasa la fecha sin ejecutar).
El job de evaluación también revisa expiración y cancela las GTD vencidas.

### Ejecución parcial

**Decision**: Para el MVP, las limit orders se ejecutan completamente o no se ejecutan (no hay
ejecución parcial). Si no hay liquidez suficiente, la orden permanece en PENDING.

**Rationale**: La ejecución parcial requiere lógica de libro de órdenes que está fuera del alcance
académico. El spec lo define como posible pero no mandatorio en el MVP.

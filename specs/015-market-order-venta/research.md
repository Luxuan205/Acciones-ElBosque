# Research: AB-20 — Generación de Market Order de Venta

## Decisiones técnicas

### Reserva de títulos

**Decision**: Tabla `title_reservation` análoga a `balance_reservation`. Almacena `investor_id`,
`symbol`, `quantity` y `order_id`. Los títulos disponibles = títulos en portafolio − suma de
`title_reservation.quantity WHERE released = FALSE AND symbol = ?`.

**Rationale**: Evita doble venta (FR-005 del spec). El portafolio real de títulos será gestionado
por el módulo `portfolio` (AB-26); mientras tanto, `TitleReservationRepository` consulta las
tenencias simuladas.

### Monto neto de venta

**Decision**: `netAmount = quantity × estimatedPrice − commission`. El campo `total_estimated`
en `market_order` representa el neto a recibir para MARKET_SELL (a diferencia de MARKET_BUY donde
es el total a debitar).

**Rationale**: Consistente con el campo existente; el tipo de orden (`order_type`) distingue la
semántica.

### Precio de mercado

**Decision**: Igual que AB-19 — se consulta in-process al `StockSnapshotService` de `market-data`.
El precio real de ejecución puede diferir; es inherente a una market order.

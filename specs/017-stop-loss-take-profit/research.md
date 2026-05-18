# Research: AB-22 — Configuración de Stop-Loss y Take-Profit

## Decisiones técnicas

### OCO (One Cancels the Other)

**Decision**: Campo `oco_partner_id` en `conditional_order` apunta al ID del stop-loss/take-profit
complementario. Al activarse uno, el servicio cancela automáticamente al otro.

**Rationale**: Implementación simple sin broker externo. El campo es nullable para órdenes
condicionales sin par.

### Activación y generación de orden de venta

**Decision**: Al activarse un `ConditionalOrder`, el `ConditionalOrderEvaluationJob` llama a
`MarketSellService.placeMarketSell(investorId, symbol, quantity)` in-process. Esto genera una
`market_order` de tipo MARKET_SELL normal que pasa por el mismo flujo de ejecución.

**Rationale**: Reutiliza la lógica de venta ya implementada en AB-20.

### Slippage (gap down)

**Decision**: Si el precio abre por debajo del stop-loss (gap), la orden de venta generada se
ejecuta al mejor precio disponible (mercado). No se rechaza. Se documenta en el log de auditoría.

### Condición de posición activa

**Decision**: Un `ConditionalOrder` solo puede crearse si el inversionista tiene `quantity` títulos
disponibles (verificado contra portfolio module). Si el inversionista vende manualmente todos los
títulos, el job cancela automáticamente los `ConditionalOrder` activos para esa posición.

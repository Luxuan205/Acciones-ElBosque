# Research: AB-37 — Reporte de Ganancias y Pérdidas

## Decisiones técnicas

### Precio promedio ponderado (PPP)

**Decision**: Se recalcula en `PositionUpdateService` cada vez que se ejecuta una nueva compra:
`newAvgPrice = (existingQuantity × avgPrice + newQuantity × executionPrice) / (existingQuantity + newQuantity)`.
Se almacena en `Position.avgPurchasePrice`.

**Rationale**: Cálculo estándar de PPP. Al almacenarlo en la posición se evita recalcular sobre
todo el historial de transacciones en cada consulta.

### Ganancia no realizada

**Decision**: Calculada bajo demanda en `PortfolioService`:
`unrealizedGain = (currentPrice - avgPurchasePrice) × currentQuantity`.
El `currentPrice` se consulta in-process al `market-data` module (StockSnapshotService).

**Rationale**: No se almacena en base de datos — siempre reflejará el precio más actualizado.

### Ganancia realizada

**Decision**: Almacenada en la tabla `transaction` como campo calculado al ejecutar la venta:
`realizedGain = (executionPrice - avgPurchasePriceAtTime) × quantity - commission`.

### Exportación

**Decision**: El endpoint `/portfolio/report/export` devuelve un CSV generado en memoria con
los datos del período seleccionado. No se persiste el archivo exportado.

**Rationale**: Suficiente para el volumen académico. Para archivos grandes se podría usar streaming.

### Saldo disponible

**Decision**: `PortfolioFacade` también expone `getAvailableBalance(investorId)` = saldo total
almacenado en `Position.cashBalance` menos `BalanceReservation` activas del módulo `orders`.
Esto resuelve cómo el módulo `orders` valida fondos disponibles sin depender de la tabla `investor`.

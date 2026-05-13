# Research: AB-27 — Visualización de Portafolio de Inversiones

## Decision 1: Precio promedio ponderado (weighted average)
- **Decision**: `PositionCalculator.recalculateAvgPrice(oldQty, oldAvg, addedQty, addedPrice)` implementa: `newAvg = (oldQty * oldAvg + addedQty * addedPrice) / (oldQty + addedQty)`. Se actualiza en `Position.avgBuyPrice` cada vez que se ejecuta una compra.
- **Rationale**: El spec requiere precio promedio ponderado para múltiples compras del mismo símbolo. Es el método estándar en plataformas de trading para calcular el costo de posición.
- **Alternatives considered**: Precio de la última compra — incorrecto para varias compras; FIFO por lotes — complejo, requiere historial de lotes; LIFO — no es el estándar para COP.

## Decision 2: Enriquecimiento con precio actual in-process desde market-data
- **Decision**: `PortfolioService` inyecta el bean `StockSnapshotService` de market-data in-process para obtener `currentPrice` al construir `PositionDto`. Si el símbolo no tiene snapshot, `currentPrice = avgBuyPrice` (fallback seguro).
- **Rationale**: Arquitectura modular monolito — los módulos comparten classpath. La interfaz pública de market-data es el contrato. El fallback evita NPE si hay un símbolo sin precio actualizado.
- **Alternatives considered**: Llamada HTTP a market-data — viola principio I; almacenar `currentPrice` en Position — stale data; sin fallback — NullPointerException en edge cases.

## Decision 3: P&L calculado en memoria, no persistido
- **Decision**: `pnlAmount = (currentPrice - avgBuyPrice) * quantity`, `pnlPercent = (pnlAmount / (avgBuyPrice * quantity)) * 100`. Calculado en `PositionCalculator` al construir el DTO, nunca almacenado en DB.
- **Rationale**: El P&L cambia con cada actualización de precio de mercado; persistirlo requeriría actualizaciones frecuentes. El cálculo en memoria es instantáneo y siempre actualizado.
- **Alternatives considered**: P&L pre-calculado en DB — stale entre actualizaciones de precio; vista materializada — sobrediseño para proyecto académico.

## Decision 4: PortfolioSummaryDto con totales agregados
- **Decision**: `GET /portfolio/summary` devuelve `totalValue` (suma de `positionValue` de todas las posiciones), `totalPnl` y `dayChange` (suma de `dayChange * qty` de cada posición). Calculado en memoria por `PortfolioService`.
- **Rationale**: El inversionista necesita una vista rápida sin necesidad de sumar manualmente cada posición. Agregación en memoria es suficiente para la escala académica.
- **Alternatives considered**: Endpoint único que incluya el summary en holdings response — válido; tabla de resumen persistida — stale data.

## Decision 5: Position con constraint UNIQUE (investor_id, symbol)
- **Decision**: La tabla `position` tiene constraint UNIQUE sobre `(investor_id, symbol)`. `PositionRepository.findByInvestorIdAndSymbol(investorId, symbol)` para update-or-insert en cada ejecución de compra.
- **Rationale**: Un investor no puede tener dos posiciones del mismo símbolo; la unicidad en DB previene duplicados incluso con condiciones de carrera. El merge en servicio es correcto con el constraint.
- **Alternatives considered**: Múltiples filas por símbolo (lotes) — compleja gestión del promedio ponderado; sin constraint único — susceptible a duplicados.

# Research: AB-19 — Generación de Market Order de Compra

## Decisiones técnicas

### Módulo `orders` como Maven module separado

**Decision**: Nuevo Maven module `backend/orders` con su propio `pom.xml` agregado al parent.

**Rationale**: Las órdenes (compra, venta, limit, stop-loss, cancelación) forman un bounded context
bien definido con entidades propias. Separarlo de `market-data` evita que ese módulo crezca sin límite.

### Reserva de saldo

**Decision**: Tabla `balance_reservation` ligada 1-to-1 con la orden. El saldo disponible del
inversionista = saldo total − suma de reservas activas. Gestionado en el módulo `orders`.

**Rationale**: Evita doble gasto (FR-005). La reserva se libera al cancelar o ejecutar la orden.

**Nota**: La gestión del saldo real (acreditaciones/débitos) será responsabilidad del módulo
`portfolio` (AB-26). Por ahora, el saldo disponible se consulta in-process al `portfolio` module.
En ausencia de `portfolio`, se simula con un campo `available_balance` en `investor`.

### Precio de mercado

**Decision**: `MarketOrderService` llama al `StockSnapshotService` del módulo `market-data`
in-process para obtener el precio estimado. El precio de ejecución real se registra al confirmar
la orden (puede diferir del estimado).

### QUEUED vs. PENDING

**Decision**: Si `MarketHoursGate.isMarketOpen()` devuelve `false`, la orden se crea con estado
QUEUED. Si el mercado está abierto, se crea con estado PENDING. La transición QUEUED → PENDING
la maneja el job de apertura de mercado (AB-24 — fuera de scope de este spec).

### Comisiones

**Decision**: El cálculo de comisiones usa las reglas del módulo `configuration` in-process
(facade `CommissionCalculator`). Para este spec, se implementa con una tasa fija configurable.
El módulo AB-25 refinará las reglas.

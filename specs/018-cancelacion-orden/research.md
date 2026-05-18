# Research: AB-23 — Cancelación de Orden

## Decisiones técnicas

### Race condition ejecución/cancelación

**Decision**: Optimistic locking con `@Version` en la entidad `Order`. Si dos transacciones
intentan modificar la misma orden simultáneamente (ejecución + cancelación), una recibirá
`OptimisticLockException`. La cancelación la interpreta como "la orden ya fue modificada" y
devuelve el estado actual al usuario.

**Rationale**: Solución estándar JPA sin necesidad de locks pesimistas ni colas de mensajes.
Para el volumen académico es suficiente.

**Migration needed**: Añadir columna `version BIGINT NOT NULL DEFAULT 0` a `market_order` (V20).

### Liberación de recursos

**Decision**: En una sola transacción: (1) cambiar `order.status = CANCELLED`, (2) marcar
`balance_reservation.released = TRUE` o `title_reservation.released = TRUE`, (3) registrar
evento de auditoría. Si algún paso falla, la transacción hace rollback completo.

### Cancelación de órdenes condicionales

**Decision**: `OrderCancellationService` cancela tanto `market_order` (con reservas) como
`conditional_order` (sin reservas de balance/títulos — solo deja de monitorearse). Se usa el
mismo endpoint pero con tipo diferente en la ruta: `/orders/conditional/{id}` ya existe en AB-22.

### Cancelación masiva

**Decision**: Se procesan en secuencia dentro de una transacción por orden. Si alguna falla
(por estar ya EXECUTED), se reporta en el resumen pero no se revierte el resto.

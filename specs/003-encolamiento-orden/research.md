# Research: AB-24 — Encolamiento de Orden Fuera de Horario Bursátil

## Decision 1: @Scheduled cron para procesamiento FIFO al abrir el mercado
- **Decision**: `OrderQueueProcessor` usa `@Scheduled(cron = "0 * * * * MON-FRI")` (cada minuto, días hábiles). Consulta `MarketStatusService.isMarketOpen()` y solo procesa si el mercado está abierto y existen órdenes QUEUED.
- **Rationale**: La apertura exacta depende del horario configurado en `configuration-service`; el cron dispara frecuentemente pero solo actúa cuando el gate de estado es `true`. Evita hard-codear hora de apertura en order.
- **Alternatives considered**: `@Scheduled` fijo a las 09:00 — acopla order a la configuración del mercado; evento de dominio — sobrediseño para proyecto académico sin message broker.

## Decision 2: Límite de 10 órdenes en cola por inversionista
- **Decision**: Validado en `OrderService.createOrder()` con `orderRepository.countByInvestorIdAndStatus(investorId, QUEUED)` antes de persistir. Devuelve HTTP 422 si el límite está alcanzado.
- **Rationale**: Validación en capa de servicio garantiza atomicidad con la inserción dentro de `@Transactional`. Un constraint a nivel de DB sería menos descriptivo para el usuario.
- **Alternatives considered**: Constraint DB CHECK — da error genérico 500; validación en controller — no thread-safe sin transacción.

## Decision 3: Estado QUEUED en el mismo campo status del Order
- **Decision**: `OrderStatus` enum: `QUEUED | ACTIVE | EXECUTED | FAILED | CANCELLED`. La misma tabla `order` soporta todos los estados.
- **Rationale**: Una sola tabla con campo status simplifica queries y evita JOINs. FIFO se garantiza con `ORDER BY created_at ASC`.
- **Alternatives considered**: Tabla separada `queued_order` — duplica modelo y complicaría las transacciones; estado en campo booleano — limita extensibilidad.

## Decision 4: Consulta del estado del mercado in-process
- **Decision**: `MarketStatusService` en order inyecta el bean publicado por `configuration-service` in-process. No hay llamada HTTP.
- **Rationale**: Arquitectura modular monolito — los módulos comparten el mismo classpath en runtime. La interfaz pública `MarketStatusService.isMarketOpen()` es el contrato definido en configuration-service.
- **Alternatives considered**: Llamada REST a configuration-service — viola el principio I (Module Cohesion); duplicar la lógica de horario en order — viola el principio de single source of truth.

## Decision 5: Cancelación solo de órdenes en estado QUEUED
- **Decision**: `DELETE /orders/{id}/cancel` verifica `order.getStatus() == QUEUED`; si no, devuelve HTTP 409 (Conflict) con mensaje descriptivo.
- **Rationale**: Una orden ACTIVE ya fue aceptada por el mercado; cancelarla requiere un flujo diferente (fuera del alcance). HTTP 409 es el código correcto para conflicto de estado del recurso.
- **Alternatives considered**: HTTP 400 — semánticamente incorrecto (no es un error de request); HTTP 422 — aceptable pero 409 es más preciso para conflictos de estado.

# Research: AB-36 — Watchlist de Acciones (Funcionalidad Premium)

## Decision 1: PremiumSubscriptionGate verifica suscripción in-process
- **Decision**: `PremiumSubscriptionGate.isPremiumActive(investorId)` consulta el bean `InvestorRepository` de `auth-security-service` in-process para verificar `investor.subscriptionType == PREMIUM && investor.subscriptionExpiresAt > now()`. Lanza `AccessDeniedException` → HTTP 403 si no es premium activo.
- **Rationale**: Arquitectura modular monolito. El estado de suscripción es dato de auth-security-service; market-data lo accede in-process sin HTTP. La verificación en capa de servicio (antes de cualquier operación) garantiza que el gate se aplica uniformemente.
- **Alternatives considered**: Verificación en controller con @PreAuthorize — menos testeable; claim en JWT — puede estar expirado sin reflejar el estado real de la suscripción; HTTP a auth-security-service — viola principio I.

## Decision 2: Watchlist creada on-demand (lazy creation)
- **Decision**: `WatchlistService.getOrCreateWatchlist(investorId)` verifica si ya existe una `Watchlist` para el investor; si no, la crea en la misma transacción antes de agregar la primera entrada.
- **Rationale**: No tiene sentido crear la watchlist al registrar el investor (podría no usarla nunca). La creación lazy evita filas vacías y simplifica el registro.
- **Alternatives considered**: Crear watchlist en el registro del investor — acopla módulos innecesariamente; exigir creación explícita por el usuario — agrega un paso innecesario de UX.

## Decision 3: Límite de 50 entradas validado con countByWatchlistId
- **Decision**: `WatchlistEntryRepository.countByWatchlistId(watchlistId)` antes de insertar. Si el conteo es ≥ 50, devuelve HTTP 422 con mensaje "Watchlist limit reached (max 50 symbols)".
- **Rationale**: Validación en capa de servicio dentro de `@Transactional` garantiza atomicidad con la inserción. HTTP 422 (Unprocessable Entity) es el código correcto para una regla de negocio violada.
- **Alternatives considered**: Constraint DB CHECK — da error genérico 500; validación en controller — no thread-safe; límite configurable en application.yaml — sobrediseño para este requisito fijo.

## Decision 4: Watchlist preservada al expirar la suscripción
- **Decision**: `WatchlistEntry` y `Watchlist` NO se eliminan cuando expira la suscripción. Solo se bloquea el acceso mediante `PremiumSubscriptionGate`. Al renovar, la watchlist previa está disponible automáticamente.
- **Rationale**: El spec lo especifica explícitamente como constraint. Preservar la watchlist mejora la UX de renovación y es más seguro (no pérdida de datos del usuario).
- **Alternatives considered**: Eliminar al expirar y restaurar en renovación — pérdida de datos; archivar en tabla separada — innecesariamente complejo.

## Decision 5: Enriquecimiento de precios actuales en WatchlistResponse
- **Decision**: `WatchlistService.getWatchlist()` construye cada entrada de `WatchlistResponse` inyectando `currentPrice`, `dayChange` y `lastUpdated` del bean `StockSnapshotService` de market-data in-process. Si el snapshot no existe, devuelve `null` para esos campos (no falla).
- **Rationale**: La watchlist es una vista enriquecida del catálogo de acciones. El enriquecimiento in-process es el patrón correcto en el monolito modular. El fallback a null evita que una acción sin precio rompa toda la watchlist.
- **Alternatives considered**: Precio almacenado en WatchlistEntry — stale data inmediatamente; campo `priceAtAdded` para comparación — ya existe en el modelo, útil como referencia histórica.

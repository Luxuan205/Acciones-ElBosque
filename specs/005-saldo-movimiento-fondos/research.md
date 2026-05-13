# Research: AB-26 — Consulta de Saldo y Movimiento de Fondos

## Decision 1: Spring Data Pageable para historial de movimientos
- **Decision**: `FundMovementRepository.findByInvestorIdAndCreatedAtBetween(id, from, to, Pageable)` con `PageRequest.of(page, 20)`. El response devuelve `Page<FundMovement>` serializado como `FundMovementPageResponse`.
- **Rationale**: Spring Data Pageable elimina SQL manual de paginación. El tamaño fijo de 20 registros/página es consistente con el spec (FR-007). `PageImpl` serializa automáticamente `totalPages` y `totalElements`.
- **Alternatives considered**: Paginación manual con LIMIT/OFFSET en query nativa — más frágil, sin conteo automático; `@Query` con paginación custom — solo si los métodos derivados fueran insuficientes (aquí son suficientes).

## Decision 2: balanceAfter en cada FundMovement
- **Decision**: `FundMovement.balanceAfter` es el saldo de la cuenta después de aplicar el movimiento. Se calcula en `FundMovementService` al crear el movimiento y se persiste.
- **Rationale**: Hace el historial self-contained: el cliente puede reconstruir el saldo en cualquier punto sin recalcular. Útil para auditoría y para renderizar el historial con saldo acumulado.
- **Alternatives considered**: Calcular balanceAfter al consultar (running sum en SQL) — costoso para historiales largos; solo almacenar el amount — no permite auditoría punto a punto.

## Decision 3: Tipos de movimiento como enum
- **Decision**: `MovementType` enum: `DEPOSIT | WITHDRAWAL | PURCHASE | SALE | COMMISSION`. Almacenado como VARCHAR en DB.
- **Rationale**: Cubre todos los flujos de dinero del sistema según el spec. El tipo COMMISSION permite desglosar comisiones separadas del PURCHASE.
- **Alternatives considered**: String libre — sin control de valores válidos; tabla de tipos — sobrediseño para conjunto fijo y pequeño.

## Decision 4: Saldo reservado calculado dinámicamente
- **Decision**: `BalanceService.getReservedBalance(investorId)` consulta `OrderRepository` por la suma del valor de órdenes con estado `ACTIVE | QUEUED`. Este valor se resta del total para calcular el disponible.
- **Rationale**: El saldo reservado cambia con cada orden; calcularlo en tiempo real evita desincronización. Es una consulta simple y rápida en un proyecto académico.
- **Alternatives considered**: Campo `reservedBalance` en `AccountBalance` — requiere actualización sincronizada con cada orden (complejo y propenso a inconsistencias); evento de dominio — sobrediseño.

## Decision 5: Moneda COP almacenada como VARCHAR(3)
- **Decision**: `AccountBalance.currency = "COP"` fijo en seed. `FundMovement.currency` hereda el valor del balance del investor.
- **Rationale**: El spec establece moneda única COP. Almacenar el código permite extensión futura sin cambio de esquema, aunque para el proyecto académico siempre será "COP".
- **Alternatives considered**: Enum de moneda — innecesario para una sola moneda; sin campo de moneda — dificulta extensión futura.

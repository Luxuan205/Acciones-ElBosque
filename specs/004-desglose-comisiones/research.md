# Research: AB-25 — Visualización y Desglose de Comisiones

## Decision 1: Tasas de comisión en tabla DB (commission_rate)
- **Decision**: Tabla `commission_rate` con columnas `subscription_type` (STANDARD | PREMIUM) y `rate_percent` (DECIMAL). Sembrada por Flyway: STANDARD = 1.50, PREMIUM = 0.80.
- **Rationale**: Las tasas pueden cambiar sin redeploy. El tipo de suscripción del investor se lee del JWT (claim `subscriptionType`) para seleccionar la tasa correcta.
- **Alternatives considered**: Tasas hard-coded en `CommissionCalculatorService` — no permite cambio sin redeploy; tabla de historial de tasas — sobrediseño para proyecto académico.

## Decision 2: BigDecimal para todos los valores monetarios
- **Decision**: `grossValue`, `commissionAmount` y `netTotal` son `BigDecimal` con escala 2 (COP no usa decimales, pero la precisión evita errores de redondeo en el cálculo de comisiones).
- **Rationale**: El SC-002 exige 100% de exactitud entre lo mostrado y lo cobrado. `double`/`float` tienen errores de representación binaria inaceptables para dinero.
- **Alternatives considered**: `long` en centavos — válido pero requiere conversión en todos los DTOs; `double` — viola exactitud monetaria.

## Decision 3: Anti-tampering — recálculo en servidor al confirmar
- **Decision**: Al confirmar la orden (`POST /orders`), `CommissionCalculatorService` recalcula con los mismos parámetros. El preview del cliente no se reutiliza.
- **Rationale**: El cliente podría manipular el monto del preview en la request de confirmación. El recálculo en servidor garantiza que el monto cobrado == el monto calculado por el sistema.
- **Alternatives considered**: Hash firmado del preview — válido pero agrega complejidad de criptografía; confiar en el preview del cliente — violación de seguridad (SC-002).

## Decision 4: OrderPreviewResponse no se persiste
- **Decision**: `OrderPreviewResponse` es un DTO de cálculo devuelto en el response de `POST /orders/preview`. No existe tabla `order_preview` ni se guarda en base de datos.
- **Rationale**: El preview es un paso informativo sin efecto de lado. Almacenarlo agregaría complejidad sin valor: el recálculo en confirmar es más simple y seguro.
- **Alternatives considered**: Persistir preview con TTL — agrega limpiezas periódicas innecesarias; session attribute — no aplica en API stateless.

## Decision 5: Precio unitario obtenido de market-data in-process
- **Decision**: `CommissionCalculatorService` recibe `unitPrice` como parámetro del request (provisto por el cliente desde el dashboard de acciones). El servicio no consulta precios por sí mismo.
- **Rationale**: El preview es inmediato (< 500ms). El precio en el request es el mostrado al usuario; si cambia antes de confirmar, el recálculo en la confirmación usa el precio al momento de ejecutar.
- **Alternatives considered**: Consultar market-data en el preview — agrega latencia y acoplamiento; precio fijo en la orden — no refleja el mercado real al momento de ejecución.

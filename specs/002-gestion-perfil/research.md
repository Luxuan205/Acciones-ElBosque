# Research: AB-17 — Gestión de Perfil de Usuario

## Decision 1: Verificación de contraseña actual antes de cambio
- **Decision**: Usar `BCryptPasswordEncoder.matches(rawCurrent, storedHash)` antes de guardar el nuevo hash.
- **Rationale**: BCrypt es el encoder ya configurado en auth-security-service para el registro (AB-15). La verificación en capa de servicio evita una segunda llamada a base de datos.
- **Alternatives considered**: Token de un solo uso para cambio de contraseña — innecesario para un cambio en sesión autenticada; agrega complejidad sin beneficio de seguridad adicional.

## Decision 2: Campos read-only (email, documentNumber)
- **Decision**: `ProfileController` ignora `email` y `documentNumber` en el DTO de actualización; `UpdatePersonalDataRequest` no los incluye. La capa de servicio no los toca.
- **Rationale**: El contrato lo define la spec (FR-001): estos campos son identificadores del investor, cambiarlos requeriría reverificación de identidad fuera del alcance.
- **Alternatives considered**: Permitir cambio con flujo de re-verificación — fuera del alcance del sprint; se especifica explícitamente como constraint.

## Decision 3: InvestorPreferences como tabla separada (1-to-1)
- **Decision**: Tabla `investor_preferences` con FK `investor_id UNIQUE`. Creada on-demand al primer acceso si no existe.
- **Rationale**: Evita agregar columnas nullable a la tabla `investor` que ya existe. Permite evolucionar el modelo de preferencias sin alterar la tabla principal.
- **Alternatives considered**: Columnas adicionales en `investor` — más simple pero acopla esquemas de entidades distintas; columnas JSON — dificulta validación y consultas.

## Decision 4: ProfileChangeLog con campo y valores anterior/nuevo
- **Decision**: Tabla `profile_change_log` con columnas: `investor_id`, `field_name`, `old_value`, `new_value`, `changed_at`. Para contraseña, `old_value` y `new_value` se registran como `[REDACTED]`.
- **Rationale**: Auditoría interna requerida por FR-011. El enmascaramiento de contraseñas evita exponer hashes en el log.
- **Alternatives considered**: Log sin valores anterior/nuevo — no cumple el requisito de auditoría; evento de dominio en tabla separada — sobrediseño para proyecto académico.

## Decision 5: Restricción de longitud y formato en DTO
- **Decision**: `fullName` @Size(min=2, max=100); `phone` @Pattern(regexp="^\\+?[0-9\\s\\-]{7,20}$") nullable; `language` enum {"es","en"}; `notifChannel` enum {"EMAIL","SMS","NONE"}.
- **Rationale**: Validación en capa de controller con Bean Validation. Valores de enum validados sin @Enumerated para devolver 400 claro en lugar de 500.
- **Alternatives considered**: Validación solo en base de datos — devuelve errores no controlados; validación manual — duplica lógica que Bean Validation ya provee.

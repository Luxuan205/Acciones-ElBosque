# Research: AB-41 — Gestión de Usuarios por el Administrador

## Decisiones técnicas

### Campo role persistido

**Decision**: Añadir columna `role VARCHAR(20) NOT NULL DEFAULT 'INVESTOR'` a la tabla `investor`.
El `JwtService` lo leerá de la base de datos al autenticar, en lugar de asumir siempre INVESTOR.

**Rationale**: Sin persistir el rol, cambiar el rol de un usuario no tiene efecto hasta que se
regenere el JWT manualmente. Con el campo en DB, el JWT nuevo emitido en el próximo login refleja
el rol actualizado.

### Invalidación de sesión al suspender

**Decision**: Al suspender un usuario: (1) cambiar `accountStatus = SUSPENDED`, (2) eliminar todas
sus `mfa_session` activas (WHERE `investor_id = ?`). En el próximo request del usuario, el JWT
seguirá siendo válido hasta su expiración natural. Para invalidación inmediata completa, se puede
añadir una lista de JWTs revocados (fuera del alcance del MVP).

**Rationale**: La invalidación completa de JWT requiere una lista de revocación (Redis o tabla DB)
que añade complejidad. Para el MVP, la suspensión evita nuevos logins; el JWT existente expira
naturalmente según su TTL.

### Búsqueda con filtros dinámicos

**Decision**: `JpaSpecificationExecutor<Investor>` con `InvestorSpecification` que construye
predicados dinámicos para los filtros: `email`, `accountStatus`, `subscriptionType`, `role`, `fullName`.

**Rationale**: Evita múltiples métodos de repositorio con combinaciones de parámetros.

### Restablecimiento de contraseña por admin

**Decision**: Reutilizar `VerificationToken` con un nuevo campo `token_type` (`VERIFICATION` | `PASSWORD_RESET`).
El admin genera un token de `PASSWORD_RESET` que se envía al correo del usuario. El admin nunca
ve ni establece la contraseña directamente.

**Migration**: V30 añade `token_type` a `verification_token` y `role` a `investor`.

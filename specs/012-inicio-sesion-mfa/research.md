# Research: AB-16 — Inicio de Sesión con MFA

## Decisiones técnicas

### OTP: TOTP (RFC 6238) vs. OTP por correo

**Decision**: OTP por correo electrónico como canal por defecto; TOTP (Google Authenticator)
como canal alternativo configurado por el usuario en preferencias.

**Rationale**: La infraestructura de correo ya existe (MailService en `auth`). TOTP requiere
registro de secreto TOTP en el perfil del usuario — se añade `totpSecret` nullable en `Investor`.

**Alternatives considered**: SMS OTP descartado (requiere integración de proveedor externo de SMS,
costo y complejidad fuera del alcance académico).

### Sesión de pre-autenticación

**Decision**: `MfaSession` almacenada en base de datos con TTL de 5 minutos (configurable).
Identificada por un token opaco (UUID) devuelto al completar el primer factor.

**Rationale**: Stateless entre peticiones — el frontend envía el `sessionToken` junto con el OTP.
Evita uso de HttpSession del lado del servidor.

**Alternatives considered**: Redis para sesiones temporales — descartado por añadir infraestructura
no justificada en proyecto académico.

### Bloqueo de cuenta

**Decision**: Columnas `failed_attempts INT` y `locked_until TIMESTAMP` en la tabla `investor`.
Bloqueo automático tras N intentos fallidos consecutivos (default 5, configurable en parámetros
globales AB-40). El bloqueo dura 30 minutos por defecto.

**Rationale**: Simple, sin dependencias externas. El administrador puede desbloquear manualmente
poniendo `failed_attempts = 0` y `locked_until = NULL`.

### JWT: rol en el token

**Decision**: El JWT incluye el claim `role` (INVESTOR, BROKER, ADMIN) y el claim `sub` con el
investorId (Long). Firmado con HS256 usando el secreto `app.jwt.secret`.

**Rationale**: Consistente con JwtAuthenticationFilter existente en el módulo.

### AccountStatus: SUSPENDED y BLOCKED

**Decision**: Añadir valores SUSPENDED (suspensión administrativa) y BLOCKED (bloqueo automático
por intentos fallidos) al enum `AccountStatus`.

**Rationale**: El spec distingue bloqueo automático (intentos) de suspensión manual (admin).
BLOCKED se usa para bloqueo de fuerza bruta; SUSPENDED para acción administrativa (AB-41).

**Nota**: En PostgreSQL, `account_status` es VARCHAR(20) — no requiere ALTER TYPE, solo documentar
los nuevos valores válidos.

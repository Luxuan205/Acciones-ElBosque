# Data Model: AB-41 — Gestión de Usuarios por el Administrador

## Modificaciones a entidades existentes

### Investor (añadir columna role)
```
investor ← añadir
└── role  VARCHAR(20)  NOT NULL DEFAULT 'INVESTOR'  ('INVESTOR' | 'BROKER' | 'ADMIN')
```

### VerificationToken (añadir tipo)
```
verification_token ← añadir
└── token_type  VARCHAR(20)  NOT NULL DEFAULT 'VERIFICATION'  ('VERIFICATION' | 'PASSWORD_RESET')
```

## Nuevo enum

### InvestorRole
```java
public enum InvestorRole {
    INVESTOR, BROKER, ADMIN
}
```

## Flyway Migrations

### V30__add_role_and_token_type.sql
```sql
ALTER TABLE investor
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'INVESTOR';

ALTER TABLE verification_token
    ADD COLUMN token_type VARCHAR(20) NOT NULL DEFAULT 'VERIFICATION';
```

## Java DTOs

```java
record AdminUserSummaryDto(
    Long id,
    String fullName,
    String email,
    String accountStatus,
    String role,
    String subscriptionType,
    LocalDateTime createdAt
)

record AdminUserDetailDto(
    Long id,
    String fullName,
    String email,
    String documentNumber,
    String phone,
    String accountStatus,
    String role,
    String subscriptionType,
    LocalDateTime subscriptionExpiresAt,
    LocalDateTime createdAt,
    List<AuditEventDto> recentActivity   // últimos 10 eventos de auditoría del usuario
)

record UpdateUserStatusRequest(
    @NotNull AccountStatus newStatus,  // solo ACTIVE o SUSPENDED (BLOCKED lo gestiona el sistema)
    @NotBlank String reason
)

record UpdateUserRoleRequest(
    @NotNull InvestorRole newRole,
    @NotBlank String reason,
    String adminConfirmation           // requerido para cambios a ADMIN ("CONFIRM_ADMIN_ROLE")
)
```

## Business Rules
- Suspender: valida que `accountStatus != SUSPENDED` y que no sea el último ADMIN activo
- Desbloquear (BLOCKED → ACTIVE): resetea `failedAttempts = 0`, `lockedUntil = NULL`
- Cambio de rol a ADMIN: requiere `adminConfirmation = "CONFIRM_ADMIN_ROLE"` en el request
- No se puede cambiar el rol del único ADMIN activo a otro rol
- Restablecimiento de contraseña: genera `VerificationToken` con `tokenType = PASSWORD_RESET`; envía email al usuario; el admin no ve ni establece la contraseña
- Historial de actividad reciente: últimos 10 registros de `audit_event WHERE investor_id = ?`

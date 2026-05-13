# Feature Specification: Inicio de Sesión con MFA

**Feature Branch**: `012-inicio-sesion-mfa`  
**Created**: 2026-05-12  
**Status**: Draft  
**Jira**: AB-16  

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Autenticación básica con credenciales (Priority: P1)

El inversionista registrado ingresa su correo y contraseña en la pantalla de login. Si las credenciales son correctas, la plataforma emite un token de acceso y redirige al dashboard principal. Si son incorrectas, se muestra un mensaje de error genérico (sin revelar si el correo existe o no).

**Why this priority**: Sin autenticación no hay acceso a ninguna funcionalidad de la plataforma. Es el punto de entrada de todos los usuarios.

**Independent Test**: Puede probarse de forma aislada creando un usuario activo e intentando iniciar sesión con credenciales correctas e incorrectas, verificando el acceso o rechazo según corresponda.

**Acceptance Scenarios**:

1. **Given** un inversionista con cuenta activa, **When** ingresa correo y contraseña correctos, **Then** recibe un token de acceso válido y accede al dashboard.
2. **Given** un inversionista con cuenta activa, **When** ingresa contraseña incorrecta, **Then** recibe un mensaje de error genérico y no obtiene acceso.
3. **Given** un inversionista con cuenta PENDING (no verificada), **When** intenta iniciar sesión, **Then** recibe un mensaje indicando que debe verificar su correo antes de acceder.
4. **Given** un correo no registrado, **When** intenta iniciar sesión, **Then** recibe el mismo mensaje de error genérico (sin revelar que el correo no existe).

---

### User Story 2 - Segundo factor de autenticación (MFA) (Priority: P2)

Después de validar correctamente las credenciales, el sistema solicita un código de verificación de un solo uso enviado al canal preferido del usuario (correo o aplicación TOTP). Solo tras ingresar el código válido se emite el token de acceso.

**Why this priority**: MFA es el diferenciador de seguridad del sistema, especialmente relevante para una plataforma financiera donde el no repudio es un requisito regulatorio.

**Independent Test**: Puede probarse habilitando MFA en un usuario de prueba, completando el primer factor exitosamente y verificando que el token solo se emite tras el segundo factor correcto.

**Acceptance Scenarios**:

1. **Given** un usuario con MFA habilitado que superó el primer factor, **When** ingresa el código OTP correcto dentro del tiempo de vigencia, **Then** recibe el token de acceso.
2. **Given** un usuario con MFA habilitado, **When** ingresa un código OTP incorrecto o expirado, **Then** se rechaza el acceso y puede reintentar.
3. **Given** un usuario con MFA habilitado, **When** no ingresa el código en el tiempo límite, **Then** la sesión de pre-autenticación expira y debe volver a ingresar sus credenciales.

---

### User Story 3 - Bloqueo por intentos fallidos consecutivos (Priority: P3)

Después de un número configurable de intentos fallidos consecutivos (credenciales incorrectas o códigos MFA inválidos), la cuenta se bloquea temporalmente para proteger contra ataques de fuerza bruta.

**Why this priority**: Protección crítica para cuentas financieras, pero el umbral y duración son configurables, por lo que no bloquea el MVP.

**Independent Test**: Puede probarse configurando el umbral a 3 intentos, ejecutando tres fallos consecutivos y verificando que el cuarto intento (incluso con credenciales correctas) es rechazado hasta que se cumpla el período de bloqueo.

**Acceptance Scenarios**:

1. **Given** una cuenta que ha acumulado el máximo de intentos fallidos, **When** se intenta iniciar sesión (incluso con credenciales correctas), **Then** la cuenta está bloqueada temporalmente y se informa al usuario.
2. **Given** una cuenta bloqueada temporalmente, **When** transcurre el período de bloqueo, **Then** la cuenta se desbloquea automáticamente y el usuario puede volver a intentarlo.
3. **Given** una cuenta bloqueada, **When** el administrador desbloquea manualmente la cuenta, **Then** el usuario puede iniciar sesión nuevamente.

---

### Edge Cases

- ¿Qué pasa si el correo de OTP llega tarde o no llega? El usuario puede solicitar reenvío con un límite de frecuencia.
- ¿Qué pasa si el usuario cambia de canal preferido mientras tiene una sesión de MFA pendiente? Se usa el canal al momento de iniciar la autenticación.
- ¿Qué pasa si el token de acceso emitido expira? Se requiere volver a iniciar sesión; no hay refresh token implícito.
- ¿Qué pasa si el usuario intenta iniciar sesión desde múltiples dispositivos simultáneamente? Cada autenticación exitosa genera su propio token independiente.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE autenticar a los usuarios mediante correo electrónico y contraseña como primer factor.
- **FR-002**: El sistema DEBE validar que la cuenta esté en estado ACTIVE antes de permitir el acceso; las cuentas PENDING, SUSPENDED o BLOCKED reciben mensajes específicos.
- **FR-003**: Los mensajes de error de credenciales incorrectas DEBEN ser genéricos (no revelar si el correo existe).
- **FR-004**: El sistema DEBE soportar un segundo factor de autenticación (MFA) mediante código OTP de un solo uso con vigencia limitada.
- **FR-005**: El OTP DEBE enviarse por el canal preferido del usuario (correo electrónico o aplicación compatible con TOTP); si el canal es NONE, se usa correo por defecto.
- **FR-006**: El token de acceso DEBE emitirse únicamente tras la validación exitosa de ambos factores.
- **FR-007**: El sistema DEBE bloquear temporalmente una cuenta tras un número configurable de intentos fallidos consecutivos.
- **FR-008**: El sistema DEBE registrar en el log de auditoría cada intento de inicio de sesión (exitoso y fallido), incluyendo timestamp e identificador de dispositivo/IP.
- **FR-009**: El token de acceso DEBE incluir el rol del usuario (INVESTOR, BROKER, ADMIN) para autorización en otros módulos.
- **FR-010**: El sistema DEBE permitir al administrador desbloquear cuentas bloqueadas manualmente.

### Key Entities

- **Sesión de pre-autenticación**: Estado temporal entre la validación del primer factor y la emisión del token; tiene expiración propia.
- **Token de acceso**: Credencial de sesión con tiempo de vida definido que contiene identidad y roles del usuario.
- **Intento de inicio de sesión**: Registro de auditoría de cada intento con resultado, timestamp, y fuente.
- **Código OTP**: Código numérico de un solo uso con vigencia limitada para el segundo factor.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El flujo completo de inicio de sesión (ambos factores) se completa en menos de 60 segundos bajo condiciones normales de red.
- **SC-002**: Los códigos OTP expiran en un máximo de 5 minutos desde su generación.
- **SC-003**: Las cuentas se bloquean automáticamente tras 5 intentos fallidos consecutivos (valor por defecto configurable).
- **SC-004**: El 100% de los intentos de inicio de sesión (exitosos y fallidos) quedan registrados en el log de auditoría.
- **SC-005**: Un usuario no puede obtener acceso con credenciales correctas si su cuenta está en estado PENDING, SUSPENDED o BLOCKED.

## Assumptions

- El sistema de correo electrónico para envío de OTP ya está operativo (usado en el módulo de registro AB-15).
- El inversionista ya completó el proceso de registro y verificación de cuenta (AB-15).
- MFA está habilitado por defecto para todos los usuarios; no es opcional desactivarlo.
- La vigencia del token de acceso se configura en los parámetros globales del sistema (AB-40).
- El canal preferido de notificación se gestiona en el módulo de gestión de perfil (AB-17).
- El soporte para aplicación TOTP (Google Authenticator, Authy) está en alcance para P2; en su defecto, OTP por correo cubre el segundo factor.

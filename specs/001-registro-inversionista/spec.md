# Feature Specification: Registro de Inversionista en la Plataforma

**Feature Branch**: `001-registro-inversionista`
**Jira**: AB-15
**Created**: 2026-05-10
**Status**: Draft
**Asignado a**: Dylan Mathyus Hospital Herrera

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Registro exitoso de nuevo inversionista (Priority: P1)

Un nuevo usuario que desea invertir en la bolsa de valores accede a la plataforma
por primera vez y completa el formulario de registro proporcionando sus datos
personales y credenciales. Al terminar, recibe una confirmación y puede iniciar
sesión para empezar a operar.

**Why this priority**: Sin registro no existe ningún otro flujo del sistema. Es el
punto de entrada obligatorio para todos los demás casos de uso.

**Independent Test**: Un usuario completa el formulario con datos válidos, el sistema
crea la cuenta y envía correo de verificación. El usuario puede iniciar sesión
después de verificar su correo.

**Acceptance Scenarios**:

1. **Given** un visitante no registrado en la página de registro,
   **When** completa todos los campos obligatorios con datos válidos y envía el formulario,
   **Then** el sistema crea la cuenta, envía un correo de verificación a la dirección
   proporcionada y muestra un mensaje de confirmación indicando que debe verificar su correo.

2. **Given** un usuario que recibió el correo de verificación,
   **When** hace clic en el enlace de verificación dentro de las 24 horas,
   **Then** su cuenta queda activa y puede iniciar sesión en la plataforma.

3. **Given** un usuario con cuenta ya verificada,
   **When** intenta acceder a cualquier funcionalidad de la plataforma,
   **Then** puede hacerlo sin restricciones de registro.

---

### User Story 2 — Rechazo de datos inválidos o duplicados (Priority: P2)

El sistema protege la integridad de los datos rechazando registros con información
incorrecta o con correos que ya tienen cuenta, brindando mensajes claros al usuario.

**Why this priority**: La validación es parte esencial del flujo de registro y
previene cuentas duplicadas o con datos inconsistentes.

**Independent Test**: Intentar registrarse con un correo ya existente o con campos
vacíos resulta en mensajes de error específicos sin crear ninguna cuenta.

**Acceptance Scenarios**:

1. **Given** un formulario de registro con el campo de correo electrónico ya asociado
   a una cuenta existente,
   **When** el usuario envía el formulario,
   **Then** el sistema rechaza el registro y muestra el mensaje
   "Este correo ya está registrado. ¿Desea iniciar sesión?", sin crear duplicados.

2. **Given** un formulario con campos obligatorios vacíos o con formato inválido
   (correo sin @, contraseña muy corta),
   **When** el usuario intenta enviar el formulario,
   **Then** el sistema indica de forma específica qué campos requieren corrección,
   sin enviar el formulario.

3. **Given** un enlace de verificación de correo expirado (más de 24 horas),
   **When** el usuario hace clic en él,
   **Then** el sistema informa que el enlace expiró y ofrece reenviar uno nuevo.

---

### Edge Cases

- ¿Qué pasa si el correo de verificación nunca llega? El usuario puede solicitar
  reenvío después de 2 minutos.
- ¿Qué pasa si el usuario cierra el navegador antes de verificar? La cuenta queda
  en estado pendiente y puede verificarse desde el correo en cualquier momento
  dentro de las 24 horas.
- ¿Qué pasa si se registra con un número de documento ya existente? El sistema
  rechaza el registro con mensaje indicando que el documento ya tiene cuenta asociada.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE permitir a un visitante no autenticado acceder a la
  página de registro sin requerir inicio de sesión previo.
- **FR-002**: El sistema DEBE recopilar los siguientes datos obligatorios: nombre
  completo, número de documento de identidad, correo electrónico, contraseña y
  confirmación de contraseña.
- **FR-003**: El sistema DEBE validar que el correo electrónico tenga formato válido
  y no esté registrado previamente.
- **FR-004**: El sistema DEBE validar que la contraseña cumpla requisitos mínimos de
  seguridad: mínimo 8 caracteres, al menos una mayúscula, una minúscula y un número.
- **FR-005**: El sistema DEBE verificar que la contraseña y su confirmación sean
  idénticas antes de procesar el registro.
- **FR-006**: El sistema DEBE verificar que el número de documento de identidad no
  esté ya asociado a otra cuenta.
- **FR-007**: El sistema DEBE enviar un correo de verificación al correo registrado
  inmediatamente después de un registro exitoso.
- **FR-008**: El correo de verificación DEBE contener un enlace único con vigencia
  de 24 horas.
- **FR-009**: La cuenta DEBE quedar en estado "pendiente de verificación" hasta que
  el usuario haga clic en el enlace de verificación.
- **FR-010**: Una cuenta no verificada NO DEBE poder iniciar sesión en la plataforma.
- **FR-011**: El sistema DEBE ofrecer la opción de reenviar el correo de verificación
  si el usuario lo solicita, con un límite de 1 reenvío cada 2 minutos.
- **FR-012**: El sistema DEBE mostrar mensajes de error específicos y comprensibles
  para cada tipo de validación fallida, sin revelar si un correo existe en el sistema
  a través de mensajes ambiguos de seguridad.

### Key Entities

- **Inversionista**: Usuario de la plataforma con rol de inversionista. Atributos:
  nombre completo, número de documento, correo electrónico, contraseña (almacenada
  de forma segura), estado de cuenta (pendiente / activa), fecha de registro.
- **Token de verificación**: Código único asociado a un registro. Atributos: valor
  único, fecha de expiración (24h desde creación), estado (usado / vigente).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Un usuario nuevo puede completar el proceso de registro (formulario +
  verificación de correo) en menos de 3 minutos en condiciones normales.
- **SC-002**: El 100% de los intentos de registro con datos duplicados (correo o
  documento) son rechazados sin crear cuentas duplicadas.
- **SC-003**: El correo de verificación llega al buzón del usuario en menos de
  60 segundos tras completar el formulario en el 95% de los casos.
- **SC-004**: El 0% de las cuentas no verificadas puede acceder a funcionalidades
  de la plataforma.
- **SC-005**: El formulario de registro es completado exitosamente en el primer
  intento por al menos el 80% de los usuarios nuevos.

## Assumptions

- El registro está disponible únicamente para el rol de **inversionista**; el
  registro de comisionistas y administradores se gestiona por un flujo diferente
  (administración del sistema).
- Se asume que la plataforma cuenta con capacidad de envío de correos electrónicos
  transaccionales ya habilitada.
- El número de documento de identidad acepta cédula colombiana (numérica, 6-10 dígitos).
- No se requiere verificación de identidad adicional (KYC) en esta versión; solo
  verificación de correo electrónico.
- El frontend ya cuenta con una ruta pública para la página de registro accesible
  sin autenticación.
- Los requisitos de contraseña descritos son los mínimos aceptables; el equipo
  puede endurecerlos pero no relajarlos.

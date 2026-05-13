# Feature Specification: Gestión de Usuarios por el Administrador

**Feature Branch**: `026-gestion-usuarios`  
**Created**: 2026-05-12  
**Status**: Draft  
**Jira**: AB-41  

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consulta y búsqueda de usuarios (Priority: P1)

El administrador puede buscar y listar usuarios del sistema filtrando por nombre, correo, estado (ACTIVE, PENDING, SUSPENDED, BLOCKED) y tipo de suscripción (BASIC, PREMIUM). Puede ver el perfil completo de un usuario específico, incluyendo su historial de actividad reciente.

**Why this priority**: La gestión de usuarios es la función administrativa más frecuente. Sin búsqueda y consulta, el administrador no puede atender solicitudes de soporte ni supervisar la plataforma.

**Independent Test**: Crear usuarios con diferentes estados y suscripciones, buscar usando diferentes filtros y verificar que los resultados coinciden exactamente con los criterios aplicados.

**Acceptance Scenarios**:

1. **Given** un administrador que busca usuarios por correo electrónico, **When** ingresa el correo, **Then** ve el perfil del usuario con su estado, tipo de suscripción e historial de actividad reciente.
2. **Given** un administrador que filtra usuarios por estado SUSPENDED, **When** aplica el filtro, **Then** ve solo los usuarios en estado SUSPENDED con sus datos básicos.
3. **Given** un administrador que filtra por suscripción PREMIUM, **When** aplica el filtro, **Then** ve todos los usuarios con suscripción PREMIUM activa y la fecha de vencimiento de cada uno.

---

### User Story 2 - Modificación de estado de usuario (Priority: P1)

El administrador puede cambiar el estado de un usuario (activar, suspender, desbloquear) con un motivo registrado. Esta acción tiene efecto inmediato y queda registrada en el log de auditoría. Los usuarios suspendidos no pueden iniciar sesión ni realizar operaciones.

**Why this priority**: El control de estado de usuarios es esencial para la gestión de seguridad y cumplimiento. Permite al administrador responder a incidentes de seguridad o fraude.

**Independent Test**: Suspender un usuario activo, intentar iniciar sesión con ese usuario y verificar que el acceso es denegado con el mensaje adecuado.

**Acceptance Scenarios**:

1. **Given** un usuario en estado ACTIVE, **When** el administrador lo suspende con un motivo, **Then** el estado cambia a SUSPENDED de inmediato, el usuario pierde acceso a la plataforma y el evento queda registrado en auditoría.
2. **Given** un usuario en estado BLOCKED (por intentos fallidos), **When** el administrador lo desbloquea, **Then** el estado cambia a ACTIVE y el usuario puede volver a iniciar sesión.
3. **Given** un administrador que intenta suspender a otro administrador, **When** intenta la acción, **Then** el sistema bloquea la operación para proteger la integridad de los roles de administración.

---

### User Story 3 - Gestión de roles de usuario (Priority: P2)

El administrador puede cambiar el rol de un usuario (INVESTOR, BROKER, ADMIN) con las restricciones de negocio correspondientes. El cambio de rol tiene efecto en la próxima sesión del usuario. Se registra en auditoría con el rol anterior y el nuevo.

**Why this priority**: La gestión de roles es necesaria para incorporar nuevos brokers o administradores sin intervención técnica directa en la base de datos.

**Independent Test**: Cambiar el rol de un usuario INVESTOR a BROKER, cerrar la sesión del usuario, volver a iniciar sesión y verificar que el usuario tiene los permisos de BROKER.

**Acceptance Scenarios**:

1. **Given** un administrador que eleva el rol de un usuario INVESTOR a BROKER, **When** confirma el cambio, **Then** el nuevo rol aplica en la próxima sesión del usuario y el cambio queda en auditoría.
2. **Given** un administrador que intenta asignar el rol ADMIN a un usuario, **When** realiza la acción, **Then** se requiere una confirmación adicional por la sensibilidad del cambio, y el evento queda registrado con la identidad del administrador que autorizó.

---

### User Story 4 - Restablecimiento de contraseña por el administrador (Priority: P2)

El administrador puede iniciar un proceso de restablecimiento de contraseña en nombre de un usuario que no puede acceder a su correo registrado, siguiendo un proceso de verificación de identidad documentado.

**Why this priority**: El restablecimiento administrativo de contraseñas es necesario para soporte a usuarios bloqueados, pero requiere controles para evitar uso malintencionado.

**Independent Test**: Iniciar el restablecimiento de contraseña para un usuario, verificar que el usuario recibe un enlace de restablecimiento y que el administrador no puede ver ni establecer la contraseña directamente.

**Acceptance Scenarios**:

1. **Given** un usuario que no puede restablecer su contraseña de forma autónoma, **When** el administrador inicia el proceso de restablecimiento, **Then** el sistema envía un enlace de restablecimiento al correo registrado del usuario; el administrador no establece ni ve la contraseña.
2. **Given** un administrador que inicia el restablecimiento, **Then** el evento queda registrado en auditoría con la identidad del administrador y el usuario afectado.

---

### Edge Cases

- ¿Qué pasa si el administrador suspende a un usuario que tiene órdenes activas? Las órdenes activas permanecen en el mercado; el usuario no puede interactuar con ellas hasta ser reactivado. El administrador puede cancelar las órdenes manualmente si es necesario.
- ¿Qué pasa si se intenta cambiar el rol del único administrador del sistema? El sistema impide la acción si resultaría en cero administradores activos.
- ¿Qué pasa si un usuario es suspendido mientras tiene sesión activa? La sesión existente se invalida; el usuario es desconectado en el próximo request autenticado.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE permitir al administrador buscar usuarios por nombre, correo, estado y tipo de suscripción.
- **FR-002**: El sistema DEBE mostrar el perfil completo de un usuario incluyendo datos personales, estado, tipo de suscripción y historial de actividad reciente.
- **FR-003**: El sistema DEBE permitir al administrador cambiar el estado de un usuario (ACTIVE, SUSPENDED) con un motivo obligatorio registrado en auditoría.
- **FR-004**: El sistema DEBE permitir al administrador desbloquear cuentas en estado BLOCKED.
- **FR-005**: El sistema DEBE invalidar la sesión activa de un usuario inmediatamente tras su suspensión.
- **FR-006**: El sistema DEBE permitir al administrador cambiar el rol de un usuario (INVESTOR, BROKER, ADMIN), con confirmación adicional para la asignación del rol ADMIN.
- **FR-007**: El sistema DEBE impedir que el número de administradores activos llegue a cero mediante cambios de rol o suspensiones.
- **FR-008**: El sistema DEBE permitir al administrador iniciar el proceso de restablecimiento de contraseña de un usuario, enviando el enlace al correo registrado sin que el administrador pueda establecer la contraseña directamente.
- **FR-009**: Toda acción administrativa sobre usuarios DEBE quedar registrada en el log de auditoría con el administrador responsable, el usuario afectado, la acción y el timestamp.

### Key Entities

- **Perfil de usuario administrativo**: Vista del usuario desde la perspectiva del administrador, con datos completos, estado, rol, suscripción e historial de actividad.
- **Cambio de estado**: Transición del estado de un usuario entre ACTIVE, PENDING, SUSPENDED y BLOCKED, iniciada por el administrador con un motivo documentado.
- **Cambio de rol**: Modificación del nivel de acceso de un usuario (INVESTOR, BROKER, ADMIN) con efecto en la próxima sesión.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Los cambios de estado de usuario (suspensión, desbloqueo) tienen efecto en menos de 5 segundos, incluyendo la invalidación de sesión activa.
- **SC-002**: El 100% de las acciones administrativas sobre usuarios quedan registradas en el log de auditoría.
- **SC-003**: El sistema bloquea el 100% de los intentos de dejar el sistema sin administradores activos.
- **SC-004**: Las búsquedas de usuarios con filtros combinados retornan resultados en menos de 3 segundos para bases de datos de hasta 10.000 usuarios.

## Assumptions

- El módulo de gestión de perfil de usuario (AB-17) gestiona los datos del perfil; este módulo gestiona el estado, rol y acceso del usuario desde la perspectiva del administrador.
- El módulo de autenticación (AB-16) es el responsable de validar el estado del usuario en cada inicio de sesión; la suspensión aquí genera el rechazo en ese módulo.
- El proceso de restablecimiento de contraseña usa la infraestructura de correo del módulo de registro (AB-15).
- Los cambios de rol tienen efecto en la próxima sesión (no invalidación inmediata) para cambios de INVESTOR a BROKER; los cambios que implican reducción de permisos (ADMIN a INVESTOR) sí invalidan la sesión activa.
- El historial de actividad reciente en el perfil del usuario muestra los últimos 10 eventos de auditoría relacionados con ese usuario.

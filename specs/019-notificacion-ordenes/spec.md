# Feature Specification: Notificación de Estado de Órdenes

**Feature Branch**: `019-notificacion-ordenes`  
**Created**: 2026-05-12  
**Status**: Draft  
**Jira**: AB-33  

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Notificación inmediata de cambio de estado de orden (Priority: P1)

Cada vez que una orden del inversionista cambia de estado (PENDING → EXECUTED, PENDING → CANCELLED, etc.), el sistema envía una notificación en tiempo real al canal preferido del usuario. El inversionista sabe de inmediato si su orden fue ejecutada, rechazada o cancelada.

**Why this priority**: Las notificaciones de órdenes son un requisito regulatorio y de confianza del usuario en plataformas financieras. Sin ellas, el inversionista no tiene visibilidad de sus operaciones.

**Independent Test**: Ejecutar una orden de compra y verificar que el inversionista recibe una notificación con el estado final dentro de los 10 segundos de la ejecución.

**Acceptance Scenarios**:

1. **Given** un inversionista con una orden en estado PENDING, **When** la orden pasa a EXECUTED, **Then** el sistema envía una notificación al canal preferido del usuario con el detalle de la ejecución (acción, cantidad, precio de ejecución, comisión).
2. **Given** un inversionista con una orden activa, **When** la orden es cancelada (por el usuario o automáticamente), **Then** el sistema envía una notificación de cancelación con los recursos liberados.
3. **Given** una orden que es rechazada por el sistema, **When** ocurre el rechazo, **Then** el sistema notifica al inversionista con el motivo del rechazo.

---

### User Story 2 - Configuración del canal de notificación preferido (Priority: P2)

El inversionista puede seleccionar su canal preferido para recibir notificaciones: correo electrónico, notificación push (aplicación móvil) o ambos. La preferencia se guarda en su perfil y aplica a todas las notificaciones del sistema.

**Why this priority**: La personalización del canal mejora la experiencia del usuario y reduce notificaciones no deseadas. Depende del módulo de gestión de perfil (AB-17).

**Independent Test**: Cambiar el canal preferido a solo correo electrónico y verificar que una ejecución de orden genera una notificación únicamente por correo, no por push.

**Acceptance Scenarios**:

1. **Given** un inversionista en la configuración de su perfil, **When** selecciona correo electrónico como canal preferido, **Then** todas las notificaciones futuras de órdenes llegan únicamente por correo.
2. **Given** un inversionista que selecciona notificaciones push, **When** su orden es ejecutada, **Then** recibe una notificación push en la aplicación móvil.
3. **Given** un inversionista que selecciona ambos canales, **When** su orden cambia de estado, **Then** recibe la notificación tanto por correo como por push.

---

### User Story 3 - Historial de notificaciones enviadas (Priority: P3)

El inversionista puede consultar el historial de notificaciones recibidas relacionadas con sus órdenes, con fecha, tipo de evento y detalle, incluso si la notificación original fue perdida o no llegó a tiempo.

**Why this priority**: El historial de notificaciones es útil para auditoría y resolución de disputas. Puede implementarse en una segunda fase sin afectar el flujo principal.

**Independent Test**: Verificar que tras múltiples cambios de estado de órdenes, el historial de notificaciones del usuario refleja todos los eventos correctamente.

**Acceptance Scenarios**:

1. **Given** un inversionista con varias órdenes ejecutadas y canceladas, **When** consulta su historial de notificaciones, **Then** ve la lista cronológica de eventos con fecha, tipo (ejecución/cancelación/rechazo) y detalle.
2. **Given** un inversionista cuya notificación no llegó por falla del canal, **When** consulta el historial, **Then** el evento sigue registrado con el intento fallido marcado.

---

### Edge Cases

- ¿Qué pasa si el canal de notificación falla (correo rebotado, push sin conexión)? El sistema reintenta la entrega un número configurable de veces; el evento queda registrado independientemente del éxito de la entrega.
- ¿Qué pasa si el inversionista no tiene canal preferido configurado? Se usa correo electrónico como canal por defecto.
- ¿Qué pasa con las notificaciones generadas durante la noche para órdenes encoladas? Se envían en el momento en que ocurre el evento, independientemente de la hora.
- ¿Qué pasa si se generan múltiples cambios de estado en rápida sucesión? Se envía una notificación por cada cambio de estado relevante, sin agrupación automática.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE enviar una notificación al inversionista cuando su orden cambie de estado (PENDING, EXECUTED, CANCELLED, REJECTED, QUEUED).
- **FR-002**: La notificación de ejecución DEBE incluir acción, cantidad de títulos, precio de ejecución, comisión aplicada y monto total.
- **FR-003**: La notificación de cancelación DEBE incluir la orden afectada y los recursos liberados (saldo o títulos).
- **FR-004**: La notificación de rechazo DEBE incluir el motivo del rechazo.
- **FR-005**: El sistema DEBE respetar el canal preferido del inversionista (correo electrónico, push o ambos) para el envío de notificaciones.
- **FR-006**: Si no hay canal preferido configurado, el sistema DEBE usar correo electrónico como canal por defecto.
- **FR-007**: El sistema DEBE reintentar la entrega de notificaciones fallidas un número configurable de veces antes de marcarlas como no entregadas.
- **FR-008**: El sistema DEBE mantener un historial de notificaciones enviadas por inversionista, con timestamp, tipo de evento, canal y estado de entrega.
- **FR-009**: El tiempo entre el cambio de estado de una orden y el envío de la notificación DEBE ser menor a 10 segundos bajo condiciones normales.

### Key Entities

- **Notificación**: Mensaje enviado al inversionista informando sobre un cambio de estado de su orden, con tipo de evento, detalle y canal de entrega.
- **Canal de notificación**: Medio por el cual se entrega la notificación (correo electrónico, notificación push).
- **Historial de notificaciones**: Registro persistente de todas las notificaciones generadas para un inversionista.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de los cambios de estado de orden generan una notificación al inversionista.
- **SC-002**: El 95% de las notificaciones son entregadas dentro de los 10 segundos del cambio de estado.
- **SC-003**: El sistema reintenta la entrega de notificaciones fallidas al menos 3 veces antes de marcarlas como no entregadas.
- **SC-004**: El historial de notificaciones retiene todos los eventos de los últimos 12 meses para cada inversionista.

## Assumptions

- La infraestructura de correo electrónico ya está operativa (usada en el módulo de registro AB-15).
- La infraestructura de notificaciones push requiere integración con un servicio de mensajería móvil externo; su implementación técnica es responsabilidad del equipo de infraestructura.
- La preferencia de canal se gestiona en el módulo de gestión de perfil (AB-17).
- Las notificaciones de órdenes son el alcance de este spec; las alertas de precio personalizadas se especifican en AB-35.
- Los administradores y brokers no reciben notificaciones de órdenes de inversionistas salvo que esté configurado explícitamente.

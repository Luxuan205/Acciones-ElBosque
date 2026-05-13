# Feature Specification: Alertas de Mercado

**Feature Branch**: `020-alertas-mercado`  
**Created**: 2026-05-12  
**Status**: Draft  
**Jira**: AB-34  

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Suscripción a alertas de eventos de mercado (Priority: P1)

El inversionista puede suscribirse a alertas automáticas sobre eventos relevantes del mercado: apertura y cierre del mercado, suspensión de negociación de una acción, cambios significativos en el volumen de negociación, y anuncios corporativos relevantes. Las alertas se envían al canal preferido del usuario.

**Why this priority**: Las alertas de mercado mantienen al inversionista informado de condiciones que pueden afectar sus posiciones y decisiones, sin que tenga que monitorear el mercado manualmente.

**Independent Test**: Suscribirse a la alerta de apertura del mercado y verificar que se recibe una notificación en el canal configurado al momento en que el mercado abre.

**Acceptance Scenarios**:

1. **Given** un inversionista que se suscribe a la alerta de apertura del mercado, **When** el mercado abre, **Then** el sistema envía una notificación al canal preferido del usuario.
2. **Given** un inversionista suscrito a alertas de suspensión de negociación de una acción, **When** la negociación de esa acción es suspendida, **Then** el sistema envía una alerta con el motivo de la suspensión.
3. **Given** un inversionista suscrito a alertas de volumen inusual para una acción, **When** el volumen supera el umbral configurado, **Then** el sistema envía una alerta con el dato de volumen actual.

---

### User Story 2 - Gestión de suscripciones a alertas (Priority: P2)

El inversionista puede ver, modificar y eliminar sus suscripciones a alertas de mercado desde su perfil. Puede ajustar los umbrales de las alertas (por ejemplo, cambiar el porcentaje de variación que activa la alerta de volumen) o desactivarlas temporalmente.

**Why this priority**: La gestión de suscripciones permite al inversionista personalizar la información que recibe y evitar sobrecarga de notificaciones.

**Independent Test**: Modificar el umbral de una alerta de volumen, ejecutar un evento de mercado con el nuevo umbral y verificar que la alerta se dispara correctamente.

**Acceptance Scenarios**:

1. **Given** un inversionista con alertas configuradas, **When** consulta su lista de suscripciones, **Then** ve todas las alertas activas con tipo, acción asociada (si aplica) y umbrales configurados.
2. **Given** un inversionista que modifica el umbral de una alerta, **When** confirma el cambio, **Then** la alerta aplica el nuevo umbral de inmediato.
3. **Given** un inversionista que desactiva una alerta, **When** ocurre el evento, **Then** no se envía ninguna notificación para esa alerta desactivada.

---

### Edge Cases

- ¿Qué pasa si el mismo evento activa múltiples alertas del mismo usuario? Cada alerta genera su propia notificación; no se agrupan automáticamente.
- ¿Qué pasa si el canal de notificación falla al entregar una alerta de mercado? El sistema reintenta la entrega; si falla definitivamente, el evento queda registrado como no entregado.
- ¿Qué pasa si el inversionista tiene demasiadas suscripciones activas y el mercado genera muchos eventos simultáneamente? El sistema encola las notificaciones y las entrega en orden; puede haber un retraso breve en escenarios de alta actividad.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE permitir al inversionista suscribirse a alertas de apertura y cierre del mercado.
- **FR-002**: El sistema DEBE permitir al inversionista suscribirse a alertas de suspensión de negociación de acciones específicas.
- **FR-003**: El sistema DEBE permitir al inversionista suscribirse a alertas de volumen inusual para acciones específicas, con umbral configurable.
- **FR-004**: El sistema DEBE enviar las alertas de mercado al canal preferido del inversionista (correo electrónico o push).
- **FR-005**: El sistema DEBE permitir al inversionista ver, modificar y eliminar sus suscripciones a alertas de mercado.
- **FR-006**: El sistema DEBE permitir desactivar temporalmente una alerta sin eliminarla.
- **FR-007**: El sistema DEBE registrar todas las alertas enviadas en el historial de notificaciones del inversionista.
- **FR-008**: Las alertas de mercado DEBEN entregarse dentro de los 30 segundos del evento que las origina bajo condiciones normales.

### Key Entities

- **Alerta de mercado**: Notificación automática generada por un evento de mercado predefinido (apertura, cierre, suspensión, volumen inusual).
- **Suscripción a alerta**: Configuración del inversionista que define qué tipo de alerta recibir, para qué acción (si aplica) y con qué umbrales.
- **Evento de mercado**: Suceso en el mercado bursátil que puede disparar una alerta (apertura, cierre, suspensión, variación de volumen).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de los eventos de mercado que cumplen los criterios de suscripción generan una alerta para los usuarios suscritos.
- **SC-002**: El 95% de las alertas de mercado son entregadas dentro de los 30 segundos del evento.
- **SC-003**: Los cambios en suscripciones o umbrales aplican de inmediato para eventos posteriores al cambio.
- **SC-004**: El sistema soporta al menos 50 tipos de eventos de alerta distintos sin degradación del rendimiento.

## Assumptions

- El módulo de market data (AB-28) provee los eventos de mercado en tiempo real que disparan las alertas.
- La preferencia de canal de notificación se gestiona en el módulo de gestión de perfil (AB-17).
- El sistema de notificaciones (AB-33) gestiona la entrega al canal preferido; este spec define qué alertas generar, no cómo entregarlas.
- Las alertas de precio personalizadas (variación porcentual de precio) se especifican en AB-35; este spec cubre alertas de eventos de mercado generales.
- Los tipos de eventos de alerta disponibles son configurables por el administrador del sistema.

# Feature Specification: Activación de Suscripción Premium

**Feature Branch**: `013-suscripcion-premium`  
**Created**: 2026-05-12  
**Status**: Draft  
**Jira**: AB-18  

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Activación de suscripción premium (Priority: P1)

El inversionista con plan básico puede activar la suscripción premium desde su perfil. Al confirmar, su cuenta pasa a estado premium y obtiene acceso inmediato a funcionalidades exclusivas: watchlist de acciones (AB-36) y alertas de precio personalizadas (AB-35).

**Why this priority**: Sin la activación, las funcionalidades premium no son accesibles. Define el estado de cuenta que habilita el resto de características.

**Independent Test**: Activar suscripción en una cuenta básica, verificar que `subscriptionType` cambia a PREMIUM y que el usuario puede acceder a la watchlist.

**Acceptance Scenarios**:

1. **Given** un inversionista con suscripción BASIC activa, **When** confirma la activación del plan premium, **Then** su cuenta pasa a PREMIUM, se registra la fecha de inicio y vencimiento, y obtiene acceso a funcionalidades premium.
2. **Given** un inversionista ya en PREMIUM, **When** intenta activar el plan premium nuevamente, **Then** se informa que ya tiene el plan activo y se muestra la fecha de vencimiento.
3. **Given** un inversionista en PREMIUM cuya suscripción venció, **When** intenta acceder a funcionalidades premium, **Then** se le informa que su suscripción expiró y se le ofrece renovar.

---

### User Story 2 - Consulta de estado y vencimiento (Priority: P2)

El inversionista puede consultar en cualquier momento el estado de su suscripción (BASIC o PREMIUM), la fecha de vencimiento si aplica, y las funcionalidades incluidas en cada plan.

**Why this priority**: La transparencia sobre el estado de la suscripción genera confianza y reduce consultas de soporte.

**Independent Test**: Consultar el perfil de un usuario premium y verificar que muestra correctamente el tipo de suscripción y la fecha de vencimiento.

**Acceptance Scenarios**:

1. **Given** un inversionista con PREMIUM activo, **When** consulta su perfil, **Then** ve su tipo de suscripción, fecha de activación y fecha de vencimiento.
2. **Given** un inversionista con BASIC, **When** consulta su perfil, **Then** ve que tiene el plan básico y una descripción de lo que obtendría con premium.

---

### User Story 3 - Vencimiento y degradación automática (Priority: P3)

Cuando la suscripción premium vence, el sistema degrada automáticamente la cuenta a BASIC. Las funcionalidades premium dejan de estar disponibles sin perder los datos creados (ej. watchlist guardada), que se reactivarán si el usuario renueva.

**Why this priority**: El ciclo de vida de la suscripción es necesario para la integridad del modelo de negocio, pero puede implementarse con un proceso periódico simple.

**Independent Test**: Configurar una suscripción con fecha de vencimiento en el pasado, ejecutar el proceso de verificación y confirmar que el usuario queda en BASIC.

**Acceptance Scenarios**:

1. **Given** una suscripción PREMIUM cuya fecha de vencimiento ya pasó, **When** el sistema ejecuta la verificación periódica, **Then** la cuenta pasa a BASIC y las funcionalidades premium quedan bloqueadas.
2. **Given** un usuario degradado a BASIC con watchlist guardada, **When** renueva su suscripción premium, **Then** recupera acceso a su watchlist preexistente sin pérdida de datos.

---

### Edge Cases

- ¿Qué pasa si el pago falla durante la activación? La suscripción no se activa y se informa al usuario del error.
- ¿Qué pasa si el usuario está en BASIC y accede a una URL de funcionalidad premium? Se redirige o bloquea con un mensaje de "requiere plan premium".
- ¿Qué pasa con las alertas activas si la suscripción vence? Se suspenden sin eliminarse; se reactivarán con la renovación.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE permitir a un inversionista con plan BASIC activar el plan PREMIUM.
- **FR-002**: Al activar PREMIUM, el sistema DEBE registrar la fecha de inicio y la fecha de vencimiento de la suscripción.
- **FR-003**: El sistema DEBE cambiar el `subscriptionType` del usuario a PREMIUM de forma inmediata tras la confirmación.
- **FR-004**: El sistema DEBE bloquear el acceso a funcionalidades premium (watchlist, alertas de precio) para usuarios con plan BASIC o con suscripción vencida.
- **FR-005**: El sistema DEBE degradar automáticamente las cuentas PREMIUM cuya suscripción haya vencido al plan BASIC mediante un proceso periódico.
- **FR-006**: Los datos creados durante el período premium (watchlist, alertas configuradas) DEBEN conservarse aunque la suscripción venza, para ser reactivados con la renovación.
- **FR-007**: El sistema DEBE mostrar al usuario su estado de suscripción actual, fecha de vencimiento (si aplica) y las funcionalidades incluidas en cada plan.
- **FR-008**: Un usuario PREMIUM que intenta activar nuevamente el plan DEBE recibir información sobre su suscripción vigente en lugar de procesarse un nuevo cobro.

### Key Entities

- **Suscripción**: Estado del plan de un inversionista (BASIC o PREMIUM), con fechas de inicio y vencimiento.
- **Funcionalidad premium**: Capacidad del sistema que solo está disponible para usuarios con suscripción PREMIUM activa (watchlist, alertas de precio personalizadas).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El cambio de estado de BASIC a PREMIUM es efectivo de forma inmediata (menos de 5 segundos) tras la confirmación.
- **SC-002**: El proceso automático de degradación de suscripciones vencidas se ejecuta al menos una vez cada 24 horas.
- **SC-003**: El 100% de los accesos a funcionalidades premium desde cuentas BASIC son bloqueados.
- **SC-004**: Los datos premium (watchlist, alertas) se conservan el 100% de las veces tras el vencimiento y se recuperan al renovar.

## Assumptions

- El proceso de pago/cobro es externo al sistema (pasarela de pago); este spec cubre solo el cambio de estado tras confirmar el pago.
- La duración estándar de la suscripción premium es de 30 días (configurable en parámetros globales AB-40).
- No hay período de prueba gratuito en el alcance de este spec.
- El sistema de notificaciones (AB-33) se encarga de avisar al usuario cuando su suscripción está próxima a vencer.
- Los roles BROKER y ADMIN no tienen restricciones de suscripción; solo aplica a INVESTOR.

# Feature Specification: Alertas de Precio Personalizadas

**Feature Branch**: `021-alertas-precio`  
**Created**: 2026-05-12  
**Status**: Draft  
**Jira**: AB-35  

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Configuración de alerta de precio por umbral absoluto (Priority: P1)

El inversionista premium puede configurar una alerta que se dispara cuando el precio de una acción específica alcanza un valor absoluto definido (por ejemplo, "notificarme cuando ECOPETROL llegue a $3.500 COP"). Puede definir alertas tanto para precios altos como para precios bajos.

**Why this priority**: Las alertas de precio son la funcionalidad premium más solicitada. Permiten al inversionista actuar oportunamente sin monitorear el mercado continuamente.

**Independent Test**: Configurar una alerta de precio para una acción a un precio diferente al actual. Simular que el precio llega al umbral y verificar que la alerta se envía al canal configurado.

**Acceptance Scenarios**:

1. **Given** un inversionista premium, **When** configura una alerta para que una acción llegue a un precio específico, **Then** la alerta queda registrada y el sistema comienza a monitorear el precio.
2. **Given** una alerta de precio activa, **When** el precio de mercado de la acción alcanza o supera el umbral definido, **Then** el sistema envía una notificación al canal preferido del usuario con el precio actual y el umbral configurado.
3. **Given** un inversionista básico (no premium), **When** intenta configurar una alerta de precio, **Then** el sistema le informa que esta funcionalidad requiere suscripción premium.

---

### User Story 2 - Configuración de alerta de precio por variación porcentual (Priority: P2)

El inversionista premium puede configurar alertas basadas en variación porcentual: "notificarme si BANCOLOMBIA sube o baja más del 5% en un día". Este tipo de alerta detecta movimientos significativos de precio sin necesidad de conocer el precio exacto.

**Why this priority**: Las alertas por variación porcentual son más útiles para detectar volatilidad. Complementan las alertas absolutas y son más flexibles para múltiples acciones.

**Independent Test**: Configurar una alerta de variación del 3% para una acción. Simular una variación del 4% en el precio y verificar que la alerta se dispara.

**Acceptance Scenarios**:

1. **Given** un inversionista premium, **When** configura una alerta de variación del X% para una acción, **Then** la alerta queda registrada con el precio de referencia del momento de configuración.
2. **Given** una alerta de variación activa, **When** el precio de la acción varía más del porcentaje definido respecto al precio de referencia, **Then** el sistema envía una notificación con la variación actual y el umbral configurado.
3. **Given** una alerta de variación que se dispara, **When** el inversionista la recibe, **Then** puede decidir si mantener la alerta activa para futuras variaciones o desactivarla.

---

### User Story 3 - Gestión de alertas de precio configuradas (Priority: P2)

El inversionista premium puede ver todas sus alertas de precio activas, modificar los umbrales, activar o desactivar alertas específicas, y eliminarlas. Las alertas desactivadas no generan notificaciones pero conservan su configuración.

**Why this priority**: La gestión de alertas es necesaria para que el inversionista mantenga control sobre las notificaciones que recibe sin tener que recrearlas.

**Independent Test**: Crear múltiples alertas, listarlas, modificar una y verificar que el cambio aplica al siguiente evento de precio.

**Acceptance Scenarios**:

1. **Given** un inversionista premium con alertas de precio configuradas, **When** consulta su listado de alertas, **Then** ve todas sus alertas con acción, tipo (absoluta/porcentual), umbral y estado (activa/inactiva).
2. **Given** un inversionista que modifica el umbral de una alerta activa, **When** confirma el cambio, **Then** la alerta aplica el nuevo umbral para verificaciones futuras.
3. **Given** un inversionista que elimina una alerta, **When** el precio alcanza el umbral eliminado, **Then** no se envía ninguna notificación.

---

### Edge Cases

- ¿Qué pasa si el precio oscila alrededor del umbral y la alerta se dispararía múltiples veces? Por defecto, cada alerta se dispara una sola vez y queda en estado TRIGGERED; el inversionista debe reactivarla manualmente si desea seguir recibiéndola.
- ¿Qué pasa si la suscripción premium del usuario vence mientras tiene alertas activas? Las alertas se suspenden sin eliminarse; se reactivarán si el usuario renueva (per AB-13 spec).
- ¿Qué pasa si se configura una alerta con un umbral ya superado por el precio actual? El sistema advierte al usuario que el umbral ya fue superado y ofrece ajustarlo.
- ¿Qué pasa si el mercado está cerrado cuando se cumple la condición de precio? Las alertas se evalúan solo durante horario bursátil con datos en tiempo real.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE permitir a los inversionistas con suscripción PREMIUM configurar alertas de precio por umbral absoluto para acciones específicas.
- **FR-002**: El sistema DEBE permitir a los inversionistas PREMIUM configurar alertas de precio por variación porcentual para acciones específicas.
- **FR-003**: El sistema DEBE bloquear el acceso a la configuración de alertas de precio para usuarios con suscripción BASIC.
- **FR-004**: El sistema DEBE monitorear continuamente el precio de las acciones con alertas activas durante el horario bursátil.
- **FR-005**: Cuando se cumpla la condición de precio, el sistema DEBE enviar una notificación al canal preferido del inversionista con el precio actual y el umbral configurado.
- **FR-006**: Después de dispararse, una alerta DEBE pasar a estado TRIGGERED y dejar de generar notificaciones hasta que el inversionista la reactive manualmente.
- **FR-007**: El sistema DEBE permitir al inversionista ver, modificar, activar, desactivar y eliminar sus alertas de precio.
- **FR-008**: Las alertas de precio DEBEN suspenderse (sin eliminarse) cuando la suscripción premium del inversionista venza.
- **FR-009**: El sistema DEBE registrar en auditoría la creación, modificación, activación/desactivación y disparo de cada alerta.

### Key Entities

- **Alerta de precio**: Configuración que define una condición de precio (umbral absoluto o variación porcentual) para una acción específica, junto con el canal de notificación.
- **Estado de alerta**: ACTIVE (monitoreando), TRIGGERED (condición cumplida, esperando reactivación), INACTIVE (desactivada por el usuario), SUSPENDED (suspendida por vencimiento de suscripción).
- **Precio de referencia**: Precio de la acción al momento de configurar la alerta de variación porcentual, usado como base para el cálculo.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de las alertas de precio para usuarios PREMIUM son monitoreadas activamente durante el horario bursátil.
- **SC-002**: El tiempo entre que el precio alcanza el umbral y el envío de la notificación es menor a 15 segundos.
- **SC-003**: El 100% de las alertas de usuarios cuya suscripción vence son suspendidas sin pérdida de configuración.
- **SC-004**: El sistema rechaza el 100% de los intentos de usuarios BASIC de configurar alertas de precio.

## Assumptions

- El precio de cotización en tiempo real de las acciones lo provee el módulo de market data (AB-28).
- La verificación de suscripción premium se realiza contra el estado de suscripción (AB-13) en el momento de configurar la alerta.
- El sistema de notificaciones (AB-33) gestiona la entrega al canal preferido.
- Las alertas de mercado generales (apertura, cierre, volumen) se especifican en AB-34; este spec cubre solo alertas de precio personalizadas.
- El número máximo de alertas de precio por usuario PREMIUM es configurable en los parámetros globales (AB-40).

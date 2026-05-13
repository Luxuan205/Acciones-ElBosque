# Feature Specification: Configuración de Stop-Loss y Take-Profit

**Feature Branch**: `017-stop-loss-take-profit`  
**Created**: 2026-05-12  
**Status**: Draft  
**Jira**: AB-22  

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Configuración de stop-loss para proteger posición (Priority: P1)

El inversionista con una posición abierta en una acción configura un precio de stop-loss: si el precio de mercado cae hasta ese nivel, el sistema genera automáticamente una orden de venta para limitar las pérdidas. El inversionista puede configurar este mecanismo en cualquier momento mientras la posición esté activa.

**Why this priority**: El stop-loss es el mecanismo de gestión de riesgo más crítico. Sin él, el inversionista está expuesto a pérdidas ilimitadas en posiciones abiertas.

**Independent Test**: Con una posición abierta en una acción, configurar un stop-loss por encima del precio actual de mercado. Verificar que cuando el precio cae al nivel configurado, el sistema genera automáticamente una orden de venta.

**Acceptance Scenarios**:

1. **Given** un inversionista con una posición activa en una acción, **When** configura un stop-loss a precio X por N títulos, **Then** el sistema registra el stop-loss y lo monitorea activamente.
2. **Given** un stop-loss activo a precio X, **When** el precio de mercado de la acción cae a X o por debajo, **Then** el sistema genera automáticamente una orden de venta de mercado por los N títulos y notifica al inversionista.
3. **Given** un inversionista que quiere modificar o eliminar un stop-loss existente, **When** actualiza o elimina el stop-loss, **Then** el cambio se aplica de inmediato sin afectar otros órdenes activas.

---

### User Story 2 - Configuración de take-profit para asegurar ganancias (Priority: P1)

El inversionista con una posición abierta configura un precio de take-profit: si el precio de mercado sube hasta ese nivel, el sistema genera automáticamente una orden de venta para asegurar las ganancias. Puede coexistir con un stop-loss en la misma posición.

**Why this priority**: Complemento del stop-loss. Permite al inversionista asegurar ganancias automáticamente sin necesitar monitorear el mercado continuamente.

**Independent Test**: Configurar un take-profit por encima del precio actual de mercado. Verificar que cuando el precio sube al nivel configurado, el sistema genera automáticamente una orden de venta.

**Acceptance Scenarios**:

1. **Given** un inversionista con una posición activa, **When** configura un take-profit a precio Y por N títulos, **Then** el sistema registra el take-profit y lo monitorea activamente.
2. **Given** un take-profit activo a precio Y, **When** el precio de mercado de la acción sube a Y o por encima, **Then** el sistema genera automáticamente una orden de venta de mercado por los N títulos y notifica al inversionista.
3. **Given** un inversionista que tiene configurados stop-loss y take-profit para la misma posición, **When** uno de ellos se activa, **Then** el otro se cancela automáticamente para evitar doble venta.

---

### User Story 3 - Gestión centralizada de stop-loss y take-profit activos (Priority: P2)

El inversionista puede consultar una vista de todos sus stop-loss y take-profit activos, modificar los precios configurados o eliminarlos sin necesidad de colocar una nueva orden manual.

**Why this priority**: La visibilidad centralizada reduce errores y permite al inversionista ajustar su gestión de riesgo de forma eficiente.

**Independent Test**: Verificar que el listado de órdenes condicionales muestra stop-loss y take-profit activos con sus respectivos precios y acciones asociadas.

**Acceptance Scenarios**:

1. **Given** un inversionista con múltiples stop-loss y take-profit configurados, **When** consulta su panel de gestión de riesgo, **Then** ve todos sus stop-loss y take-profit activos con acción, precio configurado y cantidad.
2. **Given** un inversionista que modifica el precio de un stop-loss activo, **When** confirma el cambio, **Then** el sistema actualiza el nivel de monitoreo de inmediato.

---

### Edge Cases

- ¿Qué pasa si el precio abre por debajo del stop-loss (gap down)? El sistema ejecuta la orden de venta al primer precio disponible (slippage inherente al mecanismo).
- ¿Qué pasa si el inversionista configura un stop-loss por encima del precio actual de mercado? El sistema advierte que el stop-loss se activará de inmediato, pero permite configurarlo si el usuario confirma.
- ¿Qué pasa si se activa el stop-loss pero no hay liquidez para ejecutar la venta? La orden de venta generada queda en PENDING hasta que haya contraparte; el inversionista es notificado.
- ¿Qué pasa si el inversionista cierra manualmente la posición antes de que se active el stop-loss o take-profit? Ambas órdenes condicionales se cancelan automáticamente.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE permitir al inversionista configurar un precio de stop-loss para una posición activa, especificando acción y cantidad de títulos a vender.
- **FR-002**: El sistema DEBE permitir al inversionista configurar un precio de take-profit para una posición activa, especificando acción y cantidad de títulos a vender.
- **FR-003**: El sistema DEBE monitorear continuamente el precio de mercado contra los niveles de stop-loss y take-profit configurados.
- **FR-004**: Cuando el precio de mercado alcance el nivel de stop-loss o take-profit, el sistema DEBE generar automáticamente una orden de venta de mercado por la cantidad configurada.
- **FR-005**: Si stop-loss y take-profit están configurados para la misma posición y uno se activa, el sistema DEBE cancelar automáticamente el otro.
- **FR-006**: El sistema DEBE permitir al inversionista modificar o eliminar un stop-loss o take-profit activo en cualquier momento.
- **FR-007**: El sistema DEBE notificar al inversionista cuando un stop-loss o take-profit se activa y la orden resultante es generada (AB-33).
- **FR-008**: Toda configuración, modificación y activación de stop-loss y take-profit DEBE registrarse en el log de auditoría.

### Key Entities

- **Stop-loss**: Orden condicional de venta que se activa cuando el precio cae a un nivel mínimo definido, diseñada para limitar pérdidas.
- **Take-profit**: Orden condicional de venta que se activa cuando el precio sube a un nivel máximo definido, diseñada para asegurar ganancias.
- **Orden condicional**: Instrucción de venta que permanece inactiva hasta que se cumple una condición de precio específica.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de los stop-loss y take-profit configurados son monitoreados activamente mientras el mercado está abierto.
- **SC-002**: El tiempo entre que el precio alcanza el nivel configurado y la generación de la orden de venta es menor a 5 segundos.
- **SC-003**: El 100% de las activaciones de stop-loss o take-profit generan la orden de venta correspondiente y cancelan la orden condicional complementaria.
- **SC-004**: El 100% de las activaciones son notificadas al inversionista a través de su canal preferido.

## Assumptions

- El precio de cotización en tiempo real de las acciones lo provee el módulo de market data (AB-28).
- El monitoreo de precios se realiza durante el horario bursátil; fuera de horario, los niveles permanecen activos pero no se verifican.
- Los stop-loss y take-profit solo aplican a posiciones activas (títulos en portafolio); no se pueden configurar sin una posición abierta.
- El alcance de este spec es la configuración y activación automática de stop-loss y take-profit; la gestión de portafolio se especifica en módulos separados.
- El sistema de notificaciones (AB-33) gestiona el envío de alertas al inversionista.
- Las órdenes de venta generadas por activación de stop-loss o take-profit siguen el flujo de market orders de venta (AB-20).

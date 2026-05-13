# Feature Specification: Visualización y Desglose de Comisiones Antes de Confirmar

**Feature Branch**: `004-desglose-comisiones`
**Jira**: AB-25
**Created**: 2026-05-10
**Status**: Draft
**Asignado a**: David Salazar Mora

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Ver desglose de comisiones antes de confirmar una orden (Priority: P1)

Antes de confirmar definitivamente una orden de compra o venta, el inversionista
visualiza un resumen detallado de todos los costos asociados: precio de la acción,
cantidad, valor bruto de la operación, comisión aplicable y costo total neto.

**Why this priority**: El inversionista tiene derecho a conocer el costo completo
de su operación antes de comprometer sus fondos. Es un requisito regulatorio y de
transparencia.

**Independent Test**: Un inversionista llega al paso de confirmación de una orden;
el sistema muestra el desglose completo de costos; el inversionista puede confirmar
o cancelar con plena información.

**Acceptance Scenarios**:

1. **Given** un inversionista que ha completado los datos de una orden (acción,
   cantidad, tipo),
   **When** avanza al paso de confirmación,
   **Then** el sistema muestra: precio unitario actual, cantidad de acciones,
   valor bruto de la operación, porcentaje y monto de comisión, e importe total
   a pagar o recibir (según compra o venta).

2. **Given** un inversionista visualizando el desglose de comisiones,
   **When** confirma la orden,
   **Then** el sistema procesa la orden con los montos mostrados, sin cobrar
   costos adicionales no presentados en el desglose.

3. **Given** un inversionista visualizando el desglose de comisiones,
   **When** cancela o regresa al paso anterior,
   **Then** la orden NO es enviada y el inversionista puede modificar sus parámetros.

---

### User Story 2 — Comisión diferenciada por tipo de suscripción (Priority: P2)

El porcentaje de comisión aplicado varía según si el inversionista tiene
suscripción estándar o premium. El desglose refleja la tasa correspondiente.

**Why this priority**: La diferenciación de comisiones es parte del modelo de
negocio. El inversionista premium debe ver su beneficio reflejado claramente.

**Independent Test**: Un inversionista premium y uno estándar realizan la misma
operación; cada uno ve una tasa de comisión diferente en el desglose.

**Acceptance Scenarios**:

1. **Given** un inversionista con suscripción estándar que revisa el desglose,
   **When** visualiza la comisión,
   **Then** el sistema aplica y muestra la tasa de comisión estándar vigente.

2. **Given** un inversionista con suscripción premium que revisa el desglose,
   **When** visualiza la comisión,
   **Then** el sistema aplica y muestra la tasa de comisión reducida para premium,
   con una etiqueta que indica el beneficio de su suscripción.

---

### Edge Cases

- Si el precio de la acción cambia entre el momento de preparar la orden y la
  confirmación, el desglose se actualiza con el precio más reciente.
- Si los fondos del inversionista son insuficientes para cubrir el total (incluida
  comisión), el sistema lo indica antes de permitir la confirmación.
- Para órdenes limit, la comisión se calcula sobre el precio límite definido por
  el inversionista, no sobre el precio de mercado actual.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE mostrar el desglose de comisiones como paso obligatorio
  antes de la confirmación final de cualquier orden de compra o venta.
- **FR-002**: El desglose DEBE incluir: precio unitario de la acción, cantidad de
  acciones, valor bruto de la operación, tasa de comisión aplicada (en porcentaje),
  monto de comisión (en valor monetario) e importe total de la operación.
- **FR-003**: El sistema DEBE calcular y mostrar la comisión según la tasa
  correspondiente al tipo de suscripción del inversionista (estándar o premium).
- **FR-004**: El inversionista DEBE poder confirmar o cancelar la orden desde la
  pantalla de desglose de comisiones.
- **FR-005**: Si el total de la operación (incluyendo comisión) supera el saldo
  disponible del inversionista, el sistema DEBE advertirlo e impedir la confirmación.
- **FR-006**: El sistema DEBE reflejar el precio de la acción más reciente disponible
  al momento de mostrar el desglose.
- **FR-007**: Los montos mostrados en el desglose DEBEN coincidir exactamente con
  los montos cobrados/acreditados al ejecutar la orden.

### Key Entities

- **Desglose de Comisión**: Cálculo de costos de una orden. Atributos: precio
  unitario, cantidad, valor bruto, tasa de comisión, monto de comisión, total neto.
- **Tasa de Comisión**: Porcentaje aplicable según tipo de suscripción. Atributos:
  tipo de suscripción (estándar/premium), porcentaje vigente, fecha de vigencia.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de las órdenes pasan obligatoriamente por la pantalla de
  desglose antes de ser confirmadas; ninguna orden puede saltar este paso.
- **SC-002**: El monto total cobrado o acreditado por cada orden coincide en el
  100% de los casos con el monto mostrado en el desglose previo a la confirmación.
- **SC-003**: El desglose de comisiones es comprendido y validado por el inversionista
  en menos de 30 segundos en el 90% de los casos (medido por tiempo en pantalla).
- **SC-004**: El 100% de las operaciones con fondos insuficientes son bloqueadas
  antes de la confirmación, con mensaje claro al inversionista.

## Assumptions

- Las tasas de comisión son configuradas por el administrador del sistema y
  obtenidas dinámicamente al generar el desglose.
- La moneda de operación es el peso colombiano (COP) para todas las transacciones.
- El desglose aplica tanto a órdenes de compra como de venta; en venta, la
  comisión se descuenta del monto recibido.
- Para órdenes encoladas (fuera de horario), el desglose se calcula con el precio
  actual y puede diferir del precio de ejecución real; se informará al inversionista.

# Feature Specification: Consulta de Saldo y Movimiento de Fondos

**Feature Branch**: `005-saldo-movimiento-fondos`
**Jira**: AB-26
**Created**: 2026-05-10
**Status**: Draft
**Asignado a**: José Buitrago

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Consultar saldo disponible actual (Priority: P1)

El inversionista autenticado puede ver en cualquier momento su saldo disponible
para operar: cuánto dinero tiene en cuenta, cuánto está comprometido en órdenes
pendientes y cuánto puede usar para nuevas operaciones.

**Why this priority**: El saldo disponible es información crítica que el inversionista
necesita antes de cualquier operación de compra. Sin esta vista no puede tomar
decisiones informadas.

**Independent Test**: Un inversionista accede a la sección de saldo; el sistema
muestra el saldo total, el monto comprometido en órdenes activas y el saldo
disponible para nuevas operaciones.

**Acceptance Scenarios**:

1. **Given** un inversionista autenticado,
   **When** accede a la sección de saldo y fondos,
   **Then** el sistema muestra: saldo total en cuenta, monto reservado en órdenes
   pendientes y saldo disponible para operar (total menos reservado).

2. **Given** un inversionista que acaba de ejecutar una orden de compra,
   **When** consulta su saldo,
   **Then** el saldo disponible refleja el descuento del monto de la operación
   más la comisión cobrada.

3. **Given** un inversionista que tiene órdenes encoladas fuera de horario,
   **When** consulta su saldo,
   **Then** el sistema muestra el monto de las órdenes en cola como "reservado",
   diferenciándolo del monto comprometido en órdenes activas.

---

### User Story 2 — Consultar historial de movimientos de fondos (Priority: P2)

El inversionista puede revisar el historial cronológico de todos los movimientos
de su cuenta: depósitos, retiros, compras, ventas y cobros de comisiones.

**Why this priority**: La trazabilidad de fondos es esencial para que el inversionista
verifique su situación financiera y detecte discrepancias.

**Independent Test**: Un inversionista con operaciones previas accede al historial;
el sistema lista todos los movimientos con fecha, tipo, monto y saldo resultante,
y puede filtrar por rango de fechas.

**Acceptance Scenarios**:

1. **Given** un inversionista con historial de operaciones,
   **When** accede al historial de movimientos,
   **Then** el sistema muestra una lista cronológica (más reciente primero) con
   cada movimiento: fecha y hora, tipo (depósito, retiro, compra, venta, comisión),
   monto y saldo resultante después del movimiento.

2. **Given** un inversionista en la vista del historial,
   **When** aplica un filtro por rango de fechas,
   **Then** el sistema muestra únicamente los movimientos dentro del período
   seleccionado.

3. **Given** un historial con muchos movimientos,
   **When** el inversionista navega la lista,
   **Then** el sistema pagina los resultados mostrando un máximo de 20 movimientos
   por página con navegación hacia atrás y adelante.

---

### Edge Cases

- Si el inversionista no tiene movimientos en el período filtrado, el sistema
  muestra un mensaje indicando que no hay movimientos en ese rango.
- Si el saldo disponible es cero, el sistema lo indica claramente y advierte
  que no puede realizar nuevas compras.
- Los montos de compra y venta se muestran diferenciados con signo negativo
  (egreso) y positivo (ingreso) respectivamente.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE mostrar al inversionista autenticado su saldo total
  en cuenta en tiempo real.
- **FR-002**: El sistema DEBE calcular y mostrar el saldo disponible para operar,
  descontando montos comprometidos en órdenes activas y encoladas.
- **FR-003**: El sistema DEBE diferenciar visualmente entre saldo total, saldo
  reservado (comprometido en órdenes) y saldo disponible.
- **FR-004**: El sistema DEBE mantener un historial completo y ordenado de todos
  los movimientos de fondos del inversionista.
- **FR-005**: Cada movimiento en el historial DEBE mostrar: fecha y hora, tipo de
  movimiento, monto y saldo resultante.
- **FR-006**: El inversionista DEBE poder filtrar el historial de movimientos por
  rango de fechas.
- **FR-007**: El historial DEBE paginar los resultados con un máximo de 20
  movimientos por página.
- **FR-008**: Un inversionista DEBE poder ver únicamente sus propios movimientos;
  el sistema DEBE impedir el acceso a movimientos de otros inversionistas.

### Key Entities

- **Cuenta de Fondos**: Saldo del inversionista. Atributos: saldo total, saldo
  reservado, saldo disponible, moneda (COP).
- **Movimiento de Fondos**: Registro de cada transacción. Atributos: fecha y hora,
  tipo (depósito / retiro / compra / venta / comisión / ajuste), monto, saldo
  resultante, referencia de orden asociada (si aplica).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El saldo mostrado al inversionista refleja el estado real de su cuenta
  con un desfase máximo de 30 segundos tras cualquier operación.
- **SC-002**: El historial de movimientos está disponible para consulta en menos de
  3 segundos desde que el inversionista accede a la sección.
- **SC-003**: El 100% de las operaciones ejecutadas aparecen en el historial dentro
  de los 60 segundos posteriores a su ejecución.
- **SC-004**: El 0% de inversionistas puede acceder a movimientos de fondos de
  otras cuentas.

## Assumptions

- La moneda de operación es peso colombiano (COP); no se manejan múltiples divisas
  en esta versión.
- Los depósitos y retiros de fondos son gestionados por un proceso administrativo
  externo a esta funcionalidad; este spec solo los visualiza.
- El historial conserva todos los movimientos sin fecha de expiración.
- El inversionista no puede exportar el historial en esta versión (PDF/Excel queda
  para versión futura).

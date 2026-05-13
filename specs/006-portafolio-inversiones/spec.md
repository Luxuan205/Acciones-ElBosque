# Feature Specification: Visualización de Portafolio de Inversiones

**Feature Branch**: `006-portafolio-inversiones`
**Jira**: AB-27
**Created**: 2026-05-10
**Status**: Draft
**Asignado a**: Juan Diego González Villarreal

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Ver resumen del portafolio (Priority: P1)

El inversionista autenticado accede a una vista consolidada de todas sus
posiciones abiertas: qué acciones tiene, cuántas unidades de cada una, el valor
actual de cada posición y el valor total del portafolio.

**Why this priority**: La vista del portafolio es la pantalla central de un
inversionista activo. Sin ella no puede tomar decisiones de compra o venta
informadas.

**Independent Test**: Un inversionista con posiciones abiertas accede al portafolio;
el sistema muestra todas sus acciones con cantidad, precio actual y valor de posición,
más el valor total del portafolio.

**Acceptance Scenarios**:

1. **Given** un inversionista con posiciones abiertas en una o más acciones,
   **When** accede a la sección de portafolio,
   **Then** el sistema muestra una lista de todas sus posiciones activas con:
   nombre y símbolo de la acción, cantidad de acciones, precio actual de mercado,
   valor total de la posición y variación del día (monto y porcentaje).

2. **Given** un inversionista en la vista del portafolio,
   **When** el precio de mercado de alguna acción cambia,
   **Then** los valores de la posición y el total del portafolio se actualizan
   para reflejar el precio vigente.

3. **Given** un inversionista sin posiciones abiertas,
   **When** accede al portafolio,
   **Then** el sistema muestra un mensaje indicando que no tiene inversiones activas
   y sugiere explorar el mercado.

---

### User Story 2 — Ver rendimiento de cada posición (Priority: P2)

El inversionista puede ver para cada posición cuánto ha ganado o perdido respecto
al precio al que compró, tanto en valor absoluto como en porcentaje.

**Why this priority**: Conocer el rendimiento individual de cada posición es
fundamental para decidir si mantener, aumentar o liquidar una inversión.

**Independent Test**: Para cada acción del portafolio, el sistema muestra el precio
promedio de compra, el precio actual y la ganancia/pérdida resultante en monto y
porcentaje.

**Acceptance Scenarios**:

1. **Given** un inversionista con posiciones en su portafolio,
   **When** revisa el detalle de cada posición,
   **Then** el sistema muestra: precio promedio de compra, precio actual, ganancia
   o pérdida en valor absoluto (COP) y en porcentaje respecto al precio de compra.

2. **Given** una posición con ganancia,
   **When** el inversionista la visualiza,
   **Then** la ganancia se resalta de forma positiva (color verde o indicador visual
   claro).

3. **Given** una posición con pérdida,
   **When** el inversionista la visualiza,
   **Then** la pérdida se resalta de forma diferenciada (color rojo o indicador
   visual claro).

---

### Edge Cases

- Si el inversionista tiene acciones compradas en múltiples transacciones a precios
  distintos, el sistema calcula y muestra el precio promedio ponderado de compra.
- Si una acción en el portafolio deja de cotizar (suspensión de mercado), el sistema
  muestra el último precio conocido con una indicación de que el precio no está
  actualizado.
- El valor total del portafolio NO incluye el saldo en efectivo de la cuenta.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE mostrar al inversionista todas sus posiciones abiertas
  en acciones con: símbolo, nombre, cantidad de acciones y precio actual de mercado.
- **FR-002**: El sistema DEBE calcular y mostrar el valor actual de cada posición
  (cantidad × precio actual).
- **FR-003**: El sistema DEBE mostrar la variación del precio del día para cada
  acción (monto y porcentaje respecto al cierre anterior).
- **FR-004**: El sistema DEBE mostrar el valor total del portafolio como suma de
  todas las posiciones.
- **FR-005**: El sistema DEBE calcular y mostrar el precio promedio ponderado de
  compra para cada posición.
- **FR-006**: El sistema DEBE mostrar la ganancia o pérdida de cada posición
  respecto al precio promedio de compra, en valor absoluto y porcentaje.
- **FR-007**: La vista del portafolio DEBE reflejar precios de mercado actualizados
  con un desfase máximo de 1 minuto durante el horario bursátil.
- **FR-008**: Un inversionista DEBE poder ver únicamente su propio portafolio.

### Key Entities

- **Posición**: Tenencia de una acción específica. Atributos: símbolo de acción,
  nombre de empresa, cantidad de acciones, precio promedio de compra, precio actual,
  valor de posición, ganancia/pérdida absoluta, ganancia/pérdida porcentual.
- **Portafolio**: Conjunto de posiciones del inversionista. Atributos: lista de
  posiciones, valor total, variación total del día.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El portafolio se carga completamente en menos de 3 segundos para
  inversionistas con hasta 50 posiciones distintas.
- **SC-002**: Los precios mostrados en el portafolio se actualizan con un desfase
  máximo de 60 segundos respecto al precio de mercado durante el horario bursátil.
- **SC-003**: El cálculo de ganancia/pérdida es exacto en el 100% de los casos;
  cero discrepancias respecto a los registros de compra del sistema.
- **SC-004**: El 0% de los inversionistas puede acceder al portafolio de otro
  inversionista.

## Assumptions

- Los precios de mercado en tiempo real son provistos por el módulo de datos de
  mercado ya integrado en la plataforma.
- El portafolio muestra únicamente acciones; no incluye otros instrumentos
  financieros en esta versión.
- El valor del portafolio se expresa en COP.
- La vista de portafolio no permite ejecutar órdenes directamente desde ella en
  esta versión; las órdenes se gestionan en la sección correspondiente.

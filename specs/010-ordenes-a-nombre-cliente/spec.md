# Feature Specification: Generación y Firma de Órdenes a Nombre del Cliente

**Feature Branch**: `010-ordenes-a-nombre-cliente`
**Jira**: AB-32
**Created**: 2026-05-10
**Status**: Draft
**Asignado a**: Juan Diego González Villarreal

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Generar una orden de compra o venta a nombre de un cliente (Priority: P1)

El comisionista selecciona uno de sus clientes asignados y genera una orden de
compra o venta de acciones a nombre de ese cliente, visualizando el desglose de
comisiones y confirmando la operación con su autorización.

**Why this priority**: Esta es la función principal del rol comisionista. Sin ella,
el comisionista no puede operar.

**Independent Test**: Un comisionista selecciona un cliente, genera una orden de
compra de 10 acciones de X, revisa el desglose de comisiones y la confirma; la
orden aparece en el historial del cliente y en el del comisionista.

**Acceptance Scenarios**:

1. **Given** un comisionista autenticado con clientes asignados,
   **When** selecciona un cliente y completa los datos de una orden (acción,
   cantidad, tipo de orden) y la envía,
   **Then** el sistema muestra el desglose de costos y comisiones antes de la
   confirmación final, igual que para una orden directa del inversionista.

2. **Given** un comisionista en el paso de confirmación de una orden de cliente,
   **When** confirma la orden,
   **Then** el sistema registra la orden a nombre del cliente, descuenta los fondos
   de la cuenta del cliente, y registra que la orden fue generada por el comisionista.

3. **Given** una orden generada por el comisionista a nombre de un cliente,
   **When** el cliente accede a su historial de órdenes,
   **Then** la orden aparece en su historial con una indicación de que fue generada
   por su comisionista asignado.

---

### User Story 2 — Trazabilidad de la autoría de la orden (Priority: P2)

Todas las órdenes generadas por un comisionista quedan registradas con la
identidad del comisionista que las creó, para fines de auditoría y responsabilidad.

**Why this priority**: La trazabilidad de autoría es un requisito regulatorio en
operaciones de valores; el comisionista debe responder por las órdenes que emite.

**Independent Test**: El administrador consulta el registro de auditoría de una
orden y puede ver que fue generada por el comisionista X a nombre del cliente Y,
con fecha y hora exactas.

**Acceptance Scenarios**:

1. **Given** una orden ejecutada por un comisionista a nombre de un cliente,
   **When** el módulo de auditoría consulta el registro de esa orden,
   **Then** el registro incluye: identificador del comisionista autor, identificador
   del cliente propietario, fecha y hora de creación y de ejecución.

2. **Given** un comisionista que revisa sus propias operaciones,
   **When** accede a su historial de órdenes generadas,
   **Then** el sistema muestra todas las órdenes que ese comisionista ha generado
   a nombre de sus clientes, con nombre del cliente, acción, cantidad y estado.

---

### Edge Cases

- El comisionista solo puede generar órdenes a nombre de clientes que le están
  formalmente asignados; el sistema impide operar sobre clientes de otros comisionistas.
- Si el cliente no tiene fondos suficientes para la orden, el sistema informa al
  comisionista y no permite confirmar la orden.
- Si el mercado está cerrado al momento de generar la orden, esta queda encolada
  bajo el mismo mecanismo que una orden directa del inversionista (AB-24).
- El comisionista NO puede cancelar una orden ya ejecutada; solo puede cancelar
  órdenes encoladas del cliente.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El comisionista DEBE poder seleccionar un cliente de su lista asignada
  para generar una orden a su nombre.
- **FR-002**: El comisionista DEBE poder especificar todos los parámetros de la
  orden: tipo (compra/venta), acción, cantidad y tipo de orden (market/limit).
- **FR-003**: El sistema DEBE mostrar el desglose de costos y comisiones al
  comisionista antes de la confirmación, usando la tasa correspondiente al cliente.
- **FR-004**: El sistema DEBE verificar que el cliente tiene fondos suficientes
  antes de procesar la confirmación.
- **FR-005**: El sistema DEBE registrar toda orden generada por un comisionista
  con: identidad del comisionista, identidad del cliente, parámetros de la orden,
  fecha y hora de creación.
- **FR-006**: Las órdenes generadas por el comisionista DEBEN aparecer en el
  historial de órdenes del cliente con indicación de autoría del comisionista.
- **FR-007**: El comisionista DEBE poder ver su propio historial de órdenes
  generadas, filtrable por cliente y por fecha.
- **FR-008**: El sistema DEBE impedir que un comisionista genere órdenes a nombre
  de clientes que no le están asignados.
- **FR-009**: El comisionista DEBE poder cancelar órdenes encoladas de sus clientes,
  pero no órdenes ya ejecutadas.

### Key Entities

- **Orden de Comisionista**: Orden generada por un comisionista a nombre de un
  cliente. Atributos: todos los atributos de una orden estándar más el identificador
  del comisionista autor y la indicación de autoría delegada.
- **Registro de Autoría**: Trazabilidad de la operación. Atributos: comisionista
  autor, cliente propietario, fecha/hora de creación, fecha/hora de confirmación.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de las órdenes generadas por un comisionista quedan registradas
  con la identidad del comisionista autor; cero órdenes sin trazabilidad de autoría.
- **SC-002**: El comisionista puede completar el flujo de generación de una orden
  a nombre de un cliente en menos de 2 minutos.
- **SC-003**: El 0% de los comisionistas puede generar órdenes a nombre de clientes
  que no les están asignados.
- **SC-004**: El 100% de las órdenes con fondos insuficientes del cliente son
  bloqueadas antes de la confirmación.

## Assumptions

- La lista de clientes asignados al comisionista proviene de la funcionalidad AB-31.
- El desglose de comisiones sigue la misma lógica que AB-25, usando la tasa
  correspondiente al tipo de suscripción del cliente (no del comisionista).
- El encolamiento de órdenes fuera de horario sigue el mismo mecanismo de AB-24.
- La firma digital o autorización electrónica avanzada no está en alcance para
  esta versión; la confirmación del comisionista en la interfaz equivale a la
  autorización de la operación.

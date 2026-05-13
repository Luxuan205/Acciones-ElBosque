# Feature Specification: Colocación de Limit Order

**Feature Branch**: `016-limit-order`  
**Created**: 2026-05-12  
**Status**: Draft  
**Jira**: AB-21  

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Colocación de limit order de compra (Priority: P1)

El inversionista autenticado selecciona una acción, especifica el precio máximo al que está dispuesto a comprar y la cantidad de títulos. La orden no se ejecuta de inmediato, sino que queda activa hasta que el precio de mercado alcance o supere la condición definida, o hasta que el usuario la cancele.

**Why this priority**: Las limit orders son el segundo tipo de orden más frecuente y permiten al inversionista comprar a un precio objetivo en lugar de aceptar el precio actual del mercado.

**Independent Test**: Crear una limit order de compra con un precio límite diferente al precio de mercado actual. Verificar que la orden queda en estado PENDING y no se ejecuta hasta que el precio alcance el límite configurado.

**Acceptance Scenarios**:

1. **Given** un inversionista con saldo suficiente, **When** coloca una limit order de compra con precio límite X por N títulos, **Then** la orden queda en estado PENDING, el saldo equivalente queda reservado y la orden no se ejecuta hasta que el mercado alcance el precio X.
2. **Given** una limit order de compra activa cuyo precio límite es alcanzado por el mercado, **When** el sistema detecta la condición, **Then** la orden pasa a EXECUTED y el portafolio del inversionista se actualiza.
3. **Given** un inversionista con saldo insuficiente para cubrir el total de la limit order, **When** intenta colocarla, **Then** el sistema rechaza la orden con mensaje de fondos insuficientes.

---

### User Story 2 - Colocación de limit order de venta (Priority: P1)

El inversionista autenticado selecciona una acción que posee, especifica el precio mínimo al que está dispuesto a vender y la cantidad de títulos. La orden queda activa hasta que el precio de mercado alcance o supere el precio mínimo definido.

**Why this priority**: Es el complemento simétrico de la limit order de compra. Sin ella, el inversionista solo puede vender a precio de mercado.

**Independent Test**: Crear una limit order de venta con un precio límite superior al precio de mercado actual. Verificar que la orden queda en PENDING y los títulos quedan reservados.

**Acceptance Scenarios**:

1. **Given** un inversionista que posee N títulos de una acción, **When** coloca una limit order de venta con precio mínimo X, **Then** la orden queda en PENDING, los N títulos quedan reservados y la orden no se ejecuta hasta que el mercado alcance el precio X.
2. **Given** una limit order de venta activa cuyo precio mínimo es alcanzado por el mercado, **When** el sistema detecta la condición, **Then** la orden pasa a EXECUTED y el monto neto se acredita al inversionista.
3. **Given** un inversionista que intenta colocar una limit order de venta por más títulos de los que posee disponibles, **When** intenta colocarla, **Then** el sistema rechaza la orden con mensaje de títulos insuficientes.

---

### User Story 3 - Gestión de vigencia de la limit order (Priority: P2)

El inversionista puede definir una fecha de expiración para su limit order (GTC — Good Till Cancelled o GTD — Good Till Date). Si no se ejecuta antes de la fecha de expiración, la orden se cancela automáticamente y se liberan los recursos reservados.

**Why this priority**: Sin vigencia configurable, las limit orders activas acumulan recursos reservados indefinidamente, lo que puede bloquear el capital o los títulos del inversionista.

**Independent Test**: Crear una limit order con fecha de expiración en el pasado (o en el futuro próximo), verificar que tras la fecha el sistema la cancela automáticamente y libera la reserva.

**Acceptance Scenarios**:

1. **Given** una limit order con fecha de expiración definida que no fue ejecutada, **When** llega la fecha de expiración, **Then** la orden pasa a CANCELLED automáticamente y se liberan los recursos reservados.
2. **Given** una limit order sin fecha de expiración (GTC), **Then** permanece activa hasta que el inversionista la cancele manualmente o se ejecute.

---

### Edge Cases

- ¿Qué pasa si el precio de mercado alcanza el límite pero no hay suficiente liquidez para ejecutar la orden completa? La orden puede ejecutarse parcialmente; el resto permanece PENDING hasta nueva oportunidad o expiración.
- ¿Qué pasa si el inversionista coloca múltiples limit orders que en conjunto superan su saldo o sus títulos disponibles? El sistema valida la reserva al crear cada orden; si los recursos son insuficientes para una nueva orden, se rechaza.
- ¿Qué pasa si el saldo reservado libera fondos para una limit order de compra posterior cancelación de otra orden? El sistema recalcula la disponibilidad y permite la colocación de nuevas órdenes.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE permitir al inversionista colocar una limit order de compra especificando acción, precio límite máximo y cantidad de títulos.
- **FR-002**: El sistema DEBE permitir al inversionista colocar una limit order de venta especificando acción, precio límite mínimo y cantidad de títulos.
- **FR-003**: El sistema DEBE validar que el saldo disponible sea suficiente para cubrir la limit order de compra y reservarlo al crear la orden.
- **FR-004**: El sistema DEBE validar que el inversionista posea suficientes títulos disponibles para cubrir la limit order de venta y reservarlos al crear la orden.
- **FR-005**: El sistema DEBE mantener la orden en estado PENDING hasta que se cumpla la condición de precio o se cancele.
- **FR-006**: El sistema DEBE ejecutar automáticamente la orden cuando el precio de mercado alcance el precio límite definido.
- **FR-007**: El sistema DEBE soportar fecha de expiración opcional para las limit orders; si la fecha pasa sin ejecución, la orden se cancela automáticamente.
- **FR-008**: El sistema DEBE liberar los recursos reservados (saldo o títulos) cuando una limit order sea cancelada o expirada.
- **FR-009**: Toda limit order colocada DEBE registrarse en el log de auditoría con usuario, acción, cantidad, precio límite, timestamp y estado.

### Key Entities

- **Limit Order**: Instrucción de compra o venta condicionada a un precio límite, con identificador, tipo (compra/venta), precio límite, cantidad, estado y vigencia.
- **Vigencia (GTC/GTD)**: Período durante el cual la limit order permanece activa. GTC (Good Till Cancelled) no tiene fecha de expiración; GTD (Good Till Date) expira en la fecha indicada.
- **Ejecución parcial**: Situación en que la limit order se ejecuta por una cantidad menor a la solicitada debido a liquidez insuficiente en el mercado.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de las limit orders colocadas quedan registradas con estado PENDING y recursos reservados correctamente.
- **SC-002**: El 100% de las limit orders con condición de precio cumplida son ejecutadas automáticamente sin intervención manual.
- **SC-003**: El 100% de las limit orders expiradas son canceladas automáticamente y sus recursos liberados dentro de los 5 minutos siguientes a la fecha de expiración.
- **SC-004**: El sistema rechaza el 100% de las limit orders cuando el saldo o títulos disponibles son insuficientes.

## Assumptions

- El precio de cotización en tiempo real de las acciones lo provee el módulo de market data (AB-28).
- El cálculo de comisiones sigue las reglas definidas en el módulo de desglose de comisiones (AB-25).
- El saldo y portafolio del inversionista se gestionan en el módulo de saldo y movimiento de fondos (AB-26).
- La verificación periódica de condiciones de precio y expiración es responsabilidad de un proceso interno del sistema.
- La vigencia por defecto para una limit order sin fecha de expiración es GTC (Good Till Cancelled).
- Las ejecuciones parciales son posibles y válidas; el inversionista es notificado de cada ejecución parcial.
- El alcance de este spec cubre limit orders de compra y venta; las órdenes stop-loss y take-profit se especifican en AB-22.

# Feature Specification: Generación de Market Order de Compra

**Feature Branch**: `014-market-order-compra`  
**Created**: 2026-05-12  
**Status**: Draft  
**Jira**: AB-19  

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Colocación de market order de compra en horario bursátil (Priority: P1)

El inversionista autenticado selecciona una acción, indica la cantidad de títulos a comprar y confirma la orden. El sistema valida que el saldo disponible sea suficiente para cubrir el valor estimado más comisiones, y envía la orden al mercado de inmediato.

**Why this priority**: Es la operación de compra más básica y de mayor frecuencia. Sin esta funcionalidad el sistema no tiene propósito transaccional.

**Independent Test**: Con un usuario autenticado con saldo suficiente, colocar una market order de compra durante horario bursátil y verificar que la orden queda registrada con estado PENDING o EXECUTED.

**Acceptance Scenarios**:

1. **Given** un inversionista con saldo suficiente durante horario bursátil, **When** confirma una market order de compra por N títulos de una acción, **Then** la orden se registra, se muestra el desglose de comisiones y el saldo queda reservado.
2. **Given** un inversionista con saldo insuficiente, **When** intenta colocar una market order de compra, **Then** el sistema rechaza la orden con un mensaje claro de fondos insuficientes.
3. **Given** un inversionista durante horario bursátil, **When** confirma la orden, **Then** recibe confirmación inmediata con el número de orden y el estado inicial.

---

### User Story 2 - Vista previa de comisiones antes de confirmar (Priority: P2)

Antes de confirmar la orden, el inversionista ve el desglose detallado: precio estimado por título, valor total de la compra, comisión calculada y monto total a debitar de su cuenta. Solo entonces puede confirmar o cancelar.

**Why this priority**: La transparencia de costos es un requisito regulatorio y de confianza del usuario. Vinculado con AB-25 (desglose de comisiones).

**Independent Test**: Iniciar el flujo de compra hasta la pantalla de confirmación y verificar que se muestran precio unitario estimado, comisión y total, sin ejecutar aún la orden.

**Acceptance Scenarios**:

1. **Given** un inversionista que ingresó los parámetros de la orden, **When** llega a la pantalla de confirmación, **Then** ve precio unitario estimado, comisión exacta, valor total y saldo resultante.
2. **Given** el usuario en la pantalla de confirmación, **When** cancela la operación, **Then** no se crea ninguna orden ni se modifica su saldo.

---

### User Story 3 - Encolamiento automático fuera de horario (Priority: P3)

Si el inversionista intenta colocar la orden fuera del horario bursátil (incluyendo festivos), la orden no se rechaza sino que se encola para ejecutarse en la próxima apertura del mercado, previa confirmación explícita del usuario.

**Why this priority**: Amplía la utilidad del sistema pero depende del módulo de encolamiento (AB-24). Puede diferirse sin afectar el MVP.

**Independent Test**: Colocar una market order fuera de horario bursátil, confirmar el encolamiento y verificar que la orden queda en estado QUEUED.

**Acceptance Scenarios**:

1. **Given** un inversionista fuera del horario bursátil, **When** intenta colocar una market order de compra, **Then** el sistema informa que el mercado está cerrado y ofrece encolar la orden para la próxima apertura.
2. **Given** el usuario acepta encolar la orden, **Then** la orden queda en estado QUEUED y se notifica al usuario.
3. **Given** el usuario rechaza encolar la orden, **Then** no se crea ninguna orden.

---

### Edge Cases

- ¿Qué pasa si el precio de la acción cambia significativamente entre la vista previa y la ejecución? Se usa el precio de mercado en el momento de la ejecución; es inherente a una market order.
- ¿Qué pasa si el saldo reservado supera el saldo disponible al momento de ejecutar (concurrencia)? La orden se rechaza y se libera la reserva.
- ¿Qué pasa si la acción no tiene liquidez en ese momento? La orden permanece en PENDING hasta que haya contraparte; el usuario puede cancelarla si aplica (AB-23).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE permitir al inversionista autenticado seleccionar una acción y especificar la cantidad de títulos a comprar.
- **FR-002**: El sistema DEBE calcular y mostrar el desglose de comisiones (precio estimado, comisión, total a debitar) antes de que el usuario confirme la orden.
- **FR-003**: El sistema DEBE validar que el saldo disponible del inversionista sea suficiente para cubrir el total de la compra (precio + comisiones).
- **FR-004**: El sistema DEBE registrar la orden con un identificador único y estado inicial (PENDING o QUEUED).
- **FR-005**: El sistema DEBE reservar el saldo correspondiente en la cuenta del inversionista al crear la orden, para evitar doble gasto.
- **FR-006**: Si la orden se coloca fuera del horario bursátil, el sistema DEBE ofrecer al usuario la opción de encolarla para la próxima apertura.
- **FR-007**: El sistema DEBE notificar al inversionista sobre el estado de su orden a través de su canal preferido (AB-33).
- **FR-008**: Toda orden colocada DEBE registrarse en el log de auditoría con usuario, acción, cantidad, timestamp y estado.

### Key Entities

- **Orden de compra (Market Order)**: Instrucción de compra al precio de mercado vigente, con identificador, estado, acción, cantidad, precio de ejecución y comisión.
- **Estado de orden**: QUEUED (encolada fuera de horario), PENDING (en mercado), EXECUTED (ejecutada), CANCELLED (cancelada), REJECTED (rechazada).
- **Reserva de saldo**: Bloqueo temporal del monto estimado en la cuenta del inversionista mientras la orden está activa.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El flujo completo de colocación de orden (desde selección hasta confirmación) se completa en menos de 30 segundos.
- **SC-002**: El 100% de las órdenes colocadas quedan registradas con un identificador único y estado inicial correcto.
- **SC-003**: El sistema rechaza el 100% de las órdenes cuando el saldo disponible es insuficiente.
- **SC-004**: El desglose de comisiones se muestra en la pantalla de confirmación para el 100% de las órdenes antes de su ejecución.

## Assumptions

- El precio de cotización en tiempo real de las acciones lo provee el módulo de market data (AB-28).
- El cálculo de comisiones sigue las reglas definidas en el módulo de desglose de comisiones (AB-25).
- La lógica de encolamiento fuera de horario está implementada en el módulo AB-24.
- El saldo del inversionista se gestiona en el módulo de saldo y movimiento de fondos (AB-26).
- Las market orders se ejecutan al mejor precio disponible en el mercado en el momento de ejecución; el precio de la vista previa es estimado.
- El alcance de este spec es solo market orders de compra; las de venta se especifican en AB-20.

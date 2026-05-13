# Feature Specification: Generación de Market Order de Venta

**Feature Branch**: `015-market-order-venta`  
**Created**: 2026-05-12  
**Status**: Draft  
**Jira**: AB-20  

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Colocación de market order de venta en horario bursátil (Priority: P1)

El inversionista autenticado selecciona una acción que posee en su portafolio, indica la cantidad de títulos a vender y confirma la orden. El sistema valida que el inversionista tenga suficientes títulos disponibles, calcula las comisiones aplicables y envía la orden al mercado de inmediato.

**Why this priority**: Es la operación de venta más básica. Sin ella el sistema no permite al inversionista liquidar posiciones, lo cual es esencial para cualquier plataforma de inversión.

**Independent Test**: Con un usuario autenticado que posea títulos de una acción, colocar una market order de venta durante horario bursátil y verificar que la orden queda registrada con estado PENDING o EXECUTED y los títulos quedan reservados.

**Acceptance Scenarios**:

1. **Given** un inversionista que posee al menos N títulos de una acción durante horario bursátil, **When** confirma una market order de venta por N títulos, **Then** la orden se registra, se muestra el desglose de comisiones y los títulos quedan reservados.
2. **Given** un inversionista que intenta vender más títulos de los que posee, **When** intenta colocar la orden, **Then** el sistema rechaza la orden con un mensaje de títulos insuficientes.
3. **Given** un inversionista durante horario bursátil, **When** confirma la orden, **Then** recibe confirmación inmediata con el número de orden y el estado inicial.

---

### User Story 2 - Vista previa de comisiones antes de confirmar la venta (Priority: P2)

Antes de confirmar la orden de venta, el inversionista ve el desglose detallado: precio estimado por título, valor bruto de la venta, comisión descontada y monto neto a recibir. Solo entonces puede confirmar o cancelar.

**Why this priority**: La transparencia de costos es un requisito regulatorio. El inversionista debe saber exactamente cuánto recibirá neto antes de ejecutar la venta.

**Independent Test**: Iniciar el flujo de venta hasta la pantalla de confirmación y verificar que se muestran precio unitario estimado, comisión y monto neto, sin ejecutar aún la orden.

**Acceptance Scenarios**:

1. **Given** un inversionista que ingresó los parámetros de la orden de venta, **When** llega a la pantalla de confirmación, **Then** ve precio unitario estimado, comisión exacta, valor bruto y monto neto a recibir.
2. **Given** el usuario en la pantalla de confirmación, **When** cancela la operación, **Then** no se crea ninguna orden ni se modifican sus títulos.

---

### User Story 3 - Encolamiento automático fuera de horario (Priority: P3)

Si el inversionista intenta colocar la orden de venta fuera del horario bursátil, la orden no se rechaza sino que se encola para ejecutarse en la próxima apertura del mercado, previa confirmación explícita del usuario.

**Why this priority**: Amplía la utilidad del sistema, pero depende del módulo de encolamiento (AB-24). Puede diferirse sin afectar el MVP.

**Independent Test**: Colocar una market order de venta fuera de horario bursátil, confirmar el encolamiento y verificar que la orden queda en estado QUEUED.

**Acceptance Scenarios**:

1. **Given** un inversionista fuera del horario bursátil, **When** intenta colocar una market order de venta, **Then** el sistema informa que el mercado está cerrado y ofrece encolar la orden para la próxima apertura.
2. **Given** el usuario acepta encolar la orden, **Then** la orden queda en estado QUEUED y se notifica al usuario.
3. **Given** el usuario rechaza encolar la orden, **Then** no se crea ninguna orden.

---

### Edge Cases

- ¿Qué pasa si el precio de la acción cae significativamente entre la vista previa y la ejecución? Se usa el precio de mercado en el momento de la ejecución; es inherente a una market order.
- ¿Qué pasa si el inversionista tiene títulos en múltiples lotes con diferentes precios de adquisición? La venta se aplica sobre el total disponible; el cálculo de ganancia/pérdida es responsabilidad del módulo de reporte (AB-37).
- ¿Qué pasa si la acción no tiene compradores en ese momento? La orden permanece en PENDING hasta que haya contraparte; el usuario puede cancelarla si aplica (AB-23).
- ¿Qué pasa con títulos que están reservados por otra orden activa? Solo los títulos libres (no reservados) están disponibles para nuevas órdenes de venta.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE permitir al inversionista autenticado seleccionar una acción de su portafolio y especificar la cantidad de títulos a vender.
- **FR-002**: El sistema DEBE calcular y mostrar el desglose de comisiones (precio estimado, comisión, monto neto a recibir) antes de que el usuario confirme la orden.
- **FR-003**: El sistema DEBE validar que el inversionista posea suficientes títulos disponibles (no reservados por otras órdenes activas) para cubrir la cantidad a vender.
- **FR-004**: El sistema DEBE registrar la orden con un identificador único y estado inicial (PENDING o QUEUED).
- **FR-005**: El sistema DEBE reservar los títulos correspondientes en el portafolio del inversionista al crear la orden, para evitar doble venta.
- **FR-006**: Si la orden se coloca fuera del horario bursátil, el sistema DEBE ofrecer al usuario la opción de encolarla para la próxima apertura.
- **FR-007**: El sistema DEBE notificar al inversionista sobre el estado de su orden a través de su canal preferido (AB-33).
- **FR-008**: Toda orden colocada DEBE registrarse en el log de auditoría con usuario, acción, cantidad, timestamp y estado.

### Key Entities

- **Orden de venta (Market Order)**: Instrucción de venta al precio de mercado vigente, con identificador, estado, acción, cantidad, precio de ejecución y comisión.
- **Estado de orden**: QUEUED (encolada fuera de horario), PENDING (en mercado), EXECUTED (ejecutada), CANCELLED (cancelada), REJECTED (rechazada).
- **Reserva de títulos**: Bloqueo temporal de los títulos a vender en el portafolio del inversionista mientras la orden está activa.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El flujo completo de colocación de orden de venta (desde selección hasta confirmación) se completa en menos de 30 segundos.
- **SC-002**: El 100% de las órdenes colocadas quedan registradas con un identificador único y estado inicial correcto.
- **SC-003**: El sistema rechaza el 100% de las órdenes cuando los títulos disponibles son insuficientes.
- **SC-004**: El desglose de comisiones y monto neto se muestra en la pantalla de confirmación para el 100% de las órdenes antes de su ejecución.

## Assumptions

- El precio de cotización en tiempo real de las acciones lo provee el módulo de market data (AB-28).
- El cálculo de comisiones sigue las reglas definidas en el módulo de desglose de comisiones (AB-25).
- La lógica de encolamiento fuera de horario está implementada en el módulo AB-24.
- El portafolio del inversionista (títulos disponibles) se gestiona en el módulo de saldo y movimiento de fondos (AB-26).
- Las market orders se ejecutan al mejor precio disponible en el momento de ejecución; el precio de la vista previa es estimado.
- El alcance de este spec es solo market orders de venta; las de compra se especifican en AB-19.

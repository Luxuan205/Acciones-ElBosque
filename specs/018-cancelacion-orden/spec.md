# Feature Specification: Cancelación de Orden

**Feature Branch**: `018-cancelacion-orden`  
**Created**: 2026-05-12  
**Status**: Draft  
**Jira**: AB-23  

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Cancelación de orden activa por el inversionista (Priority: P1)

El inversionista puede cancelar una orden que aún no ha sido ejecutada (estado PENDING o QUEUED). Al cancelar, los recursos reservados (saldo o títulos) se liberan de inmediato y el inversionista puede disponer de ellos para nuevas operaciones.

**Why this priority**: Sin la posibilidad de cancelar, el inversionista no puede corregir errores ni reaccionar a cambios del mercado. Es un requisito básico de toda plataforma de inversión.

**Independent Test**: Crear una limit order o una market order encolada, luego cancelarla desde la interfaz del usuario. Verificar que la orden pasa a CANCELLED y los recursos reservados son liberados.

**Acceptance Scenarios**:

1. **Given** un inversionista con una orden en estado PENDING o QUEUED, **When** solicita la cancelación, **Then** la orden pasa a estado CANCELLED y los recursos reservados se liberan de inmediato.
2. **Given** un inversionista que intenta cancelar una orden ya en estado EXECUTED, **When** envía la solicitud, **Then** el sistema informa que la orden ya fue ejecutada y no puede cancelarse.
3. **Given** un inversionista que cancela una orden de compra, **When** la cancelación es exitosa, **Then** el saldo reservado vuelve a estar disponible para nuevas operaciones.
4. **Given** un inversionista que cancela una orden de venta, **When** la cancelación es exitosa, **Then** los títulos reservados vuelven a estar disponibles en el portafolio.

---

### User Story 2 - Cancelación masiva de órdenes activas (Priority: P2)

El inversionista puede cancelar todas sus órdenes activas (PENDING y QUEUED) con una sola acción, en lugar de cancelarlas individualmente. Esto es útil ante cambios bruscos del mercado o situaciones de emergencia.

**Why this priority**: Reduce el tiempo de reacción en situaciones de alta volatilidad. Puede implementarse después del MVP sin impacto en el flujo básico.

**Independent Test**: Con múltiples órdenes activas, ejecutar la cancelación masiva y verificar que todas pasan a CANCELLED y sus recursos son liberados.

**Acceptance Scenarios**:

1. **Given** un inversionista con múltiples órdenes en estado PENDING o QUEUED, **When** selecciona cancelar todas, **Then** todas las órdenes pasan a CANCELLED y todos los recursos reservados son liberados.
2. **Given** un inversionista que confirma la cancelación masiva, **Then** recibe un resumen con la cantidad de órdenes canceladas y los recursos liberados.

---

### User Story 3 - Cancelación de órdenes condicionales (stop-loss / take-profit) (Priority: P2)

El inversionista puede cancelar órdenes condicionales (stop-loss y take-profit) configuradas para sus posiciones. Al cancelar una orden condicional, el monitoreo automático de precios se detiene para esa posición.

**Why this priority**: Complementa la gestión de riesgo (AB-22). El inversionista debe poder desactivar protecciones automáticas si cambia su estrategia.

**Independent Test**: Cancelar un stop-loss activo y verificar que el sistema deja de monitorear el precio para esa condición específica.

**Acceptance Scenarios**:

1. **Given** un inversionista con un stop-loss o take-profit activo, **When** lo cancela, **Then** la orden condicional pasa a CANCELLED y el monitoreo de precios para esa condición se detiene.
2. **Given** un inversionista que cancela un stop-loss, **Then** el take-profit asociado permanece activo si fue configurado de forma independiente.

---

### Edge Cases

- ¿Qué pasa si la orden se ejecuta en el mismo instante en que el usuario solicita la cancelación? Si la ejecución llega primero, la cancelación se rechaza con el estado actualizado EXECUTED; si la cancelación llega primero, la orden se cancela antes de ejecutarse.
- ¿Qué pasa si el sistema tarda en confirmar la cancelación al broker externo? La orden queda en estado CANCELLING hasta obtener confirmación; los recursos no se liberan hasta confirmar.
- ¿Qué pasa si una cancelación masiva falla parcialmente? Se notifica al usuario de las órdenes que no pudieron cancelarse y se reintenta automáticamente.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE permitir al inversionista cancelar cualquier orden propia en estado PENDING o QUEUED.
- **FR-002**: El sistema DEBE liberar inmediatamente los recursos reservados (saldo o títulos) cuando una orden es cancelada exitosamente.
- **FR-003**: El sistema DEBE rechazar solicitudes de cancelación de órdenes en estado EXECUTED, CANCELLED o REJECTED, informando el estado actual de la orden.
- **FR-004**: El sistema DEBE permitir al inversionista cancelar todas sus órdenes activas con una sola acción (cancelación masiva).
- **FR-005**: El sistema DEBE permitir la cancelación de órdenes condicionales (stop-loss y take-profit) configuradas en posiciones activas.
- **FR-006**: El sistema DEBE notificar al inversionista cuando una cancelación sea procesada exitosamente (AB-33).
- **FR-007**: Toda solicitud de cancelación (exitosa o rechazada) DEBE registrarse en el log de auditoría con usuario, identificador de orden, timestamp y resultado.
- **FR-008**: El sistema DEBE garantizar que si una orden se ejecuta y se cancela en el mismo instante, solo uno de los dos estados prevalece, sin pérdida de recursos del inversionista.

### Key Entities

- **Solicitud de cancelación**: Acción del inversionista para detener una orden activa antes de su ejecución.
- **Estado CANCELLING**: Estado transitorio de una orden mientras se espera confirmación de cancelación del mercado externo.
- **Liberación de recursos**: Proceso de devolver saldo o títulos reservados al saldo y portafolio disponible del inversionista.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de las cancelaciones exitosas liberan los recursos reservados en menos de 5 segundos.
- **SC-002**: El sistema rechaza el 100% de los intentos de cancelar órdenes en estado EXECUTED, CANCELLED o REJECTED.
- **SC-003**: La cancelación masiva procesa todas las órdenes activas en menos de 30 segundos independientemente de la cantidad.
- **SC-004**: El 100% de las solicitudes de cancelación (exitosas y rechazadas) quedan registradas en el log de auditoría.

## Assumptions

- Las órdenes en estado PENDING pueden cancelarse directamente; las órdenes que ya llegaron al mercado pueden requerir una solicitud de cancelación al broker externo.
- El estado CANCELLING es transitorio y no puede perdurar indefinidamente; si el broker no confirma en un tiempo razonable, el sistema notifica al usuario.
- La cancelación de órdenes condicionales (stop-loss/take-profit) no genera cargos ni libera recursos monetarios, solo desactiva el monitoreo automático.
- Solo el propietario de la orden puede cancelarla; los administradores tienen acceso de cancelación con registro de auditoría adicional.
- El alcance de este spec cubre cancelaciones iniciadas por el usuario; las cancelaciones automáticas por expiración se especifican en AB-21.

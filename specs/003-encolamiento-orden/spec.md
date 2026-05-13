# Feature Specification: Encolamiento de Orden Fuera de Horario Bursátil

**Feature Branch**: `003-encolamiento-orden`
**Jira**: AB-24
**Created**: 2026-05-10
**Status**: Draft
**Asignado a**: David Salazar Mora

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Encolar orden cuando el mercado está cerrado (Priority: P1)

Un inversionista intenta colocar una orden de compra o venta fuera del horario
bursátil. En lugar de rechazarla, el sistema la encola y la ejecuta automáticamente
cuando el mercado abra en la próxima sesión.

**Why this priority**: Sin este flujo el inversionista perdería la oportunidad de
programar operaciones fuera de horario, que es uno de los casos de uso más comunes.

**Independent Test**: Un inversionista coloca una orden a las 8pm (mercado cerrado);
el sistema confirma que está encolada; al abrir el mercado la orden se procesa y el
inversionista recibe notificación de ejecución.

**Acceptance Scenarios**:

1. **Given** un inversionista autenticado que intenta colocar una orden fuera del
   horario bursátil configurado,
   **When** completa los datos de la orden y la confirma,
   **Then** el sistema acepta la orden con estado "En cola", muestra un mensaje
   indicando que será ejecutada al abrir el mercado y notifica al inversionista.

2. **Given** una orden encolada pendiente de ejecución,
   **When** el mercado abre en la próxima sesión,
   **Then** el sistema ejecuta la orden automáticamente en el orden de llegada
   y notifica al inversionista del resultado (ejecutada o fallida).

3. **Given** un inversionista con una o más órdenes encoladas,
   **When** accede a su historial de órdenes,
   **Then** puede ver todas las órdenes en estado "En cola" diferenciadas de las
   órdenes ya ejecutadas.

---

### User Story 2 — Cancelar una orden encolada (Priority: P2)

El inversionista puede cancelar una orden que está en cola antes de que el
mercado abra y la ejecute.

**Why this priority**: Sin la posibilidad de cancelar, el inversionista quedaría
atrapado en una operación que ya no desea. Depende de que existan órdenes en cola (P1).

**Independent Test**: Un inversionista cancela una orden encolada; el sistema
confirma la cancelación y la orden no se ejecuta al abrir el mercado.

**Acceptance Scenarios**:

1. **Given** un inversionista con una orden en estado "En cola",
   **When** solicita cancelarla antes de la apertura del mercado,
   **Then** el sistema cancela la orden, actualiza su estado a "Cancelada" y
   notifica al inversionista de la cancelación exitosa.

2. **Given** una orden que ya fue enviada al mercado para ejecución,
   **When** el inversionista intenta cancelarla,
   **Then** el sistema informa que la orden ya está en proceso de ejecución y
   no puede ser cancelada.

---

### Edge Cases

- Si hay varias órdenes encoladas del mismo inversionista, se ejecutan en orden
  FIFO (primera en entrar, primera en salir).
- Si al abrir el mercado los fondos son insuficientes para una orden encolada,
  la orden se marca como "Fallida por fondos insuficientes" y se notifica al usuario.
- Si la acción objetivo ya no está disponible al abrir el mercado, la orden se
  cancela automáticamente con notificación.
- El inversionista NO puede encolar más de un límite razonable de órdenes
  simultáneas (asumido: 10 órdenes en cola por inversionista).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE detectar automáticamente si el momento de colocación
  de una orden está fuera del horario bursátil configurado.
- **FR-002**: El sistema DEBE aceptar y encolar órdenes recibidas fuera de horario
  en lugar de rechazarlas, mostrando al inversionista que la orden queda "En cola".
- **FR-003**: El sistema DEBE mostrar al inversionista la fecha y hora estimada de
  apertura del mercado en que su orden será procesada.
- **FR-004**: El sistema DEBE ejecutar automáticamente las órdenes encoladas al
  inicio de la próxima sesión bursátil, respetando el orden FIFO.
- **FR-005**: El sistema DEBE notificar al inversionista cuando su orden encolada
  sea ejecutada o falle, indicando el motivo en caso de fallo.
- **FR-006**: El inversionista DEBE poder ver todas sus órdenes en estado "En cola"
  en su historial de órdenes.
- **FR-007**: El inversionista DEBE poder cancelar cualquier orden en estado "En cola"
  antes de que el mercado abra y la procese.
- **FR-008**: El sistema DEBE impedir la cancelación de una orden que ya esté en
  proceso de ejecución.
- **FR-009**: El sistema DEBE limitar a 10 el número máximo de órdenes simultáneas
  en cola por inversionista.

### Key Entities

- **Orden Encolada**: Orden de compra o venta pendiente de ejecución. Atributos:
  tipo (compra/venta), acción, cantidad, precio (si aplica), fecha y hora de
  encolamiento, estado (En cola / Ejecutada / Fallida / Cancelada), posición en cola.
- **Sesión Bursátil**: Período de operación del mercado. Atributos: fecha, hora de
  apertura, hora de cierre, mercado al que aplica.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de las órdenes colocadas fuera de horario son encoladas en
  lugar de rechazadas, sin pérdida de datos.
- **SC-002**: El 100% de las órdenes encoladas son procesadas dentro de los primeros
  5 minutos tras la apertura oficial del mercado.
- **SC-003**: El inversionista recibe notificación del resultado de su orden encolada
  en menos de 2 minutos desde su ejecución o fallo.
- **SC-004**: El 100% de las solicitudes de cancelación de órdenes en cola son
  procesadas antes de que el mercado abra.

## Assumptions

- Los horarios del mercado son gestionados por la funcionalidad AB-29
  (Gestión de horarios y configuración de mercados); este spec los consume como dato.
- Las órdenes encoladas aplican a market orders y limit orders; el tipo específico
  de orden sigue las reglas definidas en sus respectivos specs.
- La notificación al inversionista usa el canal preferido configurado en su perfil
  (AB-17).
- El límite de 10 órdenes en cola por inversionista puede ajustarse por configuración
  del administrador.
- Solo los inversionistas pueden encolar órdenes; los comisionistas siguen el mismo
  flujo al generar órdenes a nombre de clientes.

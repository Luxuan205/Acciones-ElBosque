# Feature Specification: Gestión de Horarios y Configuración de Mercados

**Feature Branch**: `008-configuracion-mercados`
**Jira**: AB-29
**Created**: 2026-05-10
**Status**: Draft
**Asignado a**: José Buitrago

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Configurar horario de apertura y cierre del mercado (Priority: P1)

El administrador del sistema define los horarios de operación del mercado bursátil:
hora de apertura, hora de cierre y días hábiles. Estos horarios determinan cuándo
las órdenes se ejecutan en tiempo real y cuándo se encolan.

**Why this priority**: El horario del mercado es un parámetro central que afecta
a múltiples funcionalidades (encolamiento de órdenes, dashboard, notificaciones).
Debe configurarse antes de que cualquier operación sea posible.

**Independent Test**: Un administrador define el horario 9:00–15:30 de lunes a
viernes; el sistema rechaza órdenes en tiempo real fuera de ese horario y las
encola en su lugar; el dashboard indica "mercado cerrado" fuera de ese horario.

**Acceptance Scenarios**:

1. **Given** un administrador autenticado en el panel de configuración de mercados,
   **When** establece la hora de apertura, hora de cierre y días hábiles de la semana,
   **Then** el sistema guarda la configuración y la aplica inmediatamente a toda
   la lógica de procesamiento de órdenes y estado del mercado.

2. **Given** una configuración de horario activa,
   **When** el reloj del sistema alcanza la hora de apertura en un día hábil,
   **Then** el sistema cambia el estado del mercado a "Abierto" y procesa
   automáticamente las órdenes encoladas en orden FIFO.

3. **Given** una configuración de horario activa,
   **When** el reloj del sistema alcanza la hora de cierre,
   **Then** el sistema cambia el estado del mercado a "Cerrado" y suspende la
   ejecución de nuevas órdenes en tiempo real.

---

### User Story 2 — Gestionar días festivos y cierres especiales (Priority: P2)

El administrador puede marcar días específicos como no hábiles (festivos o cierres
extraordinarios), de modo que el mercado no opere esos días aunque sean días de
la semana normalmente hábiles.

**Why this priority**: Los mercados bursátiles tienen festivos que no siguen el
calendario estándar; sin esta gestión el sistema procesaría órdenes en días
en que el mercado real está cerrado.

**Independent Test**: Un administrador marca el 20 de julio como festivo; ese día
el sistema no abre el mercado aunque sea día hábil según el calendario semanal,
y encola las órdenes hasta el siguiente día hábil.

**Acceptance Scenarios**:

1. **Given** un administrador en la configuración de mercados,
   **When** agrega una fecha específica como día no hábil con una descripción
   (ej. "Día de la Independencia"),
   **Then** el sistema registra esa fecha como festivo y no abre el mercado ese día.

2. **Given** una fecha marcada como festivo,
   **When** llega ese día,
   **Then** el sistema mantiene el mercado cerrado todo el día, encola las órdenes
   recibidas y las procesa el siguiente día hábil.

3. **Given** un administrador que revisa los festivos configurados,
   **When** accede al calendario de días no hábiles,
   **Then** el sistema muestra todos los festivos configurados con su fecha y descripción,
   y permite agregar, editar o eliminar entradas.

---

### Edge Cases

- Si el administrador modifica el horario mientras el mercado está abierto, el
  nuevo horario aplica a partir de la siguiente sesión, no en la sesión actual.
- Si se elimina un festivo después de que ese día ya pasó, el historial de
  operaciones no se modifica.
- El sistema opera en la zona horaria de Colombia (UTC-5); no se soportan múltiples
  zonas horarias en esta versión.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE permitir al administrador configurar la hora de apertura
  y cierre del mercado y los días hábiles de la semana.
- **FR-002**: El sistema DEBE aplicar la configuración de horario a toda la lógica
  de procesamiento de órdenes: ejecución en tiempo real vs. encolamiento.
- **FR-003**: El sistema DEBE cambiar automáticamente el estado del mercado
  (Abierto/Cerrado) según el horario configurado.
- **FR-004**: El sistema DEBE permitir al administrador registrar días festivos o
  cierres extraordinarios con fecha y descripción.
- **FR-005**: En días festivos configurados, el sistema DEBE mantener el mercado
  cerrado independientemente del horario semanal.
- **FR-006**: El sistema DEBE mostrar al administrador el calendario de días no
  hábiles configurados, con opción de agregar, editar y eliminar entradas.
- **FR-007**: Los cambios de horario realizados mientras el mercado está abierto
  DEBEN aplicarse a partir de la siguiente sesión.
- **FR-008**: El estado actual del mercado (Abierto/Cerrado) DEBE ser consultable
  por todos los módulos del sistema.
- **FR-009**: Solo los administradores del sistema DEBEN poder modificar la
  configuración de horarios y festivos.

### Key Entities

- **Horario de Mercado**: Configuración de operación. Atributos: hora de apertura,
  hora de cierre, días hábiles (lunes a domingo), zona horaria.
- **Día Festivo**: Excepción al calendario regular. Atributos: fecha, descripción,
  tipo (festivo nacional / cierre extraordinario).
- **Estado del Mercado**: Condición operativa en tiempo real. Atributos: estado
  (Abierto / Cerrado), próxima apertura (fecha y hora), próximo cierre (fecha y hora).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El estado del mercado cambia automáticamente de Cerrado a Abierto
  dentro de los 30 segundos siguientes a la hora de apertura configurada, en el
  100% de los días hábiles.
- **SC-002**: El 100% de los días festivos configurados son respetados; el mercado
  no abre en ninguno de ellos.
- **SC-003**: Los cambios de configuración realizados por el administrador se
  propagan a todos los módulos del sistema en menos de 60 segundos.
- **SC-004**: El 0% de los usuarios no administradores pueden modificar la
  configuración de horarios o festivos.

## Assumptions

- La zona horaria de operación es UTC-5 (Colombia); no se soportan múltiples zonas
  horarias en esta versión.
- El administrador es un rol separado del inversionista y del comisionista;
  la gestión de roles se define en AB-41.
- La configuración inicial del horario del mercado se realizará durante el
  despliegue del sistema; este spec gestiona las modificaciones posteriores.
- El sistema solo gestiona un mercado (Bolsa de Valores de Colombia); no se
  contempla operación en múltiples mercados simultáneos.

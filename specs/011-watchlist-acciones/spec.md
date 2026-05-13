# Feature Specification: Watchlist de Acciones (Funcionalidad Premium)

**Feature Branch**: `011-watchlist-acciones`
**Jira**: AB-36
**Created**: 2026-05-10
**Status**: Draft
**Asignado a**: David Salazar Mora

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Crear y gestionar una watchlist personalizada (Priority: P1)

El inversionista con suscripción premium puede crear una lista personalizada de
acciones de interés (watchlist), agregar acciones a seguir y eliminarlas cuando
ya no le interesen, para tener un seguimiento concentrado de las acciones que
más le importan.

**Why this priority**: La watchlist es la funcionalidad diferenciadora del plan
premium. Sin poder crearla y gestionarla, el resto de la funcionalidad premium
de seguimiento no tiene sentido.

**Independent Test**: Un inversionista premium agrega 5 acciones a su watchlist;
el sistema las persiste; al volver a la plataforma la watchlist muestra las mismas
5 acciones; puede eliminar una y queda con 4.

**Acceptance Scenarios**:

1. **Given** un inversionista con suscripción premium activa,
   **When** accede a la sección Watchlist y agrega una acción buscándola por
   símbolo o nombre,
   **Then** el sistema añade la acción a su watchlist personal y la muestra con
   precio actual y variación del día.

2. **Given** un inversionista premium con acciones en su watchlist,
   **When** elimina una acción de la lista,
   **Then** el sistema la remueve inmediatamente de la watchlist sin afectar
   otras funcionalidades (portafolio, órdenes).

3. **Given** un inversionista con suscripción estándar (no premium),
   **When** intenta acceder a la sección Watchlist,
   **Then** el sistema muestra un mensaje indicando que esta funcionalidad es
   exclusiva para suscriptores premium, con información sobre cómo activar
   la suscripción premium.

---

### User Story 2 — Ver precios actualizados de la watchlist (Priority: P2)

El inversionista premium ve en su watchlist los precios de mercado actualizados
de todas las acciones que tiene en seguimiento, con variación del día y alertas
visuales de movimientos significativos.

**Why this priority**: El valor de la watchlist está en el seguimiento en tiempo
real; sin precios actualizados pierde su utilidad.

**Independent Test**: Un inversionista premium con 10 acciones en watchlist abre
la sección; el sistema muestra precio actual, variación del día y hora de la
última actualización para cada acción.

**Acceptance Scenarios**:

1. **Given** un inversionista premium con acciones en su watchlist durante el
   horario bursátil,
   **When** accede a la sección Watchlist,
   **Then** el sistema muestra para cada acción: símbolo, nombre, precio actual,
   variación del día (monto y porcentaje) y hora de la última actualización,
   actualizándose automáticamente.

2. **Given** una acción en la watchlist que supera una variación del 5% en el día,
   **When** el inversionista está en la plataforma,
   **Then** el sistema muestra una indicación visual destacada (ej. resaltado)
   sobre esa acción en la watchlist.

---

### Edge Cases

- Si la suscripción premium de un inversionista expira, su watchlist se conserva
  pero no es accesible hasta que renueve; al renovar, recupera su watchlist sin
  pérdida de datos.
- El límite máximo de acciones en una watchlist es de 50 acciones por inversionista.
- Si una acción en la watchlist deja de cotizar, permanece en la lista con el
  último precio conocido y una indicación de que no está activa.
- Un inversionista no puede tener múltiples watchlists en esta versión; solo una
  lista personal.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: La funcionalidad de watchlist DEBE estar disponible exclusivamente
  para inversionistas con suscripción premium activa.
- **FR-002**: El inversionista premium DEBE poder agregar acciones a su watchlist
  buscando por símbolo o nombre de empresa.
- **FR-003**: El inversionista DEBE poder eliminar acciones de su watchlist en
  cualquier momento.
- **FR-004**: El sistema DEBE persistir la watchlist del inversionista entre sesiones;
  las acciones agregadas deben estar disponibles al volver a iniciar sesión.
- **FR-005**: La watchlist DEBE mostrar precio actual, variación del día (monto y
  porcentaje) y hora de última actualización para cada acción.
- **FR-006**: Los precios de la watchlist DEBEN actualizarse automáticamente durante
  el horario bursátil con una frecuencia máxima de 60 segundos.
- **FR-007**: El sistema DEBE limitar la watchlist a un máximo de 50 acciones
  por inversionista.
- **FR-008**: El sistema DEBE resaltar visualmente acciones con variaciones del
  día superiores al 5% (positivo o negativo).
- **FR-009**: Al expirar la suscripción premium, el sistema DEBE conservar la
  watchlist sin eliminarla, restaurando el acceso cuando se renueve la suscripción.
- **FR-010**: Un inversionista sin suscripción premium que intente acceder a la
  watchlist DEBE ver un mensaje de actualización de suscripción, nunca los datos.

### Key Entities

- **Watchlist**: Lista personalizada de seguimiento. Atributos: inversionista
  propietario, lista de acciones en seguimiento, fecha de creación.
- **Entrada de Watchlist**: Acción individual en seguimiento. Atributos: símbolo
  de acción, fecha en que fue agregada, precio al momento de agregar.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de los intentos de acceso a la watchlist por inversionistas
  no premium son bloqueados con mensaje de actualización de suscripción.
- **SC-002**: Un inversionista premium puede agregar o eliminar una acción de su
  watchlist en menos de 5 segundos.
- **SC-003**: Los precios de la watchlist se actualizan con un desfase máximo de
  60 segundos durante el horario bursátil.
- **SC-004**: La watchlist de un inversionista cuya suscripción expiró se recupera
  intacta en el 100% de los casos al renovar la suscripción.

## Assumptions

- La gestión de la suscripción premium (activación y estado) es responsabilidad
  de la funcionalidad AB-18 (Activación de suscripción premium); este spec solo
  consume el estado activo/inactivo.
- Los datos de precios de mercado son provistos por el módulo de datos de mercado
  ya integrado.
- Solo existe una watchlist por inversionista en esta versión; múltiples listas
  temáticas quedan para versión futura.
- El inversionista puede tener la misma acción en su watchlist y en su portafolio
  simultáneamente; son listas independientes.

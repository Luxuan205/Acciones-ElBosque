# Feature Specification: Gestión de Clientes Asignados por el Comisionista

**Feature Branch**: `009-clientes-comisionista`
**Jira**: AB-31
**Created**: 2026-05-10
**Status**: Draft
**Asignado a**: José Buitrago

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Ver lista de clientes asignados (Priority: P1)

El comisionista autenticado puede ver la lista completa de inversionistas que le
han sido asignados: nombre, estado de cuenta, saldo disponible y resumen de
actividad reciente.

**Why this priority**: Sin visibilidad de su cartera de clientes el comisionista
no puede gestionar ni ejecutar operaciones a nombre de ellos.

**Independent Test**: Un comisionista con clientes asignados accede a su sección;
el sistema muestra la lista de clientes con nombre, estado y saldo resumido.

**Acceptance Scenarios**:

1. **Given** un comisionista autenticado en la plataforma,
   **When** accede a la sección "Mis Clientes",
   **Then** el sistema muestra la lista de todos los inversionistas asignados a
   ese comisionista con: nombre completo, estado de cuenta (activo/inactivo),
   saldo disponible y número de órdenes activas.

2. **Given** un comisionista que selecciona un cliente de la lista,
   **When** accede al detalle del cliente,
   **Then** el sistema muestra el perfil resumido del cliente, su portafolio de
   inversiones, saldo de fondos e historial de órdenes recientes.

3. **Given** un comisionista sin clientes asignados,
   **When** accede a la sección "Mis Clientes",
   **Then** el sistema muestra un mensaje indicando que aún no tiene clientes
   asignados y orienta al comisionista sobre el proceso de asignación.

---

### User Story 2 — Buscar y filtrar clientes (Priority: P2)

El comisionista puede buscar entre sus clientes asignados por nombre o filtrar
por estado de cuenta para gestionar su cartera eficientemente.

**Why this priority**: A medida que la cartera de clientes crece, la búsqueda y
filtrado son esenciales para localizar rápidamente a un cliente específico.

**Independent Test**: Un comisionista con varios clientes busca por nombre parcial;
el sistema filtra y muestra solo los clientes que coinciden.

**Acceptance Scenarios**:

1. **Given** un comisionista con múltiples clientes,
   **When** escribe el nombre o parte del nombre de un cliente en el campo de
   búsqueda,
   **Then** el sistema filtra la lista mostrando solo los clientes que coinciden
   con el término buscado.

2. **Given** un comisionista en la vista de clientes,
   **When** aplica el filtro "Solo activos" o "Solo inactivos",
   **Then** el sistema muestra únicamente los clientes en el estado seleccionado.

---

### Edge Cases

- Un comisionista SOLO puede ver a sus propios clientes asignados; no puede acceder
  a clientes asignados a otros comisionistas.
- Si un cliente es reasignado a otro comisionista, desaparece de la lista del
  comisionista original sin necesidad de acción de su parte.
- El comisionista puede ver el saldo del cliente pero NO puede modificar los datos
  personales del cliente; esa operación corresponde al inversionista o al administrador.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE mostrar al comisionista autenticado únicamente la
  lista de inversionistas que le han sido asignados formalmente.
- **FR-002**: La lista de clientes DEBE mostrar por cada cliente: nombre completo,
  estado de cuenta, saldo disponible y número de órdenes activas.
- **FR-003**: El comisionista DEBE poder acceder al detalle de cada cliente:
  portafolio resumido, saldo de fondos e historial de órdenes recientes.
- **FR-004**: El comisionista DEBE poder buscar clientes por nombre (búsqueda
  parcial) dentro de su lista asignada.
- **FR-005**: El comisionista DEBE poder filtrar su lista de clientes por estado
  de cuenta (activo / inactivo).
- **FR-006**: El sistema DEBE impedir que un comisionista acceda a información de
  clientes asignados a otros comisionistas.
- **FR-007**: La asignación de clientes a comisionistas es responsabilidad del
  administrador del sistema; el comisionista NO puede auto-asignarse clientes.

### Key Entities

- **Asignación**: Relación entre comisionista e inversionista. Atributos: comisionista
  asignado, inversionista asignado, fecha de asignación, estado (activa/inactiva).
- **Resumen de Cliente**: Vista consolidada del estado de un cliente para el
  comisionista. Atributos: nombre, estado de cuenta, saldo disponible, número de
  órdenes activas, última orden registrada.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: La lista de clientes asignados carga completamente en menos de
  3 segundos para carteras de hasta 100 clientes.
- **SC-002**: El 0% de los comisionistas puede acceder a información de clientes
  asignados a otros comisionistas.
- **SC-003**: La búsqueda de clientes por nombre filtra resultados en menos de
  1 segundo desde que el comisionista escribe.
- **SC-004**: Los cambios de asignación realizados por el administrador se reflejan
  en la vista del comisionista en menos de 60 segundos.

## Assumptions

- La asignación inicial de clientes a comisionistas es gestionada por el
  administrador del sistema (AB-41); este spec solo consume esa asignación.
- Un inversionista puede estar asignado a un único comisionista a la vez.
- El comisionista tiene acceso de lectura al portafolio y saldo del cliente;
  no puede modificar datos personales del cliente.
- El historial de órdenes del cliente visible al comisionista incluye todas las
  órdenes, incluyendo las que el mismo comisionista generó a nombre del cliente.

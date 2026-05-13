# Feature Specification: Gestión de Parámetros Globales del Sistema

**Feature Branch**: `025-parametros-globales`  
**Created**: 2026-05-12  
**Status**: Draft  
**Jira**: AB-40  

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consulta de parámetros del sistema (Priority: P1)

El administrador puede ver todos los parámetros de configuración global del sistema en una pantalla centralizada: duración de la suscripción premium, número máximo de intentos fallidos de login, tiempo de vigencia del token de acceso, umbral de retención del log de auditoría, y otros valores configurables. Los parámetros están organizados por categoría para facilitar su localización.

**Why this priority**: Los parámetros globales controlan el comportamiento de múltiples módulos del sistema. Sin visibilidad centralizada, los administradores no pueden gestionar el sistema eficientemente.

**Independent Test**: Acceder a la pantalla de parámetros globales con un usuario ADMIN y verificar que se listan todos los parámetros del sistema con sus valores actuales, organizados por categoría.

**Acceptance Scenarios**:

1. **Given** un administrador autenticado con rol ADMIN, **When** accede a la pantalla de parámetros globales, **Then** ve todos los parámetros organizados por categoría (seguridad, suscripciones, auditoría, trading) con sus valores actuales y descripciones.
2. **Given** un usuario con rol INVESTOR o BROKER, **When** intenta acceder a la gestión de parámetros, **Then** el sistema rechaza el acceso.

---

### User Story 2 - Modificación de parámetros del sistema (Priority: P1)

El administrador puede modificar el valor de un parámetro del sistema. Antes de aplicar el cambio, el sistema muestra el impacto esperado del cambio. El nuevo valor entra en vigor de inmediato o en el próximo ciclo de operación según el tipo de parámetro. Cada cambio queda registrado en el log de auditoría.

**Why this priority**: La capacidad de ajustar parámetros en producción sin deploys es esencial para la operación del sistema y para adaptarse a cambios regulatorios o de negocio.

**Independent Test**: Modificar el parámetro de máximo de intentos fallidos de login, intentar autenticarse más veces de las permitidas y verificar que el bloqueo ocurre con el nuevo límite.

**Acceptance Scenarios**:

1. **Given** un administrador que modifica el valor de un parámetro, **When** confirma el cambio, **Then** el nuevo valor se aplica de inmediato y queda registrado en el log de auditoría con el valor anterior, el nuevo valor, el usuario y el timestamp.
2. **Given** un administrador que ingresa un valor inválido para un parámetro (fuera de rango, tipo incorrecto), **When** intenta guardar, **Then** el sistema rechaza el cambio con un mensaje de validación específico.
3. **Given** un administrador que modifica la duración de la suscripción premium, **When** el cambio es aplicado, **Then** las nuevas activaciones de suscripción usan la duración actualizada; las suscripciones existentes no se afectan.

---

### User Story 3 - Historial de cambios de parámetros (Priority: P2)

El administrador puede consultar el historial completo de cambios realizados a los parámetros del sistema: qué parámetro fue cambiado, de qué valor a qué valor, quién lo cambió y cuándo. Esto permite auditar decisiones de configuración y revertir cambios si es necesario.

**Why this priority**: El historial de cambios es esencial para diagnóstico de problemas relacionados con configuración y para cumplimiento de auditoría.

**Independent Test**: Realizar múltiples cambios de parámetros con diferentes usuarios ADMIN y verificar que el historial refleja todos los cambios con los datos correctos.

**Acceptance Scenarios**:

1. **Given** un administrador que consulta el historial de cambios, **When** filtra por parámetro específico, **Then** ve todos los cambios históricos de ese parámetro con valor anterior, nuevo valor, usuario y timestamp.
2. **Given** un administrador que identifica un cambio problemático en el historial, **When** selecciona revertir al valor anterior, **Then** el sistema aplica el valor anterior como si fuera un nuevo cambio (con su propio registro de auditoría).

---

### Edge Cases

- ¿Qué pasa si dos administradores modifican el mismo parámetro simultáneamente? El sistema aplica el último cambio guardado; el primero en guardar gana; el segundo recibe una advertencia de conflicto y puede revisar el valor actualizado.
- ¿Qué pasa si un parámetro crítico se configura con un valor que hace el sistema inoperable (por ejemplo, intentos de login = 0)? El sistema valida rangos mínimos y máximos para cada parámetro y rechaza valores que estén fuera de los límites de seguridad.
- ¿Qué pasa si el administrador necesita hacer rollback de múltiples parámetros? El sistema no soporta rollback masivo en el MVP; cada parámetro debe revertirse individualmente.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE mostrar al administrador todos los parámetros de configuración global organizados por categoría, con nombre, valor actual y descripción de cada parámetro.
- **FR-002**: El sistema DEBE permitir al administrador modificar el valor de cualquier parámetro global dentro de los rangos válidos definidos para cada uno.
- **FR-003**: El sistema DEBE validar el tipo y rango de cada parámetro antes de aplicar el cambio, rechazando valores inválidos con un mensaje descriptivo.
- **FR-004**: El sistema DEBE aplicar el nuevo valor de un parámetro de inmediato tras la confirmación del administrador, sin requerir reinicio del sistema.
- **FR-005**: Toda modificación de parámetro DEBE quedar registrada en el log de auditoría con: parámetro afectado, valor anterior, nuevo valor, usuario responsable y timestamp.
- **FR-006**: El sistema DEBE permitir al administrador consultar el historial de cambios de cada parámetro.
- **FR-007**: El sistema DEBE permitir al administrador revertir un parámetro a su valor anterior mediante el historial de cambios.
- **FR-008**: El acceso a la gestión de parámetros globales DEBE estar restringido al rol ADMIN.

### Key Entities

- **Parámetro global**: Variable de configuración del sistema con nombre, valor actual, tipo, rango válido, descripción y categoría.
- **Categoría de parámetro**: Agrupación lógica de parámetros relacionados (seguridad, suscripciones, auditoría, trading).
- **Historial de cambio**: Registro inmutable de cada modificación a un parámetro, con valores anterior y nuevo, responsable y timestamp.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Los cambios de parámetros entran en vigor en menos de 5 segundos tras la confirmación del administrador.
- **SC-002**: El 100% de las modificaciones de parámetros quedan registradas en el log de auditoría.
- **SC-003**: El sistema rechaza el 100% de los valores de parámetros que estén fuera de los rangos válidos definidos.
- **SC-004**: El historial de cambios retiene el 100% de las modificaciones desde el inicio de operación del sistema.

## Assumptions

- Los parámetros globales y sus rangos válidos son definidos en el momento del desarrollo y no pueden añadirse en producción sin una actualización del sistema.
- Las categorías de parámetros iniciales son: Seguridad (intentos de login, vigencia de token, vigencia de OTP), Suscripciones (duración premium), Auditoría (retención del log), y Trading (vigencia por defecto de limit orders, máximo de alertas de precio por usuario).
- Los parámetros tienen valores por defecto predefinidos que aplican en la instalación inicial del sistema.
- Los parámetros no afectan retroactivamente los registros o estados ya creados (ej: cambiar la duración de suscripción no modifica suscripciones existentes).
- El historial de parámetros es accesible solo para el rol ADMIN y no puede eliminarse.

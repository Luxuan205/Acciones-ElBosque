# Feature Specification: Dashboard Directivo

**Feature Branch**: `024-dashboard-directivo`  
**Created**: 2026-05-12  
**Status**: Draft  
**Jira**: AB-39  

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Vista de métricas operativas en tiempo real (Priority: P1)

El administrador del sistema puede acceder a un dashboard que muestra métricas operativas clave en tiempo real: número de órdenes activas, volumen total de transacciones del día, número de usuarios activos en la sesión, estado del mercado (abierto/cerrado) y alertas del sistema activas.

**Why this priority**: El administrador necesita visibilidad operativa inmediata para supervisar la plataforma, detectar anomalías y responder a incidentes sin necesidad de acceder a logs o bases de datos directamente.

**Independent Test**: Acceder al dashboard con un usuario ADMIN durante una sesión con transacciones activas y verificar que las métricas operativas se actualizan correctamente.

**Acceptance Scenarios**:

1. **Given** un administrador autenticado con rol ADMIN, **When** accede al dashboard directivo, **Then** ve el estado del mercado, el número de órdenes activas, el volumen de transacciones del día y los usuarios conectados.
2. **Given** un administrador viendo el dashboard, **When** se genera una alerta del sistema (error crítico, anomalía de volumen), **Then** la alerta aparece en el dashboard de forma destacada.
3. **Given** un usuario con rol INVESTOR o BROKER, **When** intenta acceder al dashboard directivo, **Then** el sistema rechaza el acceso con un mensaje de permisos insuficientes.

---

### User Story 2 - Resumen financiero del período (Priority: P2)

El administrador puede consultar en el dashboard un resumen financiero: volumen total de transacciones, ingresos por comisiones, número de nuevos usuarios registrados, y número de suscripciones premium activas, para el día, semana o mes en curso.

**Why this priority**: El resumen financiero es esencial para la gestión del negocio y para reportes a la dirección. Permite identificar tendencias sin necesidad de generar reportes manuales.

**Independent Test**: Verificar que los totales del dashboard (volumen, comisiones, suscripciones) coinciden con la suma de transacciones registradas en el período seleccionado.

**Acceptance Scenarios**:

1. **Given** un administrador en el dashboard, **When** selecciona el período "hoy", **Then** ve el volumen total de transacciones, ingresos estimados por comisiones, nuevos registros y suscripciones premium activas del día.
2. **Given** un administrador que cambia el período a "este mes", **When** el dashboard se actualiza, **Then** los valores reflejan el mes en curso desde el día 1 hasta hoy.

---

### User Story 3 - Acceso rápido a acciones administrativas (Priority: P2)

Desde el dashboard, el administrador puede acceder directamente a acciones frecuentes: gestión de usuarios (AB-41), configuración de parámetros del mercado (AB-08), y consulta del log de auditoría (AB-38), sin necesidad de navegar por menús complejos.

**Why this priority**: Los accesos directos reducen el tiempo de respuesta ante situaciones que requieren acción inmediata del administrador.

**Independent Test**: Verificar que los accesos directos del dashboard llevan al módulo correcto y que el usuario mantiene sus permisos de administrador en los módulos de destino.

**Acceptance Scenarios**:

1. **Given** un administrador en el dashboard, **When** hace clic en el acceso directo a gestión de usuarios, **Then** es llevado directamente al módulo de gestión de usuarios con su sesión activa.
2. **Given** un administrador que usa el acceso directo al log de auditoría, **When** llega al módulo, **Then** el log ya muestra los eventos más recientes sin pasos adicionales de configuración.

---

### Edge Cases

- ¿Qué pasa si el sistema no puede cargar las métricas en tiempo real (alta carga)? El dashboard muestra el último valor conocido con la marca de tiempo de la última actualización exitosa.
- ¿Qué pasa si hay múltiples administradores viendo el dashboard simultáneamente? Cada sesión es independiente; las métricas se calculan por separado para cada administrador sin interferencia.
- ¿Qué pasa si los datos del resumen financiero muestran discrepancias respecto a los registros detallados? El dashboard usa los mismos datos fuente que los módulos de origen; cualquier discrepancia indica un problema de sincronización que se reporta como alerta del sistema.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE mostrar al administrador métricas operativas en tiempo real: estado del mercado, órdenes activas, volumen de transacciones del día y usuarios conectados.
- **FR-002**: El sistema DEBE mostrar alertas del sistema activas de forma destacada en el dashboard (errores críticos, anomalías de operación).
- **FR-003**: El sistema DEBE restringir el acceso al dashboard directivo exclusivamente a usuarios con rol ADMIN.
- **FR-004**: El sistema DEBE mostrar un resumen financiero del período seleccionado (día, semana, mes): volumen de transacciones, ingresos por comisiones, nuevos registros y suscripciones premium activas.
- **FR-005**: El sistema DEBE permitir al administrador seleccionar el período del resumen financiero (hoy, esta semana, este mes).
- **FR-006**: El sistema DEBE proveer accesos directos a los módulos de gestión frecuente: gestión de usuarios, configuración de mercados y log de auditoría.
- **FR-007**: Las métricas del dashboard DEBEN actualizarse automáticamente sin necesidad de recargar la página, con una frecuencia máxima de 60 segundos.

### Key Entities

- **Métrica operativa**: Dato cuantitativo sobre el estado actual del sistema (órdenes activas, usuarios conectados, estado del mercado).
- **Resumen financiero**: Agregación de transacciones y suscripciones para un período definido, expresada en totales y valores monetarios.
- **Alerta del sistema**: Evento de prioridad alta que requiere atención del administrador (error crítico, anomalía detectada).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El dashboard carga completamente en menos de 5 segundos en condiciones normales de operación.
- **SC-002**: Las métricas operativas en tiempo real se actualizan con un retraso máximo de 60 segundos respecto al estado real del sistema.
- **SC-003**: El 100% de los accesos no-ADMIN al dashboard son bloqueados.
- **SC-004**: Los totales del resumen financiero tienen una precisión del 100% respecto a los registros detallados del sistema.

## Assumptions

- El dashboard es una vista de solo lectura; las acciones de modificación se realizan en los módulos de destino.
- Las métricas del dashboard se calculan a partir de los datos ya disponibles en los módulos existentes (AB-26, AB-33, AB-38); no requieren un almacén de datos separado para el MVP.
- El módulo de configuración de mercados (AB-08) es el destino del acceso directo para parámetros del mercado.
- Los ingresos por comisiones en el resumen financiero son estimados basados en las comisiones registradas en el módulo AB-25; no son datos contables definitivos.
- El dashboard directivo es solo para el rol ADMIN; los brokers tienen vistas de gestión propias en otros módulos.

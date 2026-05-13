# Feature Specification: Auditoría y Cumplimiento Legal

**Feature Branch**: `023-auditoria-legal`  
**Created**: 2026-05-12  
**Status**: Draft  
**Jira**: AB-38  

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Registro automático de eventos de auditoría (Priority: P1)

Cada acción significativa realizada en el sistema (inicio de sesión, colocación o cancelación de orden, cambio de perfil, activación de suscripción, acceso a funcionalidades premium) queda registrada automáticamente en el log de auditoría con el usuario responsable, el timestamp, el tipo de acción y el resultado.

**Why this priority**: El log de auditoría es un requisito regulatorio para plataformas de inversión. Sin él, el sistema no puede demostrar cumplimiento ante autoridades ni resolver disputas de forma verificable.

**Independent Test**: Realizar una secuencia de acciones (login, colocar orden, cancelar orden) y verificar que cada evento aparece en el log de auditoría con todos los campos requeridos.

**Acceptance Scenarios**:

1. **Given** un inversionista que inicia sesión, **When** la autenticación se completa (exitosa o fallida), **Then** el evento queda registrado en el log con usuario, timestamp, resultado y origen (IP o identificador de dispositivo).
2. **Given** un inversionista que coloca una orden, **When** la orden es creada, **Then** el log registra el usuario, acción, cantidad, precio y estado inicial de la orden.
3. **Given** cualquier acción significativa en el sistema, **When** ocurre, **Then** el log de auditoría registra el evento en menos de 1 segundo, sin que el usuario perciba impacto en el rendimiento.

---

### User Story 2 - Consulta y filtrado del log de auditoría por el administrador (Priority: P1)

El administrador del sistema puede consultar el log de auditoría completo, filtrado por usuario, tipo de evento, rango de fechas o resultado. Los registros son inmutables: ningún usuario, incluido el administrador, puede modificarlos o eliminarlos.

**Why this priority**: La capacidad de auditoría solo tiene valor si los registros son consultables y confiables. Es el mecanismo de control de fraude y resolución de disputas.

**Independent Test**: Ejecutar eventos de múltiples tipos, consultar el log con filtros combinados y verificar que los resultados son precisos y completos.

**Acceptance Scenarios**:

1. **Given** un administrador que filtra el log por usuario y rango de fechas, **When** ejecuta la consulta, **Then** ve solo los eventos del usuario en el período indicado, en orden cronológico.
2. **Given** un administrador que intenta eliminar o modificar un registro del log, **When** intenta la acción, **Then** el sistema rechaza la operación; el log es de solo lectura.
3. **Given** un administrador que exporta el log de auditoría, **When** descarga el archivo, **Then** contiene todos los campos de cada evento sin truncamiento.

---

### User Story 3 - Retención y archivo de registros de auditoría (Priority: P2)

Los registros de auditoría se conservan por el período mínimo exigido por regulación (5 años), y los registros que superan el período activo se archivan automáticamente sin eliminación. El administrador puede acceder a registros archivados bajo solicitud.

**Why this priority**: La retención legal de registros es un requisito normativo. El archivado evita que el almacenamiento activo crezca indefinidamente.

**Independent Test**: Configurar una política de retención corta en entorno de pruebas, verificar que los registros antiguos se archivan y que el administrador puede consultarlos en el archivo.

**Acceptance Scenarios**:

1. **Given** registros de auditoría que superan el período activo configurado, **When** el proceso de archivado se ejecuta, **Then** los registros se mueven al archivo sin eliminarse y sin perder integridad.
2. **Given** un administrador que necesita consultar registros archivados, **When** accede al archivo, **Then** puede consultarlos con los mismos filtros disponibles para registros activos.

---

### Edge Cases

- ¿Qué pasa si el sistema de log falla durante una transacción crítica? La transacción puede completarse, pero el sistema intenta registrar el evento de auditoría en un modo de reintento; si falla definitivamente, genera una alerta al administrador.
- ¿Qué pasa si un registro contiene datos personales sujetos a protección de datos? Los datos personales en el log se manejan conforme a la política de privacidad; el log puede anonimizar datos sensibles según la regulación aplicable.
- ¿Qué pasa si el volumen de eventos supera la capacidad de escritura del log? El sistema encola los eventos de auditoría para escritura asíncrona; el timestamp registrado es el del momento del evento, no de la escritura.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE registrar automáticamente en el log de auditoría todo evento significativo: autenticación, órdenes, cancelaciones, cambios de perfil, activación/desactivación de suscripción, configuración de alertas.
- **FR-002**: Cada registro de auditoría DEBE contener: identificador de evento, usuario responsable, tipo de acción, timestamp, resultado (éxito/fallo) y datos contextuales relevantes (acción, cantidad, precio, motivo, IP).
- **FR-003**: El log de auditoría DEBE ser inmutable: ningún usuario puede modificar o eliminar registros una vez creados.
- **FR-004**: El sistema DEBE permitir al administrador consultar el log de auditoría con filtros por usuario, tipo de evento, rango de fechas y resultado.
- **FR-005**: El sistema DEBE permitir al administrador exportar el log de auditoría filtrado.
- **FR-006**: Los registros de auditoría DEBEN conservarse por un mínimo de 5 años desde la fecha del evento.
- **FR-007**: Los registros que superen el período activo DEBEN archivarse automáticamente sin eliminación, y el administrador debe poder consultarlos.
- **FR-008**: El registro de cada evento de auditoría DEBE completarse en menos de 1 segundo sin impacto perceptible en la operación que lo origina.

### Key Entities

- **Registro de auditoría**: Entrada inmutable en el log que documenta un evento del sistema con todos sus datos contextuales.
- **Tipo de evento**: Categoría de la acción registrada (AUTH_SUCCESS, AUTH_FAILURE, ORDER_CREATED, ORDER_CANCELLED, PROFILE_UPDATED, SUBSCRIPTION_ACTIVATED, etc.).
- **Log activo / archivo**: El log activo contiene registros recientes consultables en tiempo real; el archivo contiene registros históricos fuera del período activo.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de los eventos significativos del sistema quedan registrados en el log de auditoría.
- **SC-002**: El tiempo de escritura de un registro de auditoría es menor a 1 segundo en el 99% de los casos.
- **SC-003**: Los registros de auditoría se conservan íntegros por al menos 5 años desde su creación.
- **SC-004**: El sistema rechaza el 100% de los intentos de modificar o eliminar registros del log.
- **SC-005**: Las consultas del log con filtros combinados retornan resultados en menos de 10 segundos para períodos de hasta 1 año.

## Assumptions

- La definición de "eventos significativos" puede ampliarse por configuración del administrador sin cambios en el código.
- El período activo de retención del log y el período total de conservación son configurables en los parámetros globales (AB-40).
- El proceso de archivado de registros se ejecuta automáticamente de forma periódica; la frecuencia es configurable.
- La regulación colombiana de mercado de valores es la referencia normativa para los requisitos de retención y contenido del log.
- El acceso al log de auditoría está restringido a roles ADMIN y BROKER (con alcance limitado a sus propias operaciones para BROKER).

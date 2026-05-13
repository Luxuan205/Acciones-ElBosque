# Feature Specification: Gestión de Perfil de Usuario

**Feature Branch**: `002-gestion-perfil`
**Jira**: AB-17
**Created**: 2026-05-10
**Status**: Draft
**Asignado a**: Dylan Mathyus Hospital Herrera

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Visualización y actualización de datos personales (Priority: P1)

El inversionista autenticado accede a su perfil y puede ver sus datos actuales
(nombre completo, correo electrónico, número de documento, teléfono de contacto).
Puede modificar los campos editables y guardar los cambios.

**Why this priority**: La capacidad de ver y corregir datos personales es la función
más básica del perfil y base de las demás funcionalidades de la sección.

**Independent Test**: Un inversionista con sesión activa navega a su perfil, modifica
su nombre o teléfono, guarda los cambios y al recargar la página los datos actualizados
persisten correctamente.

**Acceptance Scenarios**:

1. **Given** un inversionista autenticado en la plataforma,
   **When** accede a la sección "Mi Perfil",
   **Then** visualiza sus datos personales actuales: nombre completo, correo electrónico,
   número de documento y teléfono de contacto.

2. **Given** un inversionista en la sección de perfil,
   **When** modifica su nombre completo y/o teléfono de contacto y confirma los cambios,
   **Then** el sistema guarda la información actualizada, muestra un mensaje de
   confirmación y los datos nuevos quedan reflejados en la vista.

3. **Given** un inversionista que intenta actualizar su perfil con un campo obligatorio
   vacío o con formato inválido (ej. teléfono con letras),
   **When** envía el formulario,
   **Then** el sistema muestra un mensaje de error específico por campo y no guarda
   ningún cambio.

---

### User Story 2 — Cambio de contraseña (Priority: P2)

El inversionista puede actualizar su contraseña desde el perfil, proporcionando
la contraseña actual para confirmar su identidad antes de establecer una nueva.

**Why this priority**: El cambio de contraseña es una función de seguridad esencial,
pero depende del perfil ya cargado (P1).

**Independent Test**: Un inversionista autenticado cambia su contraseña ingresando
la actual y una nueva válida; al cerrar sesión e intentar iniciar sesión con la
contraseña anterior falla, y con la nueva contraseña funciona.

**Acceptance Scenarios**:

1. **Given** un inversionista autenticado en la sección de cambio de contraseña,
   **When** ingresa su contraseña actual correcta, una nueva contraseña válida y
   la confirmación coincidente, y confirma el cambio,
   **Then** el sistema actualiza la contraseña, muestra confirmación de éxito y
   mantiene la sesión activa.

2. **Given** un inversionista que intenta cambiar su contraseña,
   **When** ingresa una contraseña actual incorrecta,
   **Then** el sistema rechaza el cambio y muestra el error
   "La contraseña actual no es correcta", sin modificar ningún dato.

3. **Given** un inversionista que intenta establecer una nueva contraseña,
   **When** la nueva contraseña no cumple los requisitos mínimos de seguridad
   (mínimo 8 caracteres, al menos una mayúscula, una minúscula y un número),
   **Then** el sistema muestra los requisitos incumplidos y no guarda el cambio.

4. **Given** una nueva contraseña y su confirmación que no coinciden,
   **When** el inversionista intenta confirmar el cambio,
   **Then** el sistema indica que las contraseñas no coinciden sin procesar el cambio.

---

### User Story 3 — Gestión de preferencias de la plataforma (Priority: P3)

El inversionista puede configurar sus preferencias personales de uso de la
plataforma: idioma de la interfaz y canal preferido para recibir notificaciones.

**Why this priority**: Las preferencias mejoran la experiencia de usuario pero no
bloquean ninguna funcionalidad crítica de trading; son un complemento al perfil.

**Independent Test**: Un inversionista cambia su preferencia de canal de notificaciones
a "correo electrónico", guarda los cambios, y las notificaciones posteriores de la
plataforma se envían por ese canal.

**Acceptance Scenarios**:

1. **Given** un inversionista en la sección de preferencias de su perfil,
   **When** selecciona su canal preferido de notificaciones (correo electrónico
   o notificación en plataforma) y guarda,
   **Then** el sistema almacena la preferencia y las notificaciones futuras
   respetan ese canal.

2. **Given** un inversionista que cambia el idioma de la interfaz,
   **When** selecciona un idioma disponible y confirma,
   **Then** la interfaz completa de la plataforma se muestra en el idioma elegido
   desde ese momento.

---

### Edge Cases

- El correo electrónico NO es editable desde el perfil (campo de solo lectura);
  requiere un proceso de cambio de correo separado con re-verificación.
- El número de documento NO es editable por el usuario; cualquier corrección debe
  gestionarse a través de soporte.
- Si el inversionista no tiene teléfono registrado, el campo aparece vacío y es
  opcional completarlo.
- Un inversionista no puede ver ni editar el perfil de otro inversionista.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE mostrar al inversionista autenticado su información
  de perfil actual: nombre completo, correo electrónico (solo lectura), número de
  documento (solo lectura) y teléfono de contacto.
- **FR-002**: El inversionista DEBE poder editar su nombre completo y teléfono de
  contacto; el correo y el número de documento son campos de solo lectura en esta vista.
- **FR-003**: El sistema DEBE validar que los campos editados cumplan el formato
  esperado antes de guardar (nombre no vacío; teléfono numérico si se provee).
- **FR-004**: El sistema DEBE confirmar al inversionista mediante un mensaje visible
  cuando sus datos han sido guardados exitosamente.
- **FR-005**: El inversionista DEBE poder cambiar su contraseña proporcionando primero
  su contraseña actual; el sistema DEBE verificarla antes de aceptar la nueva.
- **FR-006**: La nueva contraseña DEBE cumplir los mismos requisitos de seguridad
  definidos en el registro: mínimo 8 caracteres, al menos una mayúscula, una minúscula
  y un número.
- **FR-007**: El sistema DEBE rechazar el cambio de contraseña si la contraseña actual
  es incorrecta, mostrando un mensaje de error sin revelar información adicional.
- **FR-008**: El inversionista DEBE poder seleccionar su canal preferido de
  notificaciones entre las opciones disponibles en la plataforma.
- **FR-009**: El sistema DEBE persistir las preferencias del inversionista y aplicarlas
  en las interacciones futuras (notificaciones, idioma de interfaz).
- **FR-010**: Un inversionista autenticado DEBE poder acceder únicamente a su propio
  perfil; el sistema DEBE impedir el acceso al perfil de otros usuarios.
- **FR-011**: Todos los cambios de perfil DEBE registrarlos el sistema con fecha,
  hora y campo modificado para fines de auditoría interna.

### Key Entities

- **Perfil de Inversionista**: Información personal del inversionista. Atributos
  editables: nombre completo, teléfono de contacto. Atributos de solo lectura:
  correo electrónico, número de documento. Relación: uno a uno con la cuenta de
  inversionista.
- **Preferencias de Usuario**: Configuración personal de la plataforma. Atributos:
  canal preferido de notificaciones, idioma de interfaz. Relación: uno a uno con
  el inversionista.
- **Registro de Cambios de Perfil**: Historial de modificaciones. Atributos: campo
  modificado, valor anterior (enmascarado si sensible), fecha y hora del cambio.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Un inversionista puede ver y actualizar sus datos personales en menos
  de 2 minutos desde que accede a la sección de perfil.
- **SC-002**: El 100% de los intentos de edición con datos inválidos son rechazados
  con mensaje de error específico antes de guardar.
- **SC-003**: El cambio de contraseña toma efecto inmediatamente; intentos de
  autenticación con la contraseña anterior fallan en el 100% de los casos tras
  el cambio exitoso.
- **SC-004**: El 100% de los cambios de perfil quedan registrados con su marca
  temporal para auditoría.
- **SC-005**: Las preferencias de notificación seleccionadas son respetadas en el
  100% de las notificaciones generadas después de guardar la preferencia.

## Assumptions

- Solo los campos nombre completo y teléfono son editables por el usuario; el
  correo y el número de documento requieren procesos administrativos para cambiar.
- El idioma disponible por defecto es español; otros idiomas son opcionales y
  dependen de la disponibilidad en el sprint.
- El historial de cambios de perfil es para auditoría interna únicamente; el
  inversionista no visualiza este historial desde su perfil.
- El inversionista debe estar autenticado (sesión activa) para acceder a cualquier
  función de gestión de perfil; no existe vista pública del perfil.
- Los canales de notificación disponibles en esta versión son: correo electrónico
  y notificación dentro de la plataforma.

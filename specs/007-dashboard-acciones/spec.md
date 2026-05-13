# Feature Specification: Dashboard de Comportamiento de Acciones de Interés

**Feature Branch**: `007-dashboard-acciones`
**Jira**: AB-28
**Created**: 2026-05-10
**Status**: Draft
**Asignado a**: Juan Diego González Villarreal

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Ver comportamiento de acciones de interés (Priority: P1)

El inversionista accede a un dashboard que muestra el comportamiento actual del
mercado para las acciones que le interesan: precio, variación del día, volumen
negociado y tendencia reciente.

**Why this priority**: El dashboard de mercado es el punto de entrada para decisiones
de inversión. Sin visibilidad del mercado, el inversionista no puede actuar.

**Independent Test**: Un inversionista accede al dashboard; el sistema muestra una
lista de acciones con precio actual, variación del día y volumen; los datos se
actualizan periódicamente sin recargar la página.

**Acceptance Scenarios**:

1. **Given** un inversionista autenticado que accede al dashboard de mercado,
   **When** carga la sección,
   **Then** el sistema muestra las acciones disponibles en la plataforma con:
   símbolo, nombre de empresa, precio actual, variación del día (monto y
   porcentaje) y volumen negociado en la sesión.

2. **Given** un inversionista en el dashboard durante el horario bursátil,
   **When** los precios de mercado cambian,
   **Then** los valores del dashboard se actualizan automáticamente sin que el
   inversionista deba recargar la página.

3. **Given** un inversionista que selecciona una acción del dashboard,
   **When** hace clic o tap en ella,
   **Then** el sistema muestra el detalle ampliado de esa acción: histórico de
   precio del día (gráfico de velas o línea), máximo y mínimo del día, precio
   de apertura y precio de cierre anterior.

---

### User Story 2 — Buscar y filtrar acciones en el dashboard (Priority: P2)

El inversionista puede buscar acciones por nombre o símbolo y filtrar por sector
o variación para encontrar rápidamente las que le interesan.

**Why this priority**: Con múltiples acciones disponibles, la búsqueda y filtrado
son esenciales para la usabilidad del dashboard.

**Independent Test**: Un inversionista busca "ECO" y el dashboard filtra mostrando
únicamente las acciones cuyo símbolo o nombre contiene ese texto.

**Acceptance Scenarios**:

1. **Given** un inversionista en el dashboard con múltiples acciones,
   **When** escribe un término de búsqueda (símbolo o nombre parcial),
   **Then** el dashboard filtra en tiempo real mostrando solo las acciones que
   coinciden con el término, sin recargar la página.

2. **Given** un inversionista en el dashboard,
   **When** aplica un filtro de variación (ej. "mayores ganancias del día" o
   "mayores pérdidas del día"),
   **Then** el dashboard reordena las acciones de mayor a menor variación positiva
   o negativa respectivamente.

---

### Edge Cases

- Fuera del horario bursátil, el dashboard muestra los precios del último cierre
  con una indicación visible de que el mercado está cerrado.
- Si no hay datos de mercado disponibles (fallo en fuente de datos), el sistema
  muestra el último precio conocido con una advertencia de datos desactualizados.
- El inversionista puede acceder al dashboard sin tener posiciones abiertas
  (es informativo, no requiere tenencias previas).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE mostrar en el dashboard todas las acciones disponibles
  en la plataforma con su precio actual, variación del día y volumen negociado.
- **FR-002**: El sistema DEBE actualizar los precios del dashboard automáticamente
  durante el horario bursátil con una frecuencia máxima de actualización de 60 segundos.
- **FR-003**: El inversionista DEBE poder seleccionar una acción para ver su detalle:
  gráfico de precio del día, máximo, mínimo, precio de apertura y cierre anterior.
- **FR-004**: El sistema DEBE permitir buscar acciones por símbolo o nombre parcial
  con resultados filtrados en tiempo real.
- **FR-005**: El sistema DEBE permitir ordenar las acciones por variación del día
  (ascendente y descendente).
- **FR-006**: El sistema DEBE indicar visualmente cuando el mercado está cerrado
  y mostrar los precios del último cierre.
- **FR-007**: El sistema DEBE diferenciar visualmente las acciones con variación
  positiva de las que tienen variación negativa en el día.

### Key Entities

- **Acción de Mercado**: Instrumento financiero listado. Atributos: símbolo, nombre
  de empresa, precio actual, precio de cierre anterior, variación del día (monto
  y porcentaje), volumen negociado, precio máximo del día, precio mínimo del día,
  precio de apertura.
- **Gráfico de Precio**: Histórico intradiario de una acción. Atributos: serie
  temporal de precios con intervalos regulares durante la sesión bursátil.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El dashboard carga completamente en menos de 3 segundos en condiciones
  normales de red.
- **SC-002**: Los precios del dashboard se actualizan con un desfase máximo de
  60 segundos respecto al precio de mercado durante el horario bursátil.
- **SC-003**: La búsqueda de acciones por símbolo o nombre filtra los resultados
  en menos de 500 milisegundos desde que el inversionista deja de escribir.
- **SC-004**: El detalle de una acción seleccionada carga en menos de 2 segundos.

## Assumptions

- Los datos de precios de mercado son provistos por el módulo de datos de mercado
  ya integrado en la plataforma.
- El dashboard muestra únicamente acciones listadas y activas en la plataforma;
  no incluye índices ni otros instrumentos en esta versión.
- El gráfico de precio del día muestra datos en intervalos de 5 minutos como mínimo.
- La funcionalidad de "acciones de interés" personalizada (watchlist) se gestiona
  en AB-36; este dashboard muestra todas las acciones disponibles por defecto.

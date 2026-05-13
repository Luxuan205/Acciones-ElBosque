# Feature Specification: Reporte de Ganancias y Pérdidas

**Feature Branch**: `022-reporte-ganancias`  
**Created**: 2026-05-12  
**Status**: Draft  
**Jira**: AB-37  

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consulta de rentabilidad por posición (Priority: P1)

El inversionista puede ver para cada acción en su portafolio: el precio promedio de adquisición, el precio actual de mercado, la ganancia o pérdida no realizada (en valor absoluto y porcentaje), y la ganancia o pérdida realizada de posiciones ya cerradas.

**Why this priority**: La rentabilidad por posición es la información financiera más relevante para el inversionista. Sin ella, no puede evaluar el desempeño de sus inversiones.

**Independent Test**: Con un portafolio con posiciones abiertas y cerradas, verificar que el reporte muestra correctamente el precio promedio de adquisición, precio actual, ganancia/pérdida no realizada y realizada para cada acción.

**Acceptance Scenarios**:

1. **Given** un inversionista con posiciones abiertas en su portafolio, **When** consulta el reporte de rentabilidad, **Then** ve para cada acción el precio promedio de compra, precio actual, variación absoluta y porcentual (ganancia o pérdida no realizada).
2. **Given** un inversionista que vendió acciones en el pasado, **When** consulta el reporte, **Then** ve la ganancia o pérdida realizada de cada transacción histórica, incluyendo comisiones descontadas.
3. **Given** un inversionista con múltiples compras de la misma acción a diferentes precios, **When** consulta el reporte, **Then** ve el precio promedio ponderado de adquisición calculado correctamente.

---

### User Story 2 - Reporte de rentabilidad por período (Priority: P2)

El inversionista puede filtrar el reporte de ganancias y pérdidas por período (último mes, último trimestre, último año, rango personalizado) para evaluar el desempeño de su portafolio en un intervalo específico.

**Why this priority**: El análisis por período es esencial para evaluación fiscal y para comparar el desempeño del portafolio con benchmarks del mercado.

**Independent Test**: Aplicar el filtro de "último mes" y verificar que solo aparecen transacciones dentro del período y los cálculos de rentabilidad son correctos para ese intervalo.

**Acceptance Scenarios**:

1. **Given** un inversionista que selecciona un período de tiempo, **When** genera el reporte, **Then** ve únicamente las transacciones y posiciones con actividad dentro del período seleccionado.
2. **Given** un inversionista que selecciona un rango personalizado de fechas, **When** genera el reporte, **Then** el cálculo de ganancias y pérdidas usa como referencia las posiciones al inicio del período.

---

### User Story 3 - Exportación del reporte (Priority: P3)

El inversionista puede exportar el reporte de ganancias y pérdidas en un formato descargable para uso fiscal o personal, incluyendo el detalle de todas las transacciones del período seleccionado.

**Why this priority**: La exportación es útil para declaración de renta y control personal, pero no es crítica para el MVP.

**Independent Test**: Generar y exportar el reporte de un período. Verificar que el archivo descargado contiene todos los datos del reporte visible en pantalla.

**Acceptance Scenarios**:

1. **Given** un inversionista que visualiza su reporte de ganancias, **When** selecciona exportar, **Then** el sistema genera un archivo descargable con el detalle de transacciones, precios promedio, ganancias realizadas y no realizadas.

---

### Edge Cases

- ¿Qué pasa si el inversionista no tiene posiciones ni transacciones en el período seleccionado? El reporte muestra un resumen vacío con un mensaje informativo.
- ¿Qué pasa con las comisiones en el cálculo de ganancia realizada? Las comisiones se descuentan del monto bruto de la venta para calcular la ganancia neta realizada.
- ¿Qué pasa si el precio de mercado de una acción no está disponible temporalmente? Se muestra el último precio conocido con una indicación de que puede no ser el precio más actualizado.
- ¿Qué pasa con las acciones adquiridas antes de que el sistema entrara en funcionamiento (datos históricos)? Solo se calculan ganancias para posiciones con precio de adquisición registrado en el sistema.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE calcular y mostrar para cada posición abierta: precio promedio ponderado de adquisición, precio actual de mercado, ganancia o pérdida no realizada en valor absoluto y porcentaje.
- **FR-002**: El sistema DEBE calcular y mostrar para posiciones cerradas: ganancia o pérdida realizada neta (descontando comisiones) por transacción.
- **FR-003**: El sistema DEBE calcular el precio promedio ponderado de adquisición cuando el inversionista realizó múltiples compras de la misma acción a precios distintos.
- **FR-004**: El sistema DEBE permitir filtrar el reporte por período predefinido (último mes, trimestre, año) o rango de fechas personalizado.
- **FR-005**: El sistema DEBE mostrar un resumen agregado del portafolio: valor total invertido, valor de mercado actual, ganancia o pérdida total no realizada y total realizada en el período.
- **FR-006**: El sistema DEBE permitir exportar el reporte de ganancias y pérdidas en un formato descargable.
- **FR-007**: El precio de mercado usado en el reporte DEBE ser el más reciente disponible del módulo de market data (AB-28).

### Key Entities

- **Ganancia no realizada**: Diferencia entre el valor actual de mercado de una posición y su costo promedio de adquisición, para posiciones aún abiertas.
- **Ganancia realizada**: Diferencia entre el precio de venta y el costo promedio de adquisición de títulos vendidos, neta de comisiones.
- **Precio promedio ponderado**: Precio de adquisición calculado como el promedio de múltiples compras ponderado por la cantidad de títulos en cada compra.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El reporte de rentabilidad se genera y muestra en menos de 5 segundos para portafolios con hasta 50 posiciones.
- **SC-002**: El cálculo del precio promedio ponderado es exacto al 100% para cualquier combinación de compras múltiples.
- **SC-003**: El reporte incluye el 100% de las transacciones registradas en el sistema dentro del período seleccionado.
- **SC-004**: La exportación del reporte se completa en menos de 30 segundos independientemente del número de transacciones.

## Assumptions

- El precio de cotización en tiempo real de las acciones lo provee el módulo de market data (AB-28).
- El historial de transacciones (compras y ventas) se obtiene del módulo de saldo y movimiento de fondos (AB-26).
- El cálculo de comisiones usa las reglas del módulo de desglose de comisiones (AB-25).
- Solo se calculan ganancias y pérdidas para transacciones registradas dentro del sistema; el sistema no importa historial externo.
- La moneda base de todos los cálculos es COP (Peso Colombiano).
- Los impuestos sobre ganancias de capital son responsabilidad del inversionista; el sistema provee los datos pero no calcula la obligación tributaria.

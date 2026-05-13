# Research: AB-29 — Gestión de Horarios y Configuración de Mercados

## Decision 1: workingDays como bitmask entero (lunes=1, martes=2, ..., viernes=16)
- **Decision**: `MarketSchedule.workingDays` es `INTEGER` en DB. Cada bit representa un día de la semana (bit 0 = lunes, bit 4 = viernes). El valor inicial `31` (binary: `11111`) = lunes a viernes. La entidad Java usa `Set<DayOfWeek>` con conversión via `@Converter`.
- **Rationale**: Representación compacta y fácil de serializar. Un `@AttributeConverter<Set<DayOfWeek>, Integer>` convierte entre Java y DB. Es el enfoque estándar para flags de días.
- **Alternatives considered**: Columnas booleanas separadas (mon, tue, ...) — verbosas y rígidas; VARCHAR con días separados por coma — dificulta consultas SQL; `@ElementCollection` — tabla join adicional innecesaria.

## Decision 2: Zona horaria UTC-5 manejada en Java, no en DB
- **Decision**: Las horas `openTime` y `closeTime` en DB son `TIME WITHOUT TIME ZONE`. La conversión a UTC-5 se hace en `MarketScheduleService` usando `ZoneId.of("America/Bogota")`.
- **Rationale**: Colombia está en UTC-5 sin DST. La spec establece UTC-5 explícitamente. Guardar times locales en DB y convertir en servicio es el patrón estándar para una única zona horaria fija.
- **Alternatives considered**: Guardar como UTC — introduce conversión en cada comparación; `TIME WITH TIME ZONE` — PostgreSQL no soporta zone info real en TIME; hard-code en application.yaml — menos flexible.

## Decision 3: @Scheduled cada minuto para evaluar estado del mercado
- **Decision**: `MarketStatusService` evalúa `isMarketOpen()` como método síncrono consultando `MarketSchedule` + `MarketHoliday`. Un `@Scheduled(fixedRate = 60000)` actualiza un campo `AtomicBoolean marketOpen` en memoria cada minuto.
- **Rationale**: Otros módulos (order) necesitan respuesta inmediata sin esperar un refresh. El `AtomicBoolean` en memoria garantiza acceso thread-safe sin consulta a DB en cada llamada.
- **Alternatives considered**: Consulta a DB en cada `isMarketOpen()` — latencia innecesaria; evento de cambio de estado — sin message broker no aplica en modular monolito.

## Decision 4: Un único registro de MarketSchedule
- **Decision**: La tabla `market_schedule` tendrá exactamente un registro (el horario del único mercado). Flyway V1 hace el INSERT inicial. `MarketScheduleRepository.findFirst()` obtiene el registro; `PUT /config/market/schedule` hace un update.
- **Rationale**: El spec explícitamente dice "un único mercado" para el proyecto académico. No hay necesidad de múltiples horarios o identificadores de mercado.
- **Alternatives considered**: Tabla con `market_id` — sobrediseño; múltiples registros con vigencia temporal — sobrediseño sin requerimiento.

## Decision 5: MarketHoliday con validación de duplicados por fecha
- **Decision**: La tabla `market_holiday` tiene constraint UNIQUE en `date`. `POST /config/market/holidays` devuelve HTTP 409 si ya existe un festivo en esa fecha.
- **Rationale**: Dos festivos en la misma fecha son lógicamente duplicados. La constraint en DB garantiza integridad incluso en condiciones de carrera.
- **Alternatives considered**: Sin constraint de unicidad — permite duplicados que generan días cerrados duplicados en el cálculo; verificación solo en servicio — no thread-safe.

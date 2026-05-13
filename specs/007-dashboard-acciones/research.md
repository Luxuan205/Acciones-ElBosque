# Research: AB-28 — Dashboard de Comportamiento de Acciones

## Decision 1: MarketDataIngestor simulado en dev con @Scheduled
- **Decision**: `MarketDataIngestor` usa `@Scheduled(fixedRate = 60000)` (60s). En dev, genera precios con variación aleatoria ±2% sobre el último precio conocido. El seed de datos iniciales lo provee Flyway (V1) con ~10 acciones representativas del mercado colombiano (PFBCOLOM, NUTRESA, ISA, etc.).
- **Rationale**: El spec reconoce que es un proyecto académico con "fuente de datos simulada en dev". La variación aleatoria pequeña simula el comportamiento real sin integración de API externa.
- **Alternatives considered**: API externa (Alpha Vantage, Yahoo Finance) — requiere API key y manejo de rate limits; datos estáticos sin variación — no demuestran el comportamiento del dashboard.

## Decision 2: StockSnapshot como "estado actual" y purga de IntradayPricePoint
- **Decision**: `StockSnapshot` siempre tiene una fila por símbolo (upsert). `IntradayPricePoint` acumula puntos de 5 min durante la sesión y se purga al cierre del mercado (`@Scheduled` al cierre).
- **Rationale**: El spec especifica "datos intradiarios en intervalos de 5 min" y menciona explícitamente que se purgan al cierre para evitar acumulación ilimitada. Una sesión de 6.5h genera 78 puntos por símbolo, manejable.
- **Alternatives considered**: Conservar histórico intradiario de múltiples días — fuera del alcance; table partitioning — sobrediseño para proyecto académico.

## Decision 3: Búsqueda por símbolo/nombre con ILIKE
- **Decision**: `StockSnapshotRepository.findBySymbolContainingIgnoreCaseOrNameContainingIgnoreCase(search, search)` con Spring Data JPA. Devuelve lista sin paginación (la spec no menciona paginación para el dashboard).
- **Rationale**: La spec menciona búsqueda < 500ms (SC-003). Con un catálogo de ~50 acciones (proyecto académico), un índice B-tree en `symbol` y `name` es suficiente sin full-text search.
- **Alternatives considered**: Full-text search (PostgreSQL tsvector) — sobrediseño para catálogo pequeño; query nativa con ILIKE — menos mantenible que el método derivado de Spring Data.

## Decision 4: Indicador "mercado cerrado" en el response
- **Decision**: `StockDetailDto` incluye campo `marketOpen: boolean` obtenido de `MarketStatusService.isMarketOpen()` in-process. Cuando es `false`, el precio mostrado tiene tag `stale: true`.
- **Rationale**: El spec requiere que "fuera de horario muestra último precio conocido con indicador". El campo `stale` permite al frontend mostrar el indicador visual apropiado.
- **Alternatives considered**: Campo `lastUpdated` sin flag — requiere que el frontend calcule si está vencido; sin indicador — viola el requisito de UX del spec.

## Decision 5: Filtrado por variación del día
- **Decision**: `GET /market/stocks?sort=dayChangePct_asc|dayChangePct_desc|name_asc` con Spring Data JPA `Sort`. El parámetro `sort` se mapea a `Sort.by(direction, property)` validado en controller.
- **Rationale**: El spec menciona "filtrar por variación". El ordenamiento en DB es más eficiente que en memoria para listas grandes. La validación del parámetro `sort` previene injection.
- **Alternatives considered**: Filtrado en memoria — suficiente para pocos símbolos pero no escalable; aceptar cualquier campo para sort sin validar — riesgo de injection o error 500.

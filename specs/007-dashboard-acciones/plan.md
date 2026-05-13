# Implementation Plan: AB-28 — Dashboard de Comportamiento de Acciones

**Branch**: `007-dashboard-acciones` | **Date**: 2026-05-10 | **Spec**: [spec.md](./spec.md)
**Input**: `specs/007-dashboard-acciones/spec.md`

## Summary

El inversionista ve un dashboard con todas las acciones disponibles: precio actual,
variación del día y volumen. Puede buscar por símbolo/nombre y filtrar por variación.
Al seleccionar una acción, ve el detalle con gráfico intradiario. Todo en
`market-data`. Los precios se actualizan periódicamente desde fuente externa.

## Technical Context

**Language/Version**: Java 17
**Primary Dependencies**: Spring Boot 4.0.6, Spring Data JPA, Lombok, Bean Validation, Spring Scheduling
**Storage**: PostgreSQL — schema `market_db` (Flyway); tabla de snapshots de precio
**Testing**: JUnit 5, @WebMvcTest, @DataJpaTest, Mockito
**Target Platform**: REST API backend
**Project Type**: Módulo `market-data`
**Performance Goals**: Dashboard carga < 3s (SC-001); precios actualizados < 60s (SC-002); búsqueda < 500ms (SC-003)
**Constraints**: Fuera de horario muestra último precio conocido con indicador; datos intradiarios en intervalos de 5 min
**Scale/Scope**: Proyecto académico (fuente de datos simulada en dev)

## Constitution Check

| Principio | Requisito | Estado |
|-----------|-----------|--------|
| I. Module Cohesion | Datos de mercado en `market-data`; expone interfaz pública para otros módulos | ✅ PASS |
| II. API Contract-First | `contracts/market-api.md` define GET /market/stocks y /stocks/{symbol} | ✅ PASS |
| III. Test-Before-Ship | Tests de búsqueda, filtrado por variación, estado mercado cerrado | ✅ PASS |
| IV. Security & Compliance | JWT requerido; endpoint de solo lectura | ✅ PASS |
| V. Conventional Workflow | Rama `007-dashboard-acciones` | ✅ PASS |

**GATE PASSED — sin violaciones.**

## Project Structure

### Source Code (market-data)

```text
backend/market-data/src/main/java/com/accioneselbosque/market_data_service/
├── controller/
│   └── MarketController.java           ← GET /market/stocks?search=&sort=
│                                          GET /market/stocks/{symbol}
│                                          GET /market/stocks/{symbol}/intraday
├── service/
│   ├── StockSnapshotService.java       ← listar, buscar, filtrar acciones
│   └── MarketDataIngestor.java         ← @Scheduled — actualiza precios cada 60s
├── model/
│   ├── StockSnapshot.java              ← symbol, name, currentPrice, previousClose,
│   │                                      dayChange, dayChangePct, volume, updatedAt
│   └── IntradayPricePoint.java         ← symbol, timestamp, price, volume
├── repository/
│   ├── StockSnapshotRepository.java    ← findBySymbolContainingOrNameContaining(…)
│   └── IntradayPricePointRepository.java ← findBySymbolAndTimestampBetween(…)
└── dto/
    ├── StockSummaryDto.java
    ├── StockDetailDto.java
    └── IntradayDataDto.java
```

```text
db/migration/
├── V1__create_stock_snapshot_table.sql
└── V2__create_intraday_price_point_table.sql
```

**Structure Decision**: `StockSnapshot` almacena el estado actual de cada acción
(actualizado por `MarketDataIngestor` cada 60s). `IntradayPricePoint` guarda el
histórico intradiario (intervalos de 5 min); se purga al cierre de cada sesión para
evitar acumulación ilimitada.

# Implementation Plan: AB-36 — Watchlist de Acciones (Funcionalidad Premium)

**Branch**: `011-watchlist-acciones` | **Date**: 2026-05-10 | **Spec**: [spec.md](./spec.md)
**Input**: `specs/011-watchlist-acciones/spec.md`

## Summary

El inversionista premium gestiona una watchlist personalizada de acciones (máx 50).
El módulo `market-data` persiste la watchlist y enriquece cada entrada con
el precio actual. El acceso se bloquea para suscriptores no-premium; la watchlist
se conserva al expirar la suscripción.

## Technical Context

**Language/Version**: Java 17
**Primary Dependencies**: Spring Boot 4.0.6, Spring Data JPA, Lombok, Bean Validation
**Storage**: PostgreSQL — schema `market_db` (Flyway); tablas watchlist y watchlist_entry
**Testing**: JUnit 5, @WebMvcTest, @DataJpaTest, Mockito
**Target Platform**: REST API backend
**Project Type**: Módulo `market-data`
**Performance Goals**: Agregar/eliminar acción < 5s (SC-002); precios actualizados < 60s (SC-003)
**Constraints**: Máx 50 acciones; solo premium activo; watchlist preservada al expirar suscripción
**Scale/Scope**: Proyecto académico

## Constitution Check

| Principio | Requisito | Estado |
|-----------|-----------|--------|
| I. Module Cohesion | Watchlist en `market-data` junto a los datos de mercado; estado suscripción consultado a `auth-security-service` in-process | ✅ PASS |
| II. API Contract-First | `contracts/watchlist-api.md` define GET/POST/DELETE endpoints | ✅ PASS |
| III. Test-Before-Ship | Tests: premium agrega/elimina; no-premium bloqueado (403); límite 50 entradas | ✅ PASS |
| IV. Security & Compliance | JWT requerido; gate premium verificado en capa de servicio | ✅ PASS |
| V. Conventional Workflow | Rama `011-watchlist-acciones` | ✅ PASS |

**GATE PASSED — sin violaciones.**

## Project Structure

### Source Code (market-data)

```text
backend/market-data/src/main/java/com/accioneselbosque/market_data_service/
├── controller/
│   └── WatchlistController.java        ← GET /watchlist
│                                          POST /watchlist/entries
│                                          DELETE /watchlist/entries/{symbol}
├── service/
│   ├── WatchlistService.java           ← CRUD watchlist + verificación premium + enriquecimiento precios
│   └── PremiumSubscriptionGate.java    ← isPremiumActive(investorId) → auth-security-service
├── model/
│   ├── Watchlist.java                  ← investorId, createdAt
│   └── WatchlistEntry.java             ← watchlistId, symbol, addedAt, priceAtAdded
├── repository/
│   ├── WatchlistRepository.java        ← findByInvestorId(id)
│   └── WatchlistEntryRepository.java   ← countByWatchlistId(id) para validar límite 50
└── dto/
    ├── WatchlistEntryRequest.java      ← symbol
    └── WatchlistResponse.java          ← entries: [{symbol, name, currentPrice, dayChange, lastUpdated}]
```

```text
db/migration/
├── V3__create_watchlist_table.sql
└── V4__create_watchlist_entry_table.sql
```

**Structure Decision**: `Watchlist` es 1-to-1 con `investorId` (creada on-demand).
`WatchlistEntry` tiene unicidad (watchlist_id, symbol). El gate premium se verifica
en `WatchlistService` antes de cualquier operación. La watchlist NO se elimina al
expirar la suscripción — solo se bloquea el acceso.

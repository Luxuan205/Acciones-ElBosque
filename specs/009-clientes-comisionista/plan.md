# Implementation Plan: AB-31 — Gestión de Clientes Asignados por el Comisionista

**Branch**: `009-clientes-comisionista` | **Date**: 2026-05-10 | **Spec**: [spec.md](./spec.md)
**Input**: `specs/009-clientes-comisionista/spec.md`

## Summary

El comisionista autenticado ve la lista de sus clientes asignados con resumen de
cuenta (nombre, estado, saldo, órdenes activas) y puede buscar/filtrar. El detalle
de un cliente muestra su portafolio y historial reciente. La asignación la gestiona
el administrador; este módulo solo la consume. Todo en `auth-security-service`.

## Technical Context

**Language/Version**: Java 17
**Primary Dependencies**: Spring Boot 4.0.6, Spring Data JPA, Lombok, Bean Validation
**Storage**: PostgreSQL — schema `auth_db` (Flyway); tabla de asignaciones broker-cliente
**Testing**: JUnit 5, @WebMvcTest, @DataJpaTest, Mockito
**Target Platform**: REST API backend
**Project Type**: Módulo `auth-security-service`
**Performance Goals**: Lista de clientes en < 3s para 100 clientes (SC-001); búsqueda < 1s (SC-003)
**Constraints**: Comisionista ve SOLO sus clientes asignados; 0% acceso a clientes de otros brokers
**Scale/Scope**: Proyecto académico

## Constitution Check

| Principio | Requisito | Estado |
|-----------|-----------|--------|
| I. Module Cohesion | Asignaciones y datos del cliente en `auth-security-service` | ✅ PASS |
| II. API Contract-First | `contracts/broker-clients-api.md` antes de implementar | ✅ PASS |
| III. Test-Before-Ship | Tests de aislamiento (broker A no ve clientes de broker B) | ✅ PASS |
| IV. Security & Compliance | JWT con rol BROKER requerido; aislamiento validado en capa de servicio | ✅ PASS |
| V. Conventional Workflow | Rama `009-clientes-comisionista` | ✅ PASS |

**GATE PASSED — sin violaciones.**

## Project Structure

### Source Code (auth-security-service)

```text
backend/auth/src/main/java/com/accioneselbosque/auth/
├── controller/
│   └── BrokerClientController.java     ← GET /brokers/me/clients?search=&status=
│                                          GET /brokers/me/clients/{investorId}
├── service/
│   └── BrokerClientService.java        ← validar asignación + construir resumen del cliente
├── model/
│   └── BrokerClientAssignment.java     ← brokerId, investorId, assignedAt, active
├── repository/
│   └── BrokerClientAssignmentRepository.java  ← findByBrokerIdAndActive(…)
└── dto/
    ├── ClientSummaryDto.java           ← name, status, availableBalance, activeOrdersCount
    └── ClientDetailDto.java            ← + portfolioSummary, recentOrders
```

```text
db/migration/
└── V6__create_broker_client_assignment_table.sql
```

**Structure Decision**: `BrokerClientAssignment` es una tabla de relación entre brokers
e inversores. `BrokerClientService` verifica que el `investorId` solicitado esté
asignado al broker autenticado antes de devolver datos — garantiza aislamiento en capa
de servicio además del JWT.

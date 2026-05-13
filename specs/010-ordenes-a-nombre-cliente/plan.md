# Implementation Plan: AB-32 — Generación y Firma de Órdenes a Nombre del Cliente

**Branch**: `010-ordenes-a-nombre-cliente` | **Date**: 2026-05-10 | **Spec**: [spec.md](./spec.md)
**Input**: `specs/010-ordenes-a-nombre-cliente/spec.md`

## Summary

El comisionista genera órdenes de compra/venta a nombre de sus clientes asignados.
La orden registra el `brokerId` como autor y el `investorId` como propietario.
El flujo es idéntico al de una orden directa (desglose de comisión → confirmación),
pero requiere que el broker esté asignado al cliente. Todo en `order`.

## Technical Context

**Language/Version**: Java 17
**Primary Dependencies**: Spring Boot 4.0.6, Spring Data JPA, Lombok, Bean Validation
**Storage**: PostgreSQL — schema `order_db`; tabla `order` con campos `broker_id` (nullable) y `investor_id`
**Testing**: JUnit 5, @WebMvcTest, @DataJpaTest, Mockito
**Target Platform**: REST API backend
**Project Type**: Módulo `order`
**Performance Goals**: Flujo de generación de orden en < 2 min (SC-002)
**Constraints**: 100% trazabilidad de autoría (SC-001); 0% órdenes sobre clientes no asignados (SC-003)
**Scale/Scope**: Proyecto académico

## Constitution Check

| Principio | Requisito | Estado |
|-----------|-----------|--------|
| I. Module Cohesion | Lógica de orden en `order`; verificación de asignación consultando `auth-security-service` in-process | ✅ PASS |
| II. API Contract-First | `contracts/broker-order-api.md` define POST /orders/broker y GET /orders/broker/history | ✅ PASS |
| III. Test-Before-Ship | Tests: broker crea orden a nombre de cliente asignado; broker intenta cliente no asignado (403); fondos insuficientes (400) | ✅ PASS |
| IV. Security & Compliance | JWT rol BROKER; `broker_id` persistido en cada orden (auditoría, SC-001) | ✅ PASS |
| V. Conventional Workflow | Rama `010-ordenes-a-nombre-cliente` | ✅ PASS |

**GATE PASSED — sin violaciones.**

## Project Structure

### Source Code (order)

```text
backend/order/src/main/java/com/accioneselbosque/order_service/
├── controller/
│   └── BrokerOrderController.java      ← POST /orders/broker
│                                          GET /orders/broker/history?clientId=&from=&to=
├── service/
│   ├── BrokerOrderService.java         ← verifica asignación → delega a OrderService
│   └── BrokerAssignmentValidator.java  ← isBrokerAssignedToClient(brokerId, clientId)
├── model/
│   └── Order.java                      ← + campo brokerId (nullable; null = orden directa)
└── dto/
    ├── BrokerOrderRequest.java         ← clientId, symbol, quantity, orderType
    └── BrokerOrderHistoryResponse.java ← orders con clientName y brokerName
```

**Structure Decision**: El campo `broker_id` en la tabla `order` es nullable.
Si `broker_id IS NOT NULL`, la orden fue generada por un comisionista.
Esto permite una sola tabla para todos los tipos de orden con trazabilidad completa.
`BrokerAssignmentValidator` consulta `auth-security-service` in-process para verificar
la asignación antes de procesar la orden.

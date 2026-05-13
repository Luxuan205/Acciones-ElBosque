# Research: AB-32 — Generación y Firma de Órdenes a Nombre del Cliente

## Decision 1: broker_id nullable en la tabla order (trazabilidad completa)
- **Decision**: `Order.brokerId` es nullable (NULL = orden directa del investor; NOT NULL = generada por un broker). La columna se agrega a la tabla `order` existente mediante migración Flyway.
- **Rationale**: Una única tabla con campo nullable es más simple que dos tablas (DirectOrder/BrokerOrder) y preserva trazabilidad: cualquier query sobre órdenes puede filtrar por `broker_id IS NOT NULL` para ver órdenes de comisionistas.
- **Alternatives considered**: Tabla separada `broker_order` — duplica el modelo de datos y complica queries de historial consolidado; campo booleano `isBrokerOrder` — menos descriptivo que el ID real del broker.

## Decision 2: Verificación de asignación in-process en BrokerAssignmentValidator
- **Decision**: `BrokerAssignmentValidator.isBrokerAssignedToClient(brokerId, clientId)` consulta el bean `BrokerClientAssignmentRepository` de `auth-security-service` in-process. Devuelve `boolean`; si `false`, `BrokerOrderService` lanza `AccessDeniedException` → HTTP 403.
- **Rationale**: Arquitectura modular monolito — el validator accede in-process al repositorio de auth-security-service. No hay necesidad de HTTP. La verificación es el gate principal de seguridad (SC-003: 0% órdenes sobre clientes no asignados).
- **Alternatives considered**: Llamada REST a auth-security-service — viola principio I; confiar en el JWT del broker sin verificar asignación — insuficiente para SC-003.

## Decision 3: BrokerOrderService delega a OrderService existente
- **Decision**: `BrokerOrderService.createOrder(brokerId, request)` 1) verifica asignación, 2) invoca `OrderService.createOrder()` pasando `investorId = request.clientId` y `brokerId`. El `OrderService` existente no cambia su lógica; recibe el campo `brokerId` como parámetro opcional.
- **Rationale**: Reutilizar `OrderService` evita duplicar la lógica de validación de fondos, desglose de comisiones y encolamiento. El broker simplemente "firma" la orden estableciendo el `brokerId`.
- **Alternatives considered**: Lógica duplicada en BrokerOrderService — viola DRY; método overloaded en OrderService — contamina la interfaz del servicio principal.

## Decision 4: Historial de órdenes del broker filtrable por cliente y rango de fechas
- **Decision**: `GET /orders/broker/history?clientId=&from=&to=` usa `OrderRepository.findByBrokerIdAndInvestorIdAndCreatedAtBetween()` con los parámetros opcionales. La paginación es la misma de 20 registros que el resto del sistema.
- **Rationale**: El broker necesita ver qué órdenes generó para qué clientes y en qué período. Paginación consistente con el módulo de movimientos de fondos.
- **Alternatives considered**: Sin filtro por cliente — devuelve demasiados resultados; sin rango de fechas — mismo problema; filtros en memoria — ineficiente.

## Decision 5: BrokerOrderHistoryResponse incluye clientName y brokerName
- **Decision**: El DTO `BrokerOrderHistoryResponse` incluye `clientName` (fullName del Investor) y `brokerName` (fullName del Broker) resueltos in-process desde auth-security-service, además de los campos de la orden.
- **Rationale**: El broker necesita ver el nombre del cliente sin hacer una segunda llamada. La resolución in-process es inmediata. `brokerName` es útil para reportes de auditoría.
- **Alternatives considered**: Solo IDs en el response — fuerza al frontend a resolver nombres; llamada HTTP a auth-security-service — viola principio I.

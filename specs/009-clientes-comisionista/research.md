# Research: AB-31 — Gestión de Clientes Asignados por el Comisionista

## Decision 1: Aislamiento en capa de servicio además del JWT
- **Decision**: `BrokerClientService` siempre verifica que `investorId` esté en la lista de clientes asignados al `brokerId` extraído del JWT antes de devolver datos. El método `assertBrokerOwnsClient(brokerId, investorId)` lanza `AccessDeniedException` → HTTP 403 si no está asignado.
- **Rationale**: La spec exige 0% de acceso a clientes de otros brokers. JWT garantiza que solo BROKER accede al endpoint, pero no garantiza que el `investorId` del path pertenezca a ese broker. La doble verificación cierra ese gap.
- **Alternatives considered**: Verificación solo en JWT (rol BROKER) — insuficiente; Spring Security `@PostAuthorize` con SpEL — funciona pero dificulta devolver mensajes descriptivos; filtro en query (`AND broker_id = ?`) — válido pero menos explícito.

## Decision 2: BrokerClientAssignment con campo active para desactivación suave
- **Decision**: `BrokerClientAssignment.active` (boolean) permite que el administrador desasigne clientes sin eliminar el historial. `findByBrokerIdAndActive(brokerId, true)` lista solo clientes activos.
- **Rationale**: La asignación la gestiona el admin (fuera del alcance de este módulo, que solo la consume). Mantener registros inactivos preserva la auditoría de quién estuvo asignado a quién.
- **Alternatives considered**: DELETE lógico con `deletedAt` timestamp — más estándar pero equivalente; eliminación física — pierde auditoría.

## Decision 3: Resumen de cuenta del cliente enriquecido in-process
- **Decision**: `BrokerClientService` construye `ClientSummaryDto` inyectando beans de `portfolio` (para `availableBalance`) y `order` (para `activeOrdersCount`) in-process. Si algún módulo devuelve null, se muestra 0.
- **Rationale**: Arquitectura modular monolito — el enriquecimiento in-process es el patrón correcto. El fallback a 0 evita que un error parcial rompa la lista completa de clientes.
- **Alternatives considered**: Llamadas HTTP entre módulos — viola principio I; duplicar datos de saldo en auth-security-service — viola single source of truth.

## Decision 4: Búsqueda y filtrado en base de datos, no en memoria
- **Decision**: `BrokerClientAssignmentRepository` usa `@Query` JPQL con parámetros opcionales: `WHERE bca.brokerId = :brokerId AND bca.active = true AND (:search IS NULL OR i.fullName ILIKE %:search%)` (join con Investor). Filtro por `status` se aplica igual.
- **Rationale**: Filtra en DB para no traer todos los clientes a memoria. Con 100 clientes (SC-001), una query con índice es < 1s garantizado.
- **Alternatives considered**: Traer todos a memoria y filtrar en Java — ineficiente con volumen; Specification API de Spring Data — válida pero más compleja que una query JPQL para este caso.

## Decision 5: Portafolio del cliente en ClientDetailDto
- **Decision**: `GET /brokers/me/clients/{investorId}` incluye `ClientDetailDto` con `portfolioSummary` (total de posiciones) y `recentOrders` (últimas 5 órdenes). Cargado in-process desde portfolio y order.
- **Rationale**: El broker necesita una vista rápida del cliente sin navegar a otra sección. Las últimas 5 órdenes son suficientes como "recientes" para un resumen.
- **Alternatives considered**: Endpoint separado para portafolio del cliente (como broker) — más REST-puro pero requiere más calls del frontend; número configurable de órdenes recientes — sobrediseño.

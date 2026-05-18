# Data Model: AB-39 — Dashboard Directivo

## Sin nuevas tablas

El dashboard agrega datos de tablas existentes en otros módulos. No requiere nuevas migraciones.

## Fuentes de datos por métrica

| Métrica | Tabla fuente | Cálculo |
|---------|-------------|---------|
| Estado del mercado | `market_schedule` + `market_holiday` (configuration) | Lógica existente MarketStatusService |
| Órdenes activas | `market_order` | COUNT WHERE status IN ('PENDING','QUEUED') |
| Volumen transacciones del día | `market_order` | SUM(total_estimated) WHERE status='EXECUTED' AND date=TODAY |
| Ingresos por comisiones | `market_order` | SUM(commission) WHERE status='EXECUTED' AND período |
| Usuarios conectados (última hora) | `mfa_session` | COUNT DISTINCT investor_id WHERE completed=TRUE AND created_at > NOW()-1H |
| Nuevos registros | `investor` | COUNT WHERE created_at en período |
| Suscripciones PREMIUM activas | `investor` | COUNT WHERE subscription_type='PREMIUM' AND subscription_expires_at > NOW() |

## Java DTOs

```java
record DashboardMetricsDto(
    boolean marketOpen,
    long activeOrders,         // PENDING + QUEUED
    BigDecimal todayVolume,
    long connectedUsers,
    LocalDateTime generatedAt
)

record DashboardSummaryDto(
    String period,             // 'TODAY' | 'THIS_WEEK' | 'THIS_MONTH'
    BigDecimal transactionVolume,
    BigDecimal commissionRevenue,
    long newRegistrations,
    long activePremiumSubscriptions
)
```

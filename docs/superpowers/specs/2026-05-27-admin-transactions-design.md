# Design: Vista de Transacciones para Administrador

**Date**: 2026-05-27
**Scope**: Módulo `app` (backend) + feature `admin` (frontend)

---

## Contexto

El panel de administración ya cuenta con `admin-dashboard`, `admin-users`, `admin-audit`, `admin-markets` y `admin-parameters`. Lo que falta es una vista que permita al administrador consultar **todas las transacciones ejecutadas** de la plataforma con filtros.

La sección admin usa el navbar general de la app (no shell propio). Las páginas admin existentes siguen el patrón standalone component con `adminGuard`.

---

## Alcance

### Incluido
- Endpoint `GET /admin/dashboard/transactions` con filtros opcionales
- Nuevo método en `DashboardService`
- Nuevo componente Angular `admin-transactions`
- Método en `AdminService`
- Ruta `/admin/transactions` protegida con `adminGuard`

### Excluido
- Modal de detalle por transacción
- Exportación CSV
- Shell/sidebar propio del admin (se mantiene navbar general)

---

## Backend

### Endpoint

```
GET /admin/dashboard/transactions
Authorization: Bearer <JWT con rol ADMIN>

Query params (todos opcionales):
  from        LocalDate  ISO-8601 (yyyy-MM-dd)
  to          LocalDate  ISO-8601 (yyyy-MM-dd)
  investorId  Long       ID numérico del usuario
  symbol      String     Símbolo bursátil (ej. ECOPETROL)
  type        String     BUY | SELL
  page        int        default 0
  size        int        default 20, máx 100
```

Protegido por `@PreAuthorize("hasRole('ADMIN')")` ya declarado a nivel de clase en `AdminDashboardController`.

### DTO de respuesta

```java
// AdminTransactionDto
record AdminTransactionDto(
    Long id,
    String investorName,
    String investorEmail,
    String symbol,
    String type,           // "BUY" | "SELL"
    int quantity,
    BigDecimal grossAmount,
    BigDecimal commission,
    LocalDateTime executedAt
) {}
```

Respuesta: `Page<AdminTransactionDto>` (Spring Data).

### Implementación en `DashboardService`

Nuevo método `getAdminTransactions(params...)` que:
1. Construye un `Specification<Transaction>` con los filtros presentes (filtra por `transactionType`, `symbol` case-insensitive, `investorId`, `executedAt` entre `from` y `to`)
2. Ejecuta `transactionRepository.findAll(spec, pageable)` ordenado por `executedAt DESC`
3. Obtiene los IDs de inversores únicos del resultado y los resuelve en un `Map<Long, Investor>` vía `InvestorRepository`
4. Mapea cada `Transaction` a `AdminTransactionDto` usando el mapa

### `TransactionRepository`

Agregar `JpaSpecificationExecutor<Transaction>` — actualmente solo extiende `JpaRepository<Transaction, Long>`.

---

## Frontend

### Componente `admin-transactions`

**Ruta**: `/admin/transactions`
**Archivo**: `features/admin/admin-transactions/admin-transactions.component.ts`

**Filtros:**
| Campo | Tipo | Descripción |
|---|---|---|
| Fecha desde | `<input type="date">` | Filtra `executedAt >= from` |
| Fecha hasta | `<input type="date">` | Filtra `executedAt <= to` |
| ID Usuario | `<input type="number">` | ID exacto del inversionista |
| Símbolo | `<input type="text">` | Texto libre, case-insensitive |
| Tipo | `<select>` | Todos / BUY / SELL |

**Tabla:**
| Columna | Campo |
|---|---|
| Fecha | `executedAt` formateado `es-CO` |
| Usuario | `investorName` + `investorEmail` en línea secundaria |
| Símbolo | `symbol` |
| Tipo | Badge verde (BUY) / rojo (SELL) |
| Cantidad | `quantity` formateado |
| Monto | `grossAmount` en COP |
| Comisión | `commission` en COP |

**Paginación**: mismo patrón que `admin-users` (computed `totalPages`, `pageNumbers`, `goToPage()`).

**Estado**: signals (`loading`, `loadError`, `transactions`, `totalElements`, `currentPage`).

### `AdminService` — método nuevo

```typescript
getAdminTransactions(filters: AdminTransactionFilterDto): Observable<Page<AdminTransactionDto>>
```

Apunta a `GET /admin/dashboard/transactions`.

### Modelos nuevos en `core/models`

```typescript
interface AdminTransactionDto {
  id: number;
  investorName: string;
  investorEmail: string;
  symbol: string;
  type: 'BUY' | 'SELL';
  quantity: number;          // int en backend
  grossAmount: number;
  commission: number;
  executedAt: string;        // ISO-8601 LocalDateTime
}

interface AdminTransactionFilterDto {
  from?: string;
  to?: string;
  investorId?: number;
  symbol?: string;
  type?: string;
  page?: number;
  size?: number;
}
```

### Ruta en `app.routes.ts`

```typescript
{
  path: 'admin/transactions',
  canActivate: [authGuard, adminGuard],
  loadComponent: () =>
    import('./features/admin/admin-transactions/admin-transactions.component')
      .then(m => m.AdminTransactionsComponent)
}
```

---

## Archivos modificados / creados

| Archivo | Acción |
|---|---|
| `backend/app/.../AdminDashboardController.java` | Agregar endpoint `GET /transactions` |
| `backend/app/.../DashboardService.java` | Agregar `getAdminTransactions()` |
| `backend/app/.../AdminTransactionDto.java` | Crear DTO |
| `backend/portfolio/.../TransactionRepository.java` | Agregar `JpaSpecificationExecutor<Transaction>` |
| `frontend/.../admin.service.ts` | Agregar `getAdminTransactions()` |
| `frontend/.../models/index.ts` | Agregar `AdminTransactionDto`, `AdminTransactionFilterDto` |
| `frontend/.../admin-transactions/*.ts/.html/.scss` | Crear componente |
| `frontend/.../app.routes.ts` | Agregar ruta `/admin/transactions` |

---

## Decisiones de diseño

- **Opción A elegida**: el endpoint vive en `AdminDashboardController` y la lógica en `DashboardService`, consistente con el patrón existente donde `DashboardService` ya accede directamente a `TransactionRepository`.
- **Sin modal de detalle**: tabla plana es suficiente para el caso de uso.
- **Sin exportación CSV**: fuera de alcance en esta iteración.
- **Navbar general**: sin shell admin propio.

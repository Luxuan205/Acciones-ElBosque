# Admin Transactions Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Agregar un endpoint `GET /admin/dashboard/transactions` y un componente Angular `admin-transactions` que permita al administrador consultar todas las transacciones ejecutadas con filtros de fecha, usuario, símbolo y tipo.

**Architecture:** El endpoint vive en `AdminDashboardController` (ya protegido con `@PreAuthorize("hasRole('ADMIN')")`). La lógica de query se agrega a `DashboardService` usando `JpaSpecificationExecutor` sobre `TransactionRepository`. El frontend sigue el patrón exacto de `admin-audit`.

**Tech Stack:** Java 17, Spring Boot 4.0.6, Spring Data JPA Specifications, Mockito, Angular 18 standalone components, RxJS signals.

---

## Mapa de archivos

| Archivo | Acción |
|---|---|
| `backend/portfolio/src/main/java/com/accioneselbosque/portfolio/repository/TransactionRepository.java` | Modificar — agregar `JpaSpecificationExecutor` |
| `backend/portfolio/src/main/java/com/accioneselbosque/portfolio/repository/TransactionSpecification.java` | Crear — Specification con filtros |
| `backend/app/src/main/java/com/accioneselbosque/app/dto/AdminTransactionDto.java` | Crear — record de respuesta |
| `backend/app/pom.xml` | Modificar — agregar `spring-boot-starter-test` |
| `backend/app/src/test/java/com/accioneselbosque/app/service/DashboardServiceAdminTransactionsTest.java` | Crear — tests unitarios |
| `backend/app/src/main/java/com/accioneselbosque/app/service/DashboardService.java` | Modificar — agregar `getAdminTransactions()` |
| `backend/app/src/main/java/com/accioneselbosque/app/controller/AdminDashboardController.java` | Modificar — agregar endpoint `GET /transactions` |
| `frontend/src/app/core/models/index.ts` | Modificar — agregar interfaces |
| `frontend/src/app/core/services/admin.service.ts` | Modificar — agregar `getAdminTransactions()` |
| `frontend/src/app/features/admin/admin-transactions/admin-transactions.component.ts` | Crear |
| `frontend/src/app/features/admin/admin-transactions/admin-transactions.component.html` | Crear |
| `frontend/src/app/features/admin/admin-transactions/admin-transactions.component.scss` | Crear |
| `frontend/src/app/app.routes.ts` | Modificar — agregar ruta `/admin/transactions` |

---

## Task 1: Habilitar JpaSpecificationExecutor en TransactionRepository

**Files:**
- Modify: `backend/portfolio/src/main/java/com/accioneselbosque/portfolio/repository/TransactionRepository.java`

- [ ] **Step 1: Agregar JpaSpecificationExecutor a la interfaz**

Reemplazar la línea de declaración de la interfaz:

```java
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
```

por:

```java
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {
```

No hay imports adicionales — `JpaSpecificationExecutor` ya está en `org.springframework.data.jpa.repository` que Spring Data importa.

Agregar el import al principio del archivo:
```java
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
```

- [ ] **Step 2: Verificar compilación del módulo portfolio**

```bash
cd backend
mvn compile -pl portfolio --no-transfer-progress -q
```

Resultado esperado: `BUILD SUCCESS` sin errores.

- [ ] **Step 3: Commit**

```bash
git add backend/portfolio/src/main/java/com/accioneselbosque/portfolio/repository/TransactionRepository.java
git commit -m "feat(portfolio): habilitar JpaSpecificationExecutor en TransactionRepository"
```

---

## Task 2: Crear TransactionSpecification

**Files:**
- Create: `backend/portfolio/src/main/java/com/accioneselbosque/portfolio/repository/TransactionSpecification.java`

- [ ] **Step 1: Crear el archivo de Specification**

```java
package com.accioneselbosque.portfolio.repository;

import com.accioneselbosque.portfolio.model.Transaction;
import com.accioneselbosque.portfolio.model.TransactionType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class TransactionSpecification {

    public static Specification<Transaction> withFilters(
            LocalDate from, LocalDate to, Long investorId, String symbol, String type) {

        return Specification
                .where(fromDate(from))
                .and(toDate(to))
                .and(byInvestorId(investorId))
                .and(bySymbol(symbol))
                .and(byType(type));
    }

    private static Specification<Transaction> fromDate(LocalDate from) {
        return (root, query, cb) -> from == null ? null
                : cb.greaterThanOrEqualTo(root.get("executedAt"), from.atStartOfDay());
    }

    private static Specification<Transaction> toDate(LocalDate to) {
        return (root, query, cb) -> to == null ? null
                : cb.lessThan(root.get("executedAt"), to.plusDays(1).atStartOfDay());
    }

    private static Specification<Transaction> byInvestorId(Long investorId) {
        return (root, query, cb) -> investorId == null ? null
                : cb.equal(root.get("investorId"), investorId);
    }

    private static Specification<Transaction> bySymbol(String symbol) {
        return (root, query, cb) -> (symbol == null || symbol.isBlank()) ? null
                : cb.like(cb.lower(root.get("symbol")), "%" + symbol.trim().toLowerCase() + "%");
    }

    private static Specification<Transaction> byType(String type) {
        if (type == null || type.isBlank()) return null;
        try {
            TransactionType tt = TransactionType.valueOf(type.trim().toUpperCase());
            return (root, query, cb) -> cb.equal(root.get("transactionType"), tt);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
```

- [ ] **Step 2: Verificar compilación**

```bash
cd backend
mvn compile -pl portfolio --no-transfer-progress -q
```

Resultado esperado: `BUILD SUCCESS`.

- [ ] **Step 3: Commit**

```bash
git add backend/portfolio/src/main/java/com/accioneselbosque/portfolio/repository/TransactionSpecification.java
git commit -m "feat(portfolio): agregar TransactionSpecification para filtros dinámicos"
```

---

## Task 3: Crear AdminTransactionDto

**Files:**
- Create: `backend/app/src/main/java/com/accioneselbosque/app/dto/AdminTransactionDto.java`

- [ ] **Step 1: Crear el record DTO**

```java
package com.accioneselbosque.app.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminTransactionDto(
        Long id,
        String investorName,
        String investorEmail,
        String symbol,
        String type,
        int quantity,
        BigDecimal grossAmount,
        BigDecimal commission,
        LocalDateTime executedAt
) {}
```

- [ ] **Step 2: Verificar compilación del módulo app**

```bash
cd backend
mvn compile -pl app -am --no-transfer-progress -q
```

Resultado esperado: `BUILD SUCCESS`.

- [ ] **Step 3: Commit**

```bash
git add backend/app/src/main/java/com/accioneselbosque/app/dto/AdminTransactionDto.java
git commit -m "feat(app): agregar AdminTransactionDto"
```

---

## Task 4: Escribir tests que fallan (TDD)

**Files:**
- Modify: `backend/app/pom.xml`
- Create: `backend/app/src/test/java/com/accioneselbosque/app/service/DashboardServiceAdminTransactionsTest.java`

- [ ] **Step 1: Agregar dependencia de test a app/pom.xml**

Agregar dentro de `<dependencies>` en `backend/app/pom.xml`:

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-test</artifactId>
  <scope>test</scope>
</dependency>
```

- [ ] **Step 2: Crear directorio de tests**

```bash
mkdir -p backend/app/src/test/java/com/accioneselbosque/app/service
```

- [ ] **Step 3: Crear el archivo de test**

```java
package com.accioneselbosque.app.service;

import com.accioneselbosque.app.dto.AdminTransactionDto;
import com.accioneselbosque.auth.model.Investor;
import com.accioneselbosque.auth.repository.InvestorRepository;
import com.accioneselbosque.configuration.service.MarketStatusService;
import com.accioneselbosque.orders.repository.OrderRepository;
import com.accioneselbosque.portfolio.model.Transaction;
import com.accioneselbosque.portfolio.model.TransactionType;
import com.accioneselbosque.portfolio.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceAdminTransactionsTest {

    @Mock private MarketStatusService marketStatusService;
    @Mock private OrderRepository orderRepository;
    @Mock private InvestorRepository investorRepository;
    @Mock private TransactionRepository transactionRepository;

    @InjectMocks private DashboardService dashboardService;

    @Test
    void getAdminTransactions_retornaPageConDatosDelInversor() {
        Transaction tx = new Transaction();
        tx.setId(1L);
        tx.setInvestorId(10L);
        tx.setSymbol("ECOPETROL");
        tx.setTransactionType(TransactionType.BUY);
        tx.setQuantity(5);
        tx.setGrossAmount(new BigDecimal("500000"));
        tx.setCommission(new BigDecimal("2500"));
        tx.setNetAmount(new BigDecimal("497500"));
        tx.setExecutedAt(LocalDateTime.now());

        Investor investor = new Investor();
        investor.setId(10L);
        investor.setFullName("Juan Pérez");
        investor.setEmail("juan@test.com");

        when(transactionRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(tx)));
        when(investorRepository.findAllById(any()))
                .thenReturn(List.of(investor));

        PageRequest pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "executedAt"));

        Page<AdminTransactionDto> result = dashboardService.getAdminTransactions(
                null, null, null, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        AdminTransactionDto dto = result.getContent().get(0);
        assertThat(dto.investorName()).isEqualTo("Juan Pérez");
        assertThat(dto.investorEmail()).isEqualTo("juan@test.com");
        assertThat(dto.symbol()).isEqualTo("ECOPETROL");
        assertThat(dto.type()).isEqualTo("BUY");
        assertThat(dto.quantity()).isEqualTo(5);
        assertThat(dto.grossAmount()).isEqualByComparingTo("500000");
        assertThat(dto.commission()).isEqualByComparingTo("2500");
    }

    @Test
    void getAdminTransactions_inversoresSinRegistroMuestraDash() {
        Transaction tx = new Transaction();
        tx.setId(2L);
        tx.setInvestorId(99L);
        tx.setSymbol("ISA");
        tx.setTransactionType(TransactionType.SELL);
        tx.setQuantity(1);
        tx.setGrossAmount(new BigDecimal("100000"));
        tx.setCommission(new BigDecimal("500"));
        tx.setNetAmount(new BigDecimal("99500"));
        tx.setExecutedAt(LocalDateTime.now());

        when(transactionRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(tx)));
        when(investorRepository.findAllById(any())).thenReturn(List.of());

        PageRequest pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "executedAt"));

        Page<AdminTransactionDto> result = dashboardService.getAdminTransactions(
                null, null, null, null, null, pageable);

        assertThat(result.getContent().get(0).investorName()).isEqualTo("—");
        assertThat(result.getContent().get(0).investorEmail()).isEqualTo("—");
    }

    @Test
    void getAdminTransactions_paginaVaciaRetornaContentVacio() {
        when(transactionRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(investorRepository.findAllById(any())).thenReturn(List.of());

        PageRequest pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "executedAt"));

        Page<AdminTransactionDto> result = dashboardService.getAdminTransactions(
                null, null, null, null, null, pageable);

        assertThat(result.getContent()).isEmpty();
    }
}
```

- [ ] **Step 4: Ejecutar los tests y verificar que fallan**

```bash
cd backend
mvn test -pl app -am --no-transfer-progress -Dtest=DashboardServiceAdminTransactionsTest 2>&1 | tail -20
```

Resultado esperado: `BUILD FAILURE` con error `getAdminTransactions` no existe en `DashboardService`.

---

## Task 5: Implementar getAdminTransactions en DashboardService

**Files:**
- Modify: `backend/app/src/main/java/com/accioneselbosque/app/service/DashboardService.java`

- [ ] **Step 1: Agregar imports al principio del archivo**

Agregar después de los imports existentes:

```java
import com.accioneselbosque.app.dto.AdminTransactionDto;
import com.accioneselbosque.auth.model.Investor;
import com.accioneselbosque.portfolio.model.Transaction;
import com.accioneselbosque.portfolio.repository.TransactionSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
```

- [ ] **Step 2: Agregar el método al final de la clase (antes del `}`)**

```java
@Transactional(readOnly = true)
public Page<AdminTransactionDto> getAdminTransactions(
        LocalDate from, LocalDate to, Long investorId,
        String symbol, String type, Pageable pageable) {

    Specification<Transaction> spec =
            TransactionSpecification.withFilters(from, to, investorId, symbol, type);

    Page<Transaction> page = transactionRepository.findAll(spec, pageable);

    Set<Long> investorIds = page.getContent().stream()
            .map(Transaction::getInvestorId)
            .collect(Collectors.toSet());

    Map<Long, Investor> investors = investorRepository.findAllById(investorIds).stream()
            .collect(Collectors.toMap(Investor::getId, Function.identity()));

    return page.map(t -> {
        Investor inv = investors.get(t.getInvestorId());
        String name  = inv != null ? inv.getFullName() : "—";
        String email = inv != null ? inv.getEmail()    : "—";
        return new AdminTransactionDto(
                t.getId(), name, email,
                t.getSymbol(),
                t.getTransactionType().name(),
                t.getQuantity(),
                t.getGrossAmount(),
                t.getCommission(),
                t.getExecutedAt());
    });
}
```

- [ ] **Step 3: Ejecutar los tests y verificar que pasan**

```bash
cd backend
mvn test -pl app -am --no-transfer-progress -Dtest=DashboardServiceAdminTransactionsTest 2>&1 | tail -15
```

Resultado esperado: `BUILD SUCCESS`, `Tests run: 3, Failures: 0, Errors: 0`.

- [ ] **Step 4: Commit**

```bash
git add backend/app/pom.xml \
        backend/app/src/main/java/com/accioneselbosque/app/service/DashboardService.java \
        backend/app/src/test/java/com/accioneselbosque/app/service/DashboardServiceAdminTransactionsTest.java
git commit -m "feat(app): implementar getAdminTransactions en DashboardService con tests"
```

---

## Task 6: Agregar endpoint en AdminDashboardController

**Files:**
- Modify: `backend/app/src/main/java/com/accioneselbosque/app/controller/AdminDashboardController.java`

- [ ] **Step 1: Agregar imports al archivo**

Agregar después de los imports existentes:

```java
import com.accioneselbosque.app.dto.AdminTransactionDto;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
```

- [ ] **Step 2: Agregar el endpoint al final de la clase (antes del `}`)**

```java
@GetMapping("/transactions")
public ResponseEntity<Page<AdminTransactionDto>> getAdminTransactions(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(required = false) Long investorId,
        @RequestParam(required = false) String symbol,
        @RequestParam(required = false) String type,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {

    int clampedSize = Math.min(size, 100);
    PageRequest pageable = PageRequest.of(page, clampedSize,
            Sort.by(Sort.Direction.DESC, "executedAt"));
    return ResponseEntity.ok(
            dashboardService.getAdminTransactions(from, to, investorId, symbol, type, pageable));
}
```

- [ ] **Step 3: Compilar y correr todos los tests del backend**

```bash
cd backend
mvn test --no-transfer-progress 2>&1 | tail -20
```

Resultado esperado: `BUILD SUCCESS`.

- [ ] **Step 4: Commit**

```bash
git add backend/app/src/main/java/com/accioneselbosque/app/controller/AdminDashboardController.java
git commit -m "feat(app): agregar endpoint GET /admin/dashboard/transactions"
```

---

## Task 7: Agregar modelos al frontend

**Files:**
- Modify: `frontend/src/app/core/models/index.ts`

- [ ] **Step 1: Agregar las interfaces al final del archivo (después de `Page<T>`)**

```typescript
// ── Admin Transactions ────────────────────────────────────────
export interface AdminTransactionDto {
  id: number;
  investorName: string;
  investorEmail: string;
  symbol: string;
  type: 'BUY' | 'SELL';
  quantity: number;
  grossAmount: number;
  commission: number;
  executedAt: string;
}

export interface AdminTransactionFilterDto {
  from?: string;
  to?: string;
  investorId?: number;
  symbol?: string;
  type?: string;
  page?: number;
  size?: number;
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/app/core/models/index.ts
git commit -m "feat(frontend): agregar AdminTransactionDto y AdminTransactionFilterDto"
```

---

## Task 8: Agregar método a AdminService

**Files:**
- Modify: `frontend/src/app/core/services/admin.service.ts`

- [ ] **Step 1: Agregar la importación de los nuevos tipos**

En `admin.service.ts`, la línea de import de `'../models'` ya lista muchos tipos. Agregar `AdminTransactionDto` y `AdminTransactionFilterDto` a esa misma lista. El bloque import quedará así (los tipos existentes permanecen sin cambio):

```typescript
import {
  OperationalMetricsDto,
  FinancialSummaryDto,
  AdminLinkDto,
  AdminUserDto,
  AdminUserDetailDto,
  UpdateUserStatusRequest,
  UpdateUserRoleRequest,
  PagedUsersResponse,
  GroupedParametersResponse,
  UpdateParameterRequest,
  ParameterChangeHistoryDto,
  MarketStatusDto,
  MarketScheduleDto,
  MarketHolidayDto,
  AuditEventDto,
  AuditFilterDto,
  Page,
  AdminTransactionDto,
  AdminTransactionFilterDto
} from '../models';
```

- [ ] **Step 2: Agregar el método al final de la clase (dentro de la clase, antes del `}`)**

```typescript
// ── Admin Transactions ─────────────────────────────────────
getAdminTransactions(filters: AdminTransactionFilterDto = {}): Observable<Page<AdminTransactionDto>> {
  let params = new HttpParams();
  if (filters.from)        params = params.set('from',       filters.from);
  if (filters.to)          params = params.set('to',         filters.to);
  if (filters.investorId !== undefined)
                           params = params.set('investorId', filters.investorId.toString());
  if (filters.symbol)      params = params.set('symbol',     filters.symbol);
  if (filters.type)        params = params.set('type',       filters.type);
  if (filters.page !== undefined)
                           params = params.set('page',       filters.page.toString());
  if (filters.size !== undefined)
                           params = params.set('size',       filters.size.toString());
  return this.http.get<Page<AdminTransactionDto>>(
    `${this.base}/admin/dashboard/transactions`, { params });
}
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/app/core/services/admin.service.ts
git commit -m "feat(frontend): agregar getAdminTransactions a AdminService"
```

---

## Task 9: Crear componente admin-transactions

**Files:**
- Create: `frontend/src/app/features/admin/admin-transactions/admin-transactions.component.ts`
- Create: `frontend/src/app/features/admin/admin-transactions/admin-transactions.component.html`
- Create: `frontend/src/app/features/admin/admin-transactions/admin-transactions.component.scss`

- [ ] **Step 1: Crear el directorio**

```bash
mkdir -p frontend/src/app/features/admin/admin-transactions
```

- [ ] **Step 2: Crear admin-transactions.component.ts**

```typescript
import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../../../core/services/admin.service';
import { AdminTransactionDto, AdminTransactionFilterDto } from '../../../core/models';

@Component({
  selector: 'app-admin-transactions',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-transactions.component.html',
  styleUrl: './admin-transactions.component.scss'
})
export class AdminTransactionsComponent implements OnInit {
  private readonly adminSvc = inject(AdminService);

  transactions = signal<AdminTransactionDto[]>([]);
  totalElements = signal(0);
  totalPages = signal(0);
  currentPage = signal(0);
  readonly pageSize = 20;
  loading = signal(true);
  loadError = signal<string | null>(null);

  filterFrom      = signal('');
  filterTo        = signal('');
  filterInvestorId = signal('');
  filterSymbol    = signal('');
  filterType      = signal('');

  typeOptions = ['', 'BUY', 'SELL'];

  pageNumbers = computed(() =>
    Array.from({ length: Math.min(this.totalPages(), 7) }, (_, i) => i)
  );

  ngOnInit(): void {
    this.search();
  }

  private buildFilters(): AdminTransactionFilterDto {
    const f: AdminTransactionFilterDto = { page: this.currentPage(), size: this.pageSize };
    if (this.filterFrom())   f.from = this.filterFrom();
    if (this.filterTo())     f.to   = this.filterTo();
    const id = this.filterInvestorId().trim();
    if (id && !isNaN(Number(id))) f.investorId = Number(id);
    if (this.filterSymbol().trim()) f.symbol = this.filterSymbol().trim();
    if (this.filterType())   f.type = this.filterType();
    return f;
  }

  search(): void {
    this.currentPage.set(0);
    this.loadPage();
  }

  loadPage(): void {
    this.loading.set(true);
    this.loadError.set(null);
    this.adminSvc.getAdminTransactions(this.buildFilters()).subscribe({
      next: page => {
        this.transactions.set(page.content);
        this.totalElements.set(page.totalElements);
        this.totalPages.set(page.totalPages);
        this.loading.set(false);
      },
      error: () => {
        this.loadError.set('Error al cargar las transacciones.');
        this.loading.set(false);
      }
    });
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages()) return;
    this.currentPage.set(page);
    this.loadPage();
  }

  clearFilters(): void {
    this.filterFrom.set('');
    this.filterTo.set('');
    this.filterInvestorId.set('');
    this.filterSymbol.set('');
    this.filterType.set('');
    this.search();
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleString('es-CO', { timeZone: 'America/Bogota' });
  }

  formatCurrency(value: number): string {
    return new Intl.NumberFormat('es-CO', {
      style: 'currency', currency: 'COP', minimumFractionDigits: 0
    }).format(value);
  }
}
```

- [ ] **Step 3: Crear admin-transactions.component.html**

```html
<div class="container">
  <div class="page-header">
    <h1 class="card__title">Movimientos de la Plataforma</h1>
    <p class="card__subtitle text-muted">Todas las transacciones ejecutadas por los usuarios</p>
  </div>

  <!-- Filtros -->
  <div class="card mt-3">
    <h3 class="card__subtitle mb-3">Filtros de Búsqueda</h3>
    <div class="filter-grid">
      <div class="form-group">
        <label>Desde</label>
        <input type="date" class="form-control"
               [ngModel]="filterFrom()" (ngModelChange)="filterFrom.set($event)" />
      </div>
      <div class="form-group">
        <label>Hasta</label>
        <input type="date" class="form-control"
               [ngModel]="filterTo()" (ngModelChange)="filterTo.set($event)" />
      </div>
      <div class="form-group">
        <label>ID Usuario</label>
        <input type="number" class="form-control" placeholder="Ej: 1234" min="1"
               [ngModel]="filterInvestorId()" (ngModelChange)="filterInvestorId.set($event)" />
      </div>
      <div class="form-group">
        <label>Símbolo</label>
        <input type="text" class="form-control" placeholder="Ej: ECOPETROL"
               [ngModel]="filterSymbol()" (ngModelChange)="filterSymbol.set($event)" />
      </div>
      <div class="form-group">
        <label>Tipo</label>
        <select class="form-control"
                [ngModel]="filterType()" (ngModelChange)="filterType.set($event)">
          <option value="">Todos</option>
          <option value="BUY">BUY</option>
          <option value="SELL">SELL</option>
        </select>
      </div>
      <div class="form-group flex gap-1 items-end">
        <button class="btn-primary flex-1" (click)="search()">Buscar</button>
        <button class="btn-outline btn-sm" (click)="clearFilters()">Limpiar</button>
      </div>
    </div>
  </div>

  <!-- Resultados -->
  @if (loading()) {
    <div class="loading-container mt-3"><div class="spinner"></div></div>
  } @else if (loadError()) {
    <div class="alert alert-danger mt-3">{{ loadError() }}</div>
  } @else {
    <div class="card mt-3">
      <div class="flex-between mb-2">
        <p class="text-muted text-sm">
          {{ totalElements() }} transacción(es) — Página {{ currentPage() + 1 }} de {{ totalPages() }}
        </p>
      </div>

      @if (transactions().length === 0) {
        <div class="text-center" style="padding: 3rem;">
          <p class="text-muted">No se encontraron transacciones con los filtros aplicados.</p>
        </div>
      } @else {
        <div class="overflow-x">
          <table class="data-table">
            <thead>
              <tr>
                <th>Fecha</th>
                <th>Usuario</th>
                <th>Símbolo</th>
                <th>Tipo</th>
                <th>Cantidad</th>
                <th>Monto Bruto</th>
                <th>Comisión</th>
              </tr>
            </thead>
            <tbody>
              @for (tx of transactions(); track tx.id) {
                <tr>
                  <td class="text-muted text-nowrap">{{ formatDate(tx.executedAt) }}</td>
                  <td>
                    <p class="text-sm font-medium">{{ tx.investorName }}</p>
                    <p class="text-xs text-muted">{{ tx.investorEmail }}</p>
                  </td>
                  <td><strong>{{ tx.symbol }}</strong></td>
                  <td>
                    @if (tx.type === 'BUY') {
                      <span class="badge-green">BUY</span>
                    } @else {
                      <span class="badge-red">SELL</span>
                    }
                  </td>
                  <td>{{ tx.quantity }}</td>
                  <td class="text-cyan">{{ formatCurrency(tx.grossAmount) }}</td>
                  <td class="text-muted">{{ formatCurrency(tx.commission) }}</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </div>

    <!-- Paginación -->
    @if (totalPages() > 1) {
      <div class="pagination mt-3">
        <button class="btn-outline btn-sm"
                [disabled]="currentPage() === 0"
                (click)="goToPage(currentPage() - 1)">
          <svg width="14" height="14" viewBox="0 0 14 14" fill="none" aria-hidden="true">
            <path d="M9 3L5 7l4 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          Anterior
        </button>
        @for (page of pageNumbers(); track page) {
          <button class="btn-outline btn-sm"
                  [class.btn-primary]="page === currentPage()"
                  (click)="goToPage(page)">
            {{ page + 1 }}
          </button>
        }
        <button class="btn-outline btn-sm"
                [disabled]="currentPage() >= totalPages() - 1"
                (click)="goToPage(currentPage() + 1)">
          Siguiente
          <svg width="14" height="14" viewBox="0 0 14 14" fill="none" aria-hidden="true">
            <path d="M5 3l4 4-4 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
      </div>
    }
  }
</div>
```

- [ ] **Step 4: Crear admin-transactions.component.scss**

```scss
:host {
  display: block;
  padding: 1.5rem 0;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 1rem;
  align-items: flex-end;
}
```

- [ ] **Step 5: Commit**

```bash
git add frontend/src/app/features/admin/admin-transactions/
git commit -m "feat(frontend): crear componente admin-transactions"
```

---

## Task 10: Agregar ruta y verificar navegación

**Files:**
- Modify: `frontend/src/app/app.routes.ts`

- [ ] **Step 1: Agregar la ruta en la sección Admin**

En `frontend/src/app/app.routes.ts`, dentro del bloque `// ── Admin ──`, agregar después de la ruta `/admin/audit`:

```typescript
{
  path: 'admin/transactions',
  canActivate: [authGuard, adminGuard],
  loadComponent: () =>
    import('./features/admin/admin-transactions/admin-transactions.component')
      .then(m => m.AdminTransactionsComponent)
},
```

- [ ] **Step 2: Verificar compilación Angular**

```bash
cd frontend
npx ng build --configuration development --no-progress 2>&1 | tail -15
```

Resultado esperado: `Application bundle generation complete.` sin errores.

- [ ] **Step 3: Commit final**

```bash
git add frontend/src/app/app.routes.ts
git commit -m "feat(frontend): agregar ruta /admin/transactions con adminGuard"
```

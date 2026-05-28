# Portfolio Snapshot — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Save a daily snapshot of each investor's total portfolio value (cash + stocks) so the dashboard evolution chart shows real historical data instead of random simulation.

**Architecture:** A Spring `@Scheduled` job runs daily and writes one `portfolio_snapshot` row per investor per day (upsert). The new `GET /portfolio/history?period=30D` endpoint queries those rows and returns them ordered by date. The Angular dashboard fetches real data on period change and falls back to a simulated curve only when no snapshots exist.

**Tech Stack:** Spring Boot `@Scheduled`, Spring Data JPA, PostgreSQL (Flyway V39), Angular 18 signals, ng-apexcharts.

---

## File Structure

| Action | Path | Responsibility |
|--------|------|----------------|
| Create | `backend/app/src/main/resources/db/migration/V39__create_portfolio_snapshot_table.sql` | Table DDL |
| Create | `backend/portfolio/src/main/java/com/accioneselbosque/portfolio/model/PortfolioSnapshot.java` | JPA entity |
| Create | `backend/portfolio/src/main/java/com/accioneselbosque/portfolio/repository/PortfolioSnapshotRepository.java` | Data access |
| Modify | `backend/portfolio/src/main/java/com/accioneselbosque/portfolio/repository/PositionRepository.java` | Add `findDistinctInvestorIds()` |
| Create | `backend/portfolio/src/main/java/com/accioneselbosque/portfolio/dto/PortfolioHistoryPoint.java` | Response DTO (one point) |
| Create | `backend/portfolio/src/main/java/com/accioneselbosque/portfolio/dto/PortfolioHistoryResponse.java` | Response DTO (list wrapper) |
| Create | `backend/portfolio/src/main/java/com/accioneselbosque/portfolio/service/PortfolioSnapshotService.java` | Snapshot logic + query |
| Create | `backend/portfolio/src/main/java/com/accioneselbosque/portfolio/service/PortfolioSnapshotScheduler.java` | Daily cron trigger |
| Modify | `backend/portfolio/src/main/java/com/accioneselbosque/portfolio/controller/PortfolioController.java` | Add `/portfolio/history` endpoint |
| Modify | `frontend/src/app/core/models/index.ts` | Add `PortfolioHistoryPoint`, `PortfolioHistoryResponse` |
| Modify | `frontend/src/app/core/services/portfolio.service.ts` | Add `getPortfolioHistory(period)` |
| Modify | `frontend/src/app/features/dashboard/dashboard.component.ts` | Fetch real data on period switch |

---

### Task 1: Migration + Entity + Repository

**Files:**
- Create: `backend/app/src/main/resources/db/migration/V39__create_portfolio_snapshot_table.sql`
- Create: `backend/portfolio/src/main/java/com/accioneselbosque/portfolio/model/PortfolioSnapshot.java`
- Create: `backend/portfolio/src/main/java/com/accioneselbosque/portfolio/repository/PortfolioSnapshotRepository.java`
- Modify: `backend/portfolio/src/main/java/com/accioneselbosque/portfolio/repository/PositionRepository.java`

- [ ] **Step 1: Create the Flyway migration**

```sql
-- backend/app/src/main/resources/db/migration/V39__create_portfolio_snapshot_table.sql
CREATE TABLE portfolio_snapshot (
    id             BIGSERIAL PRIMARY KEY,
    investor_id    BIGINT NOT NULL REFERENCES investor(id),
    snapshot_date  DATE NOT NULL,
    total_value    DECIMAL(18,2) NOT NULL,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_snapshot_investor_date UNIQUE (investor_id, snapshot_date)
);
CREATE INDEX snapshot_investor_date_idx ON portfolio_snapshot(investor_id, snapshot_date DESC);
```

- [ ] **Step 2: Create the entity**

```java
// backend/portfolio/src/main/java/com/accioneselbosque/portfolio/model/PortfolioSnapshot.java
package com.accioneselbosque.portfolio.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "portfolio_snapshot")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "investor_id", nullable = false)
    private Long investorId;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "total_value", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalValue;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

- [ ] **Step 3: Create the repository**

```java
// backend/portfolio/src/main/java/com/accioneselbosque/portfolio/repository/PortfolioSnapshotRepository.java
package com.accioneselbosque.portfolio.repository;

import com.accioneselbosque.portfolio.model.PortfolioSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PortfolioSnapshotRepository extends JpaRepository<PortfolioSnapshot, Long> {

    List<PortfolioSnapshot> findByInvestorIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(
            Long investorId, LocalDate from, LocalDate to);

    Optional<PortfolioSnapshot> findByInvestorIdAndSnapshotDate(Long investorId, LocalDate date);
}
```

- [ ] **Step 4: Add `findDistinctInvestorIds` to PositionRepository**

The full file after the change (add the `@Query` import and new method):

```java
// backend/portfolio/src/main/java/com/accioneselbosque/portfolio/repository/PositionRepository.java
package com.accioneselbosque.portfolio.repository;

import com.accioneselbosque.portfolio.model.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PositionRepository extends JpaRepository<Position, Long> {

    List<Position> findByInvestorId(Long investorId);

    Optional<Position> findByInvestorIdAndSymbol(Long investorId, String symbol);

    @Query("SELECT DISTINCT p.investorId FROM Position p WHERE p.currentQuantity > 0")
    List<Long> findDistinctInvestorIds();
}
```

- [ ] **Step 5: Commit**

```bash
git add backend/app/src/main/resources/db/migration/V39__create_portfolio_snapshot_table.sql \
        backend/portfolio/src/main/java/com/accioneselbosque/portfolio/model/PortfolioSnapshot.java \
        backend/portfolio/src/main/java/com/accioneselbosque/portfolio/repository/PortfolioSnapshotRepository.java \
        backend/portfolio/src/main/java/com/accioneselbosque/portfolio/repository/PositionRepository.java
git commit -m "feat: portfolio snapshot — migración, entidad y repositorio"
```

---

### Task 2: DTOs + PortfolioSnapshotService

**Files:**
- Create: `backend/portfolio/src/main/java/com/accioneselbosque/portfolio/dto/PortfolioHistoryPoint.java`
- Create: `backend/portfolio/src/main/java/com/accioneselbosque/portfolio/dto/PortfolioHistoryResponse.java`
- Create: `backend/portfolio/src/main/java/com/accioneselbosque/portfolio/service/PortfolioSnapshotService.java`

- [ ] **Step 1: Create response DTOs**

```java
// backend/portfolio/src/main/java/com/accioneselbosque/portfolio/dto/PortfolioHistoryPoint.java
package com.accioneselbosque.portfolio.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PortfolioHistoryPoint(LocalDate date, BigDecimal totalValue) {}
```

```java
// backend/portfolio/src/main/java/com/accioneselbosque/portfolio/dto/PortfolioHistoryResponse.java
package com.accioneselbosque.portfolio.dto;

import java.util.List;

public record PortfolioHistoryResponse(List<PortfolioHistoryPoint> points) {}
```

- [ ] **Step 2: Create PortfolioSnapshotService**

```java
// backend/portfolio/src/main/java/com/accioneselbosque/portfolio/service/PortfolioSnapshotService.java
package com.accioneselbosque.portfolio.service;

import com.accioneselbosque.market_data_service.service.StockSnapshotService;
import com.accioneselbosque.portfolio.dto.PortfolioHistoryPoint;
import com.accioneselbosque.portfolio.dto.PortfolioHistoryResponse;
import com.accioneselbosque.portfolio.model.PortfolioSnapshot;
import com.accioneselbosque.portfolio.repository.AccountBalanceRepository;
import com.accioneselbosque.portfolio.repository.PortfolioSnapshotRepository;
import com.accioneselbosque.portfolio.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioSnapshotService {

    private final PortfolioSnapshotRepository snapshotRepository;
    private final PositionRepository positionRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    private final StockSnapshotService stockSnapshotService;

    @Transactional
    public void takeSnapshot() {
        List<Long> investorIds = positionRepository.findDistinctInvestorIds();
        LocalDate today = LocalDate.now();
        log.info("PortfolioSnapshotService: taking snapshot for {} investors", investorIds.size());

        for (Long investorId : investorIds) {
            BigDecimal positionsValue = positionRepository.findByInvestorId(investorId).stream()
                    .map(p -> {
                        BigDecimal price = stockSnapshotService.findBySymbol(p.getSymbol())
                                .map(s -> s.getCurrentPrice())
                                .orElse(p.getAvgPurchasePrice());
                        return price.multiply(BigDecimal.valueOf(p.getCurrentQuantity()));
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal cash = accountBalanceRepository.findByInvestorId(investorId)
                    .map(b -> b.getTotalBalance())
                    .orElse(BigDecimal.ZERO);

            BigDecimal totalValue = cash.add(positionsValue);

            PortfolioSnapshot snapshot = snapshotRepository
                    .findByInvestorIdAndSnapshotDate(investorId, today)
                    .orElseGet(() -> PortfolioSnapshot.builder()
                            .investorId(investorId)
                            .snapshotDate(today)
                            .build());
            snapshot.setTotalValue(totalValue);
            snapshotRepository.save(snapshot);
        }
        log.info("PortfolioSnapshotService: snapshot complete");
    }

    @Transactional(readOnly = true)
    public PortfolioHistoryResponse getHistory(Long investorId, String period) {
        LocalDate from = switch (period) {
            case "7D" -> LocalDate.now().minusDays(7);
            case "3M" -> LocalDate.now().minusDays(90);
            case "1A" -> LocalDate.now().minusDays(365);
            default   -> LocalDate.now().minusDays(30);
        };

        List<PortfolioHistoryPoint> points = snapshotRepository
                .findByInvestorIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(
                        investorId, from, LocalDate.now())
                .stream()
                .map(s -> new PortfolioHistoryPoint(s.getSnapshotDate(), s.getTotalValue()))
                .toList();

        return new PortfolioHistoryResponse(points);
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add backend/portfolio/src/main/java/com/accioneselbosque/portfolio/dto/PortfolioHistoryPoint.java \
        backend/portfolio/src/main/java/com/accioneselbosque/portfolio/dto/PortfolioHistoryResponse.java \
        backend/portfolio/src/main/java/com/accioneselbosque/portfolio/service/PortfolioSnapshotService.java
git commit -m "feat: portfolio snapshot — DTOs y servicio de histórico"
```

---

### Task 3: Scheduler + Endpoint

**Files:**
- Create: `backend/portfolio/src/main/java/com/accioneselbosque/portfolio/service/PortfolioSnapshotScheduler.java`
- Modify: `backend/portfolio/src/main/java/com/accioneselbosque/portfolio/controller/PortfolioController.java`

- [ ] **Step 1: Create the scheduler**

```java
// backend/portfolio/src/main/java/com/accioneselbosque/portfolio/service/PortfolioSnapshotScheduler.java
package com.accioneselbosque.portfolio.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PortfolioSnapshotScheduler {

    private final PortfolioSnapshotService snapshotService;

    // Runs daily at 11:00 PM UTC (6:00 PM COT, after market close)
    @Scheduled(cron = "0 0 23 * * *")
    public void scheduleDailySnapshot() {
        log.info("PortfolioSnapshotScheduler: starting daily snapshot");
        snapshotService.takeSnapshot();
    }
}
```

- [ ] **Step 2: Add the `/portfolio/history` endpoint to PortfolioController**

The full updated controller:

```java
// backend/portfolio/src/main/java/com/accioneselbosque/portfolio/controller/PortfolioController.java
package com.accioneselbosque.portfolio.controller;

import com.accioneselbosque.portfolio.dto.PortfolioHistoryResponse;
import com.accioneselbosque.portfolio.dto.PortfolioPositionsResponse;
import com.accioneselbosque.portfolio.dto.PortfolioReportDto;
import com.accioneselbosque.portfolio.model.ReportPeriod;
import com.accioneselbosque.portfolio.service.CsvReportExporter;
import com.accioneselbosque.portfolio.service.PortfolioService;
import com.accioneselbosque.portfolio.service.PortfolioSnapshotService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final CsvReportExporter csvReportExporter;
    private final PortfolioSnapshotService snapshotService;

    @GetMapping("/positions")
    public ResponseEntity<PortfolioPositionsResponse> getPositions(Authentication authentication) {
        Long investorId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(portfolioService.getPositions(investorId));
    }

    @GetMapping("/history")
    public ResponseEntity<PortfolioHistoryResponse> getHistory(
            @RequestParam(defaultValue = "30D") String period,
            Authentication authentication) {
        Long investorId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(snapshotService.getHistory(investorId, period));
    }

    @GetMapping("/report")
    public ResponseEntity<PortfolioReportDto> getReport(
            @RequestParam(defaultValue = "MONTH") ReportPeriod period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication) {

        Long investorId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(portfolioService.getReport(investorId, period, from, to));
    }

    @GetMapping("/report/export")
    public ResponseEntity<byte[]> exportReport(
            @RequestParam(defaultValue = "MONTH") ReportPeriod period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication) {

        Long investorId = Long.parseLong(authentication.getName());
        PortfolioReportDto report = portfolioService.getReport(investorId, period, from, to);
        byte[] csv = csvReportExporter.export(report);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"portfolio-report.csv\"")
                .body(csv);
    }
}
```

- [ ] **Step 3: Build and verify backend compiles**

Run from `backend/` directory (or project root):
```bash
docker compose build app 2>&1 | tail -10
```
Expected: `Image acciones-elbosque-app Built` with no compilation errors.

- [ ] **Step 4: Start backend and verify endpoint**

```bash
docker compose up -d app
```

Wait ~10 seconds, then test (replace `<TOKEN>` with a real JWT from login):
```bash
curl -s http://localhost:8080/portfolio/history?period=30D \
  -H "Authorization: Bearer <TOKEN>"
```
Expected: `{"points":[]}` (empty — no snapshots yet; scheduler hasn't run)

- [ ] **Step 5: Manually trigger a snapshot to seed data**

Add a temporary `@PostConstruct` in `PortfolioSnapshotScheduler` to run once on startup, then remove it after seeding:

```java
// Temporary — add below the @Scheduled method, remove after first successful snapshot
@jakarta.annotation.PostConstruct
public void seedInitialSnapshot() {
    snapshotService.takeSnapshot();
}
```

Rebuild, restart, hit the endpoint again:
```bash
curl -s http://localhost:8080/portfolio/history?period=30D \
  -H "Authorization: Bearer <TOKEN>"
```
Expected: `{"points":[{"date":"2026-05-28","totalValue":5000000.00}]}` (one point for today)

**Remove the `@PostConstruct` method after verifying**, rebuild again.

- [ ] **Step 6: Commit**

```bash
git add backend/portfolio/src/main/java/com/accioneselbosque/portfolio/service/PortfolioSnapshotScheduler.java \
        backend/portfolio/src/main/java/com/accioneselbosque/portfolio/controller/PortfolioController.java
git commit -m "feat: portfolio snapshot — scheduler diario y endpoint GET /portfolio/history"
```

---

### Task 4: Frontend — Models + Service

**Files:**
- Modify: `frontend/src/app/core/models/index.ts`
- Modify: `frontend/src/app/core/services/portfolio.service.ts`

- [ ] **Step 1: Add interfaces to models/index.ts**

Add after the `PortfolioPositionsResponse` interface (around line 130):

```typescript
export interface PortfolioHistoryPoint {
  date: string;       // ISO date string: "2026-05-28"
  totalValue: number;
}

export interface PortfolioHistoryResponse {
  points: PortfolioHistoryPoint[];
}
```

- [ ] **Step 2: Add getPortfolioHistory to PortfolioService**

Add the import and the new method:

```typescript
// In the imports at top of portfolio.service.ts, add:
import {
  BalanceSummaryResponse,
  FundMovementPageResponse,
  PortfolioHistoryResponse,
  PortfolioPositionsResponse,
  PortfolioReportDto
} from '../models';
```

Add this method inside the `PortfolioService` class:

```typescript
getPortfolioHistory(period: string): Observable<PortfolioHistoryResponse> {
  const params = new HttpParams().set('period', period);
  return this.http.get<PortfolioHistoryResponse>(`${this.base}/portfolio/history`, { params });
}
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/app/core/models/index.ts \
        frontend/src/app/core/services/portfolio.service.ts
git commit -m "feat: portfolio snapshot — modelos y método getPortfolioHistory en frontend"
```

---

### Task 5: Frontend — Dashboard Integration

**Files:**
- Modify: `frontend/src/app/features/dashboard/dashboard.component.ts`

- [ ] **Step 1: Import PortfolioHistoryResponse and catchError**

At the top of `dashboard.component.ts`, the existing imports already include `catchError` and `PortfolioPositionsResponse`. Add `PortfolioHistoryResponse` to the models import:

```typescript
import {
  BalanceSummaryResponse, ProfileResponse, Order,
  PortfolioPositionsResponse, StockSummary, WatchlistItem,
  PortfolioHistoryResponse
} from '../../core/models';
```

- [ ] **Step 2: Update selectPeriod to fetch real data**

Replace the existing `selectPeriod` method:

```typescript
selectPeriod(label: string): void {
  this.selectedPeriod.set(label);
  this.portfolio.getPortfolioHistory(label).pipe(
    catchError(() => of({ points: [] } as PortfolioHistoryResponse))
  ).subscribe(history => {
    if (history.points.length > 0) {
      this.buildPortfolioChartFromHistory(history);
    } else {
      const days = this.periods.find(p => p.label === label)?.days ?? 30;
      this.buildPortfolioChart(this.balance(), days);
    }
  });
}
```

- [ ] **Step 3: Add buildPortfolioChartFromHistory method**

Add this method after `selectPeriod`:

```typescript
private buildPortfolioChartFromHistory(history: PortfolioHistoryResponse): void {
  const categories = history.points.map(p =>
    new Date(p.date).toLocaleDateString('es-CO', { day: '2-digit', month: 'short' })
  );
  const values = history.points.map(p => Math.round(p.totalValue));
  this.portfolioSeries = [{ name: 'Valor portafolio', data: values, color: '#4aaa60' }];
  this.portfolioXAxis = { ...this.portfolioXAxis, categories };
}
```

- [ ] **Step 4: Fetch history on initial load**

In `ngOnInit`, after the `forkJoin` block that sets `balance` and `positions`, add a call to load the initial 30D history:

```typescript
// Inside the forkJoin subscribe callback, after buildDonutChart:
this.portfolio.getPortfolioHistory('30D').pipe(
  catchError(() => of({ points: [] } as PortfolioHistoryResponse))
).subscribe(history => {
  if (history.points.length > 0) {
    this.buildPortfolioChartFromHistory(history);
  }
  // If empty, the simulated chart built in buildPortfolioChart() remains
});
```

- [ ] **Step 5: Add the `of` import if not already present**

`of` is already imported via `import { forkJoin, of } from 'rxjs';` — no change needed.

- [ ] **Step 6: Build frontend and verify**

```bash
cd frontend && npx ng build --configuration=production 2>&1 | tail -5
```
Expected: `Application bundle generation complete.` with no errors.

- [ ] **Step 7: Commit and push**

```bash
cd ..
git add frontend/src/app/features/dashboard/dashboard.component.ts
git commit -m "feat: portfolio snapshot — dashboard muestra datos reales del histórico"
git push
```

---

## Self-Review

**Spec coverage:**
- ✅ Daily scheduler saves one snapshot per investor per day (Task 3)
- ✅ Upsert logic — re-running on the same day updates, doesn't duplicate (Task 2 `PortfolioSnapshotService.takeSnapshot`)
- ✅ `GET /portfolio/history?period=30D|7D|3M|1A` (Task 3)
- ✅ Frontend fetches real data on period switch (Task 5)
- ✅ Falls back to simulated chart when no snapshots exist (Task 5)
- ✅ Flyway migration (Task 1)

**Placeholder scan:** None found — all code blocks are complete.

**Type consistency:**
- `PortfolioHistoryPoint` Java record: `(LocalDate date, BigDecimal totalValue)` → serialized as `{"date":"2026-05-28","totalValue":5000000.00}`
- `PortfolioHistoryPoint` TypeScript: `{ date: string; totalValue: number }` ✅ matches
- `PortfolioHistoryResponse` Java: `record(List<PortfolioHistoryPoint> points)` → `{"points":[...]}`
- `PortfolioHistoryResponse` TypeScript: `{ points: PortfolioHistoryPoint[] }` ✅ matches
- `buildPortfolioChartFromHistory(history: PortfolioHistoryResponse)` called from `selectPeriod` with same type ✅

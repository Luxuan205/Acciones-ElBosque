# Yahoo Finance Market Data Integration — Design Spec

## Goal

Replace the simulated ±2% random price generator in `MarketDataIngestor` with real market data fetched from Yahoo Finance's public quote endpoint every 30 minutes, covering the 10 BVC (Bolsa de Valores de Colombia) stocks already seeded in the database.

## Architecture

A new `RealPriceRefresher` component replaces `MarketDataIngestor` entirely. It runs on a fixed schedule, calls Yahoo Finance in a single batch request for all tracked symbols, and updates both `StockSnapshot` (latest price) and `IntradayPricePoint` (intraday chart history) with real values. No price simulation remains in the system.

**Tech Stack:** Spring Boot 3, `RestClient` (included in `spring-boot-starter-web`), `@ConfigurationProperties`, Flyway (no new migration needed — schema unchanged), JUnit 5 + `MockRestServiceServer` for tests.

---

## Components

### New: `YahooFinanceProperties`

`@ConfigurationProperties(prefix = "app.market.yahoo-finance")` bound to `application.yaml`.

Fields:
- `String baseUrl` — Yahoo Finance API base URL
- `long refreshIntervalMs` — polling interval in milliseconds (default 1 800 000 = 30 min)
- `Map<String, String> symbolMapping` — internal symbol → Yahoo Finance symbol (e.g. `ECOPETROL` → `ECOPETROL.CL`)

### New: `MarketQuote` (DTO)

Internal record used only within the market-data module:

```java
public record MarketQuote(
    String symbol,           // Yahoo Finance symbol (e.g. ECOPETROL.CL)
    BigDecimal price,
    BigDecimal previousClose,
    BigDecimal change,
    BigDecimal changePct,
    long volume
) {}
```

### New: `YahooFinanceClient`

Spring `@Component`. Injected with a `RestClient` built from `YahooFinanceProperties.baseUrl`.

**Method:** `List<MarketQuote> fetchQuotes(Collection<String> yahooSymbols)`

- Builds one GET request: `/v7/finance/quote?symbols=SYM1,SYM2,...`
- Parses `quoteResponse.result[]` array from the JSON response
- Maps each result element to a `MarketQuote` using fields:
  - `symbol` → `MarketQuote.symbol`
  - `regularMarketPrice` → `price`
  - `regularMarketPreviousClose` → `previousClose`
  - `regularMarketChange` → `change`
  - `regularMarketChangePercent` → `changePct`
  - `regularMarketVolume` → `volume`
- Returns only the elements that parsed successfully; logs a warning for any element missing required fields.
- Throws `YahooFinanceException` (unchecked) on HTTP error or network timeout, to be caught by the caller.

**RestClient configuration:** timeout 10 s connect / 15 s read, set via `RestClient.Builder`.

### New: `RealPriceRefresher`

Spring `@Component`. Replaces `MarketDataIngestor`.

**Schedule:** `@Scheduled(fixedDelayString = "${app.market.yahoo-finance.refresh-interval-ms:1800000}")`

**Logic:**

```
1. Build the list of Yahoo symbols from YahooFinanceProperties.symbolMapping values.
2. Call YahooFinanceClient.fetchQuotes(yahooSymbols).
   - On YahooFinanceException: log warning, return early (existing snapshots unchanged).
3. Build a reverse map: yahooSymbol → internalSymbol.
4. For each MarketQuote in the result:
   a. Look up internalSymbol via the reverse map.
   b. Find the StockSnapshot by internalSymbol (skip if not found, log warning).
   c. Update: currentPrice, previousClose, dayChange, dayChangePct, volume.
   d. Save snapshot.
   e. Persist one IntradayPricePoint(symbol=internalSymbol, timestamp=now, price, volume).
5. Log info: "RealPriceRefresher: updated N snapshots from Yahoo Finance".
```

**Market-close purge:** Replicates the logic from the old `MarketDataIngestor` — when the market transitions from open to closed, delete intraday points older than start-of-day. Uses the existing `MarketStatusService.isMarketOpen()`.

### Modified: `application.yaml`

Add under the existing `app:` block:

```yaml
app:
  market:
    yahoo-finance:
      base-url: https://query1.finance.yahoo.com
      refresh-interval-ms: 1800000
      symbol-mapping:
        PFBCOLOM:  PFBCOLOM.CL
        NUTRESA:   NUTRESA.CL
        ISA:       ISA.CL
        ECOPETROL: ECOPETROL.CL
        CEMARGOS:  CEMARGOS.CL
        GRUPOSURA: GRUPOSURA.CL
        EXITO:     EXITO.CL
        ETB:       ETB.CL
        PFDAVVNDA: PFDAVVNDA.CL
        CLH:       CLH.CL
```

### Deleted: `MarketDataIngestor`

Remove the class entirely. The `@EnableScheduling` annotation (wherever it lives) stays — it is still needed by `RealPriceRefresher`.

### No database changes

The `stock_snapshot` and `intraday_price_point` schemas are unchanged. No new Flyway migration.

---

## Error Handling

| Failure scenario | Behavior |
|---|---|
| Yahoo Finance HTTP error (4xx / 5xx) | Log warning with status code; skip cycle; existing snapshots unchanged |
| Network timeout | Log warning; skip cycle |
| JSON parsing error | Log error with raw body excerpt; skip cycle |
| Symbol not in response (did not trade that session) | Log warning for that symbol; update remaining symbols |
| Symbol not found in `StockSnapshot` table | Log warning; skip that quote |

Errors never propagate out of `RealPriceRefresher.refresh()` — the scheduler must not crash.

---

## Testing

**`YahooFinanceClientTest`** (unit):
- Use `MockRestServiceServer` to stub the `/v7/finance/quote` endpoint.
- Test: happy path returns correctly mapped `MarketQuote` list.
- Test: HTTP 500 throws `YahooFinanceException`.
- Test: response missing `regularMarketPrice` field → that element is skipped, others returned.

**`RealPriceRefresherTest`** (unit):
- Mock `YahooFinanceClient`, `StockSnapshotRepository`, `IntradayPricePointRepository`, `MarketStatusService`.
- Test: successful refresh updates N snapshots and saves N intraday points.
- Test: `YahooFinanceException` → method returns without saving anything.
- Test: quote symbol not in reverse map → skipped without error.
- Test: market transitions open→closed → intraday purge called.

---

## Out of Scope

- WebSocket / push-based real-time streaming (would require a paid provider).
- Historical data backfill (only forward-looking from integration date).
- Adding new symbols dynamically at runtime (requires restart to pick up yaml changes).
- UI changes — the Angular frontend already consumes `StockSummaryDto` and `StockDetailDto` unchanged.

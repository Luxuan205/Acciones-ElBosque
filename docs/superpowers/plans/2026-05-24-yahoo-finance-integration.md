# Yahoo Finance Market Data Integration — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the random price simulator (`MarketDataIngestor`) with a real Yahoo Finance HTTP client that refreshes BVC stock prices every 30 minutes.

**Architecture:** A new `RealPriceRefresher` component runs on a fixed schedule, calls Yahoo Finance's `/v7/finance/quote` endpoint in a single batch request for all 10 BVC symbols, and updates both `StockSnapshot` and `IntradayPricePoint` with the real values returned. No simulation logic remains. A `YahooFinanceClient` handles the HTTP call and JSON parsing; `YahooFinanceProperties` holds configuration from `application.yaml`.

**Tech Stack:** Spring Boot 4, `RestTemplate` + `SimpleClientHttpRequestFactory` (timeouts), `@ConfigurationProperties`, JUnit 5, Mockito, `MockRestServiceServer`.

---

## File Map

| Action | File |
|--------|------|
| Create | `backend/market-data/src/main/java/com/accioneselbosque/market_data_service/exception/YahooFinanceException.java` |
| Create | `backend/market-data/src/main/java/com/accioneselbosque/market_data_service/dto/MarketQuote.java` |
| Create | `backend/market-data/src/main/java/com/accioneselbosque/market_data_service/config/YahooFinanceProperties.java` |
| Create | `backend/market-data/src/main/java/com/accioneselbosque/market_data_service/client/YahooFinanceClient.java` |
| Create | `backend/market-data/src/main/java/com/accioneselbosque/market_data_service/service/RealPriceRefresher.java` |
| Create (test) | `backend/market-data/src/test/java/com/accioneselbosque/market_data_service/client/YahooFinanceClientTest.java` |
| Create (test) | `backend/market-data/src/test/java/com/accioneselbosque/market_data_service/service/RealPriceRefresherTest.java` |
| Modify | `backend/app/src/main/resources/application.yaml` |
| Delete | `backend/market-data/src/main/java/com/accioneselbosque/market_data_service/service/MarketDataIngestor.java` |

---

## Task 1: Foundation types — Exception, DTO, Properties

**Files:**
- Create: `backend/market-data/src/main/java/com/accioneselbosque/market_data_service/exception/YahooFinanceException.java`
- Create: `backend/market-data/src/main/java/com/accioneselbosque/market_data_service/dto/MarketQuote.java`
- Create: `backend/market-data/src/main/java/com/accioneselbosque/market_data_service/config/YahooFinanceProperties.java`

- [ ] **Step 1: Create `YahooFinanceException`**

```java
package com.accioneselbosque.market_data_service.exception;

public class YahooFinanceException extends RuntimeException {
    public YahooFinanceException(String message) {
        super(message);
    }
    public YahooFinanceException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

- [ ] **Step 2: Create `MarketQuote` record**

```java
package com.accioneselbosque.market_data_service.dto;

import java.math.BigDecimal;

public record MarketQuote(
    String symbol,
    BigDecimal price,
    BigDecimal previousClose,
    BigDecimal change,
    BigDecimal changePct,
    long volume
) {}
```

- [ ] **Step 3: Create `YahooFinanceProperties`**

```java
package com.accioneselbosque.market_data_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "app.market.yahoo-finance")
@Getter
@Setter
public class YahooFinanceProperties {
    private String baseUrl = "https://query1.finance.yahoo.com";
    private long refreshIntervalMs = 1_800_000L;
    private Map<String, String> symbolMapping = new HashMap<>();
}
```

- [ ] **Step 4: Verify it compiles**

Run from `backend/`:
```powershell
mvn compile -pl market-data -am -q
```
Expected: `BUILD SUCCESS` with no errors.

- [ ] **Step 5: Commit**

```powershell
git add backend/market-data/src/main/java/com/accioneselbosque/market_data_service/exception/YahooFinanceException.java `
      backend/market-data/src/main/java/com/accioneselbosque/market_data_service/dto/MarketQuote.java `
      backend/market-data/src/main/java/com/accioneselbosque/market_data_service/config/YahooFinanceProperties.java
git commit -m "feat(market-data): agregar tipos base — YahooFinanceException, MarketQuote, YahooFinanceProperties"
```

---

## Task 2: YahooFinanceClient with HTTP tests

**Files:**
- Create: `backend/market-data/src/main/java/com/accioneselbosque/market_data_service/client/YahooFinanceClient.java`
- Test: `backend/market-data/src/test/java/com/accioneselbosque/market_data_service/client/YahooFinanceClientTest.java`

The client uses `RestTemplate` (not `RestClient`) because `MockRestServiceServer` binds directly to `RestTemplate` — no base-URL ambiguity in tests.

- [ ] **Step 1: Write the failing tests**

Create the directory `backend/market-data/src/test/java/com/accioneselbosque/market_data_service/client/`, then create `YahooFinanceClientTest.java`:

```java
package com.accioneselbosque.market_data_service.client;

import com.accioneselbosque.market_data_service.dto.MarketQuote;
import com.accioneselbosque.market_data_service.exception.YahooFinanceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class YahooFinanceClientTest {

    private static final String BASE_URL = "https://query1.finance.yahoo.com";

    private MockRestServiceServer mockServer;
    private YahooFinanceClient client;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        client = new YahooFinanceClient(restTemplate, BASE_URL);
    }

    @Test
    void fetchQuotes_happyPath_returnsMappedQuotes() {
        String body = """
            {
              "quoteResponse": {
                "result": [
                  {
                    "symbol": "ECOPETROL.CL",
                    "regularMarketPrice": 1950.0,
                    "regularMarketPreviousClose": 1930.0,
                    "regularMarketChange": 20.0,
                    "regularMarketChangePercent": 1.0363,
                    "regularMarketVolume": 5000000
                  }
                ],
                "error": null
              }
            }
            """;
        mockServer.expect(requestTo(BASE_URL + "/v7/finance/quote?symbols=ECOPETROL.CL"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        List<MarketQuote> quotes = client.fetchQuotes(List.of("ECOPETROL.CL"));

        assertThat(quotes).hasSize(1);
        assertThat(quotes.get(0).symbol()).isEqualTo("ECOPETROL.CL");
        assertThat(quotes.get(0).price()).isEqualByComparingTo("1950.00");
        assertThat(quotes.get(0).previousClose()).isEqualByComparingTo("1930.00");
        assertThat(quotes.get(0).change()).isEqualByComparingTo("20.00");
        assertThat(quotes.get(0).volume()).isEqualTo(5_000_000L);
        mockServer.verify();
    }

    @Test
    void fetchQuotes_httpError_throwsYahooFinanceException() {
        mockServer.expect(requestTo(BASE_URL + "/v7/finance/quote?symbols=ECOPETROL.CL"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.fetchQuotes(List.of("ECOPETROL.CL")))
                .isInstanceOf(YahooFinanceException.class);
        mockServer.verify();
    }

    @Test
    void fetchQuotes_missingPrice_skipsElementAndReturnsRest() {
        String body = """
            {
              "quoteResponse": {
                "result": [
                  {
                    "symbol": "ECOPETROL.CL",
                    "regularMarketPrice": 1950.0,
                    "regularMarketPreviousClose": 1930.0,
                    "regularMarketChange": 20.0,
                    "regularMarketChangePercent": 1.0363,
                    "regularMarketVolume": 5000000
                  },
                  {
                    "symbol": "PFBCOLOM.CL",
                    "regularMarketPreviousClose": 39150.0
                  }
                ],
                "error": null
              }
            }
            """;
        mockServer.expect(requestTo(BASE_URL + "/v7/finance/quote?symbols=ECOPETROL.CL,PFBCOLOM.CL"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        List<MarketQuote> quotes = client.fetchQuotes(List.of("ECOPETROL.CL", "PFBCOLOM.CL"));

        assertThat(quotes).hasSize(1);
        assertThat(quotes.get(0).symbol()).isEqualTo("ECOPETROL.CL");
        mockServer.verify();
    }
}
```

- [ ] **Step 2: Run tests — verify they fail with "class not found"**

```powershell
mvn test -pl market-data -am -q -Dtest=YahooFinanceClientTest
```
Expected: `COMPILATION ERROR` — `YahooFinanceClient` does not exist yet.

- [ ] **Step 3: Create `YahooFinanceClient`**

Create directory `backend/market-data/src/main/java/com/accioneselbosque/market_data_service/client/`, then create `YahooFinanceClient.java`:

```java
package com.accioneselbosque.market_data_service.client;

import com.accioneselbosque.market_data_service.config.YahooFinanceProperties;
import com.accioneselbosque.market_data_service.dto.MarketQuote;
import com.accioneselbosque.market_data_service.exception.YahooFinanceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class YahooFinanceClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    // Production constructor — Spring calls this
    public YahooFinanceClient(YahooFinanceProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(15_000);
        this.restTemplate = new RestTemplate(factory);
        this.baseUrl = props.getBaseUrl();
    }

    // Test constructor — package-private
    YahooFinanceClient(RestTemplate restTemplate, String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public List<MarketQuote> fetchQuotes(Collection<String> yahooSymbols) {
        if (yahooSymbols.isEmpty()) return List.of();

        String symbolParam = String.join(",", yahooSymbols);
        String url = baseUrl + "/v7/finance/quote?symbols=" + symbolParam;

        YahooFinanceResponse response;
        try {
            response = restTemplate.getForObject(url, YahooFinanceResponse.class);
        } catch (HttpStatusCodeException e) {
            throw new YahooFinanceException("Yahoo Finance HTTP error: " + e.getStatusCode(), e);
        } catch (ResourceAccessException e) {
            throw new YahooFinanceException("Failed to reach Yahoo Finance", e);
        }

        if (response == null
                || response.quoteResponse() == null
                || response.quoteResponse().result() == null) {
            return List.of();
        }

        return response.quoteResponse().result().stream()
                .filter(r -> {
                    if (r.regularMarketPrice() == null) {
                        log.warn("YahooFinanceClient: skipping {} — missing regularMarketPrice", r.symbol());
                        return false;
                    }
                    return true;
                })
                .map(r -> new MarketQuote(
                        r.symbol(),
                        bd(r.regularMarketPrice(), 2),
                        r.regularMarketPreviousClose() != null ? bd(r.regularMarketPreviousClose(), 2) : null,
                        r.regularMarketChange() != null ? bd(r.regularMarketChange(), 2) : BigDecimal.ZERO,
                        r.regularMarketChangePercent() != null ? bd(r.regularMarketChangePercent(), 4) : BigDecimal.ZERO,
                        r.regularMarketVolume() != null ? r.regularMarketVolume() : 0L
                ))
                .collect(Collectors.toList());
    }

    private static BigDecimal bd(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP);
    }

    // ── Inner records for Yahoo Finance JSON shape ──────────────────────────

    private record YahooResult(
            String symbol,
            Double regularMarketPrice,
            Double regularMarketPreviousClose,
            Double regularMarketChange,
            Double regularMarketChangePercent,
            Long regularMarketVolume
    ) {}

    private record YahooQuoteResponse(List<YahooResult> result) {}

    private record YahooFinanceResponse(YahooQuoteResponse quoteResponse) {}
}
```

- [ ] **Step 4: Run tests — verify they pass**

```powershell
mvn test -pl market-data -am -q -Dtest=YahooFinanceClientTest
```
Expected: `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0` — `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```powershell
git add backend/market-data/src/main/java/com/accioneselbosque/market_data_service/client/ `
      backend/market-data/src/test/java/com/accioneselbosque/market_data_service/client/
git commit -m "feat(market-data): agregar YahooFinanceClient con tests MockRestServiceServer"
```

---

## Task 3: RealPriceRefresher with Mockito tests

**Files:**
- Create: `backend/market-data/src/main/java/com/accioneselbosque/market_data_service/service/RealPriceRefresher.java`
- Test: `backend/market-data/src/test/java/com/accioneselbosque/market_data_service/service/RealPriceRefresherTest.java`

- [ ] **Step 1: Write the failing tests**

Create `RealPriceRefresherTest.java`:

```java
package com.accioneselbosque.market_data_service.service;

import com.accioneselbosque.configuration.service.MarketStatusService;
import com.accioneselbosque.market_data_service.client.YahooFinanceClient;
import com.accioneselbosque.market_data_service.config.YahooFinanceProperties;
import com.accioneselbosque.market_data_service.dto.MarketQuote;
import com.accioneselbosque.market_data_service.exception.YahooFinanceException;
import com.accioneselbosque.market_data_service.model.IntradayPricePoint;
import com.accioneselbosque.market_data_service.model.StockSnapshot;
import com.accioneselbosque.market_data_service.repository.IntradayPricePointRepository;
import com.accioneselbosque.market_data_service.repository.StockSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RealPriceRefresherTest {

    @Mock private YahooFinanceClient yahooClient;
    @Mock private YahooFinanceProperties properties;
    @Mock private StockSnapshotRepository snapshotRepository;
    @Mock private IntradayPricePointRepository intradayRepository;
    @Mock private MarketStatusService marketStatusService;

    @InjectMocks private RealPriceRefresher refresher;

    private static final Map<String, String> MAPPING = Map.of(
            "ECOPETROL", "ECOPETROL.CL",
            "PFBCOLOM",  "PFBCOLOM.CL"
    );

    @BeforeEach
    void setUp() {
        when(properties.getSymbolMapping()).thenReturn(MAPPING);
        when(marketStatusService.isMarketOpen()).thenReturn(true);
    }

    @Test
    void refresh_success_updatesSnapshotAndSavesIntradayPoint() {
        MarketQuote quote = new MarketQuote(
                "ECOPETROL.CL",
                new BigDecimal("1950.00"),
                new BigDecimal("1930.00"),
                new BigDecimal("20.00"),
                new BigDecimal("1.0363"),
                5_000_000L
        );
        StockSnapshot snap = new StockSnapshot();
        snap.setSymbol("ECOPETROL");
        snap.setCurrentPrice(new BigDecimal("1930.00"));

        when(yahooClient.fetchQuotes(anyCollection())).thenReturn(List.of(quote));
        when(snapshotRepository.findBySymbol("ECOPETROL")).thenReturn(Optional.of(snap));

        refresher.refresh();

        assertThat(snap.getCurrentPrice()).isEqualByComparingTo("1950.00");
        assertThat(snap.getDayChange()).isEqualByComparingTo("20.00");
        assertThat(snap.getDayChangePct()).isEqualByComparingTo("1.0363");
        assertThat(snap.getVolume()).isEqualTo(5_000_000L);
        verify(snapshotRepository).save(snap);
        verify(intradayRepository).save(any(IntradayPricePoint.class));
    }

    @Test
    void refresh_yahooException_returnsWithoutSaving() {
        when(yahooClient.fetchQuotes(anyCollection()))
                .thenThrow(new YahooFinanceException("timeout"));

        refresher.refresh();

        verify(snapshotRepository, never()).save(any());
        verify(intradayRepository, never()).save(any());
    }

    @Test
    void refresh_yahooSymbolNotInReverseMap_skipsWithoutError() {
        MarketQuote unknown = new MarketQuote(
                "UNKNOWN.CL",
                new BigDecimal("100.00"),
                new BigDecimal("99.00"),
                new BigDecimal("1.00"),
                new BigDecimal("1.01"),
                1_000L
        );
        when(yahooClient.fetchQuotes(anyCollection())).thenReturn(List.of(unknown));

        refresher.refresh();

        verify(snapshotRepository, never()).save(any());
    }

    @Test
    void refresh_marketTransitionsToClosed_purgesIntradayData() {
        // First call: market open
        when(yahooClient.fetchQuotes(anyCollection())).thenReturn(List.of());
        refresher.refresh();

        // Second call: market now closed
        when(marketStatusService.isMarketOpen()).thenReturn(false);
        refresher.refresh();

        verify(intradayRepository).deleteByTimestampBefore(any(LocalDateTime.class));
    }
}
```

- [ ] **Step 2: Run tests — verify they fail with "class not found"**

```powershell
mvn test -pl market-data -am -q -Dtest=RealPriceRefresherTest
```
Expected: `COMPILATION ERROR` — `RealPriceRefresher` does not exist yet.

- [ ] **Step 3: Create `RealPriceRefresher`**

```java
package com.accioneselbosque.market_data_service.service;

import com.accioneselbosque.configuration.service.MarketStatusService;
import com.accioneselbosque.market_data_service.client.YahooFinanceClient;
import com.accioneselbosque.market_data_service.config.YahooFinanceProperties;
import com.accioneselbosque.market_data_service.dto.MarketQuote;
import com.accioneselbosque.market_data_service.exception.YahooFinanceException;
import com.accioneselbosque.market_data_service.model.IntradayPricePoint;
import com.accioneselbosque.market_data_service.model.StockSnapshot;
import com.accioneselbosque.market_data_service.repository.IntradayPricePointRepository;
import com.accioneselbosque.market_data_service.repository.StockSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class RealPriceRefresher {

    private final YahooFinanceClient yahooClient;
    private final YahooFinanceProperties properties;
    private final StockSnapshotRepository snapshotRepository;
    private final IntradayPricePointRepository intradayRepository;
    private final MarketStatusService marketStatusService;

    private boolean wasMarketOpen = false;

    @Scheduled(fixedDelayString = "${app.market.yahoo-finance.refresh-interval-ms:1800000}")
    @Transactional
    public void refresh() {
        boolean marketOpen = marketStatusService.isMarketOpen();

        if (wasMarketOpen && !marketOpen) {
            intradayRepository.deleteByTimestampBefore(LocalDate.now().atStartOfDay());
            log.info("RealPriceRefresher: purged intraday data on market close");
        }
        wasMarketOpen = marketOpen;

        Map<String, String> mapping = properties.getSymbolMapping();
        Collection<String> yahooSymbols = mapping.values();

        if (yahooSymbols.isEmpty()) {
            log.warn("RealPriceRefresher: no symbol mapping configured, skipping");
            return;
        }

        List<MarketQuote> quotes;
        try {
            quotes = yahooClient.fetchQuotes(yahooSymbols);
        } catch (YahooFinanceException e) {
            log.warn("RealPriceRefresher: fetch failed — {}. Keeping existing prices.", e.getMessage());
            return;
        }

        Map<String, String> reverseMap = mapping.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

        LocalDateTime now = LocalDateTime.now();
        int updated = 0;

        for (MarketQuote quote : quotes) {
            String internalSymbol = reverseMap.get(quote.symbol());
            if (internalSymbol == null) {
                log.warn("RealPriceRefresher: no reverse mapping for {}, skipping", quote.symbol());
                continue;
            }

            Optional<StockSnapshot> snapOpt = snapshotRepository.findBySymbol(internalSymbol);
            if (snapOpt.isEmpty()) {
                log.warn("RealPriceRefresher: snapshot not found for {}, skipping", internalSymbol);
                continue;
            }

            StockSnapshot snap = snapOpt.get();
            snap.setCurrentPrice(quote.price());
            if (quote.previousClose() != null) snap.setPreviousClose(quote.previousClose());
            snap.setDayChange(quote.change());
            snap.setDayChangePct(quote.changePct());
            snap.setVolume(quote.volume());
            snapshotRepository.save(snap);

            try {
                IntradayPricePoint point = new IntradayPricePoint();
                point.setSymbol(internalSymbol);
                point.setTimestamp(now);
                point.setPrice(quote.price());
                point.setVolume(quote.volume());
                intradayRepository.save(point);
            } catch (DataIntegrityViolationException e) {
                log.warn("RealPriceRefresher: duplicate intraday point for {} at {}, skipping",
                        internalSymbol, now);
            }

            updated++;
        }

        log.info("RealPriceRefresher: updated {} snapshots from Yahoo Finance", updated);
    }
}
```

- [ ] **Step 4: Run tests — verify they pass**

```powershell
mvn test -pl market-data -am -q -Dtest=RealPriceRefresherTest
```
Expected: `Tests run: 4, Failures: 0, Errors: 0, Skipped: 0` — `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```powershell
git add backend/market-data/src/main/java/com/accioneselbosque/market_data_service/service/RealPriceRefresher.java `
      backend/market-data/src/test/java/com/accioneselbosque/market_data_service/service/RealPriceRefresherTest.java
git commit -m "feat(market-data): agregar RealPriceRefresher con tests Mockito"
```

---

## Task 4: Wire up configuration and remove the simulator

**Files:**
- Modify: `backend/app/src/main/resources/application.yaml`
- Delete: `backend/market-data/src/main/java/com/accioneselbosque/market_data_service/service/MarketDataIngestor.java`

- [ ] **Step 1: Add Yahoo Finance config to `application.yaml`**

Open `backend/app/src/main/resources/application.yaml`. At the end of the file, inside the existing `app:` block (after the `login:` section), add:

```yaml
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

The indentation must align with `jwt:`, `verification:`, and `login:` which are all nested under `app:`.

- [ ] **Step 2: Delete `MarketDataIngestor.java`**

```powershell
Remove-Item backend/market-data/src/main/java/com/accioneselbosque/market_data_service/service/MarketDataIngestor.java
```

- [ ] **Step 3: Run the full market-data test suite**

```powershell
mvn test -pl market-data -am -q
```
Expected: all tests pass — `BUILD SUCCESS`. If any test imports `MarketDataIngestor`, update it to remove the import.

- [ ] **Step 4: Verify the application builds (compile-only check)**

```powershell
mvn package -pl app -am -q -DskipTests
```
Expected: `BUILD SUCCESS`. This confirms `YahooFinanceProperties` is picked up by the Spring context via `@Component` + `@ConfigurationProperties`.

- [ ] **Step 5: Commit**

```powershell
git add backend/app/src/main/resources/application.yaml
git rm backend/market-data/src/main/java/com/accioneselbosque/market_data_service/service/MarketDataIngestor.java
git commit -m "feat(market-data): conectar Yahoo Finance y eliminar simulador de precios"
```

---

## Verification checklist

After all tasks are complete:

- [ ] `mvn test -pl market-data -am` → all tests green
- [ ] `mvn package -pl app -am -DskipTests` → BUILD SUCCESS
- [ ] No references to `MarketDataIngestor` remain in the codebase (`grep -r "MarketDataIngestor" backend/` returns nothing)
- [ ] `application.yaml` contains the `app.market.yahoo-finance` block
- [ ] Log output on startup shows `RealPriceRefresher` scheduled (no `MarketDataIngestor` log lines)

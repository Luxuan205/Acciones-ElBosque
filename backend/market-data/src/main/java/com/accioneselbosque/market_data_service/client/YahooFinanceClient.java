package com.accioneselbosque.market_data_service.client;

import com.accioneselbosque.market_data_service.config.YahooFinanceProperties;
import com.accioneselbosque.market_data_service.dto.MarketQuote;
import com.accioneselbosque.market_data_service.exception.YahooFinanceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
@Slf4j
public class YahooFinanceClient {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public YahooFinanceClient(YahooFinanceProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(15_000);
        this.restTemplate = new RestTemplate(factory);
        this.baseUrl = props.getBaseUrl();
    }

    YahooFinanceClient(RestTemplate restTemplate, String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    /**
     * Fetches quotes using the Yahoo Finance v8/finance/chart endpoint,
     * one request per symbol. The chart API does not require authentication
     * and works with a browser User-Agent header.
     */
    public List<MarketQuote> fetchQuotes(Collection<String> yahooSymbols) {
        if (yahooSymbols.isEmpty()) return List.of();

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, USER_AGENT);
        headers.set(HttpHeaders.ACCEPT, "application/json");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        List<MarketQuote> result = new ArrayList<>();
        for (String symbol : yahooSymbols) {
            try {
                MarketQuote quote = fetchOne(symbol, entity);
                if (quote != null) result.add(quote);
            } catch (YahooFinanceException e) {
                throw e;
            } catch (Exception e) {
                log.warn("YahooFinanceClient: skipping {} — {}", symbol, e.getMessage());
            }
        }
        return result;
    }

    private MarketQuote fetchOne(String symbol, HttpEntity<Void> entity) {
        String url = baseUrl + "/v8/finance/chart/" + symbol + "?range=1d&interval=1d";
        String raw;
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);
            raw = response.getBody();
        } catch (HttpStatusCodeException e) {
            throw new YahooFinanceException("Yahoo Finance HTTP error for " + symbol + ": " + e.getStatusCode(), e);
        } catch (ResourceAccessException e) {
            throw new YahooFinanceException("Failed to reach Yahoo Finance", e);
        }

        if (raw == null) return null;

        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode resultArr = root.path("chart").path("result");
            if (resultArr.isMissingNode() || resultArr.isNull() || resultArr.isEmpty()) {
                JsonNode err = root.path("chart").path("error");
                log.warn("YahooFinanceClient: no data for {} — {}", symbol, err.toString());
                return null;
            }

            JsonNode meta = resultArr.get(0).path("meta");
            JsonNode priceNode = meta.get("regularMarketPrice");
            if (priceNode == null || priceNode.isNull()) {
                log.warn("YahooFinanceClient: skipping {} — missing regularMarketPrice", symbol);
                return null;
            }

            BigDecimal price   = bd(priceNode.asDouble(), 2);
            BigDecimal prevClose = meta.has("chartPreviousClose")
                    ? bd(meta.get("chartPreviousClose").asDouble(), 2) : null;
            long volume = meta.has("regularMarketVolume")
                    ? meta.get("regularMarketVolume").asLong(0L) : 0L;

            BigDecimal change    = prevClose != null ? price.subtract(prevClose) : BigDecimal.ZERO;
            BigDecimal changePct = prevClose != null && prevClose.compareTo(BigDecimal.ZERO) != 0
                    ? change.divide(prevClose, 6, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).setScale(4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            return new MarketQuote(symbol, price, prevClose, change, changePct, volume);

        } catch (Exception e) {
            log.warn("YahooFinanceClient: failed to parse response for {} — {}", symbol, e.getMessage());
            return null;
        }
    }

    private static BigDecimal bd(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP);
    }
}

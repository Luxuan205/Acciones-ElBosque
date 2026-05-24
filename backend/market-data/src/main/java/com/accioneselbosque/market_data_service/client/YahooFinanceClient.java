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
import org.springframework.web.util.UriComponentsBuilder;

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
        String url = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/v7/finance/quote")
                .queryParam("symbols", symbolParam)
                .toUriString();

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

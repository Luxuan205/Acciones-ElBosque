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
              "chart": {
                "result": [
                  {
                    "meta": {
                      "regularMarketPrice": 1950.0,
                      "chartPreviousClose": 1930.0,
                      "regularMarketVolume": 5000000
                    }
                  }
                ],
                "error": null
              }
            }
            """;
        mockServer.expect(requestTo(BASE_URL + "/v8/finance/chart/ECOPETROL.CL?range=1d&interval=1d"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        List<MarketQuote> quotes = client.fetchQuotes(List.of("ECOPETROL.CL"));

        assertThat(quotes).hasSize(1);
        assertThat(quotes.get(0).symbol()).isEqualTo("ECOPETROL.CL");
        assertThat(quotes.get(0).price()).isEqualByComparingTo("1950.00");
        assertThat(quotes.get(0).previousClose()).isEqualByComparingTo("1930.00");
        assertThat(quotes.get(0).change()).isEqualByComparingTo("20.00");
        assertThat(quotes.get(0).changePct()).isEqualByComparingTo("1.0363");
        assertThat(quotes.get(0).volume()).isEqualTo(5_000_000L);
        mockServer.verify();
    }

    @Test
    void fetchQuotes_httpError_throwsYahooFinanceException() {
        mockServer.expect(requestTo(BASE_URL + "/v8/finance/chart/ECOPETROL.CL?range=1d&interval=1d"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.fetchQuotes(List.of("ECOPETROL.CL")))
                .isInstanceOf(YahooFinanceException.class);
        mockServer.verify();
    }

    @Test
    void fetchQuotes_missingPrice_skipsElementAndReturnsRest() {
        String validBody = """
            {
              "chart": {
                "result": [
                  {
                    "meta": {
                      "regularMarketPrice": 1950.0,
                      "chartPreviousClose": 1930.0,
                      "regularMarketVolume": 5000000
                    }
                  }
                ],
                "error": null
              }
            }
            """;
        String missingPriceBody = """
            {
              "chart": {
                "result": [
                  {
                    "meta": {
                      "chartPreviousClose": 100.0
                    }
                  }
                ],
                "error": null
              }
            }
            """;
        mockServer.expect(requestTo(BASE_URL + "/v8/finance/chart/ECOPETROL.CL?range=1d&interval=1d"))
                .andRespond(withSuccess(validBody, MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(BASE_URL + "/v8/finance/chart/GEB.CL?range=1d&interval=1d"))
                .andRespond(withSuccess(missingPriceBody, MediaType.APPLICATION_JSON));

        List<MarketQuote> quotes = client.fetchQuotes(List.of("ECOPETROL.CL", "GEB.CL"));

        assertThat(quotes).hasSize(1);
        assertThat(quotes.get(0).symbol()).isEqualTo("ECOPETROL.CL");
        mockServer.verify();
    }
}

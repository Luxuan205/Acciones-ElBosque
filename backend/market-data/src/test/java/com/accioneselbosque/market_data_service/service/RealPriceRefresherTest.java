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
        verify(intradayRepository).saveAndFlush(any(IntradayPricePoint.class));
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

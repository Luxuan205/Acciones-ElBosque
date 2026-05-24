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

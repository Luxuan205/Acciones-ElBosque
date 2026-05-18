package com.accioneselbosque.market_data_service.service;

import com.accioneselbosque.configuration.service.MarketStatusService;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MarketDataIngestor {

    private final StockSnapshotRepository snapshotRepository;
    private final IntradayPricePointRepository intradayRepository;
    private final MarketStatusService marketStatusService;
    private final SecureRandom random = new SecureRandom();

    private boolean wasMarketOpen = false;

    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void ingestPrices() {
        boolean marketOpen = marketStatusService.isMarketOpen();

        // Purge intraday when market transitions from open to closed
        if (wasMarketOpen && !marketOpen) {
            intradayRepository.deleteByTimestampBefore(LocalDate.now().atStartOfDay());
            log.info("MarketDataIngestor: purged intraday data on market close");
        }
        wasMarketOpen = marketOpen;

        if (!marketOpen) return;

        List<StockSnapshot> snapshots = snapshotRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (StockSnapshot snap : snapshots) {
            // Simulate price variation of ±2%
            double variation = (random.nextDouble() * 4.0 - 2.0) / 100.0;
            BigDecimal newPrice = snap.getCurrentPrice()
                    .multiply(BigDecimal.valueOf(1 + variation))
                    .setScale(2, RoundingMode.HALF_UP);

            snap.setDayChange(newPrice.subtract(snap.getPreviousClose()).setScale(2, RoundingMode.HALF_UP));
            if (snap.getPreviousClose() != null && snap.getPreviousClose().compareTo(BigDecimal.ZERO) > 0) {
                snap.setDayChangePct(snap.getDayChange()
                        .divide(snap.getPreviousClose(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)));
            }
            snap.setCurrentPrice(newPrice);

            // Record intraday point
            try {
                IntradayPricePoint point = new IntradayPricePoint();
                point.setSymbol(snap.getSymbol());
                point.setTimestamp(now);
                point.setPrice(newPrice);
                point.setVolume(snap.getVolume());
                intradayRepository.save(point);
            } catch (DataIntegrityViolationException e) {
                log.warn("MarketDataIngestor: duplicate intraday point for symbol={} at timestamp={}, skipping",
                        snap.getSymbol(), now);
            }
        }
        snapshotRepository.saveAll(snapshots);
    }
}

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
    @Scheduled(cron = "0 0 23 * * *", zone = "UTC")
    public void scheduleDailySnapshot() {
        log.info("PortfolioSnapshotScheduler: starting daily snapshot");
        snapshotService.takeSnapshot();
    }
}

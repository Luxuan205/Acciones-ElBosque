package com.accioneselbosque.notifications.service;

import com.accioneselbosque.market_data_service.service.StockSnapshotService;
import com.accioneselbosque.notifications.model.PriceAlert;
import com.accioneselbosque.notifications.model.PriceAlertStatus;
import com.accioneselbosque.notifications.model.PriceAlertTriggeredEvent;
import com.accioneselbosque.notifications.model.PriceAlertType;
import com.accioneselbosque.notifications.repository.PriceAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PriceAlertEvaluationJob {

    private final PriceAlertRepository priceAlertRepository;
    private final StockSnapshotService stockSnapshotService;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(fixedDelay = 30_000)
    @Transactional
    public void evaluateAlerts() {
        List<PriceAlert> activeAlerts = priceAlertRepository.findByStatus(PriceAlertStatus.ACTIVE);
        int triggered = 0;

        for (PriceAlert alert : activeAlerts) {
            Optional<BigDecimal> currentPriceOpt = stockSnapshotService.findBySymbol(alert.getSymbol())
                    .map(s -> s.getCurrentPrice());

            if (currentPriceOpt.isEmpty()) {
                continue;
            }

            BigDecimal currentPrice = currentPriceOpt.get();
            boolean shouldTrigger = false;

            if (alert.getAlertType() == PriceAlertType.ABSOLUTE) {
                shouldTrigger = currentPrice.compareTo(alert.getThreshold()) >= 0;
            } else if (alert.getAlertType() == PriceAlertType.PERCENTAGE) {
                if (alert.getReferencePrice() != null
                        && alert.getReferencePrice().compareTo(BigDecimal.ZERO) != 0) {
                    BigDecimal diff = currentPrice.subtract(alert.getReferencePrice()).abs();
                    BigDecimal pctChange = diff.divide(alert.getReferencePrice(), 10, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));
                    shouldTrigger = pctChange.compareTo(alert.getThreshold()) >= 0;
                }
            }

            if (shouldTrigger) {
                alert.setStatus(PriceAlertStatus.TRIGGERED);
                alert.setTriggeredAt(LocalDateTime.now());
                priceAlertRepository.save(alert);

                PriceAlertTriggeredEvent event = PriceAlertTriggeredEvent.builder()
                        .alertId(alert.getId())
                        .investorId(alert.getInvestorId())
                        .symbol(alert.getSymbol())
                        .currentPrice(currentPrice)
                        .threshold(alert.getThreshold())
                        .alertType(alert.getAlertType())
                        .build();
                eventPublisher.publishEvent(event);
                triggered++;
                log.info("Price alert {} triggered for investor {} on symbol {} at price {}",
                        alert.getId(), alert.getInvestorId(), alert.getSymbol(), currentPrice);
            }
        }

        if (triggered > 0) {
            log.info("Price alert evaluation complete: {} alert(s) triggered out of {} active",
                    triggered, activeAlerts.size());
        }
    }
}

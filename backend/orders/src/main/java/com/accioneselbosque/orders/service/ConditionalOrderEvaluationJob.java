package com.accioneselbosque.orders.service;

import com.accioneselbosque.configuration.service.MarketStatusService;
import com.accioneselbosque.market_data_service.service.StockSnapshotService;
import com.accioneselbosque.orders.model.ConditionalOrder;
import com.accioneselbosque.orders.model.ConditionalOrderStatus;
import com.accioneselbosque.orders.model.ConditionalOrderType;
import com.accioneselbosque.orders.repository.ConditionalOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConditionalOrderEvaluationJob {

    private final ConditionalOrderRepository conditionalOrderRepository;
    private final StockSnapshotService stockSnapshotService;
    private final MarketStatusService marketStatusService;

    @Autowired
    @Lazy
    private ConditionalOrderEvaluationJob self;

    @Scheduled(fixedRate = 30_000)
    public void evaluate() {
        if (!marketStatusService.isMarketOpen()) return;

        conditionalOrderRepository.findByStatus(ConditionalOrderStatus.ACTIVE)
                .forEach(order -> {
                    try {
                        self.evaluateSingle(order);
                    } catch (Exception e) {
                        log.warn("Failed to evaluate conditional order {}: {}", order.getId(), e.getMessage());
                    }
                });
    }

    @Transactional
    public void evaluateSingle(ConditionalOrder order) {
        stockSnapshotService.findBySymbol(order.getSymbol()).ifPresent(snapshot -> {
            BigDecimal price = snapshot.getCurrentPrice();
            boolean triggered = false;

            if (order.getType() == ConditionalOrderType.STOP_LOSS && price.compareTo(order.getTriggerPrice()) <= 0) {
                triggered = true;
            } else if (order.getType() == ConditionalOrderType.TAKE_PROFIT && price.compareTo(order.getTriggerPrice()) >= 0) {
                triggered = true;
            }

            if (triggered) {
                order.setStatus(ConditionalOrderStatus.TRIGGERED);
                conditionalOrderRepository.save(order);
                // Cancel OCO partner
                if (order.getOcoPartnerId() != null) {
                    conditionalOrderRepository.findById(order.getOcoPartnerId()).ifPresent(partner -> {
                        if (partner.getStatus() == ConditionalOrderStatus.ACTIVE) {
                            partner.setStatus(ConditionalOrderStatus.CANCELLED);
                            conditionalOrderRepository.save(partner);
                        }
                    });
                }
            }
        });
    }
}

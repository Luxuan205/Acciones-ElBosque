package com.accioneselbosque.orders.service;

import com.accioneselbosque.market_data_service.service.StockSnapshotService;
import com.accioneselbosque.orders.model.ConditionalOrder;
import com.accioneselbosque.orders.model.ConditionalOrderStatus;
import com.accioneselbosque.orders.model.ConditionalOrderType;
import com.accioneselbosque.orders.repository.ConditionalOrderRepository;
import com.accioneselbosque.portfolio.model.AccountBalance;
import com.accioneselbosque.portfolio.model.TransactionType;
import com.accioneselbosque.portfolio.repository.AccountBalanceRepository;
import com.accioneselbosque.portfolio.service.PositionUpdateService;
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
    private final PositionUpdateService positionUpdateService;
    private final AccountBalanceRepository accountBalanceRepository;
    private final ConditionalOrderService conditionalOrderService;

    @Autowired
    @Lazy
    private ConditionalOrderEvaluationJob self;

    @Scheduled(fixedRate = 30_000)
    public void evaluate() {

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
                // Stop-loss and take-profit are always sells
                positionUpdateService.onOrderExecuted(
                        order.getInvestorId(), order.getSymbol(), TransactionType.SELL,
                        order.getQuantity(), price, BigDecimal.ZERO, order.getId());

                BigDecimal proceeds = price.multiply(BigDecimal.valueOf(order.getQuantity()));
                AccountBalance balance = accountBalanceRepository.findByInvestorId(order.getInvestorId())
                        .orElseGet(() -> accountBalanceRepository.save(AccountBalance.builder()
                                .investorId(order.getInvestorId()).totalBalance(BigDecimal.ZERO).currency("COP").build()));
                balance.setTotalBalance(balance.getTotalBalance().add(proceeds));
                accountBalanceRepository.save(balance);

                order.setStatus(ConditionalOrderStatus.TRIGGERED);
                conditionalOrderRepository.save(order);
                log.info("ConditionalOrderEvaluationJob: triggered {} order {} for investor {}",
                        order.getType(), order.getId(), order.getInvestorId());

                // Release title reservation (may be on this order or on the OCO partner)
                conditionalOrderService.releaseReservationForOrderOrPartner(
                        order.getId(), order.getOcoPartnerId());

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

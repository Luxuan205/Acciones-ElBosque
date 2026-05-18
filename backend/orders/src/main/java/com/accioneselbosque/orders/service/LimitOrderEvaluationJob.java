package com.accioneselbosque.orders.service;

import com.accioneselbosque.configuration.service.MarketStatusService;
import com.accioneselbosque.market_data_service.service.StockSnapshotService;
import com.accioneselbosque.orders.model.Order;
import com.accioneselbosque.orders.model.OrderStatus;
import com.accioneselbosque.orders.model.OrderType;
import com.accioneselbosque.orders.repository.BalanceReservationRepository;
import com.accioneselbosque.orders.repository.OrderRepository;
import com.accioneselbosque.orders.repository.TitleReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class LimitOrderEvaluationJob {

    private final OrderRepository orderRepository;
    private final BalanceReservationRepository balanceReservationRepository;
    private final TitleReservationRepository titleReservationRepository;
    private final StockSnapshotService stockSnapshotService;
    private final MarketStatusService marketStatusService;

    @Autowired
    @Lazy
    private LimitOrderEvaluationJob self;

    @Scheduled(fixedRate = 30_000)
    public void evaluate() {
        if (!marketStatusService.isMarketOpen()) return;

        LocalDateTime now = LocalDateTime.now();

        // Expire GTD orders
        orderRepository.findByStatusAndOrderTypeIn(
                OrderStatus.PENDING, List.of(OrderType.LIMIT_BUY, OrderType.LIMIT_SELL))
                .forEach(order -> {
                    if (order.getExpiresAt() != null && order.getExpiresAt().isBefore(now)) {
                        try {
                            self.cancelAndRelease(order);
                        } catch (Exception e) {
                            log.warn("Failed to expire limit order {}: {}", order.getId(), e.getMessage());
                        }
                    }
                });

        // Evaluate price conditions
        orderRepository.findByStatusAndOrderTypeIn(
                OrderStatus.PENDING, List.of(OrderType.LIMIT_BUY, OrderType.LIMIT_SELL))
                .forEach(order -> {
                    if (order.getExpiresAt() != null && order.getExpiresAt().isBefore(now)) return;
                    try {
                        stockSnapshotService.findBySymbol(order.getSymbol()).ifPresent(snapshot -> {
                            BigDecimal price = snapshot.getCurrentPrice();
                            if (order.getOrderType() == OrderType.LIMIT_BUY && price.compareTo(order.getLimitPrice()) <= 0) {
                                self.executeOrder(order);
                            } else if (order.getOrderType() == OrderType.LIMIT_SELL && price.compareTo(order.getLimitPrice()) >= 0) {
                                self.executeOrder(order);
                            }
                        });
                    } catch (Exception e) {
                        log.warn("Failed to evaluate limit order {}: {}", order.getId(), e.getMessage());
                    }
                });
    }

    @Transactional
    public void executeOrder(Order order) {
        order.setStatus(OrderStatus.EXECUTED);
        orderRepository.save(order);
        if (order.getOrderType() == OrderType.LIMIT_BUY) {
            balanceReservationRepository.findByOrderId(order.getId()).ifPresent(r -> {
                r.setReleased(true);
                r.setReleasedAt(LocalDateTime.now());
                balanceReservationRepository.save(r);
            });
        } else {
            titleReservationRepository.findByOrderId(order.getId()).ifPresent(r -> {
                r.setReleased(true);
                r.setReleasedAt(LocalDateTime.now());
                titleReservationRepository.save(r);
            });
        }
    }

    @Transactional
    public void cancelAndRelease(Order order) {
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        if (order.getOrderType() == OrderType.LIMIT_BUY) {
            balanceReservationRepository.findByOrderId(order.getId()).ifPresent(r -> {
                r.setReleased(true);
                r.setReleasedAt(LocalDateTime.now());
                balanceReservationRepository.save(r);
            });
        } else {
            titleReservationRepository.findByOrderId(order.getId()).ifPresent(r -> {
                r.setReleased(true);
                r.setReleasedAt(LocalDateTime.now());
                titleReservationRepository.save(r);
            });
        }
    }
}

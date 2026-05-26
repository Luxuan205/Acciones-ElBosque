package com.accioneselbosque.orders.service;

import com.accioneselbosque.configuration.service.MarketStatusService;
import com.accioneselbosque.market_data_service.service.StockSnapshotService;
import com.accioneselbosque.orders.model.Order;
import com.accioneselbosque.orders.model.OrderStatus;
import com.accioneselbosque.orders.model.OrderType;
import com.accioneselbosque.orders.repository.BalanceReservationRepository;
import com.accioneselbosque.orders.repository.OrderRepository;
import com.accioneselbosque.orders.repository.TitleReservationRepository;
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
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderQueueProcessor {

    private final OrderRepository orderRepository;
    private final MarketStatusService marketStatusService;
    private final StockSnapshotService stockSnapshotService;
    private final PositionUpdateService positionUpdateService;
    private final AccountBalanceRepository accountBalanceRepository;
    private final BalanceReservationRepository balanceReservationRepository;
    private final TitleReservationRepository titleReservationRepository;

    @Autowired
    @Lazy
    private OrderQueueProcessor self;

    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void processQueue() {
        if (!marketStatusService.isMarketOpen()) return;

        // Move QUEUED → PENDING at market open
        List<Order> queued = orderRepository.findByStatusAndOrderTypeIn(
                OrderStatus.QUEUED, List.of(OrderType.MARKET_BUY, OrderType.MARKET_SELL));
        if (!queued.isEmpty()) {
            queued.forEach(order -> order.setStatus(OrderStatus.PENDING));
            orderRepository.saveAll(queued);
            log.info("OrderQueueProcessor: moved {} queued orders to PENDING", queued.size());
        }

        // Execute all PENDING market orders
        List<Order> pending = orderRepository.findByStatusAndOrderTypeIn(
                OrderStatus.PENDING, List.of(OrderType.MARKET_BUY, OrderType.MARKET_SELL));
        pending.forEach(order -> {
            try {
                self.executeMarketOrder(order);
            } catch (Exception e) {
                log.warn("OrderQueueProcessor: failed to execute order {}: {}", order.getId(), e.getMessage());
            }
        });
    }

    @Transactional
    public void executeMarketOrder(Order order) {
        BigDecimal execPrice = stockSnapshotService.findBySymbol(order.getSymbol())
                .map(s -> s.getCurrentPrice())
                .orElse(order.getEstimatedPrice());

        TransactionType txType = order.getOrderType() == OrderType.MARKET_BUY
                ? TransactionType.BUY : TransactionType.SELL;

        positionUpdateService.onOrderExecuted(
                order.getInvestorId(), order.getSymbol(), txType,
                order.getQuantity(), execPrice, order.getCommission(), order.getId());

        AccountBalance balance = accountBalanceRepository.findByInvestorId(order.getInvestorId())
                .orElseGet(() -> accountBalanceRepository.save(AccountBalance.builder()
                        .investorId(order.getInvestorId()).totalBalance(BigDecimal.ZERO).currency("COP").build()));

        BigDecimal gross = execPrice.multiply(BigDecimal.valueOf(order.getQuantity()));
        if (txType == TransactionType.BUY) {
            balance.setTotalBalance(balance.getTotalBalance().subtract(gross.add(order.getCommission())));
            balanceReservationRepository.findByOrderId(order.getId()).ifPresent(r -> {
                r.setReleased(true); r.setReleasedAt(LocalDateTime.now());
                balanceReservationRepository.save(r);
            });
        } else {
            balance.setTotalBalance(balance.getTotalBalance().add(gross.subtract(order.getCommission())));
            titleReservationRepository.findByOrderId(order.getId()).ifPresent(r -> {
                r.setReleased(true); r.setReleasedAt(LocalDateTime.now());
                titleReservationRepository.save(r);
            });
        }
        accountBalanceRepository.save(balance);

        order.setStatus(OrderStatus.EXECUTED);
        orderRepository.save(order);
        log.info("OrderQueueProcessor: executed {} order {} for investor {} at price {}",
                order.getOrderType(), order.getId(), order.getInvestorId(), execPrice);
    }
}

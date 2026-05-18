package com.accioneselbosque.orders.service;

import com.accioneselbosque.configuration.service.MarketStatusService;
import com.accioneselbosque.orders.model.OrderStatus;
import com.accioneselbosque.orders.model.OrderType;
import com.accioneselbosque.orders.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderQueueProcessor {

    private final OrderRepository orderRepository;
    private final MarketStatusService marketStatusService;

    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void processQueue() {
        if (!marketStatusService.isMarketOpen()) {
            return;
        }

        List<com.accioneselbosque.orders.model.Order> queued = orderRepository
                .findByStatusAndOrderTypeIn(OrderStatus.QUEUED,
                        List.of(OrderType.MARKET_BUY, OrderType.MARKET_SELL));

        if (queued.isEmpty()) return;

        queued.forEach(order -> order.setStatus(OrderStatus.PENDING));
        orderRepository.saveAll(queued);
        log.info("OrderQueueProcessor: processed {} queued market orders on market open", queued.size());
    }
}

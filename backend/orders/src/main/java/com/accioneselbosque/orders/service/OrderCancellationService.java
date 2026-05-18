package com.accioneselbosque.orders.service;

import com.accioneselbosque.orders.dto.BulkCancellationFailure;
import com.accioneselbosque.orders.dto.BulkCancellationResponse;
import com.accioneselbosque.orders.dto.CancellationResponse;
import com.accioneselbosque.orders.exception.OrderConcurrentModificationException;
import com.accioneselbosque.orders.exception.OrderNotCancellableException;
import com.accioneselbosque.orders.exception.OrderNotFoundException;
import com.accioneselbosque.orders.model.BalanceReservation;
import com.accioneselbosque.orders.model.OrderStatus;
import com.accioneselbosque.orders.model.OrderType;
import com.accioneselbosque.orders.model.TitleReservation;
import com.accioneselbosque.orders.repository.BalanceReservationRepository;
import com.accioneselbosque.orders.repository.OrderRepository;
import com.accioneselbosque.orders.repository.TitleReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderCancellationService {

    private final OrderRepository orderRepository;
    private final BalanceReservationRepository balanceReservationRepository;
    private final TitleReservationRepository titleReservationRepository;

    public CancellationResponse cancel(Long investorId, Long orderId, String reason) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        if (!order.getInvestorId().equals(investorId)) throw new RuntimeException("Access denied");

        OrderStatus prev = order.getStatus();
        if (prev != OrderStatus.PENDING && prev != OrderStatus.QUEUED) {
            throw new OrderNotCancellableException(prev.name());
        }

        try {
            order.setStatus(OrderStatus.CANCELLED);
            order.setCancellationReason(reason);
            orderRepository.save(order);
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
            var refreshed = orderRepository.findById(orderId).orElseThrow();
            throw new OrderConcurrentModificationException(refreshed.getStatus().name());
        }

        // Release reservations
        BigDecimal amountReleased = null;
        Integer titlesReleased = null;

        if (order.getOrderType() == OrderType.MARKET_BUY || order.getOrderType() == OrderType.LIMIT_BUY) {
            var res = balanceReservationRepository.findByOrderId(orderId);
            res.ifPresent(r -> {
                r.setReleased(true);
                r.setReleasedAt(LocalDateTime.now());
                balanceReservationRepository.save(r);
            });
            amountReleased = res.map(BalanceReservation::getAmount).orElse(BigDecimal.ZERO);
        } else if (order.getOrderType() == OrderType.MARKET_SELL || order.getOrderType() == OrderType.LIMIT_SELL) {
            var res = titleReservationRepository.findByOrderId(orderId);
            res.ifPresent(r -> {
                r.setReleased(true);
                r.setReleasedAt(LocalDateTime.now());
                titleReservationRepository.save(r);
            });
            titlesReleased = res.map(TitleReservation::getQuantity).orElse(0);
        }

        return new CancellationResponse(orderId, prev.name(), "CANCELLED", amountReleased, titlesReleased, LocalDateTime.now());
    }

    public BulkCancellationResponse cancelBulk(Long investorId, List<Long> orderIds, String reason) {
        List<Long> cancelled = new ArrayList<>();
        List<BulkCancellationFailure> failed = new ArrayList<>();

        for (Long id : orderIds) {
            try {
                cancel(investorId, id, reason);
                cancelled.add(id);
            } catch (Exception e) {
                String status = orderRepository.findById(id)
                        .map(o -> o.getStatus().name()).orElse("NOT_FOUND");
                failed.add(new BulkCancellationFailure(id, status, e.getMessage()));
            }
        }

        return new BulkCancellationResponse(orderIds.size(), cancelled.size(), failed.size(), cancelled, failed);
    }
}

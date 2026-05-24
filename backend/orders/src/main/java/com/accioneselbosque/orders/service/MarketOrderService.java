package com.accioneselbosque.orders.service;

import com.accioneselbosque.auth.repository.InvestorRepository;
import com.accioneselbosque.configuration.service.MarketStatusService;
import com.accioneselbosque.market_data_service.exception.SymbolNotFoundException;
import com.accioneselbosque.market_data_service.service.StockSnapshotService;
import com.accioneselbosque.orders.dto.CommissionBreakdown;
import com.accioneselbosque.orders.dto.OrderHistoryDto;
import com.accioneselbosque.orders.dto.OrderPreviewResponse;
import com.accioneselbosque.orders.dto.OrderResponse;
import com.accioneselbosque.orders.dto.PlaceMarketBuyRequest;
import com.accioneselbosque.orders.exception.InsufficientBalanceException;
import com.accioneselbosque.orders.exception.OrderQueueLimitException;
import com.accioneselbosque.orders.model.BalanceReservation;
import com.accioneselbosque.orders.model.Order;
import com.accioneselbosque.orders.model.OrderStatus;
import com.accioneselbosque.orders.model.OrderType;
import com.accioneselbosque.orders.repository.BalanceReservationRepository;
import com.accioneselbosque.orders.repository.OrderRepository;
import com.accioneselbosque.audit.model.AuditEventRecord;
import com.accioneselbosque.audit.model.AuditEventType;
import com.accioneselbosque.audit.model.AuditResult;
import com.accioneselbosque.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class MarketOrderService {

    private final InvestorRepository investorRepository;
    private final StockSnapshotService stockSnapshotService;
    private final MarketStatusService marketStatusService;
    private final OrderRepository orderRepository;
    private final BalanceReservationRepository balanceReservationRepository;
    private final CommissionCalculatorService commissionCalculatorService;
    private final AuditService auditService;

    private static final int MAX_QUEUED_ORDERS = 10;

    @Transactional(readOnly = true)
    public OrderPreviewResponse preview(String symbol, int quantity, Long investorId) {
        var snapshot = stockSnapshotService.findBySymbol(symbol)
                .orElseThrow(() -> new SymbolNotFoundException(symbol));

        BigDecimal price = snapshot.getCurrentPrice();
        BigDecimal gross = price.multiply(BigDecimal.valueOf(quantity));

        String subType = resolveSubscriptionType(investorId);
        BigDecimal ratePercent = commissionCalculatorService.getRatePercent(subType);
        BigDecimal commission = gross.multiply(ratePercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal total = gross.add(commission).setScale(2, RoundingMode.HALF_UP);

        return new OrderPreviewResponse(symbol, quantity, price, commission, total,
                marketStatusService.isMarketOpen(), subType, ratePercent);
    }

    public OrderResponse placeBuy(Long investorId, PlaceMarketBuyRequest req) {
        // Check queue limit before proceeding
        long queuedCount = orderRepository.countByInvestorIdAndStatus(investorId, OrderStatus.QUEUED);
        if (queuedCount >= MAX_QUEUED_ORDERS) {
            throw new OrderQueueLimitException();
        }

        var preview = preview(req.symbol(), req.quantity(), investorId);

        var investor = investorRepository.findById(investorId)
                .orElseThrow(() -> new RuntimeException("Investor not found"));

        BigDecimal totalReserved = balanceReservationRepository
                .findByInvestorIdAndReleasedFalse(investorId).stream()
                .map(BalanceReservation::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal available = investor.getAvailableBalance().subtract(totalReserved);

        if (available.compareTo(preview.totalEstimated()) < 0) {
            throw new InsufficientBalanceException();
        }

        OrderStatus status = marketStatusService.isMarketOpen() ? OrderStatus.PENDING : OrderStatus.QUEUED;

        Order order = Order.builder()
                .investorId(investorId)
                .orderType(OrderType.MARKET_BUY)
                .status(status)
                .symbol(req.symbol())
                .quantity(req.quantity())
                .estimatedPrice(preview.estimatedPrice())
                .commission(preview.commission())
                .totalEstimated(preview.totalEstimated())
                .build();
        order = orderRepository.save(order);

        BalanceReservation reservation = BalanceReservation.builder()
                .orderId(order.getId())
                .investorId(investorId)
                .amount(preview.totalEstimated())
                .released(false)
                .createdAt(LocalDateTime.now())
                .build();
        balanceReservationRepository.save(reservation);

        CommissionBreakdown breakdown = new CommissionBreakdown(
                preview.estimatedPrice(), req.quantity(), preview.commission(), preview.totalEstimated());

        String message = status == OrderStatus.QUEUED
                ? "El mercado está cerrado. La orden se ejecutará en la próxima apertura." : null;

        auditService.record(AuditEventRecord.builder()
                .eventType(AuditEventType.ORDER_CREATED)
                .investorId(investorId)
                .performedBy(investorId)
                .referenceType("ORDER")
                .referenceId(order.getId())
                .result(AuditResult.SUCCESS)
                .detail("MARKET_BUY " + req.symbol() + " x" + req.quantity() + " status=" + status.name())
                .build());

        return new OrderResponse(order.getId(), status.name(), req.symbol(), req.quantity(), breakdown, order.getCreatedAt(), message);
    }

    private String resolveSubscriptionType(Long investorId) {
        if (investorId == null) return "STANDARD";
        return investorRepository.findById(investorId)
                .map(inv -> inv.getSubscriptionType().name())
                .orElse("STANDARD");
    }

    @Transactional(readOnly = true)
    public java.util.List<OrderHistoryDto> getOrders(Long investorId) {
        return orderRepository.findByInvestorIdOrderByCreatedAtDesc(investorId).stream()
                .map(o -> new OrderHistoryDto(
                        o.getId(),
                        o.getInvestorId(),
                        o.getOrderType().name(),
                        o.getStatus().name(),
                        o.getSymbol(),
                        o.getQuantity(),
                        o.getEstimatedPrice(),
                        o.getCommission(),
                        o.getTotalEstimated(),
                        o.getLimitPrice(),
                        o.getCreatedAt()
                ))
                .toList();
    }
}

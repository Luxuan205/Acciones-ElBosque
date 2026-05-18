package com.accioneselbosque.orders.service;

import com.accioneselbosque.auth.repository.InvestorRepository;
import com.accioneselbosque.configuration.service.MarketStatusService;
import com.accioneselbosque.market_data_service.exception.SymbolNotFoundException;
import com.accioneselbosque.market_data_service.service.StockSnapshotService;
import com.accioneselbosque.orders.dto.CommissionBreakdown;
import com.accioneselbosque.orders.dto.OrderResponse;
import com.accioneselbosque.orders.dto.PlaceMarketSellRequest;
import com.accioneselbosque.orders.dto.SellOrderPreviewResponse;
import com.accioneselbosque.orders.exception.InsufficientTitlesException;
import com.accioneselbosque.orders.exception.OrderQueueLimitException;
import com.accioneselbosque.orders.model.Order;
import com.accioneselbosque.orders.model.OrderStatus;
import com.accioneselbosque.orders.model.OrderType;
import com.accioneselbosque.orders.model.TitleReservation;
import com.accioneselbosque.orders.repository.OrderRepository;
import com.accioneselbosque.orders.repository.TitleReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class MarketSellService {

    private final InvestorRepository investorRepository;
    private final StockSnapshotService stockSnapshotService;
    private final MarketStatusService marketStatusService;
    private final OrderRepository orderRepository;
    private final TitleReservationRepository titleReservationRepository;
    private final PortfolioQueryFacade portfolioFacade;
    private final CommissionCalculatorService commissionCalculatorService;

    private static final int MAX_QUEUED_ORDERS = 10;

    @Transactional(readOnly = true)
    public SellOrderPreviewResponse preview(String symbol, int quantity, Long investorId) {
        var snapshot = stockSnapshotService.findBySymbol(symbol)
                .orElseThrow(() -> new SymbolNotFoundException(symbol));
        BigDecimal price = snapshot.getCurrentPrice();
        BigDecimal gross = price.multiply(BigDecimal.valueOf(quantity));

        String subType = resolveSubscriptionType(investorId);
        BigDecimal ratePercent = commissionCalculatorService.getRatePercent(subType);
        BigDecimal commission = gross.multiply(ratePercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal net = gross.subtract(commission).setScale(2, RoundingMode.HALF_UP);

        return new SellOrderPreviewResponse(symbol, quantity, price, commission, net,
                marketStatusService.isMarketOpen(), subType, ratePercent);
    }

    public OrderResponse placeSell(Long investorId, PlaceMarketSellRequest req) {
        // Check queue limit
        long queuedCount = orderRepository.countByInvestorIdAndStatus(investorId, OrderStatus.QUEUED);
        if (queuedCount >= MAX_QUEUED_ORDERS) {
            throw new OrderQueueLimitException();
        }

        var preview = preview(req.symbol(), req.quantity(), investorId);

        int portfolioQty = portfolioFacade.getAvailableTitles(investorId, req.symbol());
        int reservedQty = titleReservationRepository
                .findByInvestorIdAndSymbolAndReleasedFalse(investorId, req.symbol())
                .stream().mapToInt(TitleReservation::getQuantity).sum();
        int available = portfolioQty - reservedQty;

        if (available < req.quantity()) {
            throw new InsufficientTitlesException();
        }

        OrderStatus status = marketStatusService.isMarketOpen() ? OrderStatus.PENDING : OrderStatus.QUEUED;

        Order order = Order.builder()
                .investorId(investorId)
                .orderType(OrderType.MARKET_SELL)
                .status(status)
                .symbol(req.symbol())
                .quantity(req.quantity())
                .estimatedPrice(preview.estimatedPrice())
                .commission(preview.commission())
                .totalEstimated(preview.netAmount())
                .build();
        order = orderRepository.save(order);

        TitleReservation reservation = TitleReservation.builder()
                .orderId(order.getId())
                .investorId(investorId)
                .symbol(req.symbol())
                .quantity(req.quantity())
                .released(false)
                .createdAt(LocalDateTime.now())
                .build();
        titleReservationRepository.save(reservation);

        CommissionBreakdown breakdown = new CommissionBreakdown(
                preview.estimatedPrice(), req.quantity(), preview.commission(), preview.netAmount());
        String message = status == OrderStatus.QUEUED
                ? "El mercado está cerrado. La orden se ejecutará en la próxima apertura." : null;
        return new OrderResponse(order.getId(), status.name(), req.symbol(), req.quantity(), breakdown, order.getCreatedAt(), message);
    }

    private String resolveSubscriptionType(Long investorId) {
        if (investorId == null) return "STANDARD";
        return investorRepository.findById(investorId)
                .map(inv -> inv.getSubscriptionType().name())
                .orElse("STANDARD");
    }
}

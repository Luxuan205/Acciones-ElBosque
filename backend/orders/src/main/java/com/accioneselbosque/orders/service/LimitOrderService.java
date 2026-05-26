package com.accioneselbosque.orders.service;

import com.accioneselbosque.auth.repository.InvestorRepository;
import com.accioneselbosque.orders.dto.LimitOrderResponse;
import com.accioneselbosque.orders.dto.PlaceLimitBuyRequest;
import com.accioneselbosque.orders.dto.PlaceLimitSellRequest;
import com.accioneselbosque.orders.exception.InsufficientBalanceException;
import com.accioneselbosque.orders.exception.InsufficientTitlesException;
import com.accioneselbosque.orders.model.BalanceReservation;
import com.accioneselbosque.orders.model.Order;
import com.accioneselbosque.orders.model.OrderStatus;
import com.accioneselbosque.orders.model.OrderType;
import com.accioneselbosque.orders.model.TitleReservation;
import com.accioneselbosque.orders.repository.BalanceReservationRepository;
import com.accioneselbosque.orders.repository.OrderRepository;
import com.accioneselbosque.orders.repository.TitleReservationRepository;
import com.accioneselbosque.portfolio.facade.PortfolioFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class LimitOrderService {

    private final InvestorRepository investorRepository;
    private final OrderRepository orderRepository;
    private final BalanceReservationRepository balanceReservationRepository;
    private final TitleReservationRepository titleReservationRepository;
    private final PortfolioFacade portfolioFacade;
    private final CommissionCalculatorService commissionCalculatorService;

    public LimitOrderResponse placeLimitBuy(Long investorId, PlaceLimitBuyRequest req) {
        var investor = investorRepository.findById(investorId)
                .orElseThrow(() -> new RuntimeException("Investor not found"));
        String subType = investor.getSubscriptionType().name();

        BigDecimal gross = req.limitPrice().multiply(BigDecimal.valueOf(req.quantity()));
        BigDecimal commission = commissionCalculatorService.calculateCommission(gross, subType);
        BigDecimal total = gross.add(commission).setScale(2, RoundingMode.HALF_UP);

        BigDecimal reserved = balanceReservationRepository
                .findByInvestorIdAndReleasedFalse(investorId).stream()
                .map(BalanceReservation::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (investor.getAvailableBalance().subtract(reserved).compareTo(total) < 0) {
            throw new InsufficientBalanceException();
        }

        Order order = Order.builder()
                .investorId(investorId).orderType(OrderType.LIMIT_BUY).status(OrderStatus.PENDING)
                .symbol(req.symbol()).quantity(req.quantity()).estimatedPrice(req.limitPrice())
                .commission(commission).totalEstimated(total).limitPrice(req.limitPrice())
                .expiresAt(req.expiresAt()).build();
        order = orderRepository.save(order);

        balanceReservationRepository.save(BalanceReservation.builder()
                .orderId(order.getId()).investorId(investorId).amount(total)
                .released(false).createdAt(LocalDateTime.now()).build());

        return new LimitOrderResponse(order.getId(), "LIMIT_BUY", req.symbol(), req.quantity(),
                req.limitPrice(), req.expiresAt(), "PENDING", order.getCreatedAt());
    }

    public LimitOrderResponse placeLimitSell(Long investorId, PlaceLimitSellRequest req) {
        var investor = investorRepository.findById(investorId)
                .orElseThrow(() -> new RuntimeException("Investor not found"));
        String subType = investor.getSubscriptionType().name();

        int portfolioQty = portfolioFacade.getAvailableTitles(investorId, req.symbol());
        int reservedQty = titleReservationRepository
                .findByInvestorIdAndSymbolAndReleasedFalse(investorId, req.symbol())
                .stream().mapToInt(TitleReservation::getQuantity).sum();
        if (portfolioQty - reservedQty < req.quantity()) throw new InsufficientTitlesException();

        BigDecimal gross = req.limitPrice().multiply(BigDecimal.valueOf(req.quantity()));
        BigDecimal commission = commissionCalculatorService.calculateCommission(gross, subType);
        BigDecimal net = gross.subtract(commission).setScale(2, RoundingMode.HALF_UP);

        Order order = Order.builder()
                .investorId(investorId).orderType(OrderType.LIMIT_SELL).status(OrderStatus.PENDING)
                .symbol(req.symbol()).quantity(req.quantity()).estimatedPrice(req.limitPrice())
                .commission(commission).totalEstimated(net).limitPrice(req.limitPrice())
                .expiresAt(req.expiresAt()).build();
        order = orderRepository.save(order);

        titleReservationRepository.save(TitleReservation.builder()
                .orderId(order.getId()).investorId(investorId).symbol(req.symbol()).quantity(req.quantity())
                .released(false).createdAt(LocalDateTime.now()).build());

        return new LimitOrderResponse(order.getId(), "LIMIT_SELL", req.symbol(), req.quantity(),
                req.limitPrice(), req.expiresAt(), "PENDING", order.getCreatedAt());
    }
}

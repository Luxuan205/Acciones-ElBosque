package com.accioneselbosque.orders.service;

import com.accioneselbosque.auth.repository.InvestorRepository;
import com.accioneselbosque.configuration.service.MarketStatusService;
import com.accioneselbosque.orders.dto.BrokerOrderRequest;
import com.accioneselbosque.orders.dto.BrokerOrderResponse;
import com.accioneselbosque.orders.model.Order;
import com.accioneselbosque.orders.model.OrderStatus;
import com.accioneselbosque.orders.model.OrderType;
import com.accioneselbosque.orders.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional
public class BrokerOrderService {

    private final BrokerAssignmentValidator brokerAssignmentValidator;
    private final InvestorRepository investorRepository;
    private final MarketStatusService marketStatusService;
    private final CommissionCalculatorService commissionCalculatorService;
    private final OrderRepository orderRepository;

    public BrokerOrderResponse createBrokerOrder(Long brokerId, BrokerOrderRequest req) {
        brokerAssignmentValidator.assertBrokerAssignedToClient(brokerId, req.clientId());

        var client = investorRepository.findById(req.clientId())
                .orElseThrow(() -> new RuntimeException("Client investor not found: " + req.clientId()));

        String subscriptionType = client.getSubscriptionType().name();
        BigDecimal gross = req.unitPrice().multiply(BigDecimal.valueOf(req.quantity()));
        BigDecimal commission = commissionCalculatorService.calculateCommission(gross, subscriptionType);

        OrderType orderType = resolveOrderType(req.orderType());
        boolean isBuy = orderType == OrderType.MARKET_BUY;
        BigDecimal netTotal = isBuy ? gross.add(commission) : gross.subtract(commission);

        OrderStatus status = marketStatusService.isMarketOpen() ? OrderStatus.PENDING : OrderStatus.QUEUED;

        Order order = Order.builder()
                .investorId(req.clientId())
                .brokerId(brokerId)
                .orderType(orderType)
                .status(status)
                .symbol(req.symbol().toUpperCase())
                .quantity(req.quantity())
                .estimatedPrice(req.unitPrice())
                .commission(commission)
                .totalEstimated(netTotal)
                .build();
        order = orderRepository.save(order);

        return new BrokerOrderResponse(
                order.getId(),
                req.clientId(),
                brokerId,
                order.getSymbol(),
                order.getQuantity(),
                order.getOrderType().name(),
                order.getStatus().name(),
                req.unitPrice(),
                gross,
                commission,
                netTotal,
                order.getCreatedAt()
        );
    }

    private OrderType resolveOrderType(String orderType) {
        return switch (orderType.toUpperCase()) {
            case "MARKET_BUY", "BUY" -> OrderType.MARKET_BUY;
            case "MARKET_SELL", "SELL" -> OrderType.MARKET_SELL;
            default -> throw new IllegalArgumentException("Tipo de orden inválido: " + orderType);
        };
    }
}

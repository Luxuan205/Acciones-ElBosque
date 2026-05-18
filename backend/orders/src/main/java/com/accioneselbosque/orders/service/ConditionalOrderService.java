package com.accioneselbosque.orders.service;

import com.accioneselbosque.orders.dto.ConditionalOrderResponse;
import com.accioneselbosque.orders.dto.CreateStopLossRequest;
import com.accioneselbosque.orders.dto.CreateTakeProfitRequest;
import com.accioneselbosque.orders.model.ConditionalOrder;
import com.accioneselbosque.orders.model.ConditionalOrderStatus;
import com.accioneselbosque.orders.model.ConditionalOrderType;
import com.accioneselbosque.orders.repository.ConditionalOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ConditionalOrderService {

    private final ConditionalOrderRepository conditionalOrderRepository;

    public ConditionalOrderResponse createStopLoss(Long investorId, CreateStopLossRequest req) {
        ConditionalOrder order = ConditionalOrder.builder()
                .investorId(investorId).type(ConditionalOrderType.STOP_LOSS).symbol(req.symbol())
                .quantity(req.quantity()).triggerPrice(req.triggerPrice())
                .status(ConditionalOrderStatus.ACTIVE).build();
        order = conditionalOrderRepository.save(order);

        // OCO linking
        if (req.takeProfitId() != null) {
            ConditionalOrder partner = conditionalOrderRepository.findById(req.takeProfitId())
                    .orElseThrow(() -> new RuntimeException("Take-profit not found: " + req.takeProfitId()));
            order.setOcoPartnerId(req.takeProfitId());
            partner.setOcoPartnerId(order.getId());
            conditionalOrderRepository.save(partner);
            order = conditionalOrderRepository.save(order);
        }

        return toResponse(order);
    }

    public ConditionalOrderResponse createTakeProfit(Long investorId, CreateTakeProfitRequest req) {
        ConditionalOrder order = ConditionalOrder.builder()
                .investorId(investorId).type(ConditionalOrderType.TAKE_PROFIT).symbol(req.symbol())
                .quantity(req.quantity()).triggerPrice(req.triggerPrice())
                .status(ConditionalOrderStatus.ACTIVE).build();
        order = conditionalOrderRepository.save(order);

        if (req.stopLossId() != null) {
            ConditionalOrder partner = conditionalOrderRepository.findById(req.stopLossId())
                    .orElseThrow(() -> new RuntimeException("Stop-loss not found: " + req.stopLossId()));
            order.setOcoPartnerId(req.stopLossId());
            partner.setOcoPartnerId(order.getId());
            conditionalOrderRepository.save(partner);
            order = conditionalOrderRepository.save(order);
        }

        return toResponse(order);
    }

    public void cancel(Long investorId, Long id) {
        ConditionalOrder order = conditionalOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Conditional order not found: " + id));
        if (!order.getInvestorId().equals(investorId)) throw new RuntimeException("Access denied");
        order.setStatus(ConditionalOrderStatus.CANCELLED);
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

    @Transactional(readOnly = true)
    public List<ConditionalOrderResponse> getByInvestor(Long investorId) {
        return conditionalOrderRepository
                .findByInvestorIdAndStatus(investorId, ConditionalOrderStatus.ACTIVE)
                .stream().map(this::toResponse).toList();
    }

    private ConditionalOrderResponse toResponse(ConditionalOrder o) {
        return new ConditionalOrderResponse(o.getId(), o.getType().name(), o.getSymbol(),
                o.getQuantity(), o.getTriggerPrice(), o.getStatus().name(), o.getOcoPartnerId(), o.getCreatedAt());
    }
}

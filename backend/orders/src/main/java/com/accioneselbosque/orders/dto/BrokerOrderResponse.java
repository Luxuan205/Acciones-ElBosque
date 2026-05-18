package com.accioneselbosque.orders.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BrokerOrderResponse(
        Long orderId,
        Long clientId,
        Long brokerId,
        String symbol,
        int quantity,
        String orderType,
        String status,
        BigDecimal unitPrice,
        BigDecimal grossValue,
        BigDecimal commissionAmt,
        BigDecimal netTotal,
        LocalDateTime createdAt
) {}

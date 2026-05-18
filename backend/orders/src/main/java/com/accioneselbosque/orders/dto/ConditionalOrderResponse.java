package com.accioneselbosque.orders.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ConditionalOrderResponse(
        Long id,
        String type,
        String symbol,
        int quantity,
        BigDecimal triggerPrice,
        String status,
        Long ocoPartnerId,
        LocalDateTime createdAt
) {}

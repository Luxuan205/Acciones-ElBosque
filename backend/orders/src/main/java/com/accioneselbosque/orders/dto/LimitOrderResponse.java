package com.accioneselbosque.orders.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LimitOrderResponse(
        Long orderId,
        String type,
        String symbol,
        int quantity,
        BigDecimal limitPrice,
        LocalDateTime expiresAt,
        String status,
        LocalDateTime createdAt
) {}

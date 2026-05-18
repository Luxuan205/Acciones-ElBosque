package com.accioneselbosque.orders.dto;

import java.time.LocalDateTime;

public record OrderResponse(
        Long orderId,
        String status,
        String symbol,
        int quantity,
        CommissionBreakdown breakdown,
        LocalDateTime createdAt,
        String message
) {}

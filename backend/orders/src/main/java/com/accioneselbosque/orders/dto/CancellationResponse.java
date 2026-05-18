package com.accioneselbosque.orders.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CancellationResponse(
        Long orderId,
        String previousStatus,
        String newStatus,
        BigDecimal amountReleased,
        Integer titlesReleased,
        LocalDateTime cancelledAt
) {}

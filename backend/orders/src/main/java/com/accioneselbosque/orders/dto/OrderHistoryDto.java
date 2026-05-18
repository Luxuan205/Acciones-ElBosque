package com.accioneselbosque.orders.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderHistoryDto(
        Long id,
        Long investorId,
        String orderType,
        String status,
        String symbol,
        int quantity,
        BigDecimal estimatedPrice,
        BigDecimal commission,
        BigDecimal totalEstimated,
        BigDecimal limitPrice,
        LocalDateTime createdAt
) {}

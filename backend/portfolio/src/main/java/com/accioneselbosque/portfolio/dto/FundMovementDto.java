package com.accioneselbosque.portfolio.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FundMovementDto(
        Long id,
        String type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String currency,
        String description,
        Long orderId,
        LocalDateTime createdAt
) {}

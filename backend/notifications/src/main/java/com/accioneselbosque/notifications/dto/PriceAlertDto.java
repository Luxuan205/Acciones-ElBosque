package com.accioneselbosque.notifications.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PriceAlertDto(
        Long id,
        String symbol,
        String alertType,
        BigDecimal threshold,
        BigDecimal referencePrice,
        String status,
        LocalDateTime createdAt,
        LocalDateTime triggeredAt
) {}

package com.accioneselbosque.notifications.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MarketAlertSubscriptionDto(
        Long id,
        String alertType,
        String symbol,
        BigDecimal threshold,
        boolean active,
        LocalDateTime createdAt
) {}

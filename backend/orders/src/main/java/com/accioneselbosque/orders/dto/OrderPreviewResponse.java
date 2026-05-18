package com.accioneselbosque.orders.dto;

import java.math.BigDecimal;

public record OrderPreviewResponse(
        String symbol,
        int quantity,
        BigDecimal estimatedPrice,
        BigDecimal commission,
        BigDecimal totalEstimated,
        boolean marketOpen,
        String subscriptionType,
        BigDecimal ratePercent
) {}

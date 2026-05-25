package com.accioneselbosque.orders.dto;

import java.math.BigDecimal;

public record SellOrderPreviewResponse(
        String symbol,
        int quantity,
        BigDecimal estimatedPrice,
        BigDecimal commission,
        BigDecimal netAmount,
        boolean marketOpen,
        String nextOpen,
        String subscriptionType,
        BigDecimal ratePercent
) {}

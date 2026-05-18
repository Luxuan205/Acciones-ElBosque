package com.accioneselbosque.orders.dto;

import java.math.BigDecimal;

public record CommissionBreakdown(
        BigDecimal estimatedPrice,
        int quantity,
        BigDecimal commission,
        BigDecimal totalEstimated
) {}

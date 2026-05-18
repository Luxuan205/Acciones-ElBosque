package com.accioneselbosque.portfolio.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BalanceSummaryResponse(
        BigDecimal availableBalance,
        BigDecimal reservedForOrders,
        BigDecimal totalInvested,
        BigDecimal totalPortfolioValue,
        BigDecimal unrealizedGain,
        BigDecimal unrealizedGainPercent
) {}

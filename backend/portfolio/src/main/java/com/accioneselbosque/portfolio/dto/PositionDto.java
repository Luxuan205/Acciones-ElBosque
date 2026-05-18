package com.accioneselbosque.portfolio.dto;

import java.math.BigDecimal;

public record PositionDto(
        String symbol,
        String name,
        int quantity,
        BigDecimal avgBuyPrice,
        BigDecimal currentPrice,
        BigDecimal marketValue,
        BigDecimal unrealizedGain,
        BigDecimal unrealizedGainPercent,
        BigDecimal dayChange,
        String currency
) {}

package com.accioneselbosque.portfolio.dto;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioPositionsResponse(
        List<PositionDto> positions,
        BigDecimal totalValue,
        BigDecimal unrealizedGain,
        BigDecimal unrealizedGainPercent
) {}

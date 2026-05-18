package com.accioneselbosque.market_data_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StockDetailDto(
        String symbol,
        String name,
        BigDecimal currentPrice,
        BigDecimal previousClose,
        BigDecimal dayChange,
        BigDecimal dayChangePct,
        long volume,
        LocalDateTime updatedAt,
        boolean stale,
        boolean marketOpen
) {}

package com.accioneselbosque.market_data_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WatchlistEntryDto(
        String symbol,
        String name,
        BigDecimal currentPrice,
        BigDecimal priceAtAdded,
        BigDecimal dayChange,
        BigDecimal dayChangePct,
        LocalDateTime lastUpdated,
        LocalDateTime addedAt
) {}

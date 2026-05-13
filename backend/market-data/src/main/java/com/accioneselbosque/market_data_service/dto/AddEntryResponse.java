package com.accioneselbosque.market_data_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AddEntryResponse(
        String symbol,
        String name,
        BigDecimal priceAtAdded,
        LocalDateTime addedAt,
        int entryCount,
        int maxEntries
) {}

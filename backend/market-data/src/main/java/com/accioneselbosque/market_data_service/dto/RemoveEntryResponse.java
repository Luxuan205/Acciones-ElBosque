package com.accioneselbosque.market_data_service.dto;

import java.time.LocalDateTime;

public record RemoveEntryResponse(
        String symbol,
        LocalDateTime removedAt,
        int entryCount,
        int maxEntries
) {}

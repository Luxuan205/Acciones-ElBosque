package com.accioneselbosque.market_data_service.dto;

import java.util.List;
import java.util.UUID;

public record WatchlistResponse(
        UUID watchlistId,
        Long investorId,
        int entryCount,
        int maxEntries,
        List<WatchlistEntryDto> entries
) {}

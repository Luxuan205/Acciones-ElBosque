package com.accioneselbosque.market_data_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WatchlistEntryRequest(
        @NotBlank @Size(max = 20) String symbol
) {}

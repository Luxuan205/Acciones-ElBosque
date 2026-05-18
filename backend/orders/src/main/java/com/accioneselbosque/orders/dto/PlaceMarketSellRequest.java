package com.accioneselbosque.orders.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlaceMarketSellRequest(
        @NotBlank @Size(max = 20) String symbol,
        @Min(1) int quantity
) {}

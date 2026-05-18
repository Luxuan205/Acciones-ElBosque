package com.accioneselbosque.orders.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PlaceLimitSellRequest(
        @NotBlank @Size(max = 20) String symbol,
        @Min(1) int quantity,
        @DecimalMin("0.01") BigDecimal limitPrice,
        LocalDateTime expiresAt
) {}

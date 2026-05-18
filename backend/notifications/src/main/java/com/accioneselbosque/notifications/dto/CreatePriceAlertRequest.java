package com.accioneselbosque.notifications.dto;

import com.accioneselbosque.notifications.model.PriceAlertType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreatePriceAlertRequest(
        @NotBlank String symbol,
        @NotNull PriceAlertType alertType,
        @Positive BigDecimal threshold
) {}

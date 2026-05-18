package com.accioneselbosque.notifications.dto;

import com.accioneselbosque.notifications.model.MarketAlertType;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateMarketAlertRequest(
        @NotNull MarketAlertType alertType,
        String symbol,
        BigDecimal threshold
) {}

package com.accioneselbosque.orders.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record BrokerOrderRequest(
        @NotNull Long clientId,
        @NotBlank String symbol,
        @Positive int quantity,
        @NotBlank String orderType,
        @Positive @DecimalMin("0.01") BigDecimal unitPrice
) {}

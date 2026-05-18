package com.accioneselbosque.orders.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateStopLossRequest(
        @NotBlank @Size(max = 20) String symbol,
        @Min(1) int quantity,
        @DecimalMin("0.01") BigDecimal triggerPrice,
        Long takeProfitId
) {}

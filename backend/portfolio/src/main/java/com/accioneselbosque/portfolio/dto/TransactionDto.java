package com.accioneselbosque.portfolio.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionDto(
        String transactionType,
        String symbol,
        int quantity,
        BigDecimal executionPrice,
        BigDecimal commission,
        BigDecimal grossAmount,
        BigDecimal netAmount,
        BigDecimal realizedGain,
        LocalDateTime executedAt
) {}

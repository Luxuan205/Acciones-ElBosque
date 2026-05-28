package com.accioneselbosque.app.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminTransactionDto(
        Long id,
        String investorName,
        String investorEmail,
        String symbol,
        String type,
        int quantity,
        BigDecimal grossAmount,
        BigDecimal commission,
        LocalDateTime executedAt
) {}

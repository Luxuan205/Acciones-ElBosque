package com.accioneselbosque.auth.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ClientSummaryDto(
        Long investorId,
        String fullName,
        String email,
        String accountStatus,
        BigDecimal availableBalance,
        long activeOrdersCount,
        LocalDateTime assignedAt
) {}

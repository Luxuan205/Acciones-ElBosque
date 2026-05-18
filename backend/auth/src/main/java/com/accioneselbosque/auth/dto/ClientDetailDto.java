package com.accioneselbosque.auth.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ClientDetailDto(
        Long investorId,
        String fullName,
        String email,
        String phone,
        String accountStatus,
        BigDecimal availableBalance,
        LocalDateTime assignedAt
) {}

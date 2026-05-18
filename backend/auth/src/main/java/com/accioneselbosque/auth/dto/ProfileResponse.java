package com.accioneselbosque.auth.dto;

import com.accioneselbosque.auth.model.AccountStatus;
import java.time.LocalDateTime;

public record ProfileResponse(
        Long id,
        String fullName,
        String email,
        String documentNumber,
        String phone,
        AccountStatus accountStatus,
        String subscriptionType,
        LocalDateTime subscriptionExpiresAt,
        LocalDateTime createdAt
) {}

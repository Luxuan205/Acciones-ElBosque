package com.accioneselbosque.auth.dto;

import java.time.LocalDateTime;

public record AdminUserDto(
        Long id,
        String fullName,
        String email,
        String documentNumber,
        String accountStatus,
        String subscriptionType,
        LocalDateTime subscriptionExpiry,
        String role,
        LocalDateTime createdAt
) {}

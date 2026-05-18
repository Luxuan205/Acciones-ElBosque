package com.accioneselbosque.auth.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminUserDetailDto(
        Long id,
        String fullName,
        String email,
        String documentNumber,
        String accountStatus,
        String subscriptionType,
        LocalDateTime subscriptionExpiry,
        String role,
        LocalDateTime createdAt,
        List<RecentActivityDto> recentActivity
) {}

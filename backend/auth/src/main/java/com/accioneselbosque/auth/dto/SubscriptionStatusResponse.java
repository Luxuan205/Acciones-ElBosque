package com.accioneselbosque.auth.dto;

import java.time.LocalDateTime;

public record SubscriptionStatusResponse(
        String subscriptionType,
        LocalDateTime activatedAt,  // nullable
        LocalDateTime expiresAt,    // nullable
        boolean isActive,
        long daysRemaining
) {}

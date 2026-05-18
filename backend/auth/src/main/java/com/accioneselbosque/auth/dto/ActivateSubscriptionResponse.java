package com.accioneselbosque.auth.dto;

import java.time.LocalDateTime;

public record ActivateSubscriptionResponse(
        String subscriptionType,
        LocalDateTime activatedAt,
        LocalDateTime expiresAt,
        String message  // nullable - set to "Ya tiene una suscripción PREMIUM activa." when already active
) {}

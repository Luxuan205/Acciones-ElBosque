package com.accioneselbosque.notifications.dto;

import java.time.LocalDateTime;

public record NotificationAttemptDto(
        int attemptNumber,
        String status,
        LocalDateTime attemptedAt
) {}

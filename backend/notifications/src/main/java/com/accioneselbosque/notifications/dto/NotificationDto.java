package com.accioneselbosque.notifications.dto;

import java.time.LocalDateTime;

public record NotificationDto(
        Long id,
        String eventType,
        String channel,
        String subject,
        String body,
        String status,
        Long referenceId,
        LocalDateTime createdAt
) {}

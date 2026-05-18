package com.accioneselbosque.notifications.dto;

import java.time.LocalDateTime;
import java.util.List;

public record NotificationDetailDto(
        Long id,
        String eventType,
        String channel,
        String subject,
        String body,
        String status,
        Long referenceId,
        LocalDateTime createdAt,
        List<NotificationAttemptDto> attempts
) {}

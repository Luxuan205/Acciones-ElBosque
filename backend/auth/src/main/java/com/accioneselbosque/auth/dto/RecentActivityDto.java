package com.accioneselbosque.auth.dto;

import java.time.LocalDateTime;

public record RecentActivityDto(
        String eventType,
        String description,
        LocalDateTime occurredAt
) {}

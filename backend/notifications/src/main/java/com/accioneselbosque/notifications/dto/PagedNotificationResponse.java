package com.accioneselbosque.notifications.dto;

import java.util.List;

public record PagedNotificationResponse(
        List<NotificationDto> content,
        long totalElements,
        int page,
        int size
) {}

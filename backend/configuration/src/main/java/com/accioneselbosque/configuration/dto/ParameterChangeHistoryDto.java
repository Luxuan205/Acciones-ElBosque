package com.accioneselbosque.configuration.dto;

import java.time.LocalDateTime;

public record ParameterChangeHistoryDto(
        Long id,
        String parameterKey,
        String previousValue,
        String newValue,
        Long changedBy,
        LocalDateTime changedAt,
        String reason
) {}

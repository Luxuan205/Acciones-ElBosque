package com.accioneselbosque.audit.dto;

import java.time.LocalDate;

public record AuditEventFilterDto(Long investorId, String eventType, String result,
        LocalDate from, LocalDate to, boolean includeArchived, int page, int size) {}

package com.accioneselbosque.audit.dto;

import com.accioneselbosque.audit.model.AuditEvent;

import java.time.LocalDateTime;

public record AuditEventResponseDto(Long id, String eventType, Long investorId, Long performedBy,
        String referenceType, Long referenceId, String detail, String result,
        String ipAddress, boolean archived, LocalDateTime occurredAt) {

    public static AuditEventResponseDto from(AuditEvent e) {
        return new AuditEventResponseDto(
                e.getId(),
                e.getEventType().name(),
                e.getInvestorId(),
                e.getPerformedBy(),
                e.getReferenceType(),
                e.getReferenceId(),
                e.getDetail(),
                e.getResult() != null ? e.getResult().name() : null,
                e.getIpAddress(),
                e.isArchived(),
                e.getOccurredAt());
    }
}

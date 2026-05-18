package com.accioneselbosque.audit.model;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuditEventRecord {
    private AuditEventType eventType;
    private Long investorId;
    private Long performedBy;
    private String referenceType;
    private Long referenceId;
    private String detail;
    private AuditResult result;
    private String ipAddress;
    private LocalDateTime occurredAt;
}

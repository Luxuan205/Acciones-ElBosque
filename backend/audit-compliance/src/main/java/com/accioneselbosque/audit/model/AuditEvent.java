package com.accioneselbosque.audit.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", updatable = false)
    private AuditEventType eventType;

    @Column(name = "investor_id", updatable = false)
    private Long investorId;

    @Column(name = "performed_by", updatable = false)
    private Long performedBy;

    @Column(name = "reference_type", length = 50, updatable = false)
    private String referenceType;

    @Column(name = "reference_id", updatable = false)
    private Long referenceId;

    @Column(updatable = false, columnDefinition = "TEXT")
    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(length = 10, updatable = false)
    private AuditResult result;

    @Column(name = "ip_address", length = 45, updatable = false)
    private String ipAddress;

    @Column(updatable = false)
    private boolean archived = false;

    @Column(name = "occurred_at", updatable = false)
    private LocalDateTime occurredAt;
}

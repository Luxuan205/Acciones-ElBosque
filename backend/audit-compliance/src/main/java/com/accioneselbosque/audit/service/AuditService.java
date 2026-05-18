package com.accioneselbosque.audit.service;

import com.accioneselbosque.audit.model.AuditEvent;
import com.accioneselbosque.audit.model.AuditEventRecord;
import com.accioneselbosque.audit.model.AuditResult;
import com.accioneselbosque.audit.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    /**
     * Fire-and-forget: persists the audit record asynchronously.
     * Never throws to the caller. If saving fails, logs and continues.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditEventRecord event) {
        try {
            AuditEvent entity = AuditEvent.builder()
                    .eventType(event.getEventType())
                    .investorId(event.getInvestorId())
                    .performedBy(event.getPerformedBy())
                    .referenceType(event.getReferenceType())
                    .referenceId(event.getReferenceId())
                    .detail(event.getDetail())
                    .result(event.getResult() != null ? event.getResult() : AuditResult.SUCCESS)
                    .ipAddress(event.getIpAddress())
                    .archived(false)
                    .occurredAt(event.getOccurredAt() != null ? event.getOccurredAt() : LocalDateTime.now())
                    .build();
            auditEventRepository.save(entity);
        } catch (Exception e) {
            log.error("Failed to persist audit event {}: {}", event.getEventType(), e.getMessage());
        }
    }
}

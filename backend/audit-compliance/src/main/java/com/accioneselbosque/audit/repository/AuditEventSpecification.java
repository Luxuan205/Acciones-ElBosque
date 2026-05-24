package com.accioneselbosque.audit.repository;

import com.accioneselbosque.audit.model.AuditEvent;
import com.accioneselbosque.audit.model.AuditEventType;
import com.accioneselbosque.audit.model.AuditResult;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public final class AuditEventSpecification {

    private AuditEventSpecification() {}

    public static Specification<AuditEvent> hasInvestorId(Long investorId) {
        return (root, query, cb) -> cb.equal(root.get("investorId"), investorId);
    }

    public static Specification<AuditEvent> hasEventType(AuditEventType eventType) {
        return (root, query, cb) -> cb.equal(root.get("eventType"), eventType);
    }

    public static Specification<AuditEvent> hasResult(AuditResult result) {
        return (root, query, cb) -> cb.equal(root.get("result"), result);
    }

    public static Specification<AuditEvent> occurredAtOrAfter(LocalDateTime from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("occurredAt"), from);
    }

    public static Specification<AuditEvent> occurredAtOrBefore(LocalDateTime to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("occurredAt"), to);
    }

    public static Specification<AuditEvent> notArchived() {
        return (root, query, cb) -> cb.isFalse(root.get("archived"));
    }
}

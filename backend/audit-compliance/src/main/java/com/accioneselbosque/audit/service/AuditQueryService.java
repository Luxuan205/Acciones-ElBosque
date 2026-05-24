package com.accioneselbosque.audit.service;

import com.accioneselbosque.audit.dto.AuditEventFilterDto;
import com.accioneselbosque.audit.dto.AuditEventResponseDto;
import com.accioneselbosque.audit.model.AuditEvent;
import com.accioneselbosque.audit.model.AuditEventType;
import com.accioneselbosque.audit.model.AuditResult;
import com.accioneselbosque.audit.repository.AuditEventRepository;
import com.accioneselbosque.audit.repository.AuditEventSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditQueryService {

    private final AuditEventRepository auditEventRepository;

    @Transactional(readOnly = true)
    public Page<AuditEventResponseDto> findEvents(AuditEventFilterDto filter) {
        Pageable pageable = PageRequest.of(
                filter.page(), filter.size(), Sort.by("occurredAt").descending());

        LocalDateTime from = filter.from() != null ? filter.from().atStartOfDay() : null;
        LocalDateTime to = filter.to() != null ? filter.to().atTime(23, 59, 59) : null;

        AuditEventType eventType = parseEnum(filter.eventType(), AuditEventType.class);
        AuditResult result = parseEnum(filter.result(), AuditResult.class);

        Specification<AuditEvent> spec = (root, query, cb) -> cb.conjunction();
        if (filter.investorId() != null) {
            spec = spec.and(AuditEventSpecification.hasInvestorId(filter.investorId()));
        }
        if (eventType != null) {
            spec = spec.and(AuditEventSpecification.hasEventType(eventType));
        }
        if (result != null) {
            spec = spec.and(AuditEventSpecification.hasResult(result));
        }
        if (from != null) {
            spec = spec.and(AuditEventSpecification.occurredAtOrAfter(from));
        }
        if (to != null) {
            spec = spec.and(AuditEventSpecification.occurredAtOrBefore(to));
        }
        if (!filter.includeArchived()) {
            spec = spec.and(AuditEventSpecification.notArchived());
        }

        return auditEventRepository.findAll(spec, pageable).map(AuditEventResponseDto::from);
    }

    private <E extends Enum<E>> E parseEnum(String raw, Class<E> type) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Enum.valueOf(type, raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

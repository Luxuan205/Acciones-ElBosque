package com.accioneselbosque.audit.service;

import com.accioneselbosque.audit.dto.AuditEventFilterDto;
import com.accioneselbosque.audit.dto.AuditEventResponseDto;
import com.accioneselbosque.audit.model.AuditEventType;
import com.accioneselbosque.audit.model.AuditResult;
import com.accioneselbosque.audit.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditQueryService {

    private final AuditEventRepository auditEventRepository;

    @Transactional(readOnly = true)
    public Page<AuditEventResponseDto> findEvents(AuditEventFilterDto filter) {
        Pageable pageable = PageRequest.of(filter.page(), filter.size());

        LocalDateTime from = filter.from() != null ? filter.from().atStartOfDay() : null;
        LocalDateTime to = filter.to() != null ? filter.to().atTime(23, 59, 59) : null;

        AuditEventType eventType = null;
        if (filter.eventType() != null && !filter.eventType().isBlank()) {
            try {
                eventType = AuditEventType.valueOf(filter.eventType().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // unknown event type — treat as no filter
            }
        }

        AuditResult result = null;
        if (filter.result() != null && !filter.result().isBlank()) {
            try {
                result = AuditResult.valueOf(filter.result().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // unknown result — treat as no filter
            }
        }

        return auditEventRepository.findFiltered(
                filter.investorId(),
                eventType,
                result,
                from,
                to,
                filter.includeArchived(),
                pageable
        ).map(AuditEventResponseDto::from);
    }
}

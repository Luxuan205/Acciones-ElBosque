package com.accioneselbosque.audit.service;

import com.accioneselbosque.audit.dto.AuditEventFilterDto;
import com.accioneselbosque.audit.dto.AuditEventResponseDto;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.PrintWriter;

@Service
@RequiredArgsConstructor
public class AuditExportService {

    private final AuditQueryService auditQueryService;

    public void exportToCsv(AuditEventFilterDto filter, HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"audit-log.csv\"");

        AuditEventFilterDto exportFilter = new AuditEventFilterDto(
                filter.investorId(),
                filter.eventType(),
                filter.result(),
                filter.from(),
                filter.to(),
                filter.includeArchived(),
                0,
                10000
        );

        Page<AuditEventResponseDto> events = auditQueryService.findEvents(exportFilter);

        PrintWriter writer = response.getWriter();
        writer.println("id,eventType,investorId,performedBy,referenceType,referenceId,result,ipAddress,occurredAt");

        for (AuditEventResponseDto e : events.getContent()) {
            writer.println(String.join(",",
                    nullSafe(e.id()),
                    nullSafe(e.eventType()),
                    nullSafe(e.investorId()),
                    nullSafe(e.performedBy()),
                    nullSafe(e.referenceType()),
                    nullSafe(e.referenceId()),
                    nullSafe(e.result()),
                    nullSafe(e.ipAddress()),
                    nullSafe(e.occurredAt())
            ));
        }

        writer.flush();
    }

    private String nullSafe(Object value) {
        return value != null ? value.toString() : "";
    }
}

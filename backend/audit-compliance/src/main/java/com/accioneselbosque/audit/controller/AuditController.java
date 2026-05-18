package com.accioneselbosque.audit.controller;

import com.accioneselbosque.audit.dto.AuditEventFilterDto;
import com.accioneselbosque.audit.dto.AuditEventResponseDto;
import com.accioneselbosque.audit.service.AuditExportService;
import com.accioneselbosque.audit.service.AuditQueryService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;

@RestController
@RequestMapping("/audit")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AuditController {

    private final AuditQueryService auditQueryService;
    private final AuditExportService auditExportService;

    @GetMapping("/events")
    public Page<AuditEventResponseDto> getEvents(
            @RequestParam(required = false) Long investorId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "false") boolean includeArchived,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        int clampedSize = Math.min(size, 200);
        AuditEventFilterDto filter = new AuditEventFilterDto(
                investorId, eventType, result, from, to, includeArchived, page, clampedSize);
        return auditQueryService.findEvents(filter);
    }

    @GetMapping("/events/export")
    public void exportEvents(
            @RequestParam(required = false) Long investorId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "false") boolean includeArchived,
            HttpServletResponse response) throws IOException {

        AuditEventFilterDto filter = new AuditEventFilterDto(
                investorId, eventType, result, from, to, includeArchived, 0, 10000);
        auditExportService.exportToCsv(filter, response);
    }
}

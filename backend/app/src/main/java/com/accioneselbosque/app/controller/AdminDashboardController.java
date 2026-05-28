package com.accioneselbosque.app.controller;

import com.accioneselbosque.app.dto.AdminLinkDto;
import com.accioneselbosque.app.dto.AdminTransactionDto;
import com.accioneselbosque.app.dto.DashboardPeriod;
import com.accioneselbosque.app.dto.FinancialSummaryDto;
import com.accioneselbosque.app.dto.OperationalMetricsDto;
import com.accioneselbosque.app.exception.InvalidDashboardPeriodException;
import com.accioneselbosque.app.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<OperationalMetricsDto> getOperationalMetrics() {
        return ResponseEntity.ok(dashboardService.getOperationalMetrics());
    }

    @GetMapping("/summary")
    public ResponseEntity<FinancialSummaryDto> getFinancialSummary(
            @RequestParam(defaultValue = "MONTH") String period) {
        DashboardPeriod dashboardPeriod;
        try {
            dashboardPeriod = DashboardPeriod.valueOf(period.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidDashboardPeriodException(period);
        }
        return ResponseEntity.ok(dashboardService.getFinancialSummary(dashboardPeriod));
    }

    @GetMapping("/links")
    public ResponseEntity<List<AdminLinkDto>> getAdminLinks() {
        return ResponseEntity.ok(dashboardService.getAdminLinks());
    }

    @GetMapping("/transactions")
    public ResponseEntity<Page<AdminTransactionDto>> getAdminTransactions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long investorId,
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        int clampedSize = Math.min(size, 100);
        PageRequest pageable = PageRequest.of(page, clampedSize,
                Sort.by(Sort.Direction.DESC, "executedAt"));
        return ResponseEntity.ok(
                dashboardService.getAdminTransactions(from, to, investorId, symbol, type, pageable));
    }
}

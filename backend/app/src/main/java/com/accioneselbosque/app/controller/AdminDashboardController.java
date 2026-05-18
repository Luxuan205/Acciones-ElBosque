package com.accioneselbosque.app.controller;

import com.accioneselbosque.app.dto.AdminLinkDto;
import com.accioneselbosque.app.dto.DashboardPeriod;
import com.accioneselbosque.app.dto.FinancialSummaryDto;
import com.accioneselbosque.app.dto.OperationalMetricsDto;
import com.accioneselbosque.app.exception.InvalidDashboardPeriodException;
import com.accioneselbosque.app.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}

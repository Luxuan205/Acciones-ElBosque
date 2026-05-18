package com.accioneselbosque.portfolio.controller;

import com.accioneselbosque.portfolio.dto.PortfolioPositionsResponse;
import com.accioneselbosque.portfolio.dto.PortfolioReportDto;
import com.accioneselbosque.portfolio.model.ReportPeriod;
import com.accioneselbosque.portfolio.service.CsvReportExporter;
import com.accioneselbosque.portfolio.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final CsvReportExporter csvReportExporter;

    @GetMapping("/positions")
    public ResponseEntity<PortfolioPositionsResponse> getPositions(Authentication authentication) {
        Long investorId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(portfolioService.getPositions(investorId));
    }

    @GetMapping("/report")
    public ResponseEntity<PortfolioReportDto> getReport(
            @RequestParam(defaultValue = "MONTH") ReportPeriod period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication) {

        Long investorId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(portfolioService.getReport(investorId, period, from, to));
    }

    @GetMapping("/report/export")
    public ResponseEntity<byte[]> exportReport(
            @RequestParam(defaultValue = "MONTH") ReportPeriod period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication) {

        Long investorId = Long.parseLong(authentication.getName());
        PortfolioReportDto report = portfolioService.getReport(investorId, period, from, to);
        byte[] csv = csvReportExporter.export(report);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"portfolio-report.csv\"")
                .body(csv);
    }
}

package com.accioneselbosque.portfolio.controller;

import com.accioneselbosque.portfolio.dto.BalanceSummaryResponse;
import com.accioneselbosque.portfolio.dto.FundMovementPageResponse;
import com.accioneselbosque.portfolio.service.BalanceService;
import com.accioneselbosque.portfolio.service.FundMovementService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
public class BalanceController {

    private final BalanceService balanceService;
    private final FundMovementService fundMovementService;

    @GetMapping("/balance")
    public ResponseEntity<BalanceSummaryResponse> getBalance(Authentication authentication) {
        Long investorId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(balanceService.getBalance(investorId));
    }

    @GetMapping("/movements")
    public ResponseEntity<FundMovementPageResponse> getMovements(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            Authentication authentication) {

        if (from != null && to != null && from.isAfter(to)) {
            return ResponseEntity.badRequest().build();
        }

        Long investorId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(fundMovementService.getMovements(investorId, from, to, page));
    }
}

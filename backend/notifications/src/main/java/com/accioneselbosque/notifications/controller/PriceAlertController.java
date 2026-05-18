package com.accioneselbosque.notifications.controller;

import com.accioneselbosque.notifications.dto.CreatePriceAlertRequest;
import com.accioneselbosque.notifications.dto.PriceAlertDto;
import com.accioneselbosque.notifications.service.PriceAlertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications/price-alerts")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class PriceAlertController {

    private final PriceAlertService priceAlertService;

    @PostMapping
    public ResponseEntity<PriceAlertDto> createAlert(
            @Valid @RequestBody CreatePriceAlertRequest req,
            Authentication authentication) {
        Long investorId = Long.parseLong(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(priceAlertService.createAlert(investorId, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlert(
            @PathVariable Long id,
            Authentication authentication) {
        Long investorId = Long.parseLong(authentication.getName());
        priceAlertService.deleteAlert(investorId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<PriceAlertDto>> getAlerts(Authentication authentication) {
        Long investorId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(priceAlertService.getAlerts(investorId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PriceAlertDto> updateAlert(
            @PathVariable Long id,
            @RequestBody Map<String, BigDecimal> body,
            Authentication authentication) {
        Long investorId = Long.parseLong(authentication.getName());
        BigDecimal newThreshold = body.get("threshold");
        return ResponseEntity.ok(priceAlertService.updateAlert(investorId, id, newThreshold));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<PriceAlertDto> deactivateAlert(
            @PathVariable Long id,
            Authentication authentication) {
        Long investorId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(priceAlertService.deactivateAlert(investorId, id));
    }

    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<PriceAlertDto> reactivateAlert(
            @PathVariable Long id,
            Authentication authentication) {
        Long investorId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(priceAlertService.reactivateAlert(investorId, id));
    }
}

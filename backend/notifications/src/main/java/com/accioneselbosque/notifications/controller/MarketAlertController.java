package com.accioneselbosque.notifications.controller;

import com.accioneselbosque.notifications.dto.CreateMarketAlertRequest;
import com.accioneselbosque.notifications.dto.MarketAlertSubscriptionDto;
import com.accioneselbosque.notifications.service.MarketAlertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications/market-alerts")
@RequiredArgsConstructor
public class MarketAlertController {

    private final MarketAlertService marketAlertService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MarketAlertSubscriptionDto> subscribe(
            @Valid @RequestBody CreateMarketAlertRequest req,
            Authentication authentication) {
        Long investorId = Long.parseLong(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(marketAlertService.subscribe(investorId, req));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MarketAlertSubscriptionDto>> getSubscriptions(Authentication authentication) {
        Long investorId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(marketAlertService.getSubscriptions(investorId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MarketAlertSubscriptionDto> updateSubscription(
            @PathVariable Long id,
            @Valid @RequestBody CreateMarketAlertRequest req,
            Authentication authentication) {
        Long investorId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(marketAlertService.updateSubscription(investorId, id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteSubscription(
            @PathVariable Long id,
            Authentication authentication) {
        Long investorId = Long.parseLong(authentication.getName());
        marketAlertService.deleteSubscription(investorId, id);
        return ResponseEntity.noContent().build();
    }
}

package com.accioneselbosque.auth.controller;

import com.accioneselbosque.auth.dto.ActivateSubscriptionResponse;
import com.accioneselbosque.auth.dto.SubscriptionStatusResponse;
import com.accioneselbosque.auth.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/activate")
    public ResponseEntity<ActivateSubscriptionResponse> activate(Authentication authentication) {
        Long investorId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(subscriptionService.activate(investorId));
    }

    @GetMapping("/status")
    public ResponseEntity<SubscriptionStatusResponse> getStatus(Authentication authentication) {
        Long investorId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(subscriptionService.getStatus(investorId));
    }
}

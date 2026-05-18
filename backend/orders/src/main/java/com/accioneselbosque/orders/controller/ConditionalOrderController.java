package com.accioneselbosque.orders.controller;

import com.accioneselbosque.orders.dto.ConditionalOrderResponse;
import com.accioneselbosque.orders.dto.CreateStopLossRequest;
import com.accioneselbosque.orders.dto.CreateTakeProfitRequest;
import com.accioneselbosque.orders.service.ConditionalOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders/conditional")
@RequiredArgsConstructor
public class ConditionalOrderController {

    private final ConditionalOrderService conditionalOrderService;

    @PostMapping("/stop-loss")
    public ResponseEntity<ConditionalOrderResponse> createStopLoss(
            @Valid @RequestBody CreateStopLossRequest req,
            Authentication authentication) {
        Long investorId = Long.parseLong(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(conditionalOrderService.createStopLoss(investorId, req));
    }

    @PostMapping("/take-profit")
    public ResponseEntity<ConditionalOrderResponse> createTakeProfit(
            @Valid @RequestBody CreateTakeProfitRequest req,
            Authentication authentication) {
        Long investorId = Long.parseLong(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(conditionalOrderService.createTakeProfit(investorId, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(
            @PathVariable Long id,
            Authentication authentication) {
        Long investorId = Long.parseLong(authentication.getName());
        conditionalOrderService.cancel(investorId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<ConditionalOrderResponse>> list(Authentication authentication) {
        Long investorId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(conditionalOrderService.getByInvestor(investorId));
    }
}

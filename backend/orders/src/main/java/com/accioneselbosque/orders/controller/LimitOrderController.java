package com.accioneselbosque.orders.controller;

import com.accioneselbosque.orders.dto.LimitOrderResponse;
import com.accioneselbosque.orders.dto.PlaceLimitBuyRequest;
import com.accioneselbosque.orders.dto.PlaceLimitSellRequest;
import com.accioneselbosque.orders.service.LimitOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders/limit")
@RequiredArgsConstructor
public class LimitOrderController {

    private final LimitOrderService limitOrderService;

    @PostMapping("/buy")
    public ResponseEntity<LimitOrderResponse> placeBuy(
            @Valid @RequestBody PlaceLimitBuyRequest req,
            Authentication authentication) {
        Long investorId = Long.parseLong(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(limitOrderService.placeLimitBuy(investorId, req));
    }

    @PostMapping("/sell")
    public ResponseEntity<LimitOrderResponse> placeSell(
            @Valid @RequestBody PlaceLimitSellRequest req,
            Authentication authentication) {
        Long investorId = Long.parseLong(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(limitOrderService.placeLimitSell(investorId, req));
    }
}

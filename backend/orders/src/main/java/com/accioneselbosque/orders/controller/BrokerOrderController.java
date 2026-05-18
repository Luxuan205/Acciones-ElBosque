package com.accioneselbosque.orders.controller;

import com.accioneselbosque.orders.dto.BrokerOrderRequest;
import com.accioneselbosque.orders.dto.BrokerOrderResponse;
import com.accioneselbosque.orders.service.BrokerOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders/broker")
@PreAuthorize("hasRole('BROKER')")
@RequiredArgsConstructor
public class BrokerOrderController {

    private final BrokerOrderService brokerOrderService;

    @PostMapping
    public ResponseEntity<BrokerOrderResponse> createBrokerOrder(
            @Valid @RequestBody BrokerOrderRequest request,
            Authentication authentication) {
        Long brokerId = Long.parseLong(authentication.getName());
        BrokerOrderResponse response = brokerOrderService.createBrokerOrder(brokerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

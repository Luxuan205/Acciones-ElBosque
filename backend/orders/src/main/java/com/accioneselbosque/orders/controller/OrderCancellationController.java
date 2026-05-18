package com.accioneselbosque.orders.controller;

import com.accioneselbosque.orders.dto.BulkCancellationResponse;
import com.accioneselbosque.orders.dto.CancellationResponse;
import com.accioneselbosque.orders.service.OrderCancellationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderCancellationController {

    private final OrderCancellationService orderCancellationService;

    @DeleteMapping("/{id}")
    public ResponseEntity<CancellationResponse> cancel(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            Authentication authentication) {
        Long investorId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(orderCancellationService.cancel(investorId, id, reason));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<CancellationResponse> cancelPut(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            Authentication authentication) {
        Long investorId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(orderCancellationService.cancel(investorId, id, reason));
    }

    @DeleteMapping
    public ResponseEntity<BulkCancellationResponse> cancelBulk(
            @RequestBody List<Long> orderIds,
            @RequestParam(required = false) String reason,
            Authentication authentication) {
        if (orderIds == null || orderIds.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        Long investorId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(orderCancellationService.cancelBulk(investorId, orderIds, reason));
    }
}

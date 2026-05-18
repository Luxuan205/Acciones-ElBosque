package com.accioneselbosque.orders.controller;

import com.accioneselbosque.orders.dto.OrderHistoryDto;
import com.accioneselbosque.orders.dto.OrderPreviewResponse;
import com.accioneselbosque.orders.dto.OrderResponse;
import com.accioneselbosque.orders.dto.PlaceMarketBuyRequest;
import com.accioneselbosque.orders.dto.PlaceMarketSellRequest;
import com.accioneselbosque.orders.dto.SellOrderPreviewResponse;
import com.accioneselbosque.orders.service.MarketOrderService;
import com.accioneselbosque.orders.service.MarketSellService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class MarketOrderController {

    private final MarketOrderService marketOrderService;
    private final MarketSellService marketSellService;

    @GetMapping
    public ResponseEntity<List<OrderHistoryDto>> getOrders(Authentication authentication) {
        Long investorId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(marketOrderService.getOrders(investorId));
    }

    @GetMapping("/market/buy/preview")
    public ResponseEntity<OrderPreviewResponse> preview(
            @RequestParam String symbol,
            @RequestParam int quantity,
            Authentication authentication) {
        Long investorId = authentication != null ? Long.parseLong(authentication.getName()) : null;
        return ResponseEntity.ok(marketOrderService.preview(symbol, quantity, investorId));
    }

    @PostMapping("/market/buy")
    public ResponseEntity<OrderResponse> placeBuy(
            @Valid @RequestBody PlaceMarketBuyRequest req,
            Authentication authentication) {
        Long investorId = Long.parseLong(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(marketOrderService.placeBuy(investorId, req));
    }

    @GetMapping("/market/sell/preview")
    public ResponseEntity<SellOrderPreviewResponse> previewSell(
            @RequestParam String symbol,
            @RequestParam int quantity,
            Authentication authentication) {
        Long investorId = authentication != null ? Long.parseLong(authentication.getName()) : null;
        return ResponseEntity.ok(marketSellService.preview(symbol, quantity, investorId));
    }

    @PostMapping("/market/sell")
    public ResponseEntity<OrderResponse> placeSell(
            @Valid @RequestBody PlaceMarketSellRequest req,
            Authentication authentication) {
        Long investorId = Long.parseLong(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(marketSellService.placeSell(investorId, req));
    }
}

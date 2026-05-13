package com.accioneselbosque.market_data_service.controller;

import com.accioneselbosque.market_data_service.dto.*;
import com.accioneselbosque.market_data_service.service.WatchlistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/watchlist")
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchlistService watchlistService;

    @GetMapping
    public ResponseEntity<WatchlistResponse> getWatchlist(Authentication authentication) {
        Long investorId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(watchlistService.getWatchlist(investorId));
    }

    @PostMapping("/entries")
    public ResponseEntity<AddEntryResponse> addEntry(
            Authentication authentication,
            @Valid @RequestBody WatchlistEntryRequest request) {
        Long investorId = Long.parseLong(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(watchlistService.addEntry(investorId, request.symbol()));
    }

    @DeleteMapping("/entries/{symbol}")
    public ResponseEntity<RemoveEntryResponse> removeEntry(
            Authentication authentication,
            @PathVariable String symbol) {
        Long investorId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(watchlistService.removeEntry(investorId, symbol));
    }
}

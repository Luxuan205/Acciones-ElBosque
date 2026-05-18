package com.accioneselbosque.market_data_service.controller;

import com.accioneselbosque.market_data_service.dto.IntradayDataDto;
import com.accioneselbosque.market_data_service.dto.StockDetailDto;
import com.accioneselbosque.market_data_service.service.StockSnapshotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/market/stocks")
@RequiredArgsConstructor
public class MarketController {

    private final StockSnapshotService stockSnapshotService;

    @GetMapping
    public ResponseEntity<?> listStocks(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "name_asc") String sort) {
        try {
            return ResponseEntity.ok(stockSnapshotService.listStocks(search, sort));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "INVALID_SORT", "message", e.getMessage()));
        }
    }

    @GetMapping("/{symbol}")
    public ResponseEntity<StockDetailDto> getDetail(@PathVariable String symbol) {
        return ResponseEntity.ok(stockSnapshotService.getDetail(symbol));
    }

    @GetMapping("/{symbol}/intraday")
    public ResponseEntity<IntradayDataDto> getIntraday(@PathVariable String symbol) {
        return ResponseEntity.ok(stockSnapshotService.getIntraday(symbol));
    }
}

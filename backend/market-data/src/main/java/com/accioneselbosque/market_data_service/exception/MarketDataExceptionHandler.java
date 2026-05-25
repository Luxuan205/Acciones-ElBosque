package com.accioneselbosque.market_data_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class MarketDataExceptionHandler {

    @ExceptionHandler(PremiumRequiredException.class)
    public ResponseEntity<Map<String, String>> handlePremiumRequired(PremiumRequiredException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "PREMIUM_REQUIRED", "message", ex.getMessage()));
    }

    @ExceptionHandler(SymbolAlreadyInWatchlistException.class)
    public ResponseEntity<Map<String, String>> handleDuplicate(SymbolAlreadyInWatchlistException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "SYMBOL_ALREADY_IN_WATCHLIST", "message", ex.getMessage()));
    }

    @ExceptionHandler(SymbolNotInWatchlistException.class)
    public ResponseEntity<Map<String, String>> handleNotInWatchlist(SymbolNotInWatchlistException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "SYMBOL_NOT_IN_WATCHLIST", "message", ex.getMessage()));
    }

    @ExceptionHandler(WatchlistLimitReachedException.class)
    public ResponseEntity<Map<String, String>> handleLimitReached(WatchlistLimitReachedException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("error", "WATCHLIST_LIMIT_REACHED", "message", ex.getMessage()));
    }

    @ExceptionHandler(SymbolNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleSymbolNotFound(SymbolNotFoundException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", "SYMBOL_NOT_FOUND", "message", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        var details = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList();
        return ResponseEntity.badRequest()
                .body(Map.of("error", "VALIDATION_ERROR", "details", details));
    }
}

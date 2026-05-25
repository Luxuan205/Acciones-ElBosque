package com.accioneselbosque.portfolio.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
class PortfolioExceptionHandler {

    @ExceptionHandler(InvalidPeriodException.class)
    public ResponseEntity<Map<String, String>> handle(InvalidPeriodException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", "INVALID_PERIOD", "message", ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handle(AccessDeniedException ex) {
        return ResponseEntity.status(403)
                .body(Map.of("error", "ACCESS_DENIED", "message", ex.getMessage()));
    }
}

package com.accioneselbosque.app.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(basePackages = "com.accioneselbosque.app")
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidDashboardPeriodException.class)
    public ResponseEntity<Map<String, String>> handleInvalidPeriod(InvalidDashboardPeriodException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", "INVALID_PERIOD", "message", ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "FORBIDDEN", "message", "Acceso denegado."));
    }
}

package com.accioneselbosque.notifications.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice(basePackages = "com.accioneselbosque.notifications")
public class NotificationsExceptionHandler {

    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotificationNotFound(NotificationNotFoundException ex) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(MarketAlertNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleMarketAlertNotFound(MarketAlertNotFoundException ex) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(PriceAlertNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handlePriceAlertNotFound(PriceAlertNotFoundException ex) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(AlertNotModifiableException.class)
    public ResponseEntity<Map<String, Object>> handleAlertNotModifiable(AlertNotModifiableException ex) {
        return buildError(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(PremiumRequiredException.class)
    public ResponseEntity<Map<String, Object>> handlePremiumRequired(PremiumRequiredException ex) {
        return buildError(HttpStatus.PAYMENT_REQUIRED, ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> buildError(HttpStatus status, String message) {
        Map<String, Object> body = Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message
        );
        return ResponseEntity.status(status).body(body);
    }
}

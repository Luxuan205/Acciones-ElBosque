package com.accioneselbosque.orders.exception;

import com.accioneselbosque.market_data_service.exception.SymbolNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class OrdersExceptionHandler {

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<Map<String, String>> handleInsufficientBalance(InsufficientBalanceException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("error", "INSUFFICIENT_BALANCE", "message", "Saldo insuficiente para cubrir el total de la compra."));
    }

    @ExceptionHandler(SymbolNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleSymbolNotFound(SymbolNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "SYMBOL_NOT_FOUND", "message", ex.getMessage()));
    }

    @ExceptionHandler(InsufficientTitlesException.class)
    public ResponseEntity<Map<String, String>> handleInsufficientTitles(InsufficientTitlesException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("error", "INSUFFICIENT_TITLES", "message", "Títulos insuficientes para cubrir la orden de venta."));
    }

    @ExceptionHandler(OrderNotCancellableException.class)
    public ResponseEntity<Map<String, String>> handleOrderNotCancellable(OrderNotCancellableException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("error", "CANNOT_CANCEL", "message", "La orden no puede cancelarse en su estado actual."));
    }

    @ExceptionHandler(OrderConcurrentModificationException.class)
    public ResponseEntity<Map<String, String>> handleOrderConcurrentModification(OrderConcurrentModificationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "CONCURRENT_MODIFICATION", "message", "La orden fue modificada simultáneamente. Consulte el estado actual."));
    }

    @ExceptionHandler(OrderQueueLimitException.class)
    public ResponseEntity<Map<String, String>> handleQueueLimit(OrderQueueLimitException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("error", "QUEUE_LIMIT_REACHED", "message", ex.getMessage()));
    }

    @ExceptionHandler(ClientNotAssignedException.class)
    public ResponseEntity<Map<String, String>> handleClientNotAssigned(ClientNotAssignedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "CLIENT_NOT_ASSIGNED", "message", ex.getMessage()));
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

package com.accioneselbosque.audit.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class AuditExceptionHandler {

    @ExceptionHandler(AuditAccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handle(AuditAccessDeniedException ex) {
        return ResponseEntity.status(403)
                .body(Map.of("error", "AUDIT_ACCESS_DENIED", "message", ex.getMessage()));
    }
}

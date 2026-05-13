package com.accioneselbosque.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getAllErrors().stream()
                .map(error -> {
                    if (error instanceof FieldError fieldError) {
                        return fieldError.getDefaultMessage();
                    }
                    return error.getDefaultMessage();
                })
                .toList();
        return ResponseEntity.badRequest()
                .body(Map.of("error", "Datos de registro inválidos", "details", details));
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateEmail(DuplicateEmailException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "El correo ya está registrado. ¿Desea iniciar sesión?"));
    }

    @ExceptionHandler(DuplicateDocumentException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateDocument(DuplicateDocumentException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "El número de documento ya tiene una cuenta asociada."));
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<Map<String, String>> handleTokenExpired(TokenExpiredException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", "El enlace de verificación ha expirado. Solicita uno nuevo."));
    }

    @ExceptionHandler(TokenNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleTokenNotFound(TokenNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "El enlace de verificación no es válido."));
    }

    @ExceptionHandler(TokenAlreadyUsedException.class)
    public ResponseEntity<Map<String, String>> handleTokenAlreadyUsed(TokenAlreadyUsedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "Este enlace ya fue usado. La cuenta puede estar activa; intenta iniciar sesión."));
    }

    @ExceptionHandler(AccountAlreadyActiveException.class)
    public ResponseEntity<Map<String, String>> handleAccountAlreadyActive(AccountAlreadyActiveException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", "Esta cuenta ya está verificada. Puedes iniciar sesión."));
    }

    @ExceptionHandler(ResendRateLimitException.class)
    public ResponseEntity<Map<String, String>> handleRateLimit(ResendRateLimitException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of("error", "Debes esperar al menos 2 minutos antes de solicitar otro correo de verificación."));
    }

    @ExceptionHandler(InvalidCurrentPasswordException.class)
    public ResponseEntity<Map<String, String>> handleInvalidCurrentPassword(InvalidCurrentPasswordException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "La contraseña actual es incorrecta."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", ex.getMessage()));
    }
}

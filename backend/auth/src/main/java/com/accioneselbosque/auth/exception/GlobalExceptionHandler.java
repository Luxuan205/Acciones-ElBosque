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
class AuthExceptionHandler {

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

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "INVALID_CREDENTIALS", "message", "Credenciales incorrectas o cuenta no encontrada."));
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<Map<String, String>> handleAccountLocked(AccountLockedException ex) {
        return ResponseEntity.status(423)
                .body(Map.of("error", "ACCOUNT_LOCKED", "message", "Cuenta bloqueada temporalmente. Intente de nuevo más tarde."));
    }

    @ExceptionHandler(AccountSuspendedException.class)
    public ResponseEntity<Map<String, String>> handleAccountSuspended(AccountSuspendedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "ACCOUNT_SUSPENDED", "message", "Tu cuenta ha sido suspendida."));
    }

    @ExceptionHandler(InvalidOtpException.class)
    public ResponseEntity<Map<String, String>> handleInvalidOtp(InvalidOtpException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "INVALID_OTP", "message", "Código incorrecto o expirado."));
    }

    @ExceptionHandler(MfaSessionExpiredException.class)
    public ResponseEntity<Map<String, String>> handleMfaSessionExpired(MfaSessionExpiredException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "SESSION_EXPIRED", "message", "Sesión expirada. Por favor inicie sesión nuevamente."));
    }

    @ExceptionHandler(ResendLimitException.class)
    public ResponseEntity<Map<String, String>> handleResendLimit(ResendLimitException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of("error", "RESEND_LIMIT", "message", "Demasiadas solicitudes. Espere antes de solicitar otro código."));
    }

    @ExceptionHandler(ClientNotAssignedException.class)
    public ResponseEntity<Map<String, String>> handleClientNotAssigned(ClientNotAssignedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "CLIENT_NOT_ASSIGNED", "message", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "USER_NOT_FOUND", "message", ex.getMessage()));
    }

    @ExceptionHandler(LastAdminException.class)
    public ResponseEntity<Map<String, String>> handleLastAdmin(LastAdminException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "LAST_ADMIN", "message", ex.getMessage()));
    }

    @ExceptionHandler(AdminConfirmationRequiredException.class)
    public ResponseEntity<Map<String, String>> handleAdminConfirmation(AdminConfirmationRequiredException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("error", "CONFIRMATION_REQUIRED", "message", ex.getMessage()));
    }
}

package com.accioneselbosque.auth.controller;

import com.accioneselbosque.auth.dto.RegisterRequest;
import com.accioneselbosque.auth.dto.RegisterResponse;
import com.accioneselbosque.auth.dto.ResendVerificationRequest;
import com.accioneselbosque.auth.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden");
        }
        RegisterResponse response = registrationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/verify")
    public ResponseEntity<Map<String, String>> verify(@RequestParam String token) {
        registrationService.verifyAccount(token);
        return ResponseEntity.ok(Map.of("message", "Cuenta verificada exitosamente. Ya puedes iniciar sesión."));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request) {
        registrationService.resendVerification(request.getEmail());
        return ResponseEntity.ok(Map.of(
                "message", "Si el correo está registrado y pendiente de verificación, recibirás un nuevo enlace."));
    }
}

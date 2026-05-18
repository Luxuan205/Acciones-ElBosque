package com.accioneselbosque.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MfaVerifyRequest(
        @NotBlank(message = "El token de sesión es obligatorio") String sessionToken,
        @NotBlank(message = "El código OTP es obligatorio")
        @Pattern(regexp = "\\d{6}", message = "El código OTP debe ser de 6 dígitos") String otpCode
) {}

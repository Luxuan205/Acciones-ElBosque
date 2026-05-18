package com.accioneselbosque.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record ResendOtpRequest(
        @NotBlank(message = "El token de sesión es obligatorio") String sessionToken
) {}

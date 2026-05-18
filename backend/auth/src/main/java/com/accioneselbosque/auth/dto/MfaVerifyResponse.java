package com.accioneselbosque.auth.dto;

public record MfaVerifyResponse(
        String accessToken,
        String role
) {}

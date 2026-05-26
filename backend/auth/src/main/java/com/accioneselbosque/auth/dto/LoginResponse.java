package com.accioneselbosque.auth.dto;

public record LoginResponse(
        String accessToken,
        String role
) {}

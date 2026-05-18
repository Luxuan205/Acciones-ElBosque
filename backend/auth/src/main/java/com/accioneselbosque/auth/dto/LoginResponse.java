package com.accioneselbosque.auth.dto;

public record LoginResponse(
        String sessionToken,
        String channel
) {}

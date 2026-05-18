package com.accioneselbosque.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserStatusRequest(
        String newStatus,
        @NotBlank String reason
) {}

package com.accioneselbosque.configuration.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateParameterRequest(
        @NotBlank String value,
        String reason
) {}

package com.accioneselbosque.auth.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdatePersonalDataRequest(
        @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
        String fullName,

        @Pattern(regexp = "^\\+?[0-9\\s\\-]{7,20}$", message = "Formato de teléfono inválido")
        String phone
) {}

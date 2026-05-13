package com.accioneselbosque.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "La contraseña actual es requerida")
        String currentPassword,

        @NotBlank(message = "La nueva contraseña es requerida")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String newPassword,

        @NotBlank(message = "La confirmación de contraseña es requerida")
        String confirmNewPassword
) {}

package com.accioneselbosque.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResendVerificationRequest {

    @NotBlank(message = "El correo electrónico es requerido")
    @Email(message = "El correo electrónico no tiene un formato válido")
    private String email;
}

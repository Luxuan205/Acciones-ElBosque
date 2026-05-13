package com.accioneselbosque.auth.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "El nombre completo es requerido")
    @Size(max = 150, message = "El nombre completo no puede exceder 150 caracteres")
    private String fullName;

    @NotBlank(message = "El número de documento es requerido")
    @Pattern(regexp = "^\\d{6,10}$", message = "El número de documento debe tener entre 6 y 10 dígitos numéricos")
    private String documentNumber;

    @NotBlank(message = "El correo electrónico es requerido")
    @Email(message = "El correo electrónico no tiene un formato válido")
    private String email;

    @NotBlank(message = "La contraseña es requerida")
    @Size(min = 8, max = 72, message = "La contraseña debe tener al menos 8 caracteres")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
            message = "La contraseña debe contener al menos una mayúscula, una minúscula y un número"
    )
    private String password;

    @NotBlank(message = "La confirmación de contraseña es requerida")
    private String confirmPassword;
}

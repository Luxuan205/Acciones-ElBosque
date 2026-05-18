package com.accioneselbosque.auth.dto;

import com.accioneselbosque.auth.model.InvestorRole;
import jakarta.validation.constraints.NotBlank;

public record UpdateUserRoleRequest(
        InvestorRole newRole,
        @NotBlank String reason,
        boolean confirmed
) {}

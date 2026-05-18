package com.accioneselbosque.auth.dto;

import java.util.List;

public record PagedUsersResponse(
        List<AdminUserDto> content,
        long totalElements,
        int page,
        int size
) {}

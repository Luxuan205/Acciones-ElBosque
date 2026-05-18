package com.accioneselbosque.portfolio.dto;

import java.util.List;

public record FundMovementPageResponse(
        List<FundMovementDto> content,
        long totalElements,
        int totalPages,
        int page,
        int size
) {}

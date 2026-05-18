package com.accioneselbosque.orders.dto;

import java.util.List;

public record BulkCancellationResponse(
        int totalRequested,
        int totalCancelled,
        int totalFailed,
        List<Long> cancelled,
        List<BulkCancellationFailure> failed
) {}

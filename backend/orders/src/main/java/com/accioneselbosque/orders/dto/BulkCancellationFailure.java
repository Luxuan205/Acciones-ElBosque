package com.accioneselbosque.orders.dto;

public record BulkCancellationFailure(
        Long orderId,
        String currentStatus,
        String reason
) {}

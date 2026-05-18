package com.accioneselbosque.app.dto;

public record OperationalMetricsDto(
        String marketStatus,
        long activeOrders,
        long connectedUsers,
        long todayTransactions,
        long activeSystemAlerts
) {}

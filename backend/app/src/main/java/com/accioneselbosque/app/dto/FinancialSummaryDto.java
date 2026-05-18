package com.accioneselbosque.app.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FinancialSummaryDto(
        DashboardPeriod period,
        LocalDate from,
        LocalDate to,
        BigDecimal totalTransactionVolume,
        BigDecimal estimatedCommissionRevenue,
        long newRegistrations,
        long activePremiumSubscriptions
) {}

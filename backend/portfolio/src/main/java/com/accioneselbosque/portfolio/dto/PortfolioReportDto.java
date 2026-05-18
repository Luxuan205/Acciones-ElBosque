package com.accioneselbosque.portfolio.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PortfolioReportDto(
        String period,
        LocalDate from,
        LocalDate to,
        BigDecimal totalRealizedGain,
        List<PositionDto> positions,
        List<TransactionDto> transactions
) {}

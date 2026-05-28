package com.accioneselbosque.portfolio.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PortfolioHistoryPoint(LocalDate date, BigDecimal totalValue) {}

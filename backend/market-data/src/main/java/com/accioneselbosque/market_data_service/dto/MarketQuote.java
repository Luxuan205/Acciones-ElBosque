package com.accioneselbosque.market_data_service.dto;

import java.math.BigDecimal;

public record MarketQuote(
    String symbol,
    BigDecimal price,
    BigDecimal previousClose,
    BigDecimal change,
    BigDecimal changePct,
    long volume
) {}

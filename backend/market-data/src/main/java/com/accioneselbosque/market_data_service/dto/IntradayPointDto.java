package com.accioneselbosque.market_data_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record IntradayPointDto(LocalDateTime timestamp, BigDecimal price, long volume) {}

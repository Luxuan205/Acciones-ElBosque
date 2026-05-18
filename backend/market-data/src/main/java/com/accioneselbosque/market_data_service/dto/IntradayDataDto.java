package com.accioneselbosque.market_data_service.dto;

import java.time.LocalDate;
import java.util.List;

public record IntradayDataDto(String symbol, LocalDate date, String interval, List<IntradayPointDto> points) {}

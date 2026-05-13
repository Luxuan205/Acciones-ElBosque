package com.accioneselbosque.configuration.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class MarketStatusDto {

    private String status;
    private LocalDate today;
    private String currentTime;
    private String timezone;
    private String nextClose;
    private String nextOpen;

    @JsonProperty("isHoliday")
    private boolean holiday;

    private String holidayName;
}

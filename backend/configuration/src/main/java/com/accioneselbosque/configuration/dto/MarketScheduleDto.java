package com.accioneselbosque.configuration.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MarketScheduleDto {

    @NotBlank
    private String openTime;

    @NotBlank
    private String closeTime;

    @NotEmpty
    private List<String> workingDays;

    private String timezone;

    private LocalDateTime updatedAt;
}

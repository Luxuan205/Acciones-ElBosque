package com.accioneselbosque.configuration.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MarketHolidayDto {

    private UUID id;

    @NotNull
    private LocalDate date;

    @NotBlank
    @Size(max = 200)
    private String description;

    @NotBlank
    @Pattern(regexp = "NATIONAL|REGIONAL|SPECIAL", message = "must be NATIONAL, REGIONAL, or SPECIAL")
    private String type;

    private LocalDateTime createdAt;
}

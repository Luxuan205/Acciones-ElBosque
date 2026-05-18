package com.accioneselbosque.configuration.dto;

import com.accioneselbosque.configuration.model.ParameterDataType;

public record GlobalParameterDto(
        String key,
        String value,
        ParameterDataType dataType,
        String category,
        String description,
        String minValue,
        String maxValue
) {}

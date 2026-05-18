package com.accioneselbosque.configuration.dto;

import java.util.List;
import java.util.Map;

public record GroupedParametersResponse(
        Map<String, List<GlobalParameterDto>> parameters
) {}

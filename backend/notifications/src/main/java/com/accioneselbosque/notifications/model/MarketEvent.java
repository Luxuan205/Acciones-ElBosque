package com.accioneselbosque.notifications.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MarketEvent {
    private MarketAlertType alertType;
    private String symbol;
    private String description;
}

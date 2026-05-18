package com.accioneselbosque.notifications.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PriceAlertTriggeredEvent {
    private Long alertId;
    private Long investorId;
    private String symbol;
    private BigDecimal currentPrice;
    private BigDecimal threshold;
    private PriceAlertType alertType;
}

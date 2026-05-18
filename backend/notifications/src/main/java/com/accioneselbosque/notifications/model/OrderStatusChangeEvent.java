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
public class OrderStatusChangeEvent {
    private Long investorId;
    private Long orderId;
    private NotificationEventType eventType;
    private String stockSymbol;
    private Integer quantity;
    private BigDecimal executionPrice;
    private BigDecimal commission;
    private BigDecimal totalAmount;
    private String cancellationReason;
    private String rejectionReason;
}

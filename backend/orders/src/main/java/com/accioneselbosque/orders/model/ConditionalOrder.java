package com.accioneselbosque.orders.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "conditional_order")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConditionalOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "investor_id", nullable = false)
    private Long investorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConditionalOrderType type;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "trigger_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal triggerPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConditionalOrderStatus status;

    @Column(name = "oco_partner_id")
    private Long ocoPartnerId;

    @Column(name = "triggered_order_id")
    private Long triggeredOrderId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

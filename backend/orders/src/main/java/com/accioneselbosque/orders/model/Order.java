package com.accioneselbosque.orders.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "market_order")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "investor_id", nullable = false)
    private Long investorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 20)
    private OrderType orderType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "estimated_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal estimatedPrice;

    @Column(name = "execution_price", precision = 18, scale = 2)
    private BigDecimal executionPrice;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal commission;

    @Column(name = "total_estimated", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalEstimated;

    @Column(name = "limit_price", precision = 18, scale = 2)
    private BigDecimal limitPrice;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "rejection_reason", length = 200)
    private String rejectionReason;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(name = "cancellation_reason", length = 200)
    private String cancellationReason;

    @Column(name = "broker_id")
    private Long brokerId;

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

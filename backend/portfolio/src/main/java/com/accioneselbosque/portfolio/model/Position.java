package com.accioneselbosque.portfolio.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "position",
    uniqueConstraints = @UniqueConstraint(columnNames = {"investor_id", "symbol"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "investor_id", nullable = false)
    private Long investorId;

    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    @Column(name = "current_quantity", nullable = false)
    @Builder.Default
    private int currentQuantity = 0;

    @Column(name = "avg_purchase_price", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal avgPurchasePrice = BigDecimal.ZERO;

    @Column(name = "cash_balance", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal cashBalance = BigDecimal.ZERO;

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

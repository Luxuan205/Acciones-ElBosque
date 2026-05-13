package com.accioneselbosque.market_data_service.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "stock_snapshot")
@Getter
@Setter
@NoArgsConstructor
public class StockSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 20)
    private String symbol;

    @Column(length = 100)
    private String name;

    @Column(name = "current_price", precision = 18, scale = 2)
    private BigDecimal currentPrice;

    @Column(name = "previous_close", precision = 18, scale = 2)
    private BigDecimal previousClose;

    @Column(name = "day_change", nullable = false, precision = 18, scale = 2)
    private BigDecimal dayChange = BigDecimal.ZERO;

    @Column(name = "day_change_pct", nullable = false, precision = 7, scale = 4)
    private BigDecimal dayChangePct = BigDecimal.ZERO;

    @Column(nullable = false)
    private long volume = 0L;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

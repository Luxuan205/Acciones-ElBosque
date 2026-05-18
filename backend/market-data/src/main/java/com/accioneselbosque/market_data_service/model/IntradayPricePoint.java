package com.accioneselbosque.market_data_service.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "intraday_price_point",
    uniqueConstraints = @UniqueConstraint(columnNames = {"symbol", "timestamp"})
)
@Getter
@Setter
@NoArgsConstructor
public class IntradayPricePoint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private long volume;
}

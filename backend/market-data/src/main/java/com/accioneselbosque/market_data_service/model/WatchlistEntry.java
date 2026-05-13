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
    name = "watchlist_entry",
    uniqueConstraints = @UniqueConstraint(columnNames = {"watchlist_id", "symbol"})
)
@Getter
@Setter
@NoArgsConstructor
public class WatchlistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "watchlist_id", nullable = false)
    private Watchlist watchlist;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(name = "added_at", nullable = false)
    private LocalDateTime addedAt = LocalDateTime.now();

    @Column(name = "price_at_added", nullable = false, precision = 18, scale = 2)
    private BigDecimal priceAtAdded;
}

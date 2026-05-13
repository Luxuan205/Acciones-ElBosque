package com.accioneselbosque.configuration.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "market_holiday")
@Getter
@Setter
@NoArgsConstructor
public class MarketHoliday {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private LocalDate date;

    @Column(nullable = false, length = 200)
    private String description;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

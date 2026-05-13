package com.accioneselbosque.configuration.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "market_schedule")
@Getter
@Setter
@NoArgsConstructor
public class MarketSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "open_time", nullable = false)
    private LocalTime openTime;

    @Column(name = "close_time", nullable = false)
    private LocalTime closeTime;

    @Convert(converter = WorkingDaysConverter.class)
    @Column(name = "working_days", nullable = false)
    private Set<DayOfWeek> workingDays;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "updated_by")
    private UUID updatedBy;

    @PreUpdate
    @PrePersist
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

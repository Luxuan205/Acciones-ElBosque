package com.accioneselbosque.configuration.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "parameter_change_history")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParameterChangeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parameter_key", nullable = false, length = 100, updatable = false)
    private String parameterKey;

    @Column(name = "previous_value", nullable = false, length = 500, updatable = false)
    private String previousValue;

    @Column(name = "new_value", nullable = false, length = 500, updatable = false)
    private String newValue;

    @Column(name = "changed_by", nullable = false, updatable = false)
    private Long changedBy;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    @Column(name = "reason", length = 300, updatable = false)
    private String reason;
}

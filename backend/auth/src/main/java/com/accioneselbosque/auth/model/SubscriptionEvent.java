package com.accioneselbosque.auth.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_event")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "investor_id", nullable = false)
    private Long investorId;

    @Column(name = "event_type", nullable = false, length = 30)
    private String eventType;

    @Column(name = "previous_type", nullable = false, length = 20)
    private String previousType;

    @Column(name = "new_type", nullable = false, length = 20)
    private String newType;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "triggered_by", nullable = false, length = 20)
    private String triggeredBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}

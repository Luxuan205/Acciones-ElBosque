package com.accioneselbosque.auth.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "investor_preferences")
@Getter
@Setter
@NoArgsConstructor
public class InvestorPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private java.util.UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "investor_id", nullable = false, unique = true)
    private Investor investor;

    @Enumerated(EnumType.STRING)
    @Column(name = "notif_channel", nullable = false, length = 10)
    private NotifChannel notifChannel = NotifChannel.EMAIL;

    @Enumerated(EnumType.STRING)
    @Column(name = "language", nullable = false, length = 5)
    private Language language = Language.es;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

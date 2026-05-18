package com.accioneselbosque.auth.service;

import com.accioneselbosque.auth.dto.ActivateSubscriptionResponse;
import com.accioneselbosque.auth.dto.SubscriptionStatusResponse;
import com.accioneselbosque.auth.model.Investor;
import com.accioneselbosque.auth.model.SubscriptionEvent;
import com.accioneselbosque.auth.model.SubscriptionType;
import com.accioneselbosque.auth.repository.InvestorRepository;
import com.accioneselbosque.auth.repository.SubscriptionEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Transactional
public class SubscriptionService {

    private final InvestorRepository investorRepository;
    private final SubscriptionEventRepository subscriptionEventRepository;

    @Value("${app.subscription.duration-days:30}")
    private int durationDays;

    public ActivateSubscriptionResponse activate(Long investorId) {
        Investor investor = investorRepository.findById(investorId)
                .orElseThrow(() -> new RuntimeException("Investor not found: " + investorId));

        // If already PREMIUM and not expired, return current state without creating event
        if (investor.getSubscriptionType() == SubscriptionType.PREMIUM
                && investor.getSubscriptionExpiresAt() != null
                && investor.getSubscriptionExpiresAt().isAfter(LocalDateTime.now())) {
            return new ActivateSubscriptionResponse(
                    "PREMIUM",
                    investor.getSubscriptionExpiresAt().minusDays(durationDays),
                    investor.getSubscriptionExpiresAt(),
                    "Ya tiene una suscripción PREMIUM activa."
            );
        }

        // Activate or renew
        String previousType = investor.getSubscriptionType().name();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusDays(durationDays);

        investor.setSubscriptionType(SubscriptionType.PREMIUM);
        investor.setSubscriptionExpiresAt(expiresAt);
        investorRepository.save(investor);

        // Create event
        SubscriptionEvent event = SubscriptionEvent.builder()
                .investorId(investorId)
                .eventType("ACTIVATED")
                .previousType(previousType)
                .newType("PREMIUM")
                .expiresAt(expiresAt)
                .triggeredBy("INVESTOR")
                .createdAt(now)
                .build();
        subscriptionEventRepository.save(event);

        return new ActivateSubscriptionResponse("PREMIUM", now, expiresAt, null);
    }

    @Transactional(readOnly = true)
    public SubscriptionStatusResponse getStatus(Long investorId) {
        Investor investor = investorRepository.findById(investorId)
                .orElseThrow(() -> new RuntimeException("Investor not found: " + investorId));

        boolean isPremium = investor.getSubscriptionType() == SubscriptionType.PREMIUM;
        boolean isActive = isPremium && investor.getSubscriptionExpiresAt() != null
                && investor.getSubscriptionExpiresAt().isAfter(LocalDateTime.now());

        long daysRemaining = 0;
        LocalDateTime activatedAt = null;
        LocalDateTime expiresAt = null;

        if (isPremium && investor.getSubscriptionExpiresAt() != null) {
            expiresAt = investor.getSubscriptionExpiresAt();
            if (isActive) {
                daysRemaining = ChronoUnit.DAYS.between(LocalDateTime.now(), expiresAt);
            }
            // Approximate activatedAt from most recent event
            subscriptionEventRepository.findTopByInvestorIdOrderByCreatedAtDesc(investorId)
                    .ifPresent(e -> {}); // activatedAt not critical for MVP
        }

        return new SubscriptionStatusResponse(
                investor.getSubscriptionType().name(),
                activatedAt,
                expiresAt,
                isActive,
                daysRemaining
        );
    }
}
